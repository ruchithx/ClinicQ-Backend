package com.dispensary.dms.queue;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ConflictException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.NotFoundException;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.queue.dto.CallNextRequest;
import com.dispensary.dms.queue.dto.CallNextResponse;
import com.dispensary.dms.queue.dto.CloseQueueSessionRequest;
import com.dispensary.dms.queue.dto.CreateQueueSessionRequest;
import com.dispensary.dms.queue.dto.QueueSessionResponse;
import com.dispensary.dms.security.CurrentUser;
import com.dispensary.dms.user.Role;
import com.dispensary.dms.user.UserEntity;
import com.dispensary.dms.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Queue session lifecycle and the doctor's call-next button (spec section 10.7). */
@Service
public class QueueSessionService {

    private static final int MAX_FUTURE_DAYS = 30;

    private final QueueSessionRepository sessionRepository;
    private final QueueEntryRepository entryRepository;
    private final UserRepository userRepository;
    private final QueueMapper mapper;
    private final AuditService auditService;
    private final BusinessClock clock;

    public QueueSessionService(QueueSessionRepository sessionRepository,
                               QueueEntryRepository entryRepository, UserRepository userRepository,
                               QueueMapper mapper, AuditService auditService, BusinessClock clock) {
        this.sessionRepository = sessionRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<QueueSessionResponse> list(LocalDate date, QueueSessionStatus status,
                                                   int page, int size) {
        var result = sessionRepository.search(date, status,
                PageRequest.of(page, size, Sort.by("sessionDate").descending().and(Sort.by("name"))
                        .and(Sort.by("id"))));
        return PageResponse.of(result, mapper::toSessionResponse);
    }

    @Transactional(readOnly = true)
    public QueueSessionResponse get(UUID sessionId) {
        return mapper.toSessionResponse(require(sessionId));
    }

    @Transactional(readOnly = true)
    public long versionOf(UUID sessionId) {
        return require(sessionId).getVersion();
    }

    @Transactional
    public QueueSessionResponse create(CreateQueueSessionRequest request, CurrentUser actor) {
        LocalDate today = clock.today();
        if (request.sessionDate().isBefore(today) || request.sessionDate().isAfter(today.plusDays(MAX_FUTURE_DAYS))) {
            throw new BusinessRuleException(ErrorCode.INVALID_SESSION_DATE,
                    "Session date must be between today and " + MAX_FUTURE_DAYS + " days ahead");
        }
        if (request.assignedDoctorId() != null) {
            UserEntity doctor = userRepository.findById(request.assignedDoctorId())
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.REFERENCED_RESOURCE_NOT_FOUND,
                            "Assigned doctor not found"));
            if (!doctor.isEnabled() || !doctor.getRoles().contains(Role.DOCTOR)) {
                throw new BusinessRuleException(ErrorCode.ASSIGNED_USER_NOT_DOCTOR,
                        "Assigned user is not an enabled doctor");
            }
        }

        QueueSessionEntity session = new QueueSessionEntity();
        session.setId(UUID.randomUUID());
        session.setSessionDate(request.sessionDate());
        session.setName(request.name().trim());
        session.setAssignedDoctorId(request.assignedDoctorId());
        session.setStatus(QueueSessionStatus.SCHEDULED);
        session.setCreatedAt(clock.now());
        session.setUpdatedAt(clock.now());
        session.setCreatedBy(actor.id());
        sessionRepository.saveAndFlush(session);

