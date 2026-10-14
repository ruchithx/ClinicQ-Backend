package com.dispensary.dms.audit;

import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.RequestContext;
import com.dispensary.dms.security.CurrentUser;
import com.dispensary.dms.user.Role;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Writes append-only audit rows in the current transaction (spec section 10.8). {@code details}
 * must contain only non-sensitive context (never passwords, tokens, clinical text).
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final BusinessClock clock;
    private final ObjectProvider<RequestContext> requestContext;

    public AuditService(AuditEventRepository repository, BusinessClock clock,
                        ObjectProvider<RequestContext> requestContext) {
        this.repository = repository;
        this.clock = clock;
        this.requestContext = requestContext;
    }

    /** Builder entry point. */
    public Builder event(AuditAction action, EntityType entityType) {
        return new Builder(action, entityType);
    }

    public final class Builder {
        private final AuditAction action;
        private final EntityType entityType;
        private UUID actorUserId;
        private String actorUsername;
        private UUID entityId;
        private UUID patientId;
        private AuditOutcome outcome = AuditOutcome.SUCCESS;
        private final Map<String, Object> details = new LinkedHashMap<>();

        private Builder(AuditAction action, EntityType entityType) {
            this.action = action;
            this.entityType = entityType;
        }

        public Builder actor(CurrentUser user) {
            if (user != null) {
                this.actorUserId = user.id();
                this.actorUsername = user.username();
            }
            return this;
        }

        public Builder actor(UUID userId, String username) {
            this.actorUserId = userId;
            this.actorUsername = username;
            return this;
        }

        public Builder entityId(UUID id) {
            this.entityId = id;
            return this;
        }

        public Builder patientId(UUID id) {
            this.patientId = id;
            return this;
        }

        public Builder outcome(AuditOutcome o) {
            this.outcome = o;
            return this;
        }

        public Builder detail(String key, Object value) {
            if (value != null) {
                this.details.put(key, value);
            }
            return this;
        }

        public void save() {
            RequestContext ctx = requestContext.getIfAvailable();
            AuditEventEntity entity = new AuditEventEntity(
                    UUID.randomUUID(), clock.now(), actorUserId, actorUsername, action, entityType,
                    entityId, patientId, outcome, details,
                    ctx == null ? null : ctx.getRequestId(),
                    ctx == null ? null : ctx.getIpAddress(),
                    ctx == null ? null : ctx.getUserAgent());
            repository.save(entity);
        }
    }

    /** Records a 403 on a patient/consultation/prescription path (spec section 9.1). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAccessDenied(CurrentUser user, String method, String path) {
        event(AuditAction.ACCESS_DENIED, EntityType.AUDIT)
                .actor(user)
                .outcome(AuditOutcome.DENIED)
                .detail("method", method)
                .detail("path", path)
                .save();
    }

    /** Records a failed/locked/disabled login with a username snapshot but no password. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(UUID userId, String username, String reason) {
        event(AuditAction.AUTH_LOGIN_FAILED, EntityType.SESSION)
                .actor(userId, username)
                .outcome(AuditOutcome.FAILURE)
                .detail("reason", reason)
                .save();
    }

    /** Marker used by an ArchUnit/service rule: actor roles are not serialised into details. */
    static boolean isRoleKey(String key) {
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }
}
