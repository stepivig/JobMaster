package org.demo.jobmaster.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a job entity that can be scheduled for execution.
 * This class contains details such as job configuration, execution settings,
 * scheduling details, and metadata.
 */
@Getter
@Builder
@ToString
public class Job {
    private final Long id;
    private final String name;
    private final String description;
    private final int memorySize;
    private final Runtime runtime;
    private final long timeout;
    private final boolean active;
    // Code details
    private final CodeType codeType;
    @ToString.Exclude
    private final String codeRaw;
    private final Map<String, String> codeInput;
    // Schedule details
    private final ScheduleType scheduleType;
    private final String cronExpression;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Getter
    public enum CodeType {
        RAW("RAW");

        private final String literal;

        CodeType(String literal) {
            this.literal = literal;
        }
    }

    @Getter
    public enum ScheduleType {
        ONCE("ONCE"),
        CRON("CRON");

        private final String literal;

        ScheduleType(String literal) {
            this.literal = literal;
        }
    }

}