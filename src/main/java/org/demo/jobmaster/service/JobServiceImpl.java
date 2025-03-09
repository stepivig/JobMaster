package org.demo.jobmaster.service;


import lombok.extern.slf4j.Slf4j;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.manager.JobExecutionManager;
import org.demo.jobmaster.repository.JobRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;


    private final JobExecutionManager jobExecutionManager;
    private final ThreadPoolTaskScheduler taskScheduler;


    public JobServiceImpl(JobRepository jobRepository, JobExecutionManager jobExecutionManager) {
        this.jobRepository = jobRepository;
        this.jobExecutionManager = jobExecutionManager;
        this.taskScheduler = new ThreadPoolTaskScheduler();
    }

    @Override
    public Job createJob(Job job) {
        jobRepository.save(job);
        if (job.isActive()) {
            Job.ScheduleType scheduleType = job.getScheduleType();
            if (scheduleType == Job.ScheduleType.ONCE) {
                taskScheduler.execute(() -> jobExecutionManager.submit(job));
            } else if (scheduleType == Job.ScheduleType.CRON) {
                taskScheduler.schedule(() -> jobExecutionManager.submit(job), new CronTrigger(job.getCronExpression()));
            } else {
                log.warn("Unsupported schedule type: {}", scheduleType);
            }
        }
        return job;
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
        int updated = jobRepository.update(jobId, job);
        return updated > 0;
    }

    @Override
    public boolean deleteJob(Long jobId) {
        return jobRepository.delete(jobId) > 0;
    }
}
