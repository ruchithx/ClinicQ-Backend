package com.dispensary.dms.auth;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.auth.dto.ChangePasswordRequest;
import com.dispensary.dms.auth.dto.CurrentUser;
import com.dispensary.dms.auth.dto.LoginRequest;
import com.dispensary.dms.common.error.ApiException;
import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.security.TokenService;
import com.dispensary.dms.user.UserEntity;
import com.dispensary.dms.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

/**
 * Authentication operations: login (with lockout), the current-user view and password change
 * (spec sections 9.2 / 9.3).
 */
@Service
public class AuthService {

    /** Precomputed BCrypt-12 hash used to equalise timing for unknown usernames. */
    private final String dummyHash;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordPolicy passwordPolicy;
    private final AuditService auditService;
    private final BusinessClock clock;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, RefreshTokenService refreshTokenService,
                       LoginAttemptService loginAttemptService, PasswordPolicy passwordPolicy,
                       AuditService auditService, BusinessClock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.passwordPolicy = passwordPolicy;
        this.auditService = auditService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equalisation-dummy-password");
    }

    /** Result of a successful login: the issued tokens and the current-user view. */
    public record LoginResult(TokenService.AccessToken accessToken,
                              RefreshTokenService.IssuedRefreshToken refreshToken,
                              CurrentUser user) {
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        String username = request.username().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash); // equalise timing
            auditService.recordLoginFailure(null, username, "INVALID_CREDENTIALS");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.registerFailure(user.getId());
            auditService.recordLoginFailure(user.getId(), username, "INVALID_CREDENTIALS");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
        }

        // Password correct: only now may we reveal locked/disabled status.
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(clock.now())) {
            auditService.recordLoginFailure(user.getId(), username, "ACCOUNT_LOCKED");
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED, "Account temporarily locked",
                    Map.of("lockedUntil", user.getLockedUntil().toString()), java.util.List.of());
        }
        if (!user.isEnabled()) {
            auditService.recordLoginFailure(user.getId(), username, "ACCOUNT_DISABLED");
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED, "Account disabled");
        }

        loginAttemptService.registerSuccess(user.getId());
        TokenService.AccessToken accessToken = tokenService.issueAccessToken(user);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.createFamily(user.getId());
        auditService.event(AuditAction.AUTH_LOGIN_SUCCEEDED, EntityType.SESSION)
                .actor(user.getId(), user.getUsername())
                .detail("familyId", refreshToken.familyId().toString())
                .save();
        return new LoginResult(accessToken, refreshToken, toCurrentUser(user));
    }

    @Transactional(readOnly = true)
    public CurrentUser currentUser(java.util.UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "Session no longer valid"));
        return toCurrentUser(user);
    }

    /**
     * Changes the caller's password, clears the forced-change flag and revokes other refresh
     * families (keeping the family of {@code currentRefreshCookie} if present).
     */
    @Transactional
    public void changePassword(java.util.UUID userId, ChangePasswordRequest request,
                               String currentRefreshCookie) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "Session no longer valid"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException(ErrorCode.CURRENT_PASSWORD_INCORRECT,
                    "The current password is incorrect");
        }
        passwordPolicy.validate(request.newPassword(), user.getUsername(), user.getPasswordHash());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(clock.now());
        user.setUpdatedAt(clock.now());
        userRepository.save(user);

        refreshTokenService.revokeOtherFamiliesOnPasswordChange(userId, currentRefreshCookie);

        auditService.event(AuditAction.AUTH_PASSWORD_CHANGED, EntityType.USER)
                .actor(user.getId(), user.getUsername())
                .entityId(user.getId())
                .save();
    }

    private CurrentUser toCurrentUser(UserEntity user) {
        return new CurrentUser(user.getId(), user.getUsername(), user.getFullName(), user.getRoles(),
                user.isMustChangePassword(), user.getProfessionalRegistrationNumber());
    }
}
