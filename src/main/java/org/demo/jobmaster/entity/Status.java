package org.demo.jobmaster.entity;

import lombok.Getter;

/**
 * Enum representing the status of a job execution.
 */
@Getter
public enum Status {
    SCHEDULED("SCHEDULED"),
    RUNNING("RUNNING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    CANCEL_REQUESTED("CANCEL_REQUESTED");

    private final String literal;

    Status(String literal) {
        this.literal = literal;
    }
}
