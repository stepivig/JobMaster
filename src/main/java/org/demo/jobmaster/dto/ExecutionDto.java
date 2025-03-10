package org.demo.jobmaster.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.demo.jobmaster.entity.Status;
import org.demo.jobmaster.validation.EnumValidator;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Getter
@Setter
public class ExecutionDto {
    private Long id;
    @NotNull(message = "Status cannot be null")
    @EnumValidator(enumClass = Status.class, message = "Invalid Status")
    private String status;
    private final LocalDateTime creationTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, String> customInput;
    private final String output;
}
