package org.demo.jobmaster.mapper;

import org.demo.jobmaster.dto.JobDto;
import org.demo.jobmaster.entity.Job;

/**
 * Mapper class to convert between {@link Job} and {@link JobDto}
 */
public final class JobMapper {

    private JobMapper() {
    }

    /**
     * Converts a Job entity to an ExecutionDto.
     *
     * @param entity the Job entity to convert
     * @return the converted JobDto, or null if the input is null
     */
    public static JobDto toDto(Job entity) {
        if (entity == null) {
            return null;
        }

        return JobDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .memorySize(entity.getMemorySize())
                .runtime(entity.getRuntime().getLiteral())
                .timeout(entity.getTimeout())
                .active(entity.isActive())
                .codeType(entity.getCodeType().getLiteral())
                .codeRaw(entity.getCodeRaw())
                .codeInput(entity.getCodeInput())
                .scheduleType(entity.getScheduleType().getLiteral())
                .cronExpression(entity.getCronExpression())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts a JobDto to an Job.
     *
     * @param dto the JobDto to convert
     * @return the converted Job, or null if the input is null
     */
    public static Job toEntity(JobDto dto) {
        if (dto == null) {
            return null;
        }

        return Job.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .memorySize(dto.getMemorySize())
                .runtime(dto.getRuntime() != null ? Job.Runtime.valueOf(dto.getRuntime()) : null)
                .timeout(dto.getTimeout())
                .active(dto.isActive())
                .codeType(dto.getCodeType() != null ? Job.CodeType.valueOf(dto.getCodeType()) : null)
                .codeRaw(dto.getCodeRaw())
                .codeInput(dto.getCodeInput())
                .scheduleType(dto.getScheduleType() != null ? Job.ScheduleType.valueOf(dto.getScheduleType()) : null)
                .cronExpression(dto.getCronExpression())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
