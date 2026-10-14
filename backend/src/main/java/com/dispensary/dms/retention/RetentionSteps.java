package com.dispensary.dms.retention;

import com.dispensary.dms.auth.RefreshTokenRepository;
import com.dispensary.dms.common.idempotency.IdempotencyKeyRepository;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.config.AppProperties;
import com.dispensary.dms.queue.QueueEntryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/** Individually transactional retention steps (spec section 10.9). */
@Component
public class RetentionSteps {

    private final QueueEntryRepository queueEntryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final BusinessClock clock;
    private final int queuePiiDays;

    public RetentionSteps(QueueEntryRepository queueEntryRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          IdempotencyKeyRepository idempotencyKeyRepository, BusinessClock clock,
                          AppProperties props) {
        this.queueEntryRepository = queueEntryRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.clock = clock;
        this.queuePiiDays = props.retention().queuePiiDays();
    }

    @Transactional
    public int anonymiseQueuePii() {
        var cutoff = clock.now().minus(Duration.ofDays(queuePiiDays));
        return queueEntryRepository.anonymizeOlderThan(cutoff, clock.now());
    }

    @Transactional
    public int deleteExpiredRefreshTokens() {
        return refreshTokenRepository.deleteExpiredBefore(clock.now().minus(7, ChronoUnit.DAYS));
    }

    @Transactional
    public int deleteExpiredIdempotencyKeys() {
        return idempotencyKeyRepository.deleteExpired(clock.now());
    }
}
