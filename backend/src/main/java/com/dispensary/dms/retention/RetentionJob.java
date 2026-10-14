package com.dispensary.dms.retention;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.queue.QueueSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Nightly housekeeping (spec section 10.9): auto-close past sessions, anonymise old queue PII,
 * purge expired refresh tokens and idempotency keys. Each step is idempotent and independently
 * transactional, so concurrent runs across replicas are safe.
 */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final QueueSessionService queueSessionService;
    private final RetentionSteps steps;
    private final AuditService auditService;

    public RetentionJob(QueueSessionService queueSessionService, RetentionSteps steps,
                        AuditService auditService) {
        this.queueSessionService = queueSessionService;
        this.steps = steps;
        this.auditService = auditService;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "${app.timezone}")
    public void run() {
        int autoClosed = 0;
        for (UUID sessionId : queueSessionService.findStaleSessionIds()) {
            try {
                if (queueSessionService.autoClose(sessionId)) {
                    autoClosed++;
                }
            } catch (RuntimeException ex) {
                log.warn("Auto-close failed for session {}", sessionId, ex);
            }
        }

        int anonymised = steps.anonymiseQueuePii();
        int deletedTokens = steps.deleteExpiredRefreshTokens();
        int deletedKeys = steps.deleteExpiredIdempotencyKeys();

        auditService.event(AuditAction.RETENTION_JOB_EXECUTED, EntityType.AUDIT)
                .detail("autoClosedSessions", autoClosed)
                .detail("anonymizedQueueEntries", anonymised)
                .detail("deletedRefreshTokens", deletedTokens)
                .detail("deletedIdempotencyKeys", deletedKeys)
                .save();
        log.info("Retention job completed: autoClosed={}, anonymised={}, tokens={}, keys={}",
                autoClosed, anonymised, deletedTokens, deletedKeys);
    }
}
