package org.demo.jobmaster.repository;

import org.demo.jobmaster.entity.Execution;

import java.util.List;
import java.util.Optional;

public interface ExecutionRepository {
    /**
     * Retrieves a list of executions associated with a specific job ID.
     *
     * @param jobId the ID of the job whose executions should be retrieved
     * @return a list of Execution entities associated with the given job ID
     */
    List<Execution> getExecutionsByJobId(Long jobId);

    /**
     * Retrieves an execution by its unique ID.
     *
     * @param executionId the ID of the execution to retrieve
     * @return an Optional containing the Execution entity if found, otherwise empty
     */
    Optional<Execution> getExecutionById(Long executionId);

    /**
     * Saves a new execution record associated with a given job ID.
     *
     * @param jobId     the ID of the job to which the execution belongs
     * @param execution the Execution entity to be saved
     */
    void save(Long jobId, Execution execution);

    /**
     * Updates an existing execution record.
     *
     * @param execution the Execution entity containing updated information
     */
    void updateExecution(Execution execution);
}
