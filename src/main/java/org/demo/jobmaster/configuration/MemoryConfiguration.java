package org.demo.jobmaster.configuration;

import org.demo.jobmaster.service.MemoryManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;

/**
 * Configuration class for setting up the MemoryManager bean.
 */
@Configuration
public class MemoryConfiguration {

    @Bean
    public MemoryManager memoryManager(@Value("${memory.max.mb:0}") int memoryMaxByClientMB,
                                       @Value("${memory.reserved.percents:80}") int memoryReservedPercents) {
        return new MemoryManager(memoryMaxByClientMB, memoryReservedPercents, ManagementFactory.getMemoryMXBean());
    }
}
