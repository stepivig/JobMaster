package org.demo.jobmaster.service;

import org.demo.jobmaster.entity.Execution;
import org.demo.jobmaster.entity.Status;

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

    /**
     * Creates a new queued execution record for a specific job ID.
     *
     * @param jobId the ID of the job for which to create a queued execution
     * @return the ID of the newly created queued execution
     */
    Long createQueuedExecution(Long jobId);

    /**
     * Marks an execution as completed with a specific status and output.
     *
     * @param executionId the ID of the execution to mark as completed
     * @param status      the status to set for the completed execution
     * @param output      the output of the completed execution
     * @return true if the operation was successful, false otherwise
     */
    boolean markAsCompleted(Long executionId, Status status, String output);

    /**
     * Marks an execution as running.
     *
     * @param executionId the ID of the execution to mark as running
     * @return true if the operation was successful, false otherwise
     */
    boolean markAsRunning(Long executionId);
}

