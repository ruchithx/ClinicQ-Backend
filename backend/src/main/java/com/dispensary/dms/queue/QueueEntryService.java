package com.dispensary.dms.queue;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ConflictException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.NotFoundException;
import com.dispensary.dms.common.idempotency.IdempotencyResult;
import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.queue.dto.CancelBookingRequest;
import com.dispensary.dms.queue.dto.CreateBookingRequest;
import com.dispensary.dms.queue.dto.QueueEntryResponse;
import com.dispensary.dms.queue.dto.SkipQueueEntryRequest;
import com.dispensary.dms.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bookings and queue-entry transitions (spec section 10.7). */
@Service
public class QueueEntryService {

    private static final int MAX_QUEUE_NUMBER = 999;

    private final QueueSessionRepository sessionRepository;
    private final QueueEntryRepository entryRepository;
    private final QueueMapper mapper;
    private final QueueEntryStateMachine stateMachine = new QueueEntryStateMachine();
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final BusinessClock clock;

    public QueueEntryService(QueueSessionRepository sessionRepository, QueueEntryRepository entryRepository,
                             QueueMapper mapper, IdempotencyService idempotencyService,
                             AuditService auditService, BusinessClock clock) {
        this.sessionRepository = sessionRepository;
        this.entryRepository = entryRepository;
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<QueueEntryResponse> listEntries(UUID sessionId, int page, int size) {
        requireSession(sessionId);
        var result = entryRepository.findByQueueSessionIdOrderByQueueNumber(sessionId,
                PageRequest.of(page, size, Sort.by("queueNumber")));
        return PageResponse.ofMapped(mapper.toEntryResponses(result.getContent()), result);
    }

    @Transactional(readOnly = true)
    public QueueEntryResponse getEntry(UUID entryId) {
        return mapper.toEntryResponse(requireEntry(entryId));
    }

    @Transactional
    public IdempotentHelper.Result<QueueEntryResponse> createBooking(UUID sessionId,
                                                                     CreateBookingRequest request,
                                                                     CurrentUser actor, String idempotencyKey) {
        String path = "/api/v1/queue-sessions/" + sessionId + "/entries";
        String hash = idempotencyService.requestHash("createBooking", path, request);
        IdempotencyResult idem = idempotencyService.begin(actor.id(), idempotencyKey, "createBooking", hash);
        if (idem.isReplay()) {
            return new IdempotentHelper.Result<>(mapper.toEntryResponse(requireEntry(idem.resourceId())),
                    idem.responseStatus());
        }

        QueueSessionEntity session = lockSession(sessionId);
        if (session.getStatus() == QueueSessionStatus.CLOSED) {
            throw new ConflictException(ErrorCode.QUEUE_SESSION_CLOSED, "The session is closed");
        }
        if (session.getLastIssuedNumber() >= MAX_QUEUE_NUMBER) {
            throw new ConflictException(ErrorCode.QUEUE_SESSION_FULL, "The session is full (999 numbers)");
        }
        if (request.contactPhone() != null && !request.allowDuplicateOrDefault()) {
            List<QueueEntryEntity> active = entryRepository.findActiveByPhone(sessionId, request.contactPhone());
            if (!active.isEmpty()) {
                throw new ConflictException(ErrorCode.DUPLICATE_BOOKING,
                        "An active booking with this phone number already exists",
                        Map.of("existingQueueNumber", active.get(0).getQueueNumber()));
            }
        }

        int number = session.getLastIssuedNumber() + 1;
        session.setLastIssuedNumber(number);
        session.touch(clock.now());
        sessionRepository.save(session);

        QueueEntryEntity entry = new QueueEntryEntity();
        entry.setId(UUID.randomUUID());
        entry.setQueueSessionId(sessionId);
        entry.setQueueNumber(number);
        entry.setStatus(QueueEntryStatus.WAITING);
        entry.setChannel(request.channel());
        entry.setPatientName(request.patientName());
        entry.setContactPhone(request.contactPhone());
        entry.setNotes(request.notes());
        entry.setBookedAt(clock.now());
        entry.setBookedBy(actor.id());
        entry.setUpdatedAt(clock.now());
        entryRepository.saveAndFlush(entry);

        auditService.event(AuditAction.BOOKING_CREATED, EntityType.QUEUE_ENTRY)
                .actor(actor).entityId(entry.getId())
                .detail("queueNumber", number).detail("channel", request.channel().name())
                .detail("duplicateOverride", request.allowDuplicateOrDefault()).save();

        idempotencyService.complete(idem.rowId(), entry.getId(), 201);
        return new IdempotentHelper.Result<>(mapper.toEntryResponse(entry), 201);
    }

    @Transactional
    public QueueEntryResponse call(UUID entryId, CurrentUser actor) {
        return transition(entryId, QueueEntryStateMachine.Action.CALL, true, actor, entry -> {
            entry.setCalledAt(clock.now());
            entry.setCalledBy(actor.id());
        }, AuditAction.QUEUE_ENTRY_CALLED, session -> {
            // lastCalledNumber updated after transition in caller
        });
    }

    @Transactional
    public QueueEntryResponse complete(UUID entryId, CurrentUser actor) {
        return transition(entryId, QueueEntryStateMachine.Action.COMPLETE, true, actor, entry -> {
            entry.setCompletedAt(clock.now());
            entry.setCompletedBy(actor.id());
        }, AuditAction.QUEUE_ENTRY_COMPLETED, null);
    }

    @Transactional
    public QueueEntryResponse skip(UUID entryId, SkipQueueEntryRequest request, CurrentUser actor) {
        return transition(entryId, QueueEntryStateMachine.Action.SKIP, true, actor, entry -> {
            entry.setSkippedAt(clock.now());
            entry.setSkippedBy(actor.id());
            entry.setSkipReason(request == null ? null : request.reason());
        }, AuditAction.QUEUE_ENTRY_SKIPPED, null);
    }

    @Transactional
    public QueueEntryResponse requeue(UUID entryId, CurrentUser actor) {
        return transition(entryId, QueueEntryStateMachine.Action.REQUEUE, true, actor, entry ->
                        entry.setRequeueCount((short) (entry.getRequeueCount() + 1)),
                AuditAction.QUEUE_ENTRY_REQUEUED, null);
    }

    @Transactional
    public QueueEntryResponse cancelBooking(UUID entryId, CancelBookingRequest request, CurrentUser actor) {
        return transition(entryId, QueueEntryStateMachine.Action.CANCEL, false, actor, entry -> {
            entry.setCancelledAt(clock.now());
            entry.setCancelledBy(actor.id());
            entry.setCancelReason(request.reason());
        }, AuditAction.BOOKING_CANCELLED, null);
    }

    private QueueEntryResponse transition(UUID entryId, QueueEntryStateMachine.Action action,
                                          boolean requiresOpen, CurrentUser actor,
                                          java.util.function.Consumer<QueueEntryEntity> mutator,
                                          AuditAction auditAction,
                                          java.util.function.Consumer<QueueSessionEntity> ignored) {
        QueueEntryEntity entry = requireEntry(entryId);
        QueueSessionEntity session = lockSession(entry.getQueueSessionId());
        entry = requireEntry(entryId); // re-read under the session lock

        if (session.getStatus() == QueueSessionStatus.CLOSED) {
            throw new ConflictException(ErrorCode.QUEUE_SESSION_CLOSED, "The session is closed");
        }
        if (requiresOpen && session.getStatus() != QueueSessionStatus.OPEN) {
            throw new ConflictException(ErrorCode.QUEUE_SESSION_NOT_OPEN, "The session is not open");
        }

        QueueEntryStatus from = entry.getStatus();
        var result = stateMachine.evaluate(from, action);
        if (!result.isValid()) {
            throw new ConflictException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot " + action + " an entry in status " + from,
                    Map.of("currentStatus", from.name()));
        }
        if (result.isChange()) {
            entry.setStatus(result.target());
            mutator.accept(entry);
            entry.setUpdatedAt(clock.now());
            entryRepository.save(entry);
            if (action == QueueEntryStateMachine.Action.CALL) {
                session.setLastCalledNumber(entry.getQueueNumber());
            }
            session.touch(clock.now());
            sessionRepository.save(session);

            var audit = auditService.event(auditAction, EntityType.QUEUE_ENTRY)
                    .actor(actor).entityId(entry.getId())
                    .detail("queueNumber", entry.getQueueNumber());
            if (action == QueueEntryStateMachine.Action.CALL) {
                audit.detail("fromStatus", from.name()).detail("viaCallNext", false);
            } else if (action == QueueEntryStateMachine.Action.REQUEUE) {
                audit.detail("requeueCount", entry.getRequeueCount());
            } else if (action == QueueEntryStateMachine.Action.CANCEL) {
                audit.detail("fromStatus", from.name());
            }
            audit.save();
        }
        return mapper.toEntryResponse(entry);
    }

    private QueueSessionEntity lockSession(UUID sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_SESSION_NOT_FOUND,
                        "Queue session not found"));
    }

    private QueueSessionEntity requireSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_SESSION_NOT_FOUND,
                        "Queue session not found"));
    }

    private QueueEntryEntity requireEntry(UUID entryId) {
        return entryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_ENTRY_NOT_FOUND,
                        "Queue entry not found"));
    }
}
