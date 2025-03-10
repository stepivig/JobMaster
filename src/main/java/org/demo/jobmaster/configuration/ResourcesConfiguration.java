package org.demo.jobmaster.configuration;

import org.demo.jobmaster.manager.ResourceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Configuration class for setting up resources in the application.
 * This class defines a bean for the ResourceManager, which manages memory and thread resources.
 */
@Configuration
public class ResourcesConfiguration {


    /**
     * Creates a MemoryManager bean with the specified configuration properties.
     *
     * @param memoryMaxByClientMB    Maximum memory set by client in MB.
     * @param memoryReservedPercents Percentage of memory reserved for system processes.
     * @param coreMultiplier         Multiplier for calculating the number of threads.
     * @param minThreads             Minimum number of threads to be used.
     * @param maxThreads             Maximum number of threads to be used.
     * @return ResourceManager instance configured with the specified properties.
     */
    @Bean
    public ResourceManager memoryManager(@Value("${jobmaster.memory.max.mb:0}") int memoryMaxByClientMB,
                                         @Value("${jobmaster.memory.reserved.percents:50}") int memoryReservedPercents,
                                         @Value("${jobmaster.thread.core-multiplier:1.5}") double coreMultiplier,
                                         @Value("${jobmaster.thread.min-threads:2}") int minThreads,
                                         @Value("${jobmaster.thread.max-threads:32}") int maxThreads) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        return new ResourceManager(memoryMaxByClientMB, memoryReservedPercents, coreMultiplier, minThreads, maxThreads, memoryMXBean);
    }
}
