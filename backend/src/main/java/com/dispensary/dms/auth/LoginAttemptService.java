package com.dispensary.dms.auth;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.user.UserEntity;
import com.dispensary.dms.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Updates the failed-login counter in its own transaction so the change survives the 401 response
 * (spec section 9.3). On the 5th consecutive failure the account is locked for 15 minutes.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final BusinessClock clock;
    private final AuditService auditService;

    public LoginAttemptService(UserRepository userRepository, BusinessClock clock,
                               AuditService auditService) {
        this.userRepository = userRepository;
        this.clock = clock;
        this.auditService = auditService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(UUID userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        int next = user.getFailedLoginCount() + 1;
        Instant now = clock.now();
        if (next >= MAX_FAILURES) {
            Instant lockedUntil = now.plus(LOCK_DURATION);
            user.setFailedLoginCount(0);
            user.setLockedUntil(lockedUntil);
            user.setUpdatedAt(now);
            userRepository.save(user);
            auditService.event(AuditAction.AUTH_ACCOUNT_LOCKED, EntityType.USER)
                    .actor(user.getId(), user.getUsername())
                    .entityId(user.getId())
                    .detail("lockedUntil", lockedUntil.toString())
                    .save();
        } else {
            user.setFailedLoginCount(next);
            user.setUpdatedAt(now);
            userRepository.save(user);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerSuccess(UUID userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        user.setFailedLoginCount(0);
        user.setLastLoginAt(clock.now());
        user.setUpdatedAt(clock.now());
        userRepository.save(user);
    }
}
