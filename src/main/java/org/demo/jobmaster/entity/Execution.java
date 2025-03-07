package org.demo.jobmaster.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class Execution {
    private final Long id;
    private final Status status;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Map<String, String> customInput;

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
}
