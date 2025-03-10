package org.demo.jobmaster.controller;

import jakarta.validation.Valid;
import org.demo.jobmaster.dto.ExecutionDto;
import org.demo.jobmaster.dto.JobDto;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.mapper.ExecutionMapper;
import org.demo.jobmaster.mapper.JobMapper;
import org.demo.jobmaster.service.ExecutionService;
import org.demo.jobmaster.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final ExecutionService executionService;

    @Autowired
    public JobController(JobService jobService, ExecutionService executionService) {
        this.jobService = jobService;
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<Long> createJob(@RequestBody @Valid JobDto jobDto) {
        Job job = JobMapper.toEntity(jobDto);
        Long createdJobId = jobService.createJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdJobId);
    }

    @GetMapping
    public ResponseEntity<List<JobDto>> getAllJobs() {
        List<JobDto> jobs = jobService.getAllJobs()
                .stream()
                .map(JobMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobDto> getJobById(@PathVariable Long jobId) {
        return jobService.getJobById(jobId)
                .map(job -> ResponseEntity.ok(JobMapper.toDto(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<Void> updateJob(@PathVariable Long jobId, @RequestBody @Valid JobDto jobDto) {
        Job job = JobMapper.toEntity(jobDto);
        boolean updated = jobService.updateJob(jobId, job);
        return updated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long jobId) {
        boolean deleted = jobService.deleteJob(jobId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{jobId}/executions")
    public ResponseEntity<List<ExecutionDto>> getExecutionsByJob(@PathVariable Long jobId) {
        List<ExecutionDto> dtos = executionService
                .getExecutionsByJobId(jobId).stream()
                .map(ExecutionMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{jobId}/executions/{executionId}")
    public ResponseEntity<ExecutionDto> getExecution(@PathVariable Long jobId, @PathVariable Long executionId) {
        return executionService.getExecutionById(jobId, executionId)
                .map(entity -> ResponseEntity.ok(ExecutionMapper.toDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }
}