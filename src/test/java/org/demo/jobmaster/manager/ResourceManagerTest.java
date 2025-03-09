package org.demo.jobmaster.manager;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceManagerTest {

    @Mock
    private MemoryMXBean memoryMXBean;

    @Mock
    private MemoryUsage memoryUsage;

    @BeforeEach
    void setUp() {
        when(memoryMXBean.getHeapMemoryUsage()).thenReturn(memoryUsage);
    }

    @Test
    @DisplayName("Should initialize with correct memory and thread values")
    void shouldInitializeWithCorrectValues() {
        // Given
        int memoryMaxMB = 1000;
        int memoryReservedPercents = 20;
        double coreMultiplier = 1.5;
        int minThreads = 2;
        int maxThreads = 32;

        when(memoryUsage.getMax()).thenReturn(2000L * 1024 * 1024); // 2000 MB

        // When
        ResourceManager resourceManager = new ResourceManager(
                memoryMaxMB, memoryReservedPercents, coreMultiplier, minThreads, maxThreads, memoryMXBean);

        // Then
        assertThat(resourceManager.getTotalThreads()).isGreaterThanOrEqualTo(minThreads);
        assertThat(resourceManager.getTotalThreads()).isLessThanOrEqualTo(maxThreads);

        // Verify the memory was calculated correctly (1000 MB with 20% reserved = 800 MB)
        assertThat(resourceManager.acquireResources(800)).isTrue();
        assertThat(resourceManager.acquireResources(1)).isFalse(); // Should be depleted now
    }

    @Test
    @DisplayName("Should use system memory when client memory is not provided")
    void shouldUseSystemMemoryWhenClientMemoryNotProvided() {
        // Given
        int memoryMaxMB = 0; // Not provided by client
        int memoryReservedPercents = 20;
        double coreMultiplier = 1.5;
        int minThreads = 2;
        int maxThreads = 32;

        when(memoryUsage.getMax()).thenReturn(2000L * 1024 * 1024); // 2000 MB

        // When
        ResourceManager resourceManager = new ResourceManager(
                memoryMaxMB, memoryReservedPercents, coreMultiplier, minThreads, maxThreads, memoryMXBean);

        // Then
        // 2000 MB with 20% reserved = 1600 MB
        assertThat(resourceManager.acquireResources(1600)).isTrue();
        assertThat(resourceManager.acquireResources(1)).isFalse(); // Should be depleted now
    }

    @Test
    @DisplayName("Should use min threads when calculated value is below minimum")
    void shouldUseMinThreadsWhenCalculatedValueIsBelowMinimum() {
        // Given
        int memoryMaxMB = 1000;
        int memoryReservedPercents = 20;
        double coreMultiplier = 0.1; // Very low multiplier
        int minThreads = 5;
        int maxThreads = 32;

        when(memoryUsage.getMax()).thenReturn(2000L * 1024 * 1024);

        // When
        ResourceManager resourceManager = new ResourceManager(
                memoryMaxMB, memoryReservedPercents, coreMultiplier, minThreads, maxThreads, memoryMXBean);

        // Then
        assertThat(resourceManager.getTotalThreads()).isEqualTo(minThreads);

        // Check we can acquire exactly minThreads
        for (int i = 0; i < minThreads; i++) {
            assertThat(resourceManager.acquireResources(1)).isTrue();
        }
        assertThat(resourceManager.acquireResources(1)).isFalse(); // Should be no more threads
    }

    @Test
    @DisplayName("Should use max threads when calculated value exceeds maximum")
    void shouldUseMaxThreadsWhenCalculatedValueExceedsMaximum() {
        // Given
        int memoryMaxMB = 1000;
        int memoryReservedPercents = 20;
        double coreMultiplier = 10.0; // Very high multiplier
        int minThreads = 2;
        int maxThreads = 8;

        when(memoryUsage.getMax()).thenReturn(2000L * 1024 * 1024);

        // When
        ResourceManager resourceManager = new ResourceManager(
                memoryMaxMB, memoryReservedPercents, coreMultiplier, minThreads, maxThreads, memoryMXBean);

        // Then
        assertThat(resourceManager.getTotalThreads()).isEqualTo(maxThreads);

        // Check we can acquire exactly maxThreads
        for (int i = 0; i < maxThreads; i++) {
            assertThat(resourceManager.acquireResources(1)).isTrue();
        }
        assertThat(resourceManager.acquireResources(1)).isFalse(); // Should be no more threads
    }

    @Test
    @DisplayName("Should throw exception when memory cannot be determined")
    void shouldThrowExceptionWhenMemoryCannotBeDetermined() {
        // Given
        int memoryMaxMB = 0; // Not provided by client
        int memoryReservedPercents = 20;
        double coreMultiplier = 1.5;
        int minThreads = 2;
        int maxThreads = 32;

        when(memoryUsage.getMax()).thenReturn(-1L); // Invalid memory size

        // When/Then
        assertThatThrownBy(() -> new ResourceManager(
                memoryMaxMB, memoryReservedPercents, coreMultiplier, minThreads, maxThreads, memoryMXBean))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max memory is not set");
    }

    @Test
    @DisplayName("Should successfully acquire resources when available")
    void shouldSuccessfullyAcquireResourcesWhenAvailable() {
        // Given
        when(memoryUsage.getMax()).thenReturn(1000L * 1024 * 1024);
        ResourceManager resourceManager = new ResourceManager(
                800, 20, 1.5, 4, 16, memoryMXBean);

        // When
        boolean firstAcquire = resourceManager.acquireResources(200);
        boolean secondAcquire = resourceManager.acquireResources(300);

        // Then
        assertThat(firstAcquire).isTrue();
        assertThat(secondAcquire).isTrue();
    }

    @Test
    @DisplayName("Should fail to acquire resources when memory is insufficient")
    void shouldFailToAcquireResourcesWhenMemoryIsInsufficient() {
        // Given
        when(memoryUsage.getMax()).thenReturn(1000L * 1024 * 1024);
        ResourceManager resourceManager = new ResourceManager(
                500, 20, 1.5, 4, 16, memoryMXBean);

        // When
        boolean firstAcquire = resourceManager.acquireResources(400); // Should succeed
        boolean secondAcquire = resourceManager.acquireResources(1); // Should fail, no memory left

        // Then
        assertThat(firstAcquire).isTrue();
        assertThat(secondAcquire).isFalse();
    }

    @Test
    @DisplayName("Should fail to acquire resources when threads are exhausted")
    void shouldFailToAcquireResourcesWhenThreadsAreExhausted() {
        // Given
        when(memoryUsage.getMax()).thenReturn(10000L * 1024 * 1024); // Plenty of memory
        ResourceManager resourceManager = new ResourceManager(
                8000, 20, 1.5, 3, 3, memoryMXBean); // Only 3 threads

        // When
        boolean firstAcquire = resourceManager.acquireResources(100);
        boolean secondAcquire = resourceManager.acquireResources(100);
        boolean thirdAcquire = resourceManager.acquireResources(100);
        boolean fourthAcquire = resourceManager.acquireResources(100);

        // Then
        assertThat(firstAcquire).isTrue();
        assertThat(secondAcquire).isTrue();
        assertThat(thirdAcquire).isTrue();
        assertThat(fourthAcquire).isFalse(); // No more threads available
    }

    @Test
    @DisplayName("Should successfully release resources and make them available again")
    void shouldSuccessfullyReleaseResourcesAndMakeThemAvailableAgain() {
        // Given
        when(memoryUsage.getMax()).thenReturn(1000L * 1024 * 1024);
        ResourceManager resourceManager = new ResourceManager(
                800, 20, 1.5, 2, 16, memoryMXBean);

        // When
        resourceManager.acquireResources(500); // Acquire most of the memory
        resourceManager.releaseResources(500); // Release it
        boolean reacquire = resourceManager.acquireResources(500); // Try to acquire again

        // Then
        assertThat(reacquire).isTrue(); // Should be able to reacquire
    }

    @Test
    @DisplayName("Should handle attempt to release more memory than total")
    void shouldHandleAttemptToReleaseMoreMemoryThanTotal() {
        // Given
        when(memoryUsage.getMax()).thenReturn(1000L * 1024 * 1024);
        ResourceManager resourceManager = new ResourceManager(
                800, 20, 1.5, 4, 16, memoryMXBean);
        int totalMemory = 640; // 800 - 20% = 640

        // When
        resourceManager.acquireResources(totalMemory / 2); // Acquire half of memory
        resourceManager.releaseResources(totalMemory * 2); // Try to release twice the total
        boolean reacquire = resourceManager.acquireResources(totalMemory); // Try to acquire all memory

        // Then
        assertThat(reacquire).isTrue(); // Should reset to max and allow full allocation
    }

    @Test
    @DisplayName("Should handle attempt to release more threads than total")
    void shouldHandleAttemptToReleaseMoreThreadsThanTotal() {
        // Given
        when(memoryUsage.getMax()).thenReturn(1000L * 1024 * 1024);
        ResourceManager resourceManager = new ResourceManager(
                800, 20, 1.5, 3, 3, memoryMXBean); // 3 threads total

        // When
        resourceManager.acquireResources(10); // Acquire 1 thread
        resourceManager.releaseResources(10); // Release 1 thread
        resourceManager.releaseResources(10); // Try to release another thread
        resourceManager.releaseResources(10); // Try to release another thread
        resourceManager.releaseResources(10); // Try to release another thread

        // Now try to acquire all threads
        boolean first = resourceManager.acquireResources(10);
        boolean second = resourceManager.acquireResources(10);
        boolean third = resourceManager.acquireResources(10);
        boolean fourth = resourceManager.acquireResources(10);

        // Then
        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(third).isTrue();
        assertThat(fourth).isFalse(); // Should still only have 3 total threads
    }

    @Test
    @DisplayName("Should handle concurrent resource acquisition and release")
    void shouldHandleConcurrentResourceAcquisitionAndRelease() {
        // Given
        when(memoryUsage.getMax()).thenReturn(1000L * 1024 * 1024);
        ResourceManager resourceManager = new ResourceManager(
                800, 20, 1.5, 10, 10, memoryMXBean);

        // When - simulate concurrent operations
        boolean first = resourceManager.acquireResources(100);
        boolean second = resourceManager.acquireResources(100);
        resourceManager.releaseResources(100);
        boolean third = resourceManager.acquireResources(100);

        // Then
        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(third).isTrue();
    }

    @Test
    @DisplayName("ResourceManager should maintain consistency during concurrent acquire and release operations")
    void concurrentAcquireAndRelease_MaintainsConsistency() throws InterruptedException {
        // Given
        when(memoryUsage.getMax()).thenReturn(2000L * 1024 * 1024);
        ResourceManager resourceManager = new ResourceManager(
                1000, 0, 2.0, 20, 20, memoryMXBean); // 1000MB available, 20 threads

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            final int resourceAmount = 10; // Each thread uses 10MB
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        // Each thread tries to acquire and release resources multiple times
                        if (resourceManager.acquireResources(resourceAmount)) {
                            // Small delay to increase chance of thread interleaving
                            Thread.sleep(1);
                            resourceManager.releaseResources(resourceAmount);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean allThreadsCompleted = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(2, TimeUnit.SECONDS));

        // Then
        assertThat(allThreadsCompleted).isTrue();

        // Should be able to acquire all memory
        assertThat(resourceManager.acquireResources(1000)).isTrue();

        // Release it to check we can also get all threads
        resourceManager.releaseResources(1000);

        // Should be able to acquire all threads (by acquiring minimal memory 20 times)
        boolean allThreadsAvailable = true;
        for (int i = 0; i < 20; i++) {
            allThreadsAvailable &= resourceManager.acquireResources(1);
        }
        assertThat(allThreadsAvailable).isTrue();
    }

    @Test
    @DisplayName("Should use client memory when heap memory cannot be determined")
    void shouldUseClientMemoryWhenHeapMemoryCannotBeDetermined() {
        // Given
        int memoryMaxMB = 500; // Valid client memory setting
        int memoryReservedPercents = 20;
        double coreMultiplier = 1.5;
        int minThreads = 2;
        int maxThreads = 32;

        when(memoryUsage.getMax()).thenReturn(0L); // Heap memory is not available/invalid

        // When
        ResourceManager resourceManager = new ResourceManager(
                memoryMaxMB, memoryReservedPercents, coreMultiplier, minThreads, maxThreads, memoryMXBean);

        // Then
        assertThat(resourceManager.acquireResources(400)).isTrue();
        assertThat(resourceManager.acquireResources(1)).isFalse(); // Should be depleted now

        resourceManager.releaseResources(400);
        assertThat(resourceManager.acquireResources(400)).isTrue();
    }
}