package com.dispensary.dms.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    @Query("""
            select e from AuditEventEntity e
            where (:action is null or e.action = :action)
              and (:entityType is null or e.entityType = :entityType)
              and (:patientId is null or e.patientId = :patientId)
              and (:actorUserId is null or e.actorUserId = :actorUserId)
              and e.occurredAt >= :from and e.occurredAt < :to
            order by e.occurredAt desc, e.id desc
            """)
    Page<AuditEventEntity> search(@Param("action") AuditAction action,
                                  @Param("entityType") EntityType entityType,
                                  @Param("patientId") UUID patientId,
                                  @Param("actorUserId") UUID actorUserId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to,
                                  Pageable pageable);
}
