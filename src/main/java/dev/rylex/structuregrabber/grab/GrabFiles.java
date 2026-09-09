package dev.rylex.structuregrabber.grab;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class GrabFiles {

    public static final String EXTENSION = ".snbt";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final String FALLBACK_NAME = "grab";

    private GrabFiles() {}

    public static Path directory(Path gameDirectory, String configured) {
        try {
            Path path = Paths.get(configured);
            return path.isAbsolute()
                    ? path.normalize()
                    : gameDirectory.resolve(path).normalize();
        } catch (InvalidPathException e) {
            return gameDirectory.resolve(FALLBACK_NAME);
        }
    }

    public static String timestampedName(LocalDateTime moment) {
        return FALLBACK_NAME + "_" + STAMP.format(moment);
    }

    public static String sanitize(String name) {
        String clean =
                name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_").replaceAll("^[._]+|[._]+$", "");
        return clean.isEmpty() ? FALLBACK_NAME : clean;
    }

    public static Path unique(Path directory, String name) {
        Path path = directory.resolve(name + EXTENSION);
        for (int suffix = 1; Files.exists(path); suffix++) {
            path = directory.resolve(name + "_" + suffix + EXTENSION);
        }
        return path;
    }
}
