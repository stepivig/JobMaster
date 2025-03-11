package org.demo.jobmaster;

import org.demo.jobmaster.dto.ExecutionDto;
import org.demo.jobmaster.dto.JobDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EndToEndHttpJobTest {

    private static final Logger log = LoggerFactory.getLogger(EndToEndHttpJobTest.class);
    public static final String JOBS_API = "/jobs";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("E2E Test for JobMaster API")
    public void testOneTimeAndRecurringJobs() throws InterruptedException {
        log.info("Starting E2E test for JobMaster API");
        log.info("Initially Test env has 128MB of memory allocated to jobs");
        log.info("Check there are not created jobs");
        checkingNumberOfJobs(0);
        String tenThousands = "10000";
        log.info("Creating first job on Java. It requires 64MB of memory and will sleep for 10 seconds");
        Long javaJobId = createJavaLongRunningJob(tenThousands, 64);
        log.info("Wait for the job to be created");
        Thread.sleep(500);
        log.info("Checking running status of creating job");
        checkStatusOfRunningExecutionForThisJob(javaJobId, "RUNNING");
        log.info("Checking number of jobs after creating one");
        checkingNumberOfJobs(1);
        log.info("Creating second job on Kotlin. It requires 64MB of memory and will sleep for 10 seconds");
        Long kotlinJobId = createKotlinLongRunningJob(tenThousands, 64);
        log.info("Wait for the job to be created");
        Thread.sleep(500);
        log.info("Checking running status of creating job");
        checkStatusOfRunningExecutionForThisJob(kotlinJobId, "RUNNING");
        log.info("Checking number of jobs after creating two");
        checkingNumberOfJobs(2);
        log.info("Creating third job on Java. It requires 64MB of memory and will sleep for 5 seconds");
        Long javaJobId2 = createJavaLongRunningJob("5000", 64);
        log.info("Wait for the job to be created");
        Thread.sleep(1_000);
        log.info("Checking status of last created job. We expect QUEUED status, since we have only 128MB of memory allocated to jobs");
        checkStatusOfRunningExecutionForThisJob(javaJobId2, "QUEUED");
        Thread.sleep(1_000);
        log.info("Checking status of last created job. We still expect QUEUED status, since we have only 128MB of memory allocated to jobs");
        checkStatusOfRunningExecutionForThisJob(javaJobId2, "QUEUED");

        log.info("Wait for job execution to complete");
        waitForExecutionToComplete(javaJobId);
        waitForExecutionToComplete(kotlinJobId);
        waitForExecutionToComplete(javaJobId2);
    }

    @Test
    @DisplayName("E2E Test for CRON Job Execution")
    public void testCronJobExecution() throws InterruptedException {
        log.info("Starting E2E test for CRON Job Execution");
        log.info("Check there are not created jobs");
        checkingNumberOfJobs(0);
        log.info("Creating CRON job. It requires 64MB of memory and will run every 5 seconds");
        Long cronJobId = createCronJob("*/5 * * * * *");
        log.info("Wait for the job to be created and execute three times");
        Thread.sleep(15_000);
        log.info("Checking number of executions for the CRON job");
        checkNumberOfExecutions(cronJobId, 3);
        log.info("Deleting the CRON job");
        deleteJob(cronJobId);
        log.info("Check there are no jobs");
        checkingNumberOfJobs(0);
        log.info("Check there are no executions for the deleted job");
        checkNumberOfExecutions(cronJobId, 0);
    }

    private void checkingNumberOfJobs(int numberOfJobs) {
        ResponseEntity<List<JobDto>> response = restTemplate.exchange(
                JOBS_API,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<JobDto> body = response.getBody();
        assertNotNull(body);
        int size = body.size();
        assertEquals(numberOfJobs, size);
        log.info("Received jobs: {}", body);
    }

    private static JobDto getJobDto(String sleepTime, int memorySize, String name, String description, String kotlin, String codeRaw) {
        JobDto dto = JobDto.builder().
                name(name).
                description(description)
                .memorySize(memorySize)
                .runtime(kotlin)
                .timeout(30_000)
                .active(true)
                .codeType("RAW")
                .scheduleType("ONCE")
                .codeInput(Collections.singletonList(sleepTime))
                .codeRaw(codeRaw)
                .build();
        return dto;
    }

    private void checkStatusOfRunningExecutionForThisJob(Long createdId, String status) {
        ResponseEntity<List<ExecutionDto>> response2 = restTemplate.exchange(
                JOBS_API + "/" + createdId + "/executions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ExecutionDto> body = response2.getBody();
        assertNotNull(body);
        assertFalse(body.isEmpty());
        assertEquals(1, body.size());
        assertEquals(status, body.get(0).getStatus());
        log.info("Received executions: {}", body);
    }

    private Long createJavaLongRunningJob(String sleepTime, int memorySize) {
        JobDto dto = getJobDto(sleepTime,
                memorySize,
                "java-job",
                "One-time Java job for testing",
                "JAVA",
                """
                        public class Main {
                            public static void main(String[] args) {
                                try {
                                    long sleepTime = Long.parseLong(args[0]);
                                    Thread.sleep(sleepTime);
                                    System.out.println("Sleep completed successfully.");
                                } catch (NumberFormatException e) {
                                    System.err.println("Error: Please provide a valid integer for sleep time.");
                                } catch (InterruptedException e) {
                                    System.err.println("Error: Sleep was interrupted.");
                                    Thread.currentThread().interrupt(); // Restore the interrupted status
                                } catch (Exception e) {
                                    System.err.println("An error occurred: " + e.getMessage());
                                }
                            }
                        }""");
        return createJob(dto, "Created job with ID: {}");
    }

    private Long createKotlinLongRunningJob(String sleepTime, int memorySize) {
        JobDto dto = getJobDto(
                sleepTime,
                memorySize,
                "kotlin-job",
                "One-time Kotlin job for testing",
                "KOTLIN", """
                        class Main {
                            companion object {
                                @JvmStatic
                                fun main(args: Array<String>) {
                                    try {
                                        val sleepTime = args[0].toLong()
                                        Thread.sleep(sleepTime)
                                        println("Sleep completed successfully.")
                                    } catch (e: NumberFormatException) {
                                        System.err.println("Error: Please provide a valid integer for sleep time.")
                                    } catch (e: InterruptedException) {
                                        System.err.println("Error: Sleep was interrupted.")
                                        Thread.currentThread().interrupt() // Restore the interrupted status
                                    } catch (e: Exception) {
                                        System.err.println("An error occurred: ${'$'}{e.message}")
                                    }
                                }
                            }
                        }""");
        return createJob(dto, "Created job with ID: {}");
    }

    private Long createCronJob(String cronExpression) {
        JobDto dto = JobDto.builder().
                name("Java cron job").
                description("Cron job for testing")
                .memorySize(64)
                .runtime("JAVA")
                .timeout(30_000)
                .active(true)
                .codeType("RAW")
                .scheduleType("CRON")
                .cronExpression(cronExpression)
                .codeRaw("""
                        public class Main {
                            public static void main(String[] args) {
                                try {
                                    System.out.println("Cron job executed.");
                                } catch (Exception e) {
                                    System.err.println("An error occurred: " + e.getMessage());
                                }
                            }
                        }
                        """)
                .build();
        return createJob(dto, "Created CRON job with ID: {}");
    }

    private Long createJob(JobDto dto, String logPattern) {
        ResponseEntity<Long> createResponseEntity = restTemplate.postForEntity(JOBS_API, dto, Long.class);
        assertThat(createResponseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        log.info(logPattern, createResponseEntity.getBody());
        return createResponseEntity.getBody();
    }

    private void waitForExecutionToComplete(Long jobId) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 30_000;
        boolean allExecutionsCompleted = false;

        while (!allExecutionsCompleted) {
            if (System.currentTimeMillis() - startTime > maxWaitTime) {
                throw new InterruptedException("Timeout: Job execution did not complete within the maximum wait time of 30 seconds.");
            }

            ResponseEntity<List<ExecutionDto>> response = restTemplate.exchange(
                    JOBS_API + "/" + jobId + "/executions",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<ExecutionDto> body = response.getBody();
            assertNotNull(body);
            allExecutionsCompleted = body.stream().allMatch(execution -> "COMPLETED".equals(execution.getStatus()));
            if (!allExecutionsCompleted) {
                Thread.sleep(1_000);
            }
        }
        log.info("All executions for job ID {} have completed.", jobId);
    }

    private void checkNumberOfExecutions(Long jobId, int expectedCount) {
        ResponseEntity<List<ExecutionDto>> response = restTemplate.exchange(
                JOBS_API + "/" + jobId + "/executions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ExecutionDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(expectedCount, body.size());
        log.info("Received executions: {}", body);
    }

    private void deleteJob(Long jobId) {
        ResponseEntity<Void> response = restTemplate.exchange(
                JOBS_API + "/" + jobId,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        log.info("Deleted job with ID: {}", jobId);
    }
}