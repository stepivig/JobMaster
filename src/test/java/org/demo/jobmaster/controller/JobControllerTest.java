package org.demo.jobmaster.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.jobmaster.dto.JobDto;
import org.demo.jobmaster.entity.Execution;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;
import org.demo.jobmaster.entity.Status;
import org.demo.jobmaster.service.ExecutionService;
import org.demo.jobmaster.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private ExecutionService executionService;

    @Autowired
    private ObjectMapper objectMapper;

    private JobDto.JobDtoBuilder getTemplate() {
        return JobDto.builder()
                .name("Test Job")
                .description("This is a test job")
                .memorySize(512)
                .runtime(Runtime.KOTLIN.getLiteral())
                .timeout(60)
                .active(true)
                .codeType("RAW")
                .codeRaw("System.out.println(\"Hello, World!\");")
                .scheduleType("ONCE")
                .cronExpression("0 0 12 * * ?");
    }

    @Test
    @DisplayName("Should create a new job successfully and return job ID")
    void test_createJob() throws Exception {
        // Given
        JobDto jobDto = getTemplate().build();
        when(jobService.createJob(any(Job.class))).thenReturn(1L);

        // When & Then
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobDto)))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.content().string("1"));
    }

    @Test
    @DisplayName("Should return all jobs with full field verification")
    void test_getAllJobs() throws Exception {
        // Given
        Job job1 = Job.builder()
                .id(1L)
                .name("Job 1")
                .description("Desc 1")
                .memorySize(256)
                .runtime(Runtime.JAVA)
                .timeout(30)
                .active(false)
                .codeType(Job.CodeType.RAW)
                .codeRaw("print('Hello')")
                .scheduleType(Job.ScheduleType.ONCE)
                .cronExpression("0 15 10 * * ?")
                .build();

        Job job2 = Job.builder()
                .id(2L)
                .name("Job 2")
                .description("Desc 2")
                .memorySize(1024)
                .runtime(Runtime.KOTLIN)
                .timeout(120)
                .active(true)
                .codeType(Job.CodeType.RAW)
                .codeRaw("fmt.Println(\"Hello\")")
                .scheduleType(Job.ScheduleType.CRON)
                .cronExpression("0 0 0 * * ?")
                .build();

        when(jobService.getAllJobs()).thenReturn(List.of(job1, job2));

        // When & Then
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))

                // First job
                .andExpect(jsonPath("$[0].name").value("Job 1"))
                .andExpect(jsonPath("$[0].description").value("Desc 1"))
                .andExpect(jsonPath("$[0].memorySize").value(256))
                .andExpect(jsonPath("$[0].runtime").value("JAVA"))
                .andExpect(jsonPath("$[0].timeout").value(30))
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[0].codeType").value("RAW"))
                .andExpect(jsonPath("$[0].codeRaw").value("print('Hello')"))
                .andExpect(jsonPath("$[0].scheduleType").value("ONCE"))
                .andExpect(jsonPath("$[0].cronExpression").value("0 15 10 * * ?"))

                // Second job
                .andExpect(jsonPath("$[1].name").value("Job 2"))
                .andExpect(jsonPath("$[1].description").value("Desc 2"))
                .andExpect(jsonPath("$[1].memorySize").value(1024))
                .andExpect(jsonPath("$[1].runtime").value("KOTLIN"))
                .andExpect(jsonPath("$[1].timeout").value(120))
                .andExpect(jsonPath("$[1].active").value(true))
                .andExpect(jsonPath("$[1].codeType").value("RAW"))
                .andExpect(jsonPath("$[1].codeRaw").value("fmt.Println(\"Hello\")"))
                .andExpect(jsonPath("$[1].scheduleType").value("CRON"))
                .andExpect(jsonPath("$[1].cronExpression").value("0 0 0 * * ?"));
    }

    @Test
    @DisplayName("Should return job by ID with all fields verified")
    void test_getJobById_Found() throws Exception {
        // Given
        Job job = Job.builder()
                .id(1L)
                .name("Job 1")
                .description("Desc 1")
                .memorySize(256)
                .runtime(Runtime.KOTLIN)
                .timeout(30)
                .active(false)
                .codeType(Job.CodeType.RAW)
                .codeRaw("print('Hello')")
                .scheduleType(Job.ScheduleType.ONCE)
                .cronExpression("0 15 10 * * ?")
                .build();
        when(jobService.getJobById(anyLong())).thenReturn(Optional.of(job));

        // When & Then
        mockMvc.perform(get("/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(job.getName()))
                .andExpect(jsonPath("$.description").value(job.getDescription()))
                .andExpect(jsonPath("$.memorySize").value(job.getMemorySize()))
                .andExpect(jsonPath("$.runtime").value(job.getRuntime().getLiteral()))
                .andExpect(jsonPath("$.timeout").value(job.getTimeout()))
                .andExpect(jsonPath("$.active").value(job.isActive()))
                .andExpect(jsonPath("$.codeType").value(job.getCodeType().getLiteral()))
                .andExpect(jsonPath("$.codeRaw").value(job.getCodeRaw()))
                .andExpect(jsonPath("$.scheduleType").value(job.getScheduleType().getLiteral()))
                .andExpect(jsonPath("$.cronExpression").value(job.getCronExpression()));
    }

    @Test
    @DisplayName("Should return 400 when creating a job with missing required fields")
    void test_createJob_MissingFields() throws Exception {
        // Given: Invalid job DTO with missing required fields
        JobDto invalidJobDto = JobDto.builder().name("").runtime(null).build();

        // When & Then
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidJobDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists()) // Field-level validation error for 'name'
                .andExpect(jsonPath("$.runtime").exists()); // Field-level validation error for 'runtime'
    }

    @Test
    @DisplayName("Should return 400 when creating a job with invalid enum values")
    void test_createJob_InvalidEnumValues() throws Exception {
        // Given: Invalid `codeType` and `scheduleType`
        JobDto invalidJobDto = getTemplate()
                .codeType("INVALID_TYPE")
                .scheduleType("INVALID_SCHEDULE")
                .build();

        // When & Then
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidJobDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codeType").exists()) // Field-level validation error for 'codeType'
                .andExpect(jsonPath("$.scheduleType").exists()); // Field-level validation error for 'scheduleType'
    }

    @Test
    @DisplayName("Should return 400 when updating a job with invalid data")
    void test_updateJob_InvalidData() throws Exception {
        // Given: Invalid `timeout` value (must be > 0)
        JobDto invalidJobDto = getTemplate()
                .timeout(0)
                .build();
        when(jobService.updateJob(anyLong(), any(Job.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidJobDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timeout").exists()); // Field-level validation error for 'timeout'
    }

    @Test
    @DisplayName("Should return 400 when getting a job with an invalid ID format")
    void test_getJobById_InvalidId() throws Exception {
        // Given: Invalid job ID (non-numeric value)

        // When & Then
        mockMvc.perform(get("/jobs/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when deleting a job with an invalid ID format")
    void test_deleteJob_InvalidId() throws Exception {
        // Given: Invalid job ID (non-numeric value)

        // When & Then
        mockMvc.perform(delete("/jobs/invalid"))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("Should return all executions for a given job with full field verification")
    void test_getExecutionsByJob() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesAgo = now.minusMinutes(10);

        Execution execution1 = Execution.builder()
                .id(1L)
                .status(Status.RUNNING)
                .creationTime(tenMinutesAgo)
                .startTime(now.minusMinutes(5))
                .customInput(Map.of("key1", "value1"))
                .output(null) // Still running, no output yet
                .build();

        Execution execution2 = Execution.builder()
                .id(2L)
                .status(Status.COMPLETED)
                .creationTime(tenMinutesAgo.minusMinutes(5))
                .startTime(tenMinutesAgo)
                .endTime(now)
                .customInput(Map.of("key2", "value2"))
                .output("Success result")
                .build();

        when(executionService.getExecutionsByJobId(anyLong())).thenReturn(List.of(execution1, execution2));

        // When & Then
        mockMvc.perform(get("/jobs/1/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))

                // First execution (RUNNING)
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("RUNNING"))
                .andExpect(jsonPath("$[0].creationTime").exists())
                .andExpect(jsonPath("$[0].startTime").exists())
                .andExpect(jsonPath("$[0].endTime").doesNotExist()) // Should be null
                .andExpect(jsonPath("$[0].customInput.key1").value("value1"))
                .andExpect(jsonPath("$[0].output").doesNotExist()) // Should be null

                // Second execution (COMPLETED)
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$[1].creationTime").exists())
                .andExpect(jsonPath("$[1].startTime").exists())
                .andExpect(jsonPath("$[1].endTime").exists())
                .andExpect(jsonPath("$[1].customInput.key2").value("value2"))
                .andExpect(jsonPath("$[1].output").value("Success result"));
    }

    @Test
    @DisplayName("Should return execution details when execution exists with all fields verified")
    void test_getExecution_Found() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Execution execution = Execution.builder()
                .id(1L)
                .status(Status.COMPLETED)
                .creationTime(now.minusMinutes(15))
                .startTime(now.minusMinutes(10))
                .endTime(now.minusMinutes(5))
                .customInput(Map.of("key1", "value1", "key2", "value2"))
                .output("Execution output with logs")
                .build();

        when(executionService.getExecutionById(anyLong(), anyLong())).thenReturn(Optional.of(execution));

        // When & Then
        mockMvc.perform(get("/jobs/1/executions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.creationTime").exists())
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists())
                .andExpect(jsonPath("$.customInput").isMap())
                .andExpect(jsonPath("$.customInput.key1").value("value1"))
                .andExpect(jsonPath("$.customInput.key2").value("value2"))
                .andExpect(jsonPath("$.output").value("Execution output with logs"));
    }

    @Test
    @DisplayName("Should return 404 when execution does not exist")
    void test_getExecution_NotFound() throws Exception {
        // Given
        when(executionService.getExecutionById(anyLong(), anyLong())).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/jobs/1/executions/999"))
                .andExpect(status().isNotFound());
    }
}
