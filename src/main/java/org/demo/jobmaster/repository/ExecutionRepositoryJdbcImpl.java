package org.demo.jobmaster.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.jobmaster.entity.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
                SELECT id, status, start_time, end_time, custom_input
                FROM executions WHERE job_id = :jobId""";

        MapSqlParameterSource params = new MapSqlParameterSource("jobId", jobId);

        return jdbcTemplate.query(sql, params, this::mapRowToExecution);
    }

    @Override
    public Optional<Execution> getExecutionById(Long executionId) {
        String sql = """
                SELECT id, status, start_time, end_time, custom_input
                FROM executions WHERE id = :executionId""";

        MapSqlParameterSource params = new MapSqlParameterSource("executionId", executionId);

        List<Execution> executions = jdbcTemplate.query(sql, params, this::mapRowToExecution);
        return executions.stream().findFirst();
    }

    @Override
    public void save(Long jobId, Execution execution) {
        String sql = """
                INSERT INTO executions (job_id, status, start_time, end_time, custom_input)
                VALUES (:jobId, :status, :startTime, :endTime, :customInput)""";

        LocalDateTime startTime = execution.getStartTime();
        LocalDateTime endTime = execution.getEndTime();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobId", jobId)
                .addValue("status", execution.getStatus().name())
                .addValue("startTime", startTime != null ? Timestamp.valueOf(startTime) : null)
                .addValue("endTime", endTime != null ? Timestamp.valueOf(endTime) : null)
                .addValue("customInput", serialize(execution.getCustomInput()));

        jdbcTemplate.update(sql, params);
    }

    @Override
    public void updateExecution(Execution execution) {
        String sql = """
                UPDATE executions SET
                    status = :status,
                    start_time = :startTime,
                    end_time = :endTime
                WHERE id = :executionId""";

        LocalDateTime startTime = execution.getStartTime();
        LocalDateTime endTime = execution.getEndTime();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("executionId", execution.getId())
                .addValue("status", execution.getStatus().name())
                .addValue("startTime", startTime != null ? Timestamp.valueOf(startTime) : null)
                .addValue("endTime", endTime != null ? Timestamp.valueOf(endTime) : null);

        jdbcTemplate.update(sql, params);
    }


    private Execution mapRowToExecution(ResultSet rs, int rowNum) throws SQLException {
        Timestamp startTime = rs.getTimestamp("start_time");
        Timestamp endTime = rs.getTimestamp("end_time");
        return Execution.builder()
                .id(rs.getLong("id"))
                .status(Execution.Status.valueOf(rs.getString("status")))
                .startTime(startTime != null ? startTime.toLocalDateTime() : null)
                .endTime(endTime != null ? endTime.toLocalDateTime() : null)
                .customInput(deserialize(rs, "custom_input"))
                .build();
    }
}
