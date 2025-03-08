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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(TestConfig.class)
@ActiveProfiles("test")
public class ExecutionRepositoryJdbcTest {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private ExecutionRepositoryJdbcImpl executionRepository;

    @BeforeEach
    void setUp() {
        executionRepository = new ExecutionRepositoryJdbcImpl(jdbcTemplate, objectMapper);
        saveTestJob();
    }

    @Test
    void shouldSaveExecution() {
        // Given
        Execution execution = Execution.builder()
                .status(Status.RUNNING)
                .startTime(LocalDateTime.now())
                .build();

        // When
        executionRepository.save(1L, execution);
        List<Execution> executions = executionRepository.getExecutionsByJobId(1L);

        // Then
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).getStatus()).isEqualTo(Status.RUNNING);
    }

    @Test
    void shouldUpdateExecution() {
        // Given
        Execution execution = Execution.builder()
                .id(1L)
                .status(Status.RUNNING)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .endTime(LocalDateTime.now())
                .build();
        executionRepository.save(1L, execution);

        // When
        Execution updatedExecution = Execution.builder()
                .id(1L)
                .status(Status.COMPLETED)
                .startTime(LocalDateTime.now().minusMinutes(20))
                .endTime(LocalDateTime.now().plusMinutes(5))
                .build();
        executionRepository.updateExecution(updatedExecution);

        // Then
        Optional<Execution> fetchedExecution = executionRepository.getExecutionById(1L);
        assertThat(fetchedExecution).isPresent();
        assertThat(fetchedExecution.get().getStatus()).isEqualTo(updatedExecution.getStatus());

        assertThat(fetchedExecution.get().getStartTime().truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(updatedExecution.getStartTime().truncatedTo(ChronoUnit.MILLIS));

        assertThat(fetchedExecution.get().getEndTime().truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(updatedExecution.getEndTime().truncatedTo(ChronoUnit.MILLIS));

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
                .addValue("createdAt", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("updatedAt", Timestamp.valueOf(LocalDateTime.now()));

        jdbcTemplate.update(insertJobSql, jobParams);
    }
}
