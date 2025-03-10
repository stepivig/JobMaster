package org.demo.jobmaster.repository;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of the {@link JobRepository}
 */
@Repository
public class JobRepositoryJdbcImpl extends SerializableObjectAbstractRepository implements JobRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public JobRepositoryJdbcImpl(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Job> JOB_ROW_MAPPER = (rs, rowNum) -> Job.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .memorySize(rs.getInt("memory_size"))
            .runtime(Runtime.valueOf(rs.getString("runtime")))
            .timeout(rs.getLong("timeout"))
            .active(rs.getBoolean("active"))
            .codeType(Job.CodeType.valueOf(rs.getString("code_type")))
            .codeRaw(rs.getString("code_raw"))
            .codeInput(deserialize(rs, "code_input"))
            .scheduleType(Job.ScheduleType.valueOf(rs.getString("schedule_type")))
            .cronExpression(rs.getString("schedule_cron_expression"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .updatedAt(rs.getObject("updated_at", LocalDateTime.class))
            .build();

    @Override
    public List<Job> findAll() {
        String sql = """
                    SELECT id, name, description, memory_size, runtime, timeout, active, code_type, code_raw, code_input,
                           schedule_type, schedule_cron_expression, created_at, updated_at
                    FROM jobs ORDER BY id
                """;
        return jdbcTemplate.query(sql, JOB_ROW_MAPPER);
    }

    @Override
    public List<Job> findAllActiveCron() {
        String sql = """
                    SELECT id, name, description, memory_size, runtime, timeout, active, code_type, code_raw, code_input,
                           schedule_type, schedule_cron_expression, created_at, updated_at
                    FROM jobs
                    WHERE active = true AND schedule_type = 'CRON'
                    ORDER BY id
                """;
        return jdbcTemplate.query(sql, JOB_ROW_MAPPER);
    }

    @Override
    public Optional<Job> findById(Long id) {
        String sql = """
                    SELECT id, name, description, memory_size, runtime, timeout, active, code_type, code_raw, code_input,
                           schedule_type, schedule_cron_expression, created_at, updated_at
                    FROM jobs
                    WHERE id = :id
                """;
        SqlParameterSource params = new MapSqlParameterSource("id", id);
        List<Job> jobs = jdbcTemplate.query(sql, params, JOB_ROW_MAPPER);
        return jobs.stream().findFirst();
    }

    @Override
    public Long save(Job job) {
        String sql = """
                    INSERT INTO jobs (name, description, memory_size,
                                      runtime, timeout, active,
                                      code_type, code_raw, code_input,
                                      schedule_type, schedule_cron_expression)
                    VALUES (:name, :description, :memorySize,
                            :runtime, :timeout, :active,
                            :codeType, :codeRaw, :codeInput,
                            :scheduleType, :cronExpression)
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, buildSource(job), keyHolder, new String[]{"id"});
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    @Override
    public int update(Long jobId, Job job) {
        String sql = """
                    UPDATE jobs
                    SET name = :name,
                        description = :description,
                        memory_size = :memorySize,
                        runtime = :runtime,
                        timeout = :timeout,
                        active = :active,
                        code_type = :codeType,
                        code_raw = :codeRaw,
                        code_input = :codeInput,
                        schedule_type = :scheduleType,
                        schedule_cron_expression = :cronExpression
                    WHERE id = :id
                """;
        MapSqlParameterSource paramSource = buildSource(job);
        paramSource.addValue("id", jobId);

        return jdbcTemplate.update(sql, paramSource);
    }

    @Override
    public int delete(Long id) {
        String sql = "DELETE FROM jobs WHERE id = :id";
        SqlParameterSource params = new MapSqlParameterSource("id", id);
        return jdbcTemplate.update(sql, params);
    }

    private MapSqlParameterSource buildSource(Job job) {
        return new MapSqlParameterSource()
                .addValue("name", job.getName())
                .addValue("description", job.getDescription())
                .addValue("memorySize", job.getMemorySize())
                .addValue("runtime", job.getRuntime().getLiteral())
                .addValue("timeout", job.getTimeout())
                .addValue("active", job.isActive())
                .addValue("codeType", job.getCodeType().getLiteral())
                .addValue("codeRaw", job.getCodeRaw())
                .addValue("codeInput", serialize(job.getCodeInput()))
                .addValue("scheduleType", job.getScheduleType().getLiteral())
                .addValue("cronExpression", job.getCronExpression());
    }
}
