package org.demo.jobmaster.service;

import lombok.RequiredArgsConstructor;
import org.demo.jobmaster.entity.Execution;
import org.demo.jobmaster.repository.ExecutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    private final ExecutionRepository executionRepository;

    @Override
    public List<Execution> getExecutionsByJobId(Long jobId) {
        return executionRepository.getExecutionsByJobId(jobId);
    }

    @Override
    public Optional<Execution> getExecutionById(Long jobId, Long executionId) {
        return executionRepository.getExecutionById(executionId);
    }
}
