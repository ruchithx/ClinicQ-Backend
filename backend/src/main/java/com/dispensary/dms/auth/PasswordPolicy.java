package com.dispensary.dms.auth;

import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.FieldError;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Enforces the password policy (spec section 9.3): length 12-128; not equal to the current
 * password; must not contain the username; not on the common-password deny-list.
 */
@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;

    private final PasswordEncoder passwordEncoder;
    private final Set<String> denyList = new HashSet<>();

    public PasswordPolicy(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void loadDenyList() throws IOException {
        ClassPathResource resource = new ClassPathResource("security/common-passwords.txt");
        if (!resource.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim().toLowerCase(Locale.ROOT);
                if (!trimmed.isEmpty()) {
                    denyList.add(trimmed);
                }
            }
        }
    }

    /**
     * Validates a new password. {@code currentPasswordHash} may be {@code null} (new accounts).
     */
    public void validate(String newPassword, String username, String currentPasswordHash) {
        String violation = findViolation(newPassword, username, currentPasswordHash);
        if (violation != null) {
            throw new BusinessRuleException(ErrorCode.PASSWORD_POLICY_VIOLATION, violation,
                    List.of(new FieldError("newPassword", "PasswordPolicy", violation)));
        }
    }

    private String findViolation(String newPassword, String username, String currentPasswordHash) {
        if (newPassword == null || newPassword.length() < MIN_LENGTH || newPassword.length() > MAX_LENGTH) {
            return "Password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters";
        }
        if (username != null && newPassword.toLowerCase(Locale.ROOT)
                .contains(username.toLowerCase(Locale.ROOT))) {
            return "Password must not contain the username";
        }
        if (denyList.contains(newPassword.toLowerCase(Locale.ROOT))) {
            return "Password is too common";
        }
        if (currentPasswordHash != null && passwordEncoder.matches(newPassword, currentPasswordHash)) {
            return "New password must be different from the current password";
        }
        return null;
    }
}
