package org.demo.jobmaster.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.jobmaster.TestConfig;
import org.demo.jobmaster.entity.Execution;
import org.demo.jobmaster.entity.Runtime;
import org.demo.jobmaster.entity.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@Import(TestConfig.class)
@ActiveProfiles("test")
public class ExecutionRepositoryJdbcTest {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private ExecutionRepositoryJdbcImpl executionRepository;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        executionRepository = new ExecutionRepositoryJdbcImpl(jdbcTemplate, objectMapper);
        baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        saveTestJob();
    }

    @Test
    void shouldSaveExecutionWithAllFields() {
        // Given
        Map<String, String> customInput = new HashMap<>();
        customInput.put("param1", "value1");
        customInput.put("param2", "value2");

        Execution execution = Execution.builder()
                .status(Status.RUNNING)
                .creationTime(baseTime)
                .startTime(baseTime.plusMinutes(1))
                .endTime(baseTime.plusMinutes(10))
                .customInput(customInput)
                .output("Test execution output")
                .build();

        // When
        Long executionId = executionRepository.save(1L, execution);
        Optional<Execution> savedExecution = executionRepository.getExecutionById(executionId);

        // Then
        assertThat(savedExecution).isPresent();
        assertThat(savedExecution.get().getId()).isEqualTo(executionId);
        assertThat(savedExecution.get().getStatus()).isEqualTo(Status.RUNNING);
        assertThat(savedExecution.get().getCreationTime()).isEqualTo(baseTime);
        assertThat(savedExecution.get().getStartTime()).isEqualTo(baseTime.plusMinutes(1));
        assertThat(savedExecution.get().getEndTime()).isEqualTo(baseTime.plusMinutes(10));
        assertThat(savedExecution.get().getCustomInput()).containsEntry("param1", "value1");
        assertThat(savedExecution.get().getCustomInput()).containsEntry("param2", "value2");
        assertThat(savedExecution.get().getOutput()).isEqualTo("Test execution output");
    }

    @Test
    void shouldSaveExecutionWithMinimumRequiredFields() {
        // Given
        Execution execution = Execution.builder()
                .status(Status.QUEUED)
                .creationTime(baseTime)
                .build();

        // When
        Long executionId = executionRepository.save(1L, execution);
        Optional<Execution> savedExecution = executionRepository.getExecutionById(executionId);

        // Then
        assertThat(savedExecution).isPresent();
        assertThat(savedExecution.get().getId()).isEqualTo(executionId);
        assertThat(savedExecution.get().getStatus()).isEqualTo(Status.QUEUED);
        assertThat(savedExecution.get().getCreationTime()).isEqualTo(baseTime);
        assertThat(savedExecution.get().getStartTime()).isNull();
        assertThat(savedExecution.get().getEndTime()).isNull();
        assertThat(savedExecution.get().getCustomInput()).isNull();
        assertThat(savedExecution.get().getOutput()).isNull();
    }

    @Test
    void shouldGetExecutionsByJobId() {
        // Given
        Execution execution1 = Execution.builder()
                .status(Status.COMPLETED)
                .creationTime(baseTime)
                .startTime(baseTime.plusMinutes(1))
                .endTime(baseTime.plusMinutes(5))
                .build();

        Execution execution2 = Execution.builder()
                .status(Status.FAILED)
                .creationTime(baseTime.plusHours(1))
                .startTime(baseTime.plusHours(1).plusMinutes(1))
                .endTime(baseTime.plusHours(1).plusMinutes(3))
                .build();

        executionRepository.save(1L, execution1);
        executionRepository.save(1L, execution2);

        // When
        List<Execution> executions = executionRepository.getExecutionsByJobId(1L);

        // Then
        assertThat(executions).hasSize(2);
        assertThat(executions).extracting(Execution::getStatus)
                .containsExactlyInAnyOrder(Status.COMPLETED, Status.FAILED);
    }

    @Test
    void shouldUpdateStatus() {
        // Given
        Execution execution = Execution.builder()
                .status(Status.RUNNING)
                .creationTime(baseTime)
                .startTime(baseTime)
                .build();
        Long executionId = executionRepository.save(1L, execution);

        // When - update status
        LocalDateTime updateTime = baseTime.plusMinutes(5);
        int updated = executionRepository.updateStatus(executionId, Status.CANCEL_REQUESTED, updateTime);

        // Then
        assertThat(updated).isEqualTo(1);
        Optional<Execution> fetchedExecution = executionRepository.getExecutionById(executionId);
        assertThat(fetchedExecution).isPresent();
        assertThat(fetchedExecution.get().getStatus()).isEqualTo(Status.CANCEL_REQUESTED);
    }

    @Test
    void shouldMarkAsRunning() {
        // Given
        Execution execution = Execution.builder()
                .status(Status.QUEUED)
                .creationTime(baseTime)
                .build();
        Long executionId = executionRepository.save(1L, execution);

        // When
        LocalDateTime startTime = baseTime.plusMinutes(5);
        int marked = executionRepository.markAsRunning(executionId, startTime);

        // Then
        assertThat(marked).isEqualTo(1);
        Optional<Execution> fetchedExecution = executionRepository.getExecutionById(executionId);
        assertThat(fetchedExecution).isPresent();
        assertThat(fetchedExecution.get().getStatus()).isEqualTo(Status.RUNNING);
        assertThat(fetchedExecution.get().getStartTime()).isEqualTo(startTime);
    }

    @Test
    void shouldMarkAsCompleted() {
        // Given
        Execution execution = Execution.builder()
                .status(Status.RUNNING)
                .creationTime(baseTime)
                .startTime(baseTime.plusMinutes(1))
                .build();
        Long executionId = executionRepository.save(1L, execution);

        // When
        LocalDateTime endTime = baseTime.plusMinutes(10);
        String output = "Execution completed successfully with data: [result=123]";
        int marked = executionRepository.markAsCompleted(executionId, Status.COMPLETED, endTime, output);

        // Then
        assertThat(marked).isEqualTo(1);
        Optional<Execution> fetchedExecution = executionRepository.getExecutionById(executionId);
        assertThat(fetchedExecution).isPresent();
        assertThat(fetchedExecution.get().getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(fetchedExecution.get().getEndTime()).isEqualTo(endTime);
        assertThat(fetchedExecution.get().getOutput()).isEqualTo(output);
    }

    @Test
    void shouldMarkAsFailedWithOutput() {
        // Given
        Execution execution = Execution.builder()
                .status(Status.RUNNING)
                .creationTime(baseTime)
                .startTime(baseTime.plusMinutes(1))
                .build();
        Long executionId = executionRepository.save(1L, execution);

        // When
        LocalDateTime endTime = baseTime.plusMinutes(3);
        String errorOutput = "Execution failed with exception: NullPointerException at line 42";
        int marked = executionRepository.markAsCompleted(executionId, Status.FAILED, endTime, errorOutput);

        // Then
        assertThat(marked).isEqualTo(1);
        Optional<Execution> fetchedExecution = executionRepository.getExecutionById(executionId);
        assertThat(fetchedExecution).isPresent();
        assertThat(fetchedExecution.get().getStatus()).isEqualTo(Status.FAILED);
        assertThat(fetchedExecution.get().getEndTime()).isEqualTo(endTime);
        assertThat(fetchedExecution.get().getOutput()).isEqualTo(errorOutput);
    }

    @Test
    void shouldValidateCompletionStatusWithOutput() {
        // Given
        Execution execution = Execution.builder()
                .status(Status.RUNNING)
                .creationTime(baseTime)
                .startTime(baseTime.plusMinutes(1))
                .build();
        Long executionId = executionRepository.save(1L, execution);

        // When/Then
        LocalDateTime endTime = baseTime.plusMinutes(5);
        String output = "Some output";
        assertThrows(IllegalArgumentException.class, () -> {
            executionRepository.markAsCompleted(executionId, Status.RUNNING, endTime, output);
        });
    }

    @Test
    void shouldReturnEmptyOptionalForNonExistentExecution() {
        // When
        Optional<Execution> result = executionRepository.getExecutionById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    private void saveTestJob() {
        String insertJobSql = """
                INSERT INTO jobs (
                    id, name, description, memory_size, runtime, timeout, active,
                    code_type, code_raw, code_input, schedule_type, schedule_cron_expression, created_at, updated_at
                ) VALUES (
                    :id, :name, :description, :memorySize, :runtime, :timeout, :active,
                    :codeType, :codeRaw, :codeInput, :scheduleType, :scheduleCronExpression, :createdAt, :updatedAt
                )""";

        MapSqlParameterSource jobParams = new MapSqlParameterSource()
                .addValue("id", 1L)
                .addValue("name", "Test Job")
                .addValue("description", "This is a test job")
                .addValue("memorySize", 512)
                .addValue("runtime", Runtime.JAVA.getLiteral())
                .addValue("timeout", 30000)
                .addValue("active", true)
                .addValue("codeType", "RAW")
                .addValue("codeRaw", "public class Main {}")
                .addValue("codeInput", "{}")
                .addValue("scheduleType", "ONCE")
                .addValue("scheduleCronExpression", null)
                .addValue("createdAt", Timestamp.valueOf(baseTime))
                .addValue("updatedAt", Timestamp.valueOf(baseTime));

        jdbcTemplate.update(insertJobSql, jobParams);
    }
}