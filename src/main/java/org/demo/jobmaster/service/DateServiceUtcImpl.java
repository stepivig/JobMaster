package org.demo.jobmaster.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Implementation of DateService that provides the current date and time in UTC.
 */
@Service
public class DateServiceUtcImpl implements DateService {

    private final Clock clock;

    @Autowired
    public DateServiceUtcImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(clock);
    }
}
