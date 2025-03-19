package org.demo.jobmaster.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;
import org.demo.jobmaster.util.FilesUtils;
import org.springframework.stereotype.Component;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Java job executor implementation.
 * This class is responsible for executing Java jobs.
 */
@Slf4j
@Component
public class JobExecutorJavaImpl implements JobExecutor {

    private static final String CLASS_NAME = "Main";

    @Override
    public Runtime getRuntime() {
        return Runtime.JAVA;
    }

    @Override
    public void execute(Job job, Long executionId, Consumer<String> onSuccess, Consumer<String> onError) {
        if (job.getCodeType() == Job.CodeType.RAW) {
            processRawCode(job, executionId, onSuccess, onError);
        } else {
            log.error("Unsupported code type: {}", job.getCodeType());
            onError.accept("Unsupported code type: " + job.getCodeType());
        }
    }

    private void processRawCode(Job job, Long executionId, Consumer<String> onSuccess, Consumer<String> onError) {
        String sourceCode = job.getCodeRaw();
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            onError.accept("Source code is empty");
            return;
        }
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("job_" + job.getId() + "_" + "execution_" + executionId);
            Result compilationResult = compileCode(sourceCode, tempDir);
            if (!compilationResult.success()) {
                onError.accept("Compilation failed: " + compilationResult.output());
                return;
            }

            Result result = execute(tempDir, job.getMemorySize(), job.getTimeout(), job.getCodeInput());
            if (result.success()) {
                onSuccess.accept(result.output());
            } else {
                onError.accept("Execution failed: " + result.output());
            }
        } catch (Exception e) {
            log.error("Error executing Java job", e);
            onError.accept("Execution error: " + e.getMessage());
        } finally {
            FilesUtils.deleteDirectoryRecursively(tempDir);
        }
    }

    /**
     * Compiles the Java source code and returns any compilation errors
     */
    private Result compileCode(String sourceCode, Path directory) {
        Path sourceFilePath = directory.resolve(CLASS_NAME + ".java");
        try {
            Files.writeString(sourceFilePath, sourceCode);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return new Result(false, "Java compiler not found");
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 ByteArrayOutputStream errorStream = new ByteArrayOutputStream()) {
                int result = compiler.run(null, outputStream, errorStream, sourceFilePath.toString());
                if (result != 0) {
                    String errorMessage = errorStream.toString(StandardCharsets.UTF_8);
                    return new Result(false, "Compilation failed:\n" + errorMessage);
                }
            }
            return new Result(true, null);
        } catch (IOException e) {
            log.error("Error writing source file", e);
            return new Result(false, "Error writing source file: " + e.getMessage());
        }
    }

    private Result execute(Path directory,
                           int memoryMB,
                           long timeoutMillis,
                           List<String> inputs) throws IOException, InterruptedException {
        List<String> command = constructCommand(directory, memoryMB, inputs);


        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(directory.toFile());

        Process process = processBuilder.start();

        boolean completed = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);

        if (!completed) {
            process.destroyForcibly();
            return new Result(false, "Execution timed out after " + timeoutMillis + " ms");
        }

        int exitCode = process.exitValue();

        if (exitCode != 0) {
            try (InputStream errorStream = process.getErrorStream()) {
                String stderr = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                return new Result(false, "Execution failed with exit code " + exitCode + ":\n" + stderr);
            }
        } else {
            try (InputStream stream = process.getInputStream()) {
                return new Result(true, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    private List<String> constructCommand(Path directory, int memoryMB, List<String> inputs) {
        List<String> command = new ArrayList<>(6);
        command.add("java");
        command.add("-Xmx" + memoryMB + "M");
        command.add("-Xms" + memoryMB + "M");
        command.add("-cp");
        command.add(directory.toString());
        command.add(CLASS_NAME);
        if (inputs != null && !inputs.isEmpty()) {
            command.addAll(inputs);
        }
        return command;
    }

    private record Result(boolean success, String output) {
    }
}