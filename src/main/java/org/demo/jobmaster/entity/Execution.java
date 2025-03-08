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
}
