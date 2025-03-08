package org.demo.jobmaster.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.jobmaster.entity.Execution;
import org.demo.jobmaster.entity.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of the {@link ExecutionRepository}
 */
@Repository
public class ExecutionRepositoryJdbcImpl extends SerializableObjectAbstractRepository implements ExecutionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    protected ExecutionRepositoryJdbcImpl(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Execution> getExecutionsByJobId(Long jobId) {
        String sql = """
                SELECT id, status, creation_time, start_time, end_time, custom_input, output
                FROM executions WHERE job_id = :jobId""";

        MapSqlParameterSource params = new MapSqlParameterSource("jobId", jobId);

        return jdbcTemplate.query(sql, params, this::mapRowToExecution);
    }

    @Override
    public Optional<Execution> getExecutionById(Long executionId) {
        String sql = """
                SELECT id, status, creation_time, start_time, end_time, custom_input, output
                FROM executions WHERE id = :executionId""";

        MapSqlParameterSource params = new MapSqlParameterSource("executionId", executionId);

        List<Execution> executions = jdbcTemplate.query(sql, params, this::mapRowToExecution);
        return executions.stream().findFirst();
    }

    @Override
    public Long save(Long jobId, Execution execution) {
        String sql = """
                INSERT INTO executions (job_id, status, creation_time, start_time, end_time, custom_input, output)
                VALUES (:jobId, :status, :creationTime, :startTime, :endTime, :customInput, :output)""";

        LocalDateTime creationTime = execution.getCreationTime();
        LocalDateTime startTime = execution.getStartTime();
        LocalDateTime endTime = execution.getEndTime();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobId", jobId)
                .addValue("status", execution.getStatus().getLiteral())
                .addValue("creationTime", creationTime != null ? Timestamp.valueOf(creationTime) : null)
                .addValue("startTime", startTime != null ? Timestamp.valueOf(startTime) : null)
                .addValue("endTime", endTime != null ? Timestamp.valueOf(endTime) : null)
                .addValue("customInput", serialize(execution.getCustomInput()))
                .addValue("output", execution.getOutput());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);

        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    @Override
    public int updateStatus(Long executionId, Status status, LocalDateTime updateTime) {
        String sql = "UPDATE executions SET status = :status WHERE id = :executionId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("executionId", executionId)
                .addValue("status", status.getLiteral());

        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int markAsRunning(Long executionId, LocalDateTime startTime) {
        String sql = """
                UPDATE executions SET
                    status = :status,
                    start_time = :startTime
                WHERE id = :executionId""";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("executionId", executionId)
                .addValue("status", Status.RUNNING.getLiteral())
                .addValue("startTime", Timestamp.valueOf(startTime));

        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int markAsCompletedWithOutput(Long executionId,
                                         Status status,
                                         LocalDateTime endTime,
                                         String output) {
        if (status != Status.COMPLETED && status != Status.FAILED) {
            throw new IllegalArgumentException("Status must be COMPLETED or FAILED");
        }

        String sql = """
                UPDATE executions SET
                    status = :status,
                    end_time = :endTime,
                    output = :output
                WHERE id = :executionId""";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("executionId", executionId)
                .addValue("status", status.getLiteral())
                .addValue("endTime", Timestamp.valueOf(endTime))
                .addValue("output", output);

        return jdbcTemplate.update(sql, params);
    }

    private Execution mapRowToExecution(ResultSet rs, int rowNum) throws SQLException {
        Timestamp creationTime = rs.getTimestamp("creation_time");
        Timestamp startTime = rs.getTimestamp("start_time");
        Timestamp endTime = rs.getTimestamp("end_time");
        return Execution.builder()
                .id(rs.getLong("id"))
                .status(Status.valueOf(rs.getString("status")))
                .creationTime(creationTime != null ? creationTime.toLocalDateTime() : null)
                .startTime(startTime != null ? startTime.toLocalDateTime() : null)
                .endTime(endTime != null ? endTime.toLocalDateTime() : null)
                .customInput(deserialize(rs, "custom_input"))
                .output(rs.getString("output"))
                .build();
    }
}