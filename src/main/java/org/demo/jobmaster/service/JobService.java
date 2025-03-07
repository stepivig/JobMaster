package org.demo.jobmaster.service;

import org.demo.jobmaster.entity.Job;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing job executions.
 * <p>
 * This interface provides methods for creating, retrieving, updating, and deleting jobs.
 * Implementations of this interface should handle the business logic associated with job management.
 */
public interface JobService {

    /**
     * Creates a new job and stores it in the system.
     *
     * @param job the job entity to be created
     * @return the created job entity
     */
    Job createJob(Job job);

    /**
     * Retrieves a list of all jobs in the system.
     *
     * @return a list of {@link Job} entities
     */
    List<Job> getAllJobs();

    /**
     * Retrieves the details of a specific job by its ID.
     *
     * @param jobId the unique identifier of the job
     * @return an {@link Optional} containing the job if found, otherwise empty
     */
    Optional<Job> getJobById(Long jobId);

    /**
     * Updates an existing job identified by its ID.
     * <p>
     * If the job exists, it will be updated with the new values provided.
     * </p>
     *
     * @param jobId the unique identifier of the job to be updated
     * @param job   the job entity containing updated details
     * @return {@code true} if updated, {@code false} otherwise
     */
    boolean updateJob(Long jobId, Job job);

    /**
     * Deletes a job from the system by its ID.
     * <p>
     * If the job exists and is successfully deleted, the method returns {@code true}.
     * Otherwise, it returns {@code false}.
     * </p>
     *
     * @param jobId the unique identifier of the job to be deleted
     * @return {@code true} if the job was deleted, {@code false} otherwise
     */
    boolean deleteJob(Long jobId);
}
