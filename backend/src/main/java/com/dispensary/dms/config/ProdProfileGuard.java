package com.dispensary.dms.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Fail-fast guard for the prod profile (spec section 4.2): refuses to start if the Flyway seed
 * location is active or the refresh cookie is not Secure.
 */
@Component
@Profile("prod")
public class ProdProfileGuard implements ApplicationRunner {

    private final Environment environment;
    private final AppProperties props;

    public ProdProfileGuard(Environment environment, AppProperties props) {
        this.environment = environment;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        String locations = environment.getProperty("spring.flyway.locations", "");
        if (locations.contains("db/seed")) {
            throw new IllegalStateException("prod profile must not include the db/seed Flyway location");
        }
        if (!props.refreshCookie().secure()) {
            throw new IllegalStateException("prod profile requires app.refresh-cookie.secure=true");
        }
    }

    // exposed for completeness / testing of the active locations string
    String activeLocations() {
        return String.join(",", Arrays.asList(
                environment.getProperty("spring.flyway.locations", "").split(",")));
    }
}
