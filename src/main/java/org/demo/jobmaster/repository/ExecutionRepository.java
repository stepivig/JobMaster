package org.demo.jobmaster.repository;

import org.demo.jobmaster.entity.Execution;
import org.demo.jobmaster.entity.Status;

import java.time.LocalDateTime;
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
     * @return the ID of the saved execution
     */
    Long save(Long jobId, Execution execution);

    /**
     * Updates only the status of an execution.
     *
     * @param executionId the ID of the execution to update
     * @param status      the new status to set
     * @param updateTime  the time when the status was updated
     * @return the number of affected rows
     */
    int updateStatus(Long executionId, Status status, LocalDateTime updateTime);

    /**
     * Updates the status and sets the start time for a running execution.
     *
     * @param executionId the ID of the execution to update
     * @param startTime   the start time to set
     * @return the number of affected rows
     */
    int markAsRunning(Long executionId, LocalDateTime startTime);

    /**
     * Updates the status, end time, and output for a completed execution.
     *
     * @param executionId the ID of the execution to update
     * @param status      the completion status (COMPLETED or FAILED)
     * @param endTime     the end time to set
     * @param output      the output to set
     * @return the number of affected rows
     */
    int markAsCompleted(Long executionId, Status status, LocalDateTime endTime, String output);
}