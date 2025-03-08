package org.demo.jobmaster.service;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.MemoryMXBean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MemoryManager is responsible for managing the memory allocation and deallocation
 * for a job. It ensures that the memory usage does not exceed the maximum allowed
 * memory and provides methods to acquire and release memory.
 */
@Slf4j
public class MemoryManager {

    private final AtomicInteger availableMemoryMB;
    private final int availableMemoryStaticMB;

    public MemoryManager(int memoryMaxByClientMB, int memoryReservedPercents, MemoryMXBean memoryMXBean) {
        int maxMemoryMB = calculateMaxMemoryMB(memoryMaxByClientMB, memoryMXBean);
        this.availableMemoryStaticMB = maxMemoryMB - (maxMemoryMB * memoryReservedPercents) / 100;
        this.availableMemoryMB = new AtomicInteger(this.availableMemoryStaticMB);
    }

    public boolean acquire(int toAcquire) {
        while (true) {
            int current = availableMemoryMB.get();
            if (toAcquire > current) {
                return false;
            }

            if (availableMemoryMB.compareAndSet(current, current - toAcquire)) {
                return true;
            }
        }
    }

    public void release(int toRelease) {
        availableMemoryMB.updateAndGet(current -> {
            if (current + toRelease > availableMemoryStaticMB) {
                log.warn("Attempted to release more memory than available. Current: {}, Attempted to release: {}", current, toRelease);
                return availableMemoryStaticMB;
            } else {
                return current + toRelease;
            }
        });
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
}
