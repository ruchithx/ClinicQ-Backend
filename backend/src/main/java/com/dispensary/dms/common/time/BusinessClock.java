package com.dispensary.dms.common.time;

import com.dispensary.dms.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Single source of "now" and "today" for the whole application (spec section 10.1). Wraps an
 * injectable UTC {@link Clock} and the dispensary time zone. Services must never call
 * {@code Instant.now()} / {@code LocalDate.now()} directly (enforced by ArchUnit).
 */
@Component
public class BusinessClock {

    private final Clock clock;
    private final ZoneId zone;

    public BusinessClock(Clock clock, AppProperties props) {
        this.clock = clock;
        this.zone = ZoneId.of(props.timezone());
    }

    /** Current instant (UTC). */
    public Instant now() {
        return clock.instant();
    }

    /** Today's business date in the dispensary time zone. */
    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), zone);
    }

    /** Instant of the start of today's business date, in the dispensary time zone. */
    public Instant startOfToday() {
        return today().atStartOfDay(zone).toInstant();
    }

    public ZoneId zone() {
        return zone;
    }
}
