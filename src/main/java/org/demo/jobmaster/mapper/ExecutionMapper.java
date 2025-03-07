package org.demo.jobmaster.mapper;

import org.demo.jobmaster.dto.ExecutionDto;
import org.demo.jobmaster.entity.Execution;

/**
 * Mapper class to convert between {@link Execution} and {@link ExecutionDto}
 */
public final class ExecutionMapper {
    private ExecutionMapper() {
    }

    /**
     * Converts an Execution entity to an ExecutionDto.
     *
     * @param execution the Execution entity to convert
     * @return the converted ExecutionDto, or null if the input is null
     */
    public static ExecutionDto toDto(Execution execution) {
        if (execution == null) {
            return null;
        }
        return ExecutionDto.builder()
                .id(execution.getId())
                .status(execution.getStatus().name())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .customInput(execution.getCustomInput())
                .build();
    }

    /**
     * Converts an ExecutionDto to an Execution entity.
     *
     * @param executionDto the ExecutionDto to convert
     * @return the converted Execution entity, or null if the input is null
     */
    public static Execution toEntity(ExecutionDto executionDto) {
        if (executionDto == null) {
            return null;
        }
        return Execution.builder()
                .id(executionDto.getId())
                .status(Execution.Status.valueOf(executionDto.getStatus()))
                .startTime(executionDto.getStartTime())
                .endTime(executionDto.getEndTime())
                .customInput(executionDto.getCustomInput())
                .build();
    }
}
