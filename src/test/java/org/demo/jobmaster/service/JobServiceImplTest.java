package org.demo.jobmaster.service;

import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.manager.JobExecutionManager;
import org.demo.jobmaster.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExecutionManager jobExecutionManager;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Captor
    private ArgumentCaptor<CronTrigger> triggerCaptor;

    private JobServiceImpl jobService;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        jobService = new JobServiceImpl(jobRepository, jobExecutionManager, taskScheduler);
        now = LocalDateTime.parse("2025-03-09T17:13:11");
    }

    @Test
    @DisplayName("Should load and schedule active cron jobs during initialization")
    void afterPropertiesSet_shouldLoadAndScheduleActiveJobs() {
        // Given
        Job job1 = createCronJob(1L);
        Job job2 = createCronJob(2L);
        List<Job> activeJobs = Arrays.asList(job1, job2);

        when(jobRepository.findAllActiveCron()).thenReturn(activeJobs);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));

        // When
        jobService.afterPropertiesSet();

        // Then
        verify(jobRepository).findAllActiveCron();
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @DisplayName("Should cancel all scheduled tasks during shutdown")
    void destroy_shouldCancelAllScheduledTasks() {
        // Given
        Job job = createCronJob(1L);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));

        jobService.createJob(job);

        // When
        jobService.destroy();

        // Then
        verify(scheduledFuture).cancel(false);
    }

    @Test
    @DisplayName("Should save job and schedule if active")
    void createJob_shouldSaveAndScheduleIfActive() {
        // Given
        Job job = createCronJob(null);
        when(jobRepository.save(job)).thenReturn(1L);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));

        // When
        Long jobId = jobService.createJob(job);

        // Then
        assertEquals(1L, jobId);
        assertEquals(1L, job.getId());
        verify(jobRepository).save(job);
        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @DisplayName("Should save job without scheduling if not active")
    void createJob_shouldSaveWithoutSchedulingIfNotActive() {
        // Given
        Job job = createJob(null, false, Job.ScheduleType.CRON, "0 0 * * * ?");
        when(jobRepository.save(job)).thenReturn(1L);

        // When
        Long jobId = jobService.createJob(job);

        // Then
        assertEquals(1L, jobId);
        verify(jobRepository).save(job);
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @DisplayName("Should execute immediately for one-time jobs")
    void createJob_shouldExecuteImmediatelyForOneTimeJobs() {
        // Given
        Job job = createOneTimeJob();
        when(jobRepository.save(job)).thenReturn(1L);

        // When
        jobService.createJob(job);

        // Then
        verify(taskScheduler).execute(runnableCaptor.capture());

        runnableCaptor.getValue().run();
        verify(jobExecutionManager).submit(job);
    }

    @Test
    @DisplayName("Should retrieve all jobs")
    void getAllJobs_shouldReturnAllJobs() {
        // Given
        List<Job> jobs = Arrays.asList(createCronJob(1L), createCronJob(2L));
        when(jobRepository.findAll()).thenReturn(jobs);

        // When
        List<Job> result = jobService.getAllJobs();

        // Then
        assertEquals(jobs, result);
    }

    @Test
    @DisplayName("Should retrieve job by id when exists")
    void getJobById_shouldReturnJobWhenExists() {
        // Given
        Job job = createCronJob(1L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        // When
        Optional<Job> result = jobService.getJobById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(job, result.get());
    }

    @Test
    @DisplayName("Should return empty when job does not exist")
    void getJobById_shouldReturnEmptyWhenNotExists() {
        // Given
        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Job> result = jobService.getJobById(1L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should update job and reschedule if active")
    void updateJob_shouldUpdateAndRescheduleIfActive() {
        // Given
        Job job = createCronJob(1L);
        when(jobRepository.update(1L, job)).thenReturn(1);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));

        // When
        boolean updated = jobService.updateJob(1L, job);

        // Then
        assertTrue(updated);
        verify(jobRepository).update(1L, job);
        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @DisplayName("Should update job without rescheduling if not active")
    void updateJob_shouldUpdateWithoutReschedulingIfNotActive() {
        // Given
        Job job = createJob(1L, false, Job.ScheduleType.CRON, "0 0 * * * ?");
        when(jobRepository.update(1L, job)).thenReturn(1);

        // When
        boolean updated = jobService.updateJob(1L, job);

        // Then
        assertTrue(updated);
        verify(jobRepository).update(1L, job);
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @DisplayName("Should return false when update fails")
    void updateJob_shouldReturnFalseWhenUpdateFails() {
        // Given
        Job job = createCronJob(1L);
        when(jobRepository.update(1L, job)).thenReturn(0);

        // When
        boolean updated = jobService.updateJob(1L, job);

        // Then
        assertFalse(updated);
    }

    @Test
    @DisplayName("Should delete job and cancel scheduled task")
    void deleteJob_shouldDeleteAndCancelScheduledTask() {
        // Given
        Job job = createCronJob(1L);
        when(jobRepository.delete(1L)).thenReturn(1);

        when(jobRepository.save(job)).thenReturn(1L);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
        jobService.createJob(job);

        // When
        boolean deleted = jobService.deleteJob(1L);

        // Then
        assertTrue(deleted);
        verify(scheduledFuture).cancel(false);
    }

    @Test
    @DisplayName("Should handle exceptions when executing job")
    void executeJob_shouldHandleExceptions() {
        // Given
        Job job = createCronJob(1L);
        when(jobRepository.save(job)).thenReturn(1L);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
        doThrow(new RuntimeException("Test exception")).when(jobExecutionManager).submit(job);

        // When
        jobService.createJob(job);

        // Then
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(CronTrigger.class));

        assertThrows(RuntimeException.class, () -> runnableCaptor.getValue().run());
    }

    @Test
    @DisplayName("Should schedule with correct cron expression")
    void scheduleJob_shouldUseCorrectCronExpression() {
        // Given
        String specificCron = "0 0 12 * * ?"; // Noon every day
        Job job = createJob(null, true, Job.ScheduleType.CRON, specificCron);

        when(jobRepository.save(job)).thenReturn(1L);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));

        // When
        jobService.createJob(job);

        // Then
        verify(taskScheduler).schedule(any(Runnable.class), triggerCaptor.capture());
        CronTrigger trigger = triggerCaptor.getValue();
        assertEquals(specificCron, trigger.getExpression());
    }

    /**
     * Creates a job with specified parameters
     */
    private Job createJob(Long id, boolean active, Job.ScheduleType scheduleType, String cronExpression) {
        var builder = Job.builder()
                .id(id)
                .name("Test Job")
                .description("Test job description")
                .active(active)
                .memorySize(256)
                .timeout(30000)
                .codeType(Job.CodeType.RAW)
                .codeRaw("console.log('test')")
                .codeInput(Collections.emptyList())
                .scheduleType(scheduleType)
                .cronExpression(cronExpression)
                .createdAt(now)
                .updatedAt(now);
        return builder.build();
    }

    private Job createCronJob(Long id) {
        return createJob(id, true, Job.ScheduleType.CRON, "0 */5 * * * ?");
    }

    private Job createOneTimeJob() {
        return createJob(null, true, Job.ScheduleType.ONCE, null);
    }
}