package com.dispensary.dms.audit;

import com.dispensary.dms.audit.dto.AuditActor;
import com.dispensary.dms.audit.dto.AuditEventResponse;
import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Admin audit-log viewer (spec section 10.8). */
@Service
public class AuditQueryService {

    private static final Duration MAX_RANGE = Duration.ofDays(366);

    private final AuditEventRepository repository;
    private final AuditService auditService;
    private final BusinessClock clock;

    public AuditQueryService(AuditEventRepository repository, AuditService auditService,
                             BusinessClock clock) {
        this.repository = repository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public PageResponse<AuditEventResponse> list(AuditAction action, EntityType entityType, UUID patientId,
                                                 UUID actorUserId, Instant from, Instant to,
                                                 int page, int size, CurrentUser actor) {
        Instant effectiveTo = to != null ? to : clock.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(Duration.ofDays(30));
        if (Duration.between(effectiveFrom, effectiveTo).compareTo(MAX_RANGE) > 0) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "The requested date range exceeds 366 days");
        }

        var result = repository.search(action, entityType, patientId, actorUserId, effectiveFrom,
                effectiveTo, PageRequest.of(page, size));

        auditService.event(AuditAction.AUDIT_LOG_VIEWED, EntityType.AUDIT)
                .actor(actor)
                .detail("filters", filters(action, entityType, patientId, actorUserId))
                .save();

        return PageResponse.of(result, this::toResponse);
    }

    private String filters(AuditAction action, EntityType entityType, UUID patientId, UUID actorUserId) {
        StringBuilder sb = new StringBuilder();
        if (action != null) {
            sb.append("action=").append(action).append(' ');
        }
        if (entityType != null) {
            sb.append("entityType=").append(entityType).append(' ');
        }
        if (patientId != null) {
            sb.append("patientId=").append(patientId).append(' ');
        }
        if (actorUserId != null) {
            sb.append("actorUserId=").append(actorUserId);
        }
        return sb.toString().trim();
    }

    private AuditEventResponse toResponse(AuditEventEntity e) {
        AuditActor actor = e.getActorUserId() == null && e.getActorUsername() == null ? null
                : new AuditActor(e.getActorUserId(), e.getActorUsername());
        return new AuditEventResponse(e.getId(), e.getOccurredAt(), actor, e.getAction(),
                e.getEntityType().name(), e.getEntityId(), e.getPatientId(), e.getOutcome(),
                e.getDetails(), e.getRequestId(), e.getIpAddress());
    }
}