        auditService.event(AuditAction.QUEUE_SESSION_CREATED, EntityType.QUEUE_SESSION)
                .actor(actor).entityId(session.getId())
                .detail("sessionDate", request.sessionDate().toString())
                .detail("name", session.getName()).save();
        return mapper.toSessionResponse(session);
    }

    @Transactional
    public QueueSessionResponse open(UUID sessionId, CurrentUser actor) {
        QueueSessionEntity session = lock(sessionId);
        switch (session.getStatus()) {
            case OPEN -> { /* no-op */ }
            case CLOSED -> throw new ConflictException(ErrorCode.QUEUE_SESSION_CLOSED,
                    "The session is closed");
            case SCHEDULED -> {
                if (!session.getSessionDate().equals(clock.today())) {
                    throw new BusinessRuleException(ErrorCode.INVALID_SESSION_DATE,
                            "A session can only be opened on its session date");
                }
                session.setStatus(QueueSessionStatus.OPEN);
                session.setOpenedAt(clock.now());
                session.setOpenedBy(actor.id());
                session.touch(clock.now());
                sessionRepository.save(session);
                auditService.event(AuditAction.QUEUE_SESSION_OPENED, EntityType.QUEUE_SESSION)
                        .actor(actor).entityId(session.getId()).save();
            }
            default -> throw new IllegalStateException();
        }
        return mapper.toSessionResponse(session);
    }

    @Transactional
    public QueueSessionResponse close(UUID sessionId, CloseQueueSessionRequest request, CurrentUser actor) {
        QueueSessionEntity session = lock(sessionId);
        if (session.getStatus() == QueueSessionStatus.CLOSED) {
            return mapper.toSessionResponse(session);
        }
        int waiting = 0;
        int serving = 0;
        for (var sc : entryRepository.countByStatus(sessionId)) {
            if (sc.getStatus() == QueueEntryStatus.WAITING) {
                waiting = (int) sc.getCount();
            } else if (sc.getStatus() == QueueEntryStatus.SERVING) {
                serving = (int) sc.getCount();
            }
        }
        if ((waiting > 0 || serving > 0) && !request.forceOrDefault()) {
            throw new ConflictException(ErrorCode.QUEUE_SESSION_HAS_ACTIVE_ENTRIES,
                    "The session has active entries; close with force=true to override",
                    Map.of("waitingCount", waiting, "servingCount", serving));
        }
        int cancelled = 0;
        int completed = 0;
        if (request.forceOrDefault()) {
            cancelled = entryRepository.cancelWaitingForClose(sessionId, actor.id(), clock.now());
            completed = entryRepository.completeServingForClose(sessionId, actor.id(), clock.now());
        }
        session.setStatus(QueueSessionStatus.CLOSED);
        session.setClosedAt(clock.now());
        session.setClosedBy(actor.id());
        session.touch(clock.now());
        sessionRepository.save(session);

        auditService.event(AuditAction.QUEUE_SESSION_CLOSED, EntityType.QUEUE_SESSION)
                .actor(actor).entityId(session.getId())
                .detail("force", request.forceOrDefault())
                .detail("cancelledCount", cancelled).detail("completedCount", completed)
                .detail("automatic", false).save();
        return mapper.toSessionResponse(session);
    }

    @Transactional
    public CallNextResponse callNext(UUID sessionId, CallNextRequest request, CurrentUser actor) {
        QueueSessionEntity session = lock(sessionId);
        if (session.getStatus() == QueueSessionStatus.CLOSED) {
            throw new ConflictException(ErrorCode.QUEUE_SESSION_CLOSED, "The session is closed");
        }
        if (session.getStatus() != QueueSessionStatus.OPEN) {
            throw new ConflictException(ErrorCode.QUEUE_SESSION_NOT_OPEN, "The session is not open");
        }
        if (!Objects.equals(request.expectedLastCalledNumber(), session.getLastCalledNumber())) {
            throw new ConflictException(ErrorCode.QUEUE_STATE_CHANGED,
                    "The queue changed since you last loaded it",
                    nullableContext("currentServingNumber", session.getLastCalledNumber()));
        }

        QueueEntryEntity completed = null;
        if (request.completeCurrentEntryId() != null) {
            QueueEntryEntity entry = entryRepository.findById(request.completeCurrentEntryId())
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.QUEUE_ENTRY_NOT_IN_SESSION,
                            "Entry to complete was not found"));
            if (!entry.getQueueSessionId().equals(sessionId)) {
                throw new BusinessRuleException(ErrorCode.QUEUE_ENTRY_NOT_IN_SESSION,
                        "Entry to complete belongs to another session");
            }
            if (entry.getStatus() == QueueEntryStatus.SERVING) {
                entry.setStatus(QueueEntryStatus.COMPLETED);
                entry.setCompletedAt(clock.now());
                entry.setCompletedBy(actor.id());
                entry.setUpdatedAt(clock.now());
                entryRepository.save(entry);
                completed = entry;
                auditService.event(AuditAction.QUEUE_ENTRY_COMPLETED, EntityType.QUEUE_ENTRY)
                        .actor(actor).entityId(entry.getId())
                        .detail("queueNumber", entry.getQueueNumber()).save();
            } else if (entry.getStatus() != QueueEntryStatus.COMPLETED) {
                throw new ConflictException(ErrorCode.INVALID_STATUS_TRANSITION,
                        "The entry cannot be completed from status " + entry.getStatus(),
                        Map.of("currentStatus", entry.getStatus().name()));
            }
        }

        QueueEntryEntity next = entryRepository.findNextWaiting(sessionId).orElse(null);
        QueueEntryEntity called = null;
        if (next != null) {
            next.setStatus(QueueEntryStatus.SERVING);
            next.setCalledAt(clock.now());
            next.setCalledBy(actor.id());
            next.setUpdatedAt(clock.now());
            entryRepository.save(next);
            session.setLastCalledNumber(next.getQueueNumber());
            called = next;
            auditService.event(AuditAction.QUEUE_ENTRY_CALLED, EntityType.QUEUE_ENTRY)
                    .actor(actor).entityId(next.getId())
                    .detail("queueNumber", next.getQueueNumber())
                    .detail("fromStatus", QueueEntryStatus.WAITING.name())
                    .detail("viaCallNext", true).save();
        }
        session.touch(clock.now());
        sessionRepository.save(session);

        return new CallNextResponse(
                called == null ? null : mapper.toEntryResponse(called),
                completed == null ? null : mapper.toRef(completed),
                mapper.toSessionResponse(session));
    }

    /** Nightly auto-close of a past-dated session (spec section 10.9); actor is the system (null). */
    @Transactional
    public boolean autoClose(UUID sessionId) {
        QueueSessionEntity session = lock(sessionId);
        if (session.getStatus() == QueueSessionStatus.CLOSED) {
            return false;
        }
        int cancelled = entryRepository.cancelWaitingForClose(sessionId, null, clock.now());
        int completed = entryRepository.completeServingForClose(sessionId, null, clock.now());
        session.setStatus(QueueSessionStatus.CLOSED);
        session.setClosedAt(clock.now());
        session.touch(clock.now());
        sessionRepository.save(session);
        auditService.event(AuditAction.QUEUE_SESSION_CLOSED, EntityType.QUEUE_SESSION)
                .entityId(session.getId())
                .detail("force", true).detail("cancelledCount", cancelled)
                .detail("completedCount", completed).detail("automatic", true).save();
        return true;
    }

    /** Ids of sessions dated before today that are not yet CLOSED. */
    @Transactional(readOnly = true)
    public List<UUID> findStaleSessionIds() {
        return sessionRepository.findStaleOpenSessions(clock.today())
                .stream().map(QueueSessionEntity::getId).toList();
    }

    private Map<String, Object> nullableContext(String key, Object value) {
        java.util.HashMap<String, Object> m = new java.util.HashMap<>();
        m.put(key, value);
        return m;
    }

    private QueueSessionEntity lock(UUID sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_SESSION_NOT_FOUND,
                        "Queue session not found"));
    }

    QueueSessionEntity require(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_SESSION_NOT_FOUND,
                        "Queue session not found"));
    }
}
