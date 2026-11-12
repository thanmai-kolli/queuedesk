package com.queuedesk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * @Configuration + @Bean: a way to give Spring an object it can't find by component scanning.
 *
 * The Clock says what "now" is. The report and ages use it instead of calling now() directly,
 * so a test can replace it with a clock two days in the future and see overdue tickets.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
