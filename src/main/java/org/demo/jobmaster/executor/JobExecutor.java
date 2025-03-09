package org.demo.jobmaster.executor;

import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;

import java.util.function.Consumer;

/**
 * Interface for executing jobs in different runtimes.
 * Implementations should provide the logic for executing jobs in their respective runtimes.
 */
public interface JobExecutor {

    /**
     * Returns the runtime supported by this executor.
     *
     * @return the runtime
     */
    Runtime getRuntime();

    /**
     * Executes the given job with the specified execution ID.
     *
     * @param job         the job to execute
     * @param ExecutionId the execution ID
     * @param onSuccess   a callback to be executed on successful completion
     * @param onError     a callback to be executed on error
     */
    void execute(Job job, Long ExecutionId, Consumer<String> onSuccess, Consumer<String> onError);
}
