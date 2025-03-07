package org.demo.jobmaster.service;


import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public Job createJob(Job job) {
        jobRepository.save(job);
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
