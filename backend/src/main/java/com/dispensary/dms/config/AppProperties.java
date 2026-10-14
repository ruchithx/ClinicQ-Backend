package com.dispensary.dms.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Validated binding of the {@code app.*} configuration tree (spec section 4.1).
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String timezone,
        Jwt jwt,
        RefreshCookie refreshCookie,
        Cors cors,
        Swagger swagger,
        RateLimit rateLimit,
        Retention retention,
        BootstrapAdmin bootstrapAdmin) {

    public record Jwt(
            @NotBlank String secret,
            @NotBlank String issuer,
            Duration accessTokenTtl,
            Duration refreshTokenTtl) {
    }

    public record RefreshCookie(boolean secure) {
    }

    public record Cors(String allowedOrigins) {
    }

    public record Swagger(boolean enabled) {
    }

    public record RateLimit(int loginPerMinute, int publicPerMinute) {
    }

    public record Retention(int queuePiiDays) {
    }

    public record BootstrapAdmin(String username, String password) {
    }
}
