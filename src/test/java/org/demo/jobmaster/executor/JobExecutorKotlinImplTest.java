package org.demo.jobmaster.executor;

import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Kotlin Job Executor Tests")
class JobExecutorKotlinImplTest {

    private JobExecutorKotlinImpl jobExecutor;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should return Kotlin as runtime")
    void shouldReturnKotlinRuntime() {
        // Given
        jobExecutor = new JobExecutorKotlinImpl();

        // When
        Runtime result = jobExecutor.getRuntime();

        // Then
        assertEquals(Runtime.KOTLIN, result);
    }

    @Test
    @DisplayName("Should successfully execute valid Kotlin code")
    void shouldExecuteValidKotlinCode() {
        // Given
        jobExecutor = new JobExecutorKotlinImpl();
        String sourceCode = "class Main { companion object { @JvmStatic fun main(args: Array<String>) { println(\"Hello World\") } } }";
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
        jobExecutor = new JobExecutorKotlinImpl();
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
        jobExecutor = new JobExecutorKotlinImpl();
        String sourceCode = "class Main { companion object { @JvmStatic fun main(args: Array<String>) { println(\"Hello World\" } } }"; // Missing closing parenthesis
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
        jobExecutor = new JobExecutorKotlinImpl();
        String sourceCode = "class Main { companion object { @JvmStatic fun main(args: Array<String>) { throw RuntimeException(\"Runtime error\") } } }";
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
        jobExecutor = new JobExecutorKotlinImpl();
        String codeRaw = "class Main { companion object { @JvmStatic fun main(args: Array<String>) { Thread.sleep(5000) } } }";
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
        jobExecutor = new JobExecutorKotlinImpl();
        Map<String, String> inputs = new HashMap<>();
        inputs.put("name", "John");

        // Fixed: Using correct Kotlin split syntax with named parameters
        String codeRaw = "class Main { companion object { \n" +
                "   @JvmStatic fun main(args: Array<String>) { \n" +
                "       for (arg in args) { \n" +
                "           val parts = arg.split(\"=\", limit = 2) \n" + // Fixed: added 'limit =' parameter name
                "           if (parts.size == 2 && parts[0] == \"name\") { \n" +
                "               println(\"Hello, ${parts[1]}\") \n" +
                "           } \n" +
                "       } \n" +
                "   } \n" +
                "}}";
        Job job = createJob(codeRaw, 1000L, 256, inputs);

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
    @DisplayName("Should handle multiple input parameters")
    void shouldHandleMultipleInputParameters() {
        // Given
        jobExecutor = new JobExecutorKotlinImpl();
        Map<String, String> inputs = new HashMap<>();
        inputs.put("firstName", "John");
        inputs.put("lastName", "Doe");

        String codeRaw = """
                class Main { companion object {\s
                   @JvmStatic fun main(args: Array<String>) {\s
                       var firstName = ""
                       var lastName = ""
                       for (arg in args) {\s
                           val parts = arg.split("=", limit = 2)\s
                           if (parts.size == 2) {\s
                               when (parts[0]) {
                                   "firstName" -> firstName = parts[1]
                                   "lastName" -> lastName = parts[1]
                               }
                           }\s
                       }\s
                       println("Hello, $firstName $lastName")
                   }\s
                }}""";
        Job job = createJob(codeRaw, 1000L, 256, inputs);

        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        String normalizedResult = normalizeLineEndings(successResult.get());
        assertEquals("Hello, John Doe\n", normalizedResult);
        assertNull(errorResult.get());
    }

    @Test
    @DisplayName("Should fail when memory is exceeded (OOM)")
    void shouldFailWhenMemoryExceeded() {
        // Given
        jobExecutor = new JobExecutorKotlinImpl();
        String codeRaw = "class Main { companion object { \n" +
                "   @JvmStatic fun main(args: Array<String>) { \n" +
                "       try { \n" +
                "           val memory = Array(1024) { ByteArray(1024 * 1024) } \n" + // Try to allocate ~1 TB
                "           println(\"Allocated memory: ${memory.size}\") \n" +
                "       } catch (e: OutOfMemoryError) { \n" +
                "           System.err.println(\"OOM: ${e.message}\") \n" +
                "           System.exit(1) \n" +  // Ensure the process exits with error
                "       } \n" +
                "   } \n" +
                "}}";
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
    @DisplayName("Should support System.out.println syntax")
    void shouldSupportSystemOutPrintln() {
        // Given
        jobExecutor = new JobExecutorKotlinImpl();
        String sourceCode = "class Main { companion object { @JvmStatic fun main(args: Array<String>) { System.out.println(\"Hello System.out\") } } }";
        Job job = createJob(sourceCode, 1000L, 256, null);
        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        String normalizedResult = normalizeLineEndings(successResult.get());
        assertEquals("Hello System.out\n", normalizedResult);
        assertNull(errorResult.get());
    }

    @Test
    @DisplayName("Should clean up temp directory after execution")
    void shouldCleanupTempDirectory() throws Exception {
        // Given
        jobExecutor = new JobExecutorKotlinImpl();
        Job job = createJob("class Main { companion object { @JvmStatic fun main(args: Array<String>) { println(\"Hello\") } } }", 1000L, 256, null);

        // When
        jobExecutor.execute(job, 1L, s -> {
        }, s -> {
        });

        // Then
        Thread.sleep(100);
        Path systemTempDir = tempDir.getParent();
        try (Stream<Path> list = Files.list(systemTempDir)) {
            long jobDirs =
                    list.filter(p -> p.getFileName().toString().startsWith("job_" + job.getId()))
                            .count();
            assertEquals(0, jobDirs, "No temp directories for this job should remain");
        }
    }

    @Test
    @DisplayName("Should support multi-line Kotlin code and String templates")
    void shouldSupportMultilineCodeAndTemplates() {
        // Given
        jobExecutor = new JobExecutorKotlinImpl();
        String sourceCode =
                """
                        class Main {
                          companion object {
                            @JvmStatic fun main(args: Array<String>) {
                              val name = "Kotlin"
                              val version = "1.9"
                              println("Hello $name $version!")
                            }
                          }
                        }""";
        Job job = createJob(sourceCode, 1000L, 256, null);
        AtomicReference<String> successResult = new AtomicReference<>();
        AtomicReference<String> errorResult = new AtomicReference<>();

        // When
        jobExecutor.execute(job, 1L, successResult::set, errorResult::set);

        // Then
        String normalizedResult = normalizeLineEndings(successResult.get());
        assertEquals("Hello Kotlin 1.9!\n", normalizedResult);
        assertNull(errorResult.get());
    }

    private Job createJob(String sourceCode, long timeout, int memorySize, Map<String, String> codeInput) {
        return Job.builder()
                .id(1L)
                .codeType(Job.CodeType.RAW)
                .codeRaw(sourceCode)
                .timeout(timeout)
                .memorySize(memorySize)
                .runtime(Runtime.KOTLIN)
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