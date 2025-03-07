package org.demo.jobmaster.service;

import org.demo.jobmaster.entity.Execution;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing Execution entities.
 * This interface defines methods for retrieving execution records.
 */
public interface ExecutionService {

    /**
     * Retrieves a list of executions associated with a specific job ID.
     *
     * @param jobId the ID of the job whose executions should be retrieved
     * @return a list of Execution entities associated with the given job ID
     */
    List<Execution> getExecutionsByJobId(Long jobId);

    /**
     * Retrieves a specific execution by its execution ID and associated job ID.
     *
     * @param jobId       the ID of the job to which the execution belongs
     * @param executionId the ID of the execution to retrieve
     * @return an Optional containing the Execution entity if found, otherwise empty
     */
    Optional<Execution> getExecutionById(Long jobId, Long executionId);
}

