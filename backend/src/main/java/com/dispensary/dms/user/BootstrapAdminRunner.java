package com.dispensary.dms.user;

import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Creates an initial ADMIN account when the users table is empty and both bootstrap variables are
 * set (spec section 9.3). The password is never logged.
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;
    private final BusinessClock clock;

    public BootstrapAdminRunner(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                AppProperties props, BusinessClock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        String username = props.bootstrapAdmin() == null ? null : props.bootstrapAdmin().username();
        String password = props.bootstrapAdmin() == null ? null : props.bootstrapAdmin().password();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("Users table is empty and no bootstrap administrator is configured "
                    + "(APP_BOOTSTRAP_ADMIN_USERNAME / APP_BOOTSTRAP_ADMIN_PASSWORD). "
                    + "No one can log in until an admin is created.");
            return;
        }
        UserEntity admin = new UserEntity();
        admin.setId(UUID.randomUUID());
        admin.setUsername(username.toLowerCase());
        admin.setFullName("Bootstrap Administrator");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRoles(new LinkedHashSet<>(Set.of(Role.ADMIN)));
        admin.setEnabled(true);
        admin.setMustChangePassword(true);
        admin.setCreatedAt(clock.now());
        admin.setUpdatedAt(clock.now());
        admin.setPasswordChangedAt(clock.now());
        userRepository.save(admin);
        log.info("Bootstrap administrator {} created", admin.getUsername());
    }
}
