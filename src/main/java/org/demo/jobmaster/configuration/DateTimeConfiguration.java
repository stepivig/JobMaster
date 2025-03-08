package org.demo.jobmaster.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuration class for Date and Time related beans.
 */
@Configuration
public class DateTimeConfiguration {

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }
}
