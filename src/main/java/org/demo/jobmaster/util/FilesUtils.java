package org.demo.jobmaster.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Utility class for file operations.
 */
@Slf4j
public final class FilesUtils {
    private FilesUtils() {
        // Utility class
    }

    /**
     * Deletes a directory and all its contents recursively.
     *
     * @param path the path to the directory to delete
     */
    public static void deleteDirectoryRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.error("Failed to delete file: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk through directory: {}", path, e);
        }
    }
}
