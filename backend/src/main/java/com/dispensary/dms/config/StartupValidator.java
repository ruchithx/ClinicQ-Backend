package com.dispensary.dms.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Base64;

/**
 * Validates security-critical configuration at startup (spec section 4.1): the JWT secret must
 * Base64-decode to at least 32 bytes, and a bootstrap admin password, if set, must be >= 12 chars.
 */
@Component
public class StartupValidator {

    private final AppProperties props;

    public StartupValidator(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void validate() {
        String secret = props.jwt().secret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("APP_JWT_SECRET is required");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("APP_JWT_SECRET must be valid Base64", ex);
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must decode to at least 32 bytes (got "
                    + decoded.length + ")");
        }
        String bootstrapPassword = props.bootstrapAdmin() == null ? null : props.bootstrapAdmin().password();
        if (StringUtils.hasText(bootstrapPassword) && bootstrapPassword.length() < 12) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
        }
    }
}
