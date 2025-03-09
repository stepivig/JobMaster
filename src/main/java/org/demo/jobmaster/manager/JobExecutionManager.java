package org.demo.jobmaster.manager;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;
import org.demo.jobmaster.entity.Status;
import org.demo.jobmaster.executor.JobExecutor;
import org.demo.jobmaster.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the execution of jobs, including queuing and executing them based on available resources.
 */
@Slf4j
@Service
public class JobExecutionManager {

    private final ResourceManager resourceManager;
    private final Queue<JobExecutionPair> jobQueue;
    private final ExecutionService executionService;
    private final Map<Runtime, JobExecutor> jobExecutorMap;
    private final ExecutorService executorService;
    private final Lock queueLock = new ReentrantLock();

    @Autowired
    public JobExecutionManager(ResourceManager resourceManager,
                               ExecutionService executionService,
                               List<JobExecutor> jobExecutors) {
        this.resourceManager = resourceManager;
        this.executionService = executionService;
        this.jobQueue = new ConcurrentLinkedQueue<>();
        this.jobExecutorMap = jobExecutors.stream()
                .collect(Collectors.toMap(JobExecutor::getRuntime, Function.identity()));
        this.executorService = Executors.newCachedThreadPool();
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        log.info("Waiting for executor service to terminate...");
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Executor service did not terminate in the specified time. Shutting down now...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void submit(Job job) {
        log.info("Submitting job: {}", job);
        Long executionId = executionService.createQueuedExecution(job.getId());
        jobQueue.add(new JobExecutionPair(job, executionId));
        processQueue();
    }

    private void processQueue() {
        queueLock.lock();
        try {
            while (!jobQueue.isEmpty()) {
                JobExecutionPair pair = jobQueue.peek();
                Job job = pair.job();
                int memorySize = job.getMemorySize();


                boolean resourcesAcquired = resourceManager.acquireResources(memorySize);
                if (!resourcesAcquired) {
                    break;
                }

                jobQueue.poll();
                executionService.markAsRunning(pair.executionId());

                executorService.submit(() -> executeJob(pair));
            }
        } finally {
            queueLock.unlock();
        }
    }

    private void executeJob(JobExecutionPair pair) {
        Job job = pair.job();
        log.info("Executing job: {} with execution {}", job, pair.executionId());
        JobExecutor jobExecutor = jobExecutorMap.get(job.getRuntime());

        if (jobExecutor != null) {
            try {
                jobExecutor.execute(job,
                        output -> onJobComplete(pair, output),
                        error -> onJobError(pair, error));
            } catch (Exception e) {
                onJobError(pair, "Execution error: " + e.getMessage());
            }
        } else {
            resourceManager.releaseResources(job.getMemorySize());
            handleMissingExecutor(job, pair.executionId());

            processQueue();
        }
    }

    private void onJobComplete(JobExecutionPair pair, String output) {
        log.info("Job completed successfully: {} with execution {}", pair.job(), pair.executionId());
        Job job = pair.job();
        int memorySize = job.getMemorySize();
        try {
            executionService.markAsCompleted(pair.executionId(), Status.COMPLETED, output);
        } finally {
            resourceManager.releaseResources(memorySize);
            processQueue();
        }
    }

    private void onJobError(JobExecutionPair pair, String error) {
        log.error("Job failed: {} with execution {}. Error: {}", pair.job(), pair.executionId(), error);
        try {
            executionService.markAsCompleted(pair.executionId(), Status.FAILED, error);
        } finally {
            resourceManager.releaseResources(pair.job().getMemorySize());
            processQueue();
        }
    }

    private void handleMissingExecutor(Job job, Long executionId) {
        log.error("No executor found for job runtime: {}", job.getRuntime());
        executionService.markAsCompleted(executionId, Status.FAILED, "No executor found for runtime: " + job.getRuntime());
    }

    /**
     * A record to hold a job and its associated execution ID
     */
    private record JobExecutionPair(Job job, Long executionId) {
    }
}
