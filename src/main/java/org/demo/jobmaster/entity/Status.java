package org.demo.jobmaster.entity;

import lombok.Getter;

/**
 * Enum representing the status of a job execution.
 */
@Getter
public enum Status {
    QUEUED("QUEUED"),
    RUNNING("RUNNING"),
    CANCEL_REQUESTED("CANCEL_REQUESTED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    TERMINATED("TERMINATED"),
    CANCELLED("CANCELLED");

    private final String literal;

    Status(String literal) {
        this.literal = literal;
    }
}
