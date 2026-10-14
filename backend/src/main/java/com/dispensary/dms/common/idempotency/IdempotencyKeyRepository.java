package com.dispensary.dms.common.idempotency;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {

    /**
     * Insert-if-absent in a single statement. Returns the number of rows inserted (0 or 1).
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (id, user_id, idempotency_key, operation, request_hash, created_at, expires_at)
            VALUES (:id, :userId, :key, :operation, :requestHash, :createdAt, :expiresAt)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("userId") UUID userId,
                       @Param("key") String key,
                       @Param("operation") String operation,
                       @Param("requestHash") String requestHash,
                       @Param("createdAt") Instant createdAt,
                       @Param("expiresAt") Instant expiresAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from IdempotencyKeyEntity k where k.userId = :userId and k.idempotencyKey = :key")
    Optional<IdempotencyKeyEntity> findForUpdate(@Param("userId") UUID userId, @Param("key") String key);

    @Modifying
    @Query("delete from IdempotencyKeyEntity k where k.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
