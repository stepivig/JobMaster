package org.demo.jobmaster.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.jobmaster.TestConfig;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Job.CodeType;
import org.demo.jobmaster.entity.Job.ScheduleType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(TestConfig.class)
@ActiveProfiles("test")
class JobRepositoryJdbcTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private JobRepositoryJdbcImpl jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository = new JobRepositoryJdbcImpl(jdbcTemplate, objectMapper);
    }

    @AfterEach
    void tearDown() {
        List<Job> all = jobRepository.findAll();
        for (Job job : all) {
            jobRepository.delete(job.getId());
        }
    }

    private Job.JobBuilder getTestJobTemplate() {
        return Job.builder()
                .name("Test Job")
                .description("This is a test job")
                .memorySize(512)
                .runtime(Job.Runtime.JAVA)
                .timeout(60)
                .active(true)
                .codeType(CodeType.RAW)
                .codeRaw("System.out.println(\"Hello, World!\");")
                .scheduleType(ScheduleType.ONCE)
                .cronExpression(null);
    }

    @Test
    @DisplayName("Should return empty Optional when job is not found")
    void test01() {
        // Given
        Long nonExistentId = 999L;

        // When
        Optional<Job> retrievedJob = jobRepository.findById(nonExistentId);

        // Then
        assertThat(retrievedJob).isEmpty();
    }

    @Test
    @DisplayName("Should not update non-existent job")
    void test02() {
        // Given
        Long nonExistentId = 999L;
        Job jobToUpdate = getTestJobTemplate().id(nonExistentId).name("Non-existent Job").build();

        // When
        int updatedCount = jobRepository.update(nonExistentId, jobToUpdate);

        // Then
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not delete non-existent job")
    void test03() {
        // Given
        Long nonExistentId = 999L;

        // When
        int deletedCount = jobRepository.delete(nonExistentId);

        // Then
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return jobs in correct insertion order")
    void test04() {
        // Given
        Job job1 = getTestJobTemplate().name("First Job").build();
        Job job2 = getTestJobTemplate().name("Second Job").build();
        Job job3 = getTestJobTemplate().name("Third Job").build();

        jobRepository.save(job1);
        jobRepository.save(job2);
        jobRepository.save(job3);

        // When
        List<Job> jobs = jobRepository.findAll();

        // Then
        assertThat(jobs).hasSize(3);
        assertThat(jobs.get(0).getName()).isEqualTo("First Job");
        assertThat(jobs.get(1).getName()).isEqualTo("Second Job");
        assertThat(jobs.get(2).getName()).isEqualTo("Third Job");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Test
    @DisplayName("Should correctly update timestamps on update")
    void test05() {
        // Given
        Job job = getTestJobTemplate().build();
        Long jobId = jobRepository.save(job);
        Job savedJob = jobRepository.findById(jobId).orElseThrow();

        // Ensure initial timestamps
        assertThat(savedJob).isNotNull();
        assertThat(savedJob.getCreatedAt()).isNotNull();
        assertThat(savedJob.getUpdatedAt()).isNotNull();

        Job jobToUpdate = getTestJobTemplate()
                .id(jobId)
                .name("Updated Job Name")
                .build();

        // When
        int updatedCount = jobRepository.update(jobId, jobToUpdate);
        Job updatedJob = jobRepository.findById(jobId).orElseThrow();

        // Then
        assertThat(updatedCount).isEqualTo(1);
        assertThat(updatedJob.getName()).isEqualTo("Updated Job Name");
        assertThat(updatedJob.getCreatedAt()).isEqualTo(savedJob.getCreatedAt()); // CreatedAt should not change
        assertThat(updatedJob.getUpdatedAt()).isAfter(savedJob.getUpdatedAt()); // UpdatedAt should be later
    }

    @Test
    @DisplayName("Should correctly insert and fetch job with input data")
    void test06() {
        // Given
        Map<String, String> inputData = Map.of("key1", "value1", "key2", "value2");
        Job job = getTestJobTemplate()
                .codeInput(inputData)
                .build();

        // When
        Long jobId = jobRepository.save(job);
        Optional<Job> retrievedJobOpt = jobRepository.findById(jobId);

        // Then
        assertThat(retrievedJobOpt).isPresent();
        Job retrievedJob = retrievedJobOpt.get();
        assertThat(retrievedJob.getCodeInput()).isNotNull();
        assertThat(retrievedJob.getCodeInput()).containsExactlyInAnyOrderEntriesOf(inputData);
    }
}
