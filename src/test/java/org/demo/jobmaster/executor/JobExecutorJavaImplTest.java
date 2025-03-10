package org.demo.jobmaster.executor;

import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Java Job Executor Tests")
class JobExecutorJavaImplTest {

    private JobExecutorJavaImpl jobExecutor;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should return Java as runtime")
    void shouldReturnJavaRuntime() {
        // Given
        jobExecutor = new JobExecutorJavaImpl();

        // When
        Runtime result = jobExecutor.getRuntime();

        // Then
        assertEquals(Runtime.JAVA, result);
    }

    @Test
    @DisplayName("Should successfully execute valid Java code")
    void shouldExecuteValidJavaCode() {
        // Given
        jobExecutor = new JobExecutorJavaImpl();
        String sourceCode = "public class Main { public static void main(String[] args) { System.out.println(\"Hello World\"); } }";
        Job job = createJob(sourceCode, 1000L, 256, null);
        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();
        Consumer<String> onSuccess = successResult::set;
        Consumer<String> onError = errorResult::set;

        // When
        jobExecutor.execute(job, 1L, onSuccess, onError);

        // Then
        String normalizedResult = normalizeLineEndings(successResult.get());
        assertEquals("Hello World\n", normalizedResult);
        assertNull(errorResult.get());
    }

    @Test
    @DisplayName("Should handle empty source code")
    void shouldHandleEmptySourceCode() {
        // Given
        jobExecutor = new JobExecutorJavaImpl();
        Job job = createJob("", 1000L, 256, null);
        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        assertNull(successResult.get());
        assertEquals("Source code is empty", errorResult.get());
    }

    @Test
    @DisplayName("Should handle compilation errors")
    void shouldHandleCompilationErrors() {
        // Given
        jobExecutor = new JobExecutorJavaImpl();
        String sourceCode = "public class Main { public static void main(String[] args) { System.out.println(\"Hello World\" } }";
        Job job = createJob(sourceCode, 1000L, 256, null);
        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        assertNull(successResult.get());
        assertTrue(errorResult.get().contains("Compilation failed"));
    }

    @Test
    @DisplayName("Should handle runtime errors")
    void shouldHandleRuntimeErrors() {
        // Given
        jobExecutor = new JobExecutorJavaImpl();
        String sourceCode = "public class Main { public static void main(String[] args) { throw new RuntimeException(\"Runtime error\"); } }";
        Job job = createJob(sourceCode, 1000L, 256, null);
        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        assertNull(successResult.get());
        assertTrue(errorResult.get().contains("Execution failed"));
    }

    @Test
    @DisplayName("Should handle timeouts")
    void shouldHandleTimeouts() {
        // Given
        jobExecutor = new JobExecutorJavaImpl();
        String codeRaw = "public class Main { public static void main(String[] args) throws InterruptedException { Thread.sleep(5000); } }";
        Job job = createJob(codeRaw, 100L, 256, null);
        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        assertNull(successResult.get());
        assertTrue(errorResult.get().contains("timed out"), "Expected timeout error, but got: " + errorResult.get());
    }

    @Test
    @DisplayName("Should handle input parameters")
    void shouldHandleInputParameters() {
        // Given
        jobExecutor = new JobExecutorJavaImpl();

        String codeRaw = "public class Main { " +
                "   public static void main(String[] args) { " +
                "       for (String arg : args) {" +
                "           System.out.println(\"Hello, \" + arg);" +
                "       }" +
                "   } " +
                "}";
        Job job = createJob(codeRaw, 1000L, 256, List.of("John"));

        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        String normalizedResult = normalizeLineEndings(successResult.get());
        assertEquals("Hello, John\n", normalizedResult);
        assertNull(errorResult.get());
    }

    @Test
    @DisplayName("Should fail when memory is exceeded (OOM)")
    void shouldFailWhenMemoryExceeded() {
        // Given
        jobExecutor = new JobExecutorJavaImpl();
        String codeRaw = "public class Main {" +
                "   public static void main(String[] args) {" +
                "       try {" +
                "           byte[][] memory = new byte[1024][1024*1024];" + // Try to allocate ~1 TB of memory
                "           System.out.println(\"Allocated memory: \" + memory.length);" +
                "       } catch (OutOfMemoryError e) {" +
                "           System.err.println(\"OOM: \" + e.getMessage());" +
                "           System.exit(1);" +  // Ensure the process exits with error
                "       }" +
                "   }" +
                "}";
        Job job = createJob(codeRaw, 1000L, 10, null);

        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        assertNull(successResult.get());
        assertTrue(errorResult.get().contains("Execution failed"),
                "Expected OOM error, but got: " + errorResult.get());
        assertTrue(errorResult.get().toLowerCase().contains("memory") ||
                        errorResult.get().contains("OOM"),
                "Error should mention memory issues");
    }

    @Test
    @DisplayName("Should clean up temp directory after execution")
    void shouldCleanupTempDirectory() throws Exception {
        // Given
        jobExecutor = new JobExecutorJavaImpl();
        Job job = createJob("public class Main { public static void main(String[] args) { System.out.println(\"Hello\"); } }", 1000L, 256, null);

        // When
        jobExecutor.execute(job, 1L, s -> {
        }, s -> {
        });

        // Then
        Path systemTempDir = tempDir.getParent();
        try (Stream<Path> list = Files.list(systemTempDir)) {
            long jobDirs =
                    list.filter(p -> p.getFileName().toString().startsWith("job_" + job.getId()))
                            .count();
            assertEquals(0, jobDirs, "No temp directories for this job should remain");
        }
    }

    private Job createJob(String sourceCode, long timeout, int memorySize, List<String> codeInput) {
        return Job.builder()
                .id(1L)
                .codeType(Job.CodeType.RAW)
                .codeRaw(sourceCode)
                .timeout(timeout)
                .memorySize(memorySize)
                .runtime(Runtime.JAVA)
                .active(true)
                .name("Test Job")
                .codeInput(codeInput)
                .build();
    }

    /**
     * Normalizes line endings to LF (\n) for consistent comparison across platforms
     */
    private String normalizeLineEndings(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\r\n", "\n");
    }
}