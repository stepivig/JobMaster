package org.demo.jobmaster.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@ToString
@Getter
@Builder
public class Execution {
    private final Long id;
    private final Status status;
    private final LocalDateTime creationTime;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<String> customInput;
    private final String output;
}
