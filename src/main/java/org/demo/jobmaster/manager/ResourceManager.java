package org.demo.jobmaster.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.MemoryMXBean;

/**
 * Manages both memory and thread resources for job execution.
 */
@Slf4j
public class ResourceManager {

    private int availableMemoryMB;
    @Getter
    private final int totalMemoryMB;

    private int availableThreads;
    @Getter
    private final int totalThreads;

    private final Object resourceLock = new Object();

    public ResourceManager(int memoryMaxByClientMB,
                           int memoryReservedPercents,
                           double coreMultiplier,
                           int minThreads,
                           int maxThreads,
                           MemoryMXBean memoryMXBean) {
        int maxMemoryMB = calculateMaxMemoryMB(memoryMaxByClientMB, memoryMXBean);
        this.totalMemoryMB = maxMemoryMB - (maxMemoryMB * memoryReservedPercents) / 100;
        this.availableMemoryMB = this.totalMemoryMB;

        int optimalThreads = calculateOptimalTreads(coreMultiplier, minThreads, maxThreads);
        this.totalThreads = optimalThreads;
        this.availableThreads = optimalThreads;

        log.info("ResourceManager initialized with {} MB memory and {} concurrent jobs",
                this.totalMemoryMB, this.totalThreads);
    }

    /**
     * Atomically acquires both memory and thread resources.
     *
     * @param memoryMB Memory required in MB
     * @return true if resources were successfully acquired, false otherwise
     */
    public boolean acquireResources(int memoryMB) {
        synchronized (resourceLock) {
            if (memoryMB > availableMemoryMB || availableThreads <= 0) {
                return false;
            }

            availableMemoryMB -= memoryMB;
            availableThreads--;

            return true;
        }
    }

    /**
     * Releases resources.
     *
     * @param memoryMB Amount of memory to release in MB
     */
    public void releaseResources(int memoryMB) {
        synchronized (resourceLock) {
            int newMemory = availableMemoryMB + memoryMB;
            if (newMemory > totalMemoryMB) {
                log.warn("Attempted to release more memory than total. Current: {}, Release: {}, Total: {}",
                        availableMemoryMB, memoryMB, totalMemoryMB);
                availableMemoryMB = totalMemoryMB;
            } else {
                availableMemoryMB = newMemory;
            }

            if (availableThreads < totalThreads) {
                availableThreads++;
            } else {
                log.warn("Attempted to release more threads than total. Current: {}, Total: {}",
                        availableThreads, totalThreads);
            }
        }
    }

    private int calculateMaxMemoryMB(int memoryMaxByUserMB, MemoryMXBean memoryMXBean) {
        long heapMaxMemoryBytes = memoryMXBean.getHeapMemoryUsage().getMax();
        if (memoryMaxByUserMB <= 0 && heapMaxMemoryBytes <= 0) {
            throw new IllegalArgumentException("Max memory is not set and cannot be determined from the system.");
        } else if (memoryMaxByUserMB <= 0) {
            return Math.toIntExact((heapMaxMemoryBytes / (1024 * 1024)));
        } else if (heapMaxMemoryBytes <= 0) {
            return memoryMaxByUserMB;
        } else {
            return Math.toIntExact(Math.min(memoryMaxByUserMB, heapMaxMemoryBytes / (1024L * 1024L)));
        }
    }

    private int calculateOptimalTreads(double coreMultiplier, int minThreads, int maxThreads) {
        int availableCores = Runtime.getRuntime().availableProcessors();
        int calculatedThreads = (int) Math.ceil(availableCores * coreMultiplier);
        return Math.max(minThreads, Math.min(calculatedThreads, maxThreads));
    }

}