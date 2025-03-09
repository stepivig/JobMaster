package org.demo.jobmaster.executor;

import lombok.extern.slf4j.Slf4j;
import org.demo.jobmaster.entity.Job;
import org.demo.jobmaster.entity.Runtime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;
import org.springframework.stereotype.Component;
import util.FilesUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Kotlin job executor implementation.
 * This class is responsible for executing Kotlin jobs.
 */
@Slf4j
@Component
public class JobExecutorKotlinImpl implements JobExecutor {

    private static final String CLASS_NAME = "Main";
    private static final String KOTLIN_FILE_NAME = CLASS_NAME + ".kt";
    private final String classPath;

    public JobExecutorKotlinImpl() {
        this.classPath = System.getProperty("java.class.path");
    }

    @Override
    public Runtime getRuntime() {
        return Runtime.KOTLIN;
    }

    @Override
    public void execute(Job job, Long executionId, Consumer<String> onSuccess, Consumer<String> onError) {
        if (job.getCodeType() == Job.CodeType.RAW) {
            processRawCode(job, executionId, onSuccess, onError);
        } else {
            log.error("Unsupported code type: {}", job.getCodeType());
        }
    }

    private void processRawCode(Job job, Long executionId, Consumer<String> onSuccess, Consumer<String> onError) {
        if (job.getCodeRaw() == null || job.getCodeRaw().trim().isEmpty()) {
            onError.accept("Source code is empty");
            return;
        }
        Path sourceDirPath = null;
        Path outputDirPath = null;
        try {
            sourceDirPath = Files.createTempDirectory("job_" + job.getId() + "_execution_" + executionId + "_source");
            outputDirPath = Files.createTempDirectory("job_" + job.getId() + "_execution_" + executionId + "_output");

            File sourceDir = sourceDirPath.toFile();
            File outputDir = outputDirPath.toFile();

            File kotlinFile = new File(sourceDir, KOTLIN_FILE_NAME);
            try (FileWriter writer = new FileWriter(kotlinFile)) {
                writer.write(job.getCodeRaw());
            }

            K2JVMCompilerArguments compilerArgs = constructCompilerArgs(outputDir, kotlinFile);
            K2JVMCompiler compiler = new K2JVMCompiler();
            ArrayList<String> errors = new ArrayList<>();
            ExitCode exitCode = compiler.exec(getMessageCollector(errors), Services.EMPTY, compilerArgs);

            if (exitCode != ExitCode.OK) {
                onError.accept("Compilation failed:" + String.join(", ", errors));
                return;
            }

            ProcessBuilder processBuilder = buildProcessBuilder(outputDir, job);
            Process process = processBuilder.start();

            boolean completed = process.waitFor(job.getTimeout(), TimeUnit.MILLISECONDS);

            if (!completed) {
                process.destroyForcibly();
                onError.accept("Execution timed out");
            } else {
                int exitValue = process.exitValue();
                if (exitValue != 0) {
                    try (InputStream errorStream = process.getErrorStream()) {
                        String stderr = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        onError.accept("Execution failed with exit code " + exitValue + ":\n" + stderr);
                    }
                } else {
                    try (InputStream stream = process.getInputStream()) {
                        onSuccess.accept(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error executing Kotlin job", e);
            onError.accept("An error occurred: " + e.getMessage());
        } finally {
            FilesUtils.deleteDirectoryRecursively(sourceDirPath);
            FilesUtils.deleteDirectoryRecursively(outputDirPath);
        }
    }

    private @NotNull ProcessBuilder buildProcessBuilder(File outputDir, Job job) {
        List<String> command = constructCommand(outputDir.toPath(), job);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(outputDir);
        return processBuilder;
    }

    private static @NotNull MessageCollector getMessageCollector(List<String> errors) {
        return new MessageCollector() {

            @Override
            public void clear() {

            }

            @Override
            public void report(@NotNull CompilerMessageSeverity severity,
                               @NotNull String message,
                               CompilerMessageSourceLocation location) {
                if (severity == CompilerMessageSeverity.ERROR ||
                        severity == CompilerMessageSeverity.EXCEPTION) {
                    errors.add(message);
                    log.error("Kotlin compiler error: {}", message);
                } else {
                    log.debug("Kotlin compiler message: {}", message);
                }
            }

            @Override
            public boolean hasErrors() {
                return !errors.isEmpty();
            }
        };
    }

    private @NotNull K2JVMCompilerArguments constructCompilerArgs(File outputDir, File kotlinFile) {
        K2JVMCompilerArguments compilerArgs = new K2JVMCompilerArguments();
        compilerArgs.setDestination(outputDir.getAbsolutePath());
        compilerArgs.setFreeArgs(Collections.singletonList(kotlinFile.getAbsolutePath()));
        compilerArgs.setSuppressWarnings(true);
        compilerArgs.setJvmTarget(String.valueOf(java.lang.Runtime.version().feature()));
        compilerArgs.setClasspath(classPath);
        compilerArgs.setVerbose(false);
        compilerArgs.setNoReflect(true);
        compilerArgs.setReportPerf(false);
        return compilerArgs;
    }

    private List<String> constructCommand(Path directory, Job job) {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx" + job.getMemorySize() + "M");
        command.add("-Xms" + job.getMemorySize() + "M");
        command.add("-cp");
        if (classPath != null && !classPath.isEmpty()) {
            command.add(directory.toString() + File.pathSeparator + classPath);
        } else {
            command.add(directory.toString());
        }
        command.add(CLASS_NAME);
        Map<String, String> inputs = job.getCodeInput();
        if (inputs != null && !inputs.isEmpty()) {
            for (Map.Entry<String, String> entry : inputs.entrySet()) {
                command.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return command;
    }

}