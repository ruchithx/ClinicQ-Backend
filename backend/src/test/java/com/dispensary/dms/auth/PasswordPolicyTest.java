package com.dispensary.dms.auth;

import com.dispensary.dms.common.error.ApiException;
import com.dispensary.dms.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** BT-004 (policy part): password policy enforcement. */
class PasswordPolicyTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private PasswordPolicy policy;

    @BeforeEach
    void setUp() throws Exception {
        policy = new PasswordPolicy(encoder);
        policy.loadDenyList();
    }

    @Test
    void acceptsAStrongPassword() {
        policy.validate("Zq7!kLm92wText", "dr.perera", null);
    }

    @Test
    void rejectsTooShort() {
        assertThatThrownBy(() -> policy.validate("short1!", "dr.perera", null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsPasswordContainingUsername() {
        var ex = catchThrowableOfType(
                () -> policy.validate("dr.pereraSecret99", "dr.perera", null), ApiException.class);
        assertThat(ex.code()).isEqualTo(ErrorCode.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    void rejectsCommonPassword() {
        var ex = catchThrowableOfType(
                () -> policy.validate("password1234", "someuser", null), ApiException.class);
        assertThat(ex.code()).isEqualTo(ErrorCode.PASSWORD_POLICY_VIOLATION);
    }

    @Test
    void rejectsReuseOfCurrentPassword() {
        String hash = encoder.encode("CurrentPass99X");
        var ex = catchThrowableOfType(
                () -> policy.validate("CurrentPass99X", "someuser", hash), ApiException.class);
        assertThat(ex.code()).isEqualTo(ErrorCode.PASSWORD_POLICY_VIOLATION);
    }
}
