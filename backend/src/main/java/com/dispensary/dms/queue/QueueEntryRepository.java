package com.dispensary.dms.queue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueEntryRepository extends JpaRepository<QueueEntryEntity, UUID> {

    Page<QueueEntryEntity> findByQueueSessionIdOrderByQueueNumber(UUID queueSessionId, Pageable pageable);

    /** Lowest WAITING entry in a session (example query Q8). */
    @Query("""
            select e from QueueEntryEntity e
            where e.queueSessionId = :sessionId and e.status = com.dispensary.dms.queue.QueueEntryStatus.WAITING
            order by e.queueNumber
            """)
    List<QueueEntryEntity> findWaiting(@Param("sessionId") UUID sessionId, Pageable pageable);

    default Optional<QueueEntryEntity> findNextWaiting(UUID sessionId) {
        List<QueueEntryEntity> rows = findWaiting(sessionId, org.springframework.data.domain.PageRequest.of(0, 1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Query("""
            select e.status as status, count(e) as count
            from QueueEntryEntity e where e.queueSessionId = :sessionId group by e.status
            """)
    List<StatusCount> countByStatus(@Param("sessionId") UUID sessionId);

    interface StatusCount {
        QueueEntryStatus getStatus();

        long getCount();
    }

    @Query("""
            select e from QueueEntryEntity e
            where e.queueSessionId = :sessionId and e.status = com.dispensary.dms.queue.QueueEntryStatus.SERVING
            order by e.calledAt desc
            """)
    List<QueueEntryEntity> findServing(@Param("sessionId") UUID sessionId);

    @Query("""
            select e from QueueEntryEntity e
            where e.queueSessionId = :sessionId and e.contactPhone = :phone
              and e.status in (com.dispensary.dms.queue.QueueEntryStatus.WAITING,
                               com.dispensary.dms.queue.QueueEntryStatus.SERVING)
            order by e.queueNumber
            """)
    List<QueueEntryEntity> findActiveByPhone(@Param("sessionId") UUID sessionId, @Param("phone") String phone);

    @Query("""
            select e.queueNumber from QueueEntryEntity e
            where e.queueSessionId = :sessionId and e.status = com.dispensary.dms.queue.QueueEntryStatus.WAITING
            order by e.queueNumber
            """)
    List<Integer> findWaitingNumbers(@Param("sessionId") UUID sessionId, Pageable pageable);

    @Query("""
            select e.queueNumber from QueueEntryEntity e
            where e.queueSessionId = :sessionId and e.status = com.dispensary.dms.queue.QueueEntryStatus.SERVING
            order by e.queueNumber
            """)
    List<Integer> findServingNumbers(@Param("sessionId") UUID sessionId);

    @Modifying
    @Query("""
            update QueueEntryEntity e set e.status = com.dispensary.dms.queue.QueueEntryStatus.CANCELLED,
                e.cancelledAt = :now, e.cancelledBy = :userId, e.cancelReason = 'Session closed',
                e.updatedAt = :now
            where e.queueSessionId = :sessionId and e.status = com.dispensary.dms.queue.QueueEntryStatus.WAITING
            """)
    int cancelWaitingForClose(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId,
                              @Param("now") Instant now);

    @Modifying
    @Query("""
            update QueueEntryEntity e set e.status = com.dispensary.dms.queue.QueueEntryStatus.COMPLETED,
                e.completedAt = :now, e.completedBy = :userId, e.updatedAt = :now
            where e.queueSessionId = :sessionId and e.status = com.dispensary.dms.queue.QueueEntryStatus.SERVING
            """)
    int completeServingForClose(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId,
                                @Param("now") Instant now);

    /** Retention: anonymise old entries (example query Q12). */
    @Modifying
    @Query("""
            update QueueEntryEntity e set e.patientName = null, e.contactPhone = null, e.notes = null,
                e.anonymizedAt = :now
            where e.bookedAt < :cutoff and e.anonymizedAt is null
            """)
    int anonymizeOlderThan(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
