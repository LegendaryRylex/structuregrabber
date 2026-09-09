package dev.rylex.structuregrabber;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

final class ProjectFiles {

    private ProjectFiles() {}

    static Path root() {
        String projectDir = System.getProperty("structuregrabber.projectDir");
        if (projectDir != null && Files.isDirectory(Paths.get(projectDir, "src", "main"))) {
            return Paths.get(projectDir).toAbsolutePath().normalize();
        }
        for (Path candidate : List.of(Paths.get(""), Paths.get("structuregrabber"))) {
            if (Files.isDirectory(candidate.resolve(Paths.get("src", "main")))) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException(
                "Could not locate the project root from " + Paths.get("").toAbsolutePath());
    }

    static String read(Path relative) {
        try {
            return Files.readString(root().resolve(relative));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
