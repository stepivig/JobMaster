package org.demo.jobmaster.entity;

import lombok.Getter;

/**
 * Enum representing the runtime environment for a job.
 */
@Getter
public enum Runtime {
    JAVA("JAVA"),
    KOTLIN("KOTLIN");

    private final String literal;

    Runtime(String literal) {
        this.literal = literal;
    }
}
