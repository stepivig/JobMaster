package org.demo.jobmaster.service;

import java.time.LocalDateTime;

/**
 * Service to provide the current date and time.
 */
public interface DateService {

    /**
     * Get the current date and time.
     *
     * @return the current date and time
     */
    LocalDateTime getCurrentDateTime();
}
