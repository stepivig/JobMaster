package org.demo.jobmaster.repository;

import org.demo.jobmaster.entity.Job;

import java.util.List;
import java.util.Optional;


/**
 * Repository interface for managing Job entities.
 */
public interface JobRepository {

    /**
     * Retrieves all jobs from the repository.
     *
     * @return a list of all jobs.
     */
    List<Job> findAll();

    /**
     * Finds a job by its unique identifier.
     *
     * @param id the unique identifier of the job.
     * @return an optional containing the found job, or empty if not found.
     */
    Optional<Job> findById(Long id);

    /**
     * Saves a new job to the repository.
     *
     * @param job the job entity to be saved.
     * @return the generated id
     */
    Long save(Job job);

    /**
     * Updates an existing job in the repository.
     *
     * @param jobId the job id
     * @param job   the job entity with updated information.
     * @return the number of affected rows.
     */
    int update(Long jobId, Job job);

    /**
     * Deletes a job from the repository by its unique identifier.
     *
     * @param id the unique identifier of the job to be deleted.
     * @return the number of affected rows.
     */
    int delete(Long id);
}

