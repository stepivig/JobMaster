package org.demo.jobmaster.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.demo.jobmaster.entity.Status;
import org.demo.jobmaster.validation.EnumValidator;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExecutionDto {
    private Long id;
    @NotNull(message = "Status cannot be null")
    @EnumValidator(enumClass = Status.class, message = "Invalid Status")
    private String status;
    private LocalDateTime creationTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> customInput;
    private String output;
}
