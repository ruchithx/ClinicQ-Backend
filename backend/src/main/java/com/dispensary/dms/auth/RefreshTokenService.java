package com.dispensary.dms.auth;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.RequestContext;
import com.dispensary.dms.config.AppProperties;
import com.dispensary.dms.user.UserEntity;
import com.dispensary.dms.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues and rotates opaque refresh tokens with reuse detection (spec section 9.2). Only the
 * SHA-256 hash of a token is ever stored; the raw value lives only in the HttpOnly cookie.
 */
@Service
public class RefreshTokenService {

    /** Grace window for parallel refresh from another tab. */
    static final Duration ROTATION_GRACE = Duration.ofSeconds(30);

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final BusinessClock clock;
    private final Duration refreshTtl;
    private final ObjectProvider<RequestContext> requestContext;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, UserRepository userRepository,
                               BusinessClock clock, AppProperties props,
                               ObjectProvider<RequestContext> requestContext,
                               AuditService auditService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.refreshTtl = props.jwt().refreshTokenTtl();
        this.requestContext = requestContext;
        this.auditService = auditService;
    }

    /** Raw refresh token plus the remaining lifetime for the cookie maxAge. */
    public record IssuedRefreshToken(String rawToken, Duration maxAge, UUID familyId) {
    }

    /** Result of a successful rotation: the user and the new raw refresh token. */
    public record Rotation(UserEntity user, IssuedRefreshToken refreshToken) {
    }

    /** Creates a brand-new token family at login. */
    @Transactional
    public IssuedRefreshToken createFamily(UUID userId) {
        Instant now = clock.now();
        Instant expiry = now.plus(refreshTtl);
        UUID familyId = UUID.randomUUID();
        String raw = randomToken();
        persist(userId, familyId, raw, now, expiry);
        return new IssuedRefreshToken(raw, Duration.between(now, expiry), familyId);
    }

    /** Rotates a presented refresh token (one transaction, spec section 9.2 step 1-5). */
    @Transactional
    public Rotation rotate(String rawCookieValue, boolean hasXRequestedWith) {
        if (!hasXRequestedWith) {
            throw new RefreshException(ErrorCode.REFRESH_TOKEN_INVALID,
                    "Missing X-Requested-With header", false);
        }
        if (rawCookieValue == null || rawCookieValue.isBlank()) {
            throw new RefreshException(ErrorCode.REFRESH_TOKEN_INVALID, "No refresh cookie", true);
        }
        Instant now = clock.now();
        String hash = sha256Hex(rawCookieValue);
        RefreshTokenEntity token = repository.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new RefreshException(ErrorCode.REFRESH_TOKEN_INVALID,
                        "Session expired", true));

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new RefreshException(ErrorCode.REFRESH_TOKEN_INVALID, "Session expired", true);
        }

        if (token.getRotatedAt() != null) {
            if (Duration.between(token.getRotatedAt(), now).compareTo(ROTATION_GRACE) < 0) {
                // Parallel refresh from another tab within the grace window: do not revoke/clear.
                throw new RefreshException(ErrorCode.REFRESH_TOKEN_ROTATED,
                        "Refresh token already rotated", false);
            }
            // Reuse of a rotated token after the grace window: possible theft -> revoke the family.
            repository.revokeFamily(token.getFamilyId(), now, "REUSE_DETECTED");
            auditService.event(AuditAction.AUTH_REFRESH_REUSE_DETECTED, EntityType.SESSION)
                    .actor(token.getUserId(), null)
                    .detail("familyId", token.getFamilyId().toString())
                    .save();
            throw new RefreshException(ErrorCode.SESSION_REVOKED, "Session revoked", true);
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new RefreshException(ErrorCode.REFRESH_TOKEN_INVALID,
                        "Session expired", true));
        if (!user.isEnabled()) {
            repository.revokeFamily(token.getFamilyId(), now, "USER_DISABLED");
            throw new RefreshException(ErrorCode.ACCOUNT_DISABLED, "Account disabled", true);
        }

        // Rotate: mark current token rotated, insert the successor in the same family/expiry.
        String raw = randomToken();
        RefreshTokenEntity successor = persist(user.getId(), token.getFamilyId(), raw, now,
                token.getExpiresAt());
        token.setRotatedAt(now);
        token.setReplacedById(successor.getId());
        repository.save(token);

        return new Rotation(user,
                new IssuedRefreshToken(raw, Duration.between(now, token.getExpiresAt()), token.getFamilyId()));
    }

    /** Logout: revoke the presented token's family (best-effort) and audit AUTH_LOGOUT. */
    @Transactional
    public void logout(String rawCookieValue) {
        if (rawCookieValue == null || rawCookieValue.isBlank()) {
            return;
        }
        repository.findByTokenHashForUpdate(sha256Hex(rawCookieValue)).ifPresent(token -> {
            repository.revokeFamily(token.getFamilyId(), clock.now(), "LOGOUT");
            auditService.event(AuditAction.AUTH_LOGOUT, EntityType.SESSION)
                    .actor(token.getUserId(), null)
                    .detail("familyId", token.getFamilyId().toString())
                    .save();
        });
    }

    /**
     * On password change: revoke every family except the one owning {@code rawCookieValue} (so the
     * caller's current device stays signed in). If the cookie is absent, revoke all families.
     */
    @Transactional
    public void revokeOtherFamiliesOnPasswordChange(UUID userId, String rawCookieValue) {
        Instant now = clock.now();
        if (rawCookieValue == null || rawCookieValue.isBlank()) {
            repository.revokeAllForUser(userId, now, "PASSWORD_CHANGED");
            return;
        }
        repository.findByTokenHashForUpdate(sha256Hex(rawCookieValue))
                .ifPresentOrElse(
                        token -> repository.revokeOtherFamilies(userId, token.getFamilyId(), now, "PASSWORD_CHANGED"),
                        () -> repository.revokeAllForUser(userId, now, "PASSWORD_CHANGED"));
    }

    @Transactional
    public void revokeFamily(UUID familyId, String reason) {
        repository.revokeFamily(familyId, clock.now(), reason);
    }

    @Transactional
    public void revokeAllForUser(UUID userId, String reason) {
        repository.revokeAllForUser(userId, clock.now(), reason);
    }

    @Transactional
    public void revokeOtherFamilies(UUID userId, UUID exceptFamily, String reason) {
        repository.revokeOtherFamilies(userId, exceptFamily, clock.now(), reason);
    }

    private RefreshTokenEntity persist(UUID userId, UUID familyId, String raw, Instant issuedAt,
                                       Instant expiresAt) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setFamilyId(familyId);
        entity.setTokenHash(sha256Hex(raw));
        entity.setIssuedAt(issuedAt);
        entity.setExpiresAt(expiresAt);
        RequestContext ctx = requestContext.getIfAvailable();
        if (ctx != null) {
            entity.setCreatedIp(ctx.getIpAddress());
            String ua = ctx.getUserAgent();
            entity.setUserAgent(ua != null && ua.length() > 255 ? ua.substring(0, 255) : ua);
        }
        return repository.save(entity);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
