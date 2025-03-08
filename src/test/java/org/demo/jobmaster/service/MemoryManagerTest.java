package org.demo.jobmaster.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MemoryManagerTest {

    @Mock
    private MemoryMXBean memoryMXBean;

    @Mock
    private MemoryUsage memoryUsage;

    @BeforeEach
    void setUp() {
        // Given
        when(memoryMXBean.getHeapMemoryUsage()).thenReturn(memoryUsage);
        when(memoryUsage.getMax()).thenReturn(1024L * 1024L * 1024L); // 1GB default
    }

    @Test
    @DisplayName("Constructor should initialize correctly when client specifies positive max memory")
    void constructor_WithPositiveMaxMemoryByClient_InitializesCorrectly() {
        // Given
        int maxMemoryByClient = 500; // 500MB
        int reservedPercent = 20;

        // When
        MemoryManager memoryManager = new MemoryManager(maxMemoryByClient, reservedPercent, memoryMXBean);

        // Then
        assertTrue(memoryManager.acquire(400));
        assertFalse(memoryManager.acquire(1));
    }

    @Test
    @DisplayName("Constructor should use system memory when client max memory is zero")
    void constructor_WithZeroMaxMemoryByClient_UsesSystemMemory() {
        // Given
        when(memoryUsage.getMax()).thenReturn(1024L * 1024L * 512L); // 512MB
        int maxMemoryByClient = 0; // 0MB means use system memory
        int reservedPercent = 0;

        // When
        MemoryManager memoryManager = new MemoryManager(maxMemoryByClient, reservedPercent, memoryMXBean);

        // Then
        assertTrue(memoryManager.acquire(512));
        assertFalse(memoryManager.acquire(1));
    }

    @Test
    @DisplayName("Constructor should use minimum of client and system memory when both are available")
    void constructor_WithBothMemorySources_UsesMinimum() {
        // Given
        when(memoryUsage.getMax()).thenReturn(1024L * 1024L * 700L); // 700MB
        int maxMemoryByClient = 800; // 800MB
        int reservedPercent = 0;

        // When
        MemoryManager memoryManager = new MemoryManager(maxMemoryByClient, reservedPercent, memoryMXBean);

        // Then
        assertTrue(memoryManager.acquire(700));
        assertFalse(memoryManager.acquire(1));
    }

    @Test
    @DisplayName("Constructor should throw exception when no memory is available")
    void constructor_ThrowsException_WhenNoMemoryAvailable() {
        // Given
        when(memoryUsage.getMax()).thenReturn(-1L); // -1 indicates unavailable
        int maxMemoryByClient = 0; // 0MB
        int reservedPercent = 0;

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> new MemoryManager(maxMemoryByClient, reservedPercent, memoryMXBean));
    }

    @Test
    @DisplayName("acquire() should return false when requested memory exceeds available memory")
    void acquire_ReturnsFalse_WhenRequestedMemoryExceedsAvailable() {
        // Given
        MemoryManager memoryManager =
                new MemoryManager(100, 0, memoryMXBean); // 100MB available

        // When & Then
        assertFalse(memoryManager.acquire(101)); // Request more than available
        assertTrue(memoryManager.acquire(100)); // Request exactly available
    }

    @Test
    @DisplayName("acquire() should return true and update available memory when successful")
    void acquire_ReturnsTrue_AndUpdatesAvailableMemory() {
        // Given
        MemoryManager memoryManager =
                new MemoryManager(100, 0, memoryMXBean); // 100MB available

        // When
        boolean firstAcquisition = memoryManager.acquire(60);
        boolean secondAcquisition = memoryManager.acquire(40);
        boolean thirdAcquisition = memoryManager.acquire(1);

        // Then
        assertTrue(firstAcquisition);
        assertTrue(secondAcquisition);
        assertFalse(thirdAcquisition); // Should be full now
    }

    @Test
    @DisplayName("release() should increase available memory")
    void release_IncreasesAvailableMemory() {
        // Given
        MemoryManager memoryManager =
                new MemoryManager(100, 0, memoryMXBean); // 100MB available
        boolean initialAcquisition = memoryManager.acquire(100);

        // When
        memoryManager.release(50);

        // Then
        assertTrue(initialAcquisition);
        assertFalse(memoryManager.acquire(51)); // Can't acquire more than released
        assertTrue(memoryManager.acquire(50)); // Should have 50MB available now
    }

    @Test
    @DisplayName("release() should not allow memory to exceed maximum capacity")
    void release_DoesNotExceedMaximum() {
        // Given
        MemoryManager memoryManager =
                new MemoryManager(100, 0, memoryMXBean); // 100MB available

        // When
        memoryManager.release(150);

        // Then
        assertTrue(memoryManager.acquire(100));
        assertFalse(memoryManager.acquire(1));
    }

    @Test
    @DisplayName("release() should log a warning when trying to release more memory than maximum capacity")
    void release_LogsWarning_WhenReleasingTooMuch() {
        // Given
        MemoryManager memoryManager =
                new MemoryManager(100, 0, memoryMXBean); // 100MB available
        boolean initialAcquisition = memoryManager.acquire(50);

        // When
        memoryManager.release(100); // This should log a warning

        // Then
        assertTrue(initialAcquisition);
        assertTrue(memoryManager.acquire(100));
        assertFalse(memoryManager.acquire(1));
    }

    @Test
    @DisplayName("Memory manager should maintain consistency during concurrent acquire and release operations")
    void concurrentAcquireAndRelease_MaintainsConsistency() throws InterruptedException {
        // Given
        MemoryManager memoryManager =
                new MemoryManager(1000, 0, memoryMXBean); // 1000MB available
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        // Each thread tries to acquire and release 5MB multiple times
                        if (memoryManager.acquire(5)) {
                            memoryManager.release(5);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean allThreadsCompleted = latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertTrue(allThreadsCompleted);
        assertTrue(memoryManager.acquire(1000)); // Should have all memory available again
    }

    @Test
    @DisplayName("Constructor should properly apply reserved memory percentage")
    void constructor_WithReservedMemory_ReducesAvailableMemory() {
        // Given
        int maxMemoryByClient = 1000; // 1000MB
        int reservedPercent = 30; // 30%

        // When
        MemoryManager memoryManager = new MemoryManager(maxMemoryByClient, reservedPercent, memoryMXBean);

        // Then
        assertTrue(memoryManager.acquire(700)); // 1000MB - 30% = 700MB
        assertFalse(memoryManager.acquire(1)); // No more memory available
    }
}