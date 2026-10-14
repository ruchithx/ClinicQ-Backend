package com.dispensary.dms.auth;

import com.dispensary.dms.auth.dto.ChangePasswordRequest;
import com.dispensary.dms.auth.dto.CurrentUser;
import com.dispensary.dms.auth.dto.LoginRequest;
import com.dispensary.dms.auth.dto.TokenResponse;
import com.dispensary.dms.config.AppProperties;
import com.dispensary.dms.security.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Authentication endpoints (spec section 8.1). Public endpoints are permitted by the security
 * chain; {@code /me} and {@code /change-password} require authentication.
 */
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private static final String REFRESH_COOKIE = "dms_refresh";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService,
                          TokenService tokenService, AppProperties props) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.tokenService = tokenService;
        this.cookieSecure = props.refreshCookie().secure();
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken().rawToken(),
                        result.refreshToken().maxAge()).toString())
                .cacheControl(CacheControl.noStore())
                .body(tokenResponse(result.accessToken(), result.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshCookie,
            @RequestHeader(name = "X-Requested-With", required = false) String xRequestedWith,
            HttpServletResponse response) {
        boolean hasXrw = "XMLHttpRequest".equals(xRequestedWith);
        try {
            RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshCookie, hasXrw);
            TokenService.AccessToken accessToken = tokenService.issueAccessToken(rotation.user());
            CurrentUser user = authService.currentUser(rotation.user().getId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(rotation.refreshToken().rawToken(),
                            rotation.refreshToken().maxAge()).toString())
                    .cacheControl(CacheControl.noStore())
                    .body(tokenResponse(accessToken, user));
        } catch (RefreshException ex) {
            if (ex.shouldClearCookie()) {
                response.addHeader(HttpHeaders.SET_COOKIE, clearingCookie().toString());
            }
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshCookie) {
        refreshTokenService.logout(refreshCookie);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearingCookie().toString())
                .build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUser> getCurrentUser(
            com.dispensary.dms.security.CurrentUser principal) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(authService.currentUser(principal.id()));
    }

    @PostMapping(path = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            com.dispensary.dms.security.CurrentUser principal,
            @Valid @RequestBody ChangePasswordRequest request,
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshCookie) {
        authService.changePassword(principal.id(), request, refreshCookie);
        return ResponseEntity.noContent().build();
    }

    private TokenResponse tokenResponse(TokenService.AccessToken accessToken, CurrentUser user) {
        return TokenResponse.bearer(accessToken.value(), accessToken.expiresInSeconds(), user);
    }

    private ResponseCookie refreshCookie(String raw, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, raw)
                .httpOnly(true).secure(cookieSecure).sameSite("Strict").path(COOKIE_PATH)
                .maxAge(maxAge).build();
    }

    private ResponseCookie clearingCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(cookieSecure).sameSite("Strict").path(COOKIE_PATH)
                .maxAge(0).build();
    }
}
