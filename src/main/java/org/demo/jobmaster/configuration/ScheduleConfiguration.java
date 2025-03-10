package org.demo.jobmaster.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration class for setting up a ThreadPoolTaskScheduler.
 * This scheduler is used for executing scheduled tasks in a separate thread pool.
 */
@Slf4j
@Configuration
public class ScheduleConfiguration {

    @Value("${scheduler.await-termination-seconds:30}")
    private int awaitTerminationSeconds;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("job-scheduler-");
        taskScheduler.setThreadPriority(Thread.NORM_PRIORITY);

        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(awaitTerminationSeconds);

        taskScheduler.setErrorHandler(throwable -> log.error("Scheduled task execution failed", throwable));

        taskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                log.warn("Thread pool saturated, executing task in caller thread");
                super.rejectedExecution(r, executor);
            }
        });

        return taskScheduler;
    }
}