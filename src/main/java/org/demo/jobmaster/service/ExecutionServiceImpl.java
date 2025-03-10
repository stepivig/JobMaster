package org.demo.jobmaster.service;

import org.demo.jobmaster.entity.Execution;
import org.demo.jobmaster.entity.Status;
import org.demo.jobmaster.repository.ExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ExecutionServiceImpl implements ExecutionService {
    /**
     * Set of statuses that indicate a terminated execution.
     */
    private static final Set<Status> TERMINATED_STATUSES =
            Set.of(Status.COMPLETED, Status.FAILED, Status.TERMINATED, Status.CANCELLED);

    private final ExecutionRepository executionRepository;
    private final DateService dateService;

    @Autowired
    public ExecutionServiceImpl(ExecutionRepository executionRepository, DateService dateService) {
        this.executionRepository = executionRepository;
        this.dateService = dateService;
    }

    @Override
    public List<Execution> getExecutionsByJobId(Long jobId) {
        return executionRepository.getExecutionsByJobId(jobId);
    }

    @Override
    public Optional<Execution> getExecutionById(Long jobId, Long executionId) {
        return executionRepository.getExecutionById(executionId);
    }

    @Override
    public Long createQueuedExecution(Long jobId) {
        LocalDateTime now = dateService.getCurrentDateTime();
        Execution execution = Execution.builder().status(Status.QUEUED).creationTime(now).build();
        return executionRepository.save(jobId, execution);
    }

    @Override
    public Long createFailedExecution(Long jobId, String errorMessage) {
        LocalDateTime now = dateService.getCurrentDateTime();
        Execution execution = Execution.builder()
                .status(Status.FAILED)
                .creationTime(now)
                .endTime(now)
                .output(errorMessage).build();
        return executionRepository.save(jobId, execution);
    }

    @Override
    public boolean markAsCompleted(Long executionId, Status status, String output) {
        if (!TERMINATED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Status must be COMPLETED or FAILED");
        }
        LocalDateTime endTime = dateService.getCurrentDateTime();
        return executionRepository.markAsCompleted(executionId, status, endTime, output) > 0;
    }

    @Override
    public boolean markAsRunning(Long executionId) {
        return executionRepository.markAsRunning(executionId, dateService.getCurrentDateTime()) > 0;
    }
}
