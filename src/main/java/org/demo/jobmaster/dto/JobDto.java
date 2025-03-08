package org.demo.jobmaster.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;
import org.demo.jobmaster.validation.EnumValidator;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for {@link org.demo.jobmaster.entity.Job}
 */
@Getter
@Setter
@Builder
public class JobDto {
    private Long id;
    @NotBlank(message = "Job name cannot be empty")
    private String name;
    private String description;
    @Min(value = 1, message = "Memory size must be at least 1 MB")
    private int memorySize;
    @NotBlank(message = "Runtime cannot be empty")
    @EnumValidator(enumClass = Runtime.class, message = "Invalid ScheduleType")
    private String runtime;
    @Min(value = 1, message = "Timeout must be at least 1 second")
    private long timeout;
    private boolean active;
    @NotNull(message = "CodeType cannot be null")
    @EnumValidator(enumClass = Job.CodeType.class, message = "Invalid CodeType")
    private String codeType;
    @NotBlank(message = "CodeRaw cannot be empty")
    private String codeRaw;
    private Map<String, String> codeInput;
    @NotNull(message = "ScheduleType cannot be null")
    @EnumValidator(enumClass = Job.ScheduleType.class, message = "Invalid ScheduleType")
    private String scheduleType;
    private String cronExpression;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}