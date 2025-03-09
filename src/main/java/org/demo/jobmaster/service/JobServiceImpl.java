package org.demo.jobmaster.service;

import lombok.extern.slf4j.Slf4j;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.manager.JobExecutionManager;
import org.demo.jobmaster.repository.JobRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class JobServiceImpl implements JobService, InitializingBean, DisposableBean {

    private final JobRepository jobRepository;
    private final JobExecutionManager jobExecutionManager;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Autowired
    public JobServiceImpl(JobRepository jobRepository,
                          JobExecutionManager jobExecutionManager,
                          ThreadPoolTaskScheduler taskScheduler) {
        this.jobRepository = jobRepository;
        this.jobExecutionManager = jobExecutionManager;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("JobService initialized, loading active cron jobs");
        List<Job> activeJobs = jobRepository.findAllActiveCron();
        for (Job job : activeJobs) {
            scheduleJob(job);
        }
        log.info("Loaded {} active cron jobs at startup", activeJobs.size());
    }

    @Override
    public void destroy() {
        log.info("Shutting down JobService, cancelling {} scheduled tasks", scheduledTasks.size());
        for (Map.Entry<Long, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
            entry.getValue().cancel(false);
        }
        scheduledTasks.clear();
    }

    @Override
    public Long createJob(Job job) {
        log.info("Creating job: {}", job);
        Long jobId = jobRepository.save(job);
        job.setId(jobId);
        if (job.isActive()) {
            scheduleJob(job);
            log.info("Job created and scheduled: {}", jobId);
        } else {
            log.info("Job created but not active, not scheduled: {}", jobId);
        }
        return jobId;
    }

    @Override
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @Override
    public Optional<Job> getJobById(Long jobId) {
        return jobRepository.findById(jobId);
    }

    @Override
    public boolean updateJob(Long jobId, Job job) {
        log.info("Updating job: {}", jobId);
        cancelJobSchedule(jobId);

        int updated = jobRepository.update(jobId, job);
        if (updated > 0) {
            if (job.isActive()) {
                job.setId(jobId); // Ensure ID is set for scheduling
                scheduleJob(job);
                log.info("Job updated and rescheduled: {}", jobId);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteJob(Long jobId) {
        log.info("Deleting job: {}", jobId);
        cancelJobSchedule(jobId);
        return jobRepository.delete(jobId) > 0;
    }

    /**
     * Schedule a job based on its schedule type
     */
    private void scheduleJob(Job job) {
        try {
            Job.ScheduleType scheduleType = job.getScheduleType();
            if (scheduleType == Job.ScheduleType.ONCE) {
                taskScheduler.execute(() -> executeJob(job));
                log.info("Scheduled one-time job: {}", job.getId());
            } else if (scheduleType == Job.ScheduleType.CRON) {
                var future = taskScheduler.schedule(() -> executeJob(job), new CronTrigger(job.getCronExpression()));
                scheduledTasks.put(job.getId(), future);
                log.info("Scheduled recurring job with cron expression '{}': {}", job.getCronExpression(), job.getId());
            } else {
                log.warn("Unsupported schedule type: {} for job: {}", scheduleType, job.getId());
            }
        } catch (Exception e) {
            log.error("Failed to schedule job: {}", job.getId(), e);
        }
    }

    private void cancelJobSchedule(Long jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled scheduled execution for job: {}", jobId);
        }
    }

    private void executeJob(Job job) {
        try {
            log.info("Executing job: {}", job.getId());
            jobExecutionManager.submit(job);
        } catch (Exception e) {
            log.error("Error executing job: {}", job.getId(), e);
        }
    }
}