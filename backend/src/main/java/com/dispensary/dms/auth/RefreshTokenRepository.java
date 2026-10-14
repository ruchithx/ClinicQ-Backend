package com.dispensary.dms.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshTokenEntity t where t.tokenHash = :hash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("""
            update RefreshTokenEntity t set t.revokedAt = :now, t.revokeReason = :reason
            where t.familyId = :familyId and t.revokedAt is null
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now,
                     @Param("reason") String reason);

    @Modifying
    @Query("""
            update RefreshTokenEntity t set t.revokedAt = :now, t.revokeReason = :reason
            where t.userId = :userId and t.revokedAt is null
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now,
                         @Param("reason") String reason);

    @Modifying
    @Query("""
            update RefreshTokenEntity t set t.revokedAt = :now, t.revokeReason = :reason
            where t.userId = :userId and t.familyId <> :exceptFamily and t.revokedAt is null
            """)
    int revokeOtherFamilies(@Param("userId") UUID userId, @Param("exceptFamily") UUID exceptFamily,
                            @Param("now") Instant now, @Param("reason") String reason);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
