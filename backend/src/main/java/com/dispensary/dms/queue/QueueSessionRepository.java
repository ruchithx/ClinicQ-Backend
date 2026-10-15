package com.dispensary.dms.queue;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueSessionRepository extends JpaRepository<QueueSessionEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from QueueSessionEntity s where s.id = :id")
    Optional<QueueSessionEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select s from QueueSessionEntity s
            where (cast(:date as LocalDate) is null or s.sessionDate = :date)
              and (:status is null or s.status = :status)
            """)
    Page<QueueSessionEntity> search(@Param("date") LocalDate date,
                                    @Param("status") QueueSessionStatus status, Pageable pageable);

    List<QueueSessionEntity> findBySessionDateOrderByName(LocalDate sessionDate);

    @Query("select s from QueueSessionEntity s where s.sessionDate < :today and s.status <> 'CLOSED'")
    List<QueueSessionEntity> findStaleOpenSessions(@Param("today") LocalDate today);
}
