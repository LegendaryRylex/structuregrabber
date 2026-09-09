package dev.rylex.structuregrabber.grab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrabFilesTest {

    @Test
    void relativeDirectoriesResolveAgainstTheGameDirectory() {
        Path gameDirectory = Paths.get("").toAbsolutePath();
        assertEquals(gameDirectory.resolve("structuregrabber"), GrabFiles.directory(gameDirectory, "structuregrabber"));
    }

    @Test
    void absoluteDirectoriesAreKept() {
        Path gameDirectory = Paths.get("").toAbsolutePath();
        Path absolute = gameDirectory.resolve("elsewhere").toAbsolutePath();
        assertEquals(absolute, GrabFiles.directory(gameDirectory, absolute.toString()));
    }

    @Test
    void namesAreReducedToResourceLocationCharacters() {
        assertEquals("my_base", GrabFiles.sanitize("My Base"));
        assertEquals("tower-2.a", GrabFiles.sanitize("Tower-2.a"));
        assertEquals("grab", GrabFiles.sanitize("***"));
        assertEquals("grab", GrabFiles.sanitize(""));
    }

    @Test
    void timestampedNamesSurviveSanitising() {
        String name = GrabFiles.timestampedName(LocalDateTime.of(2026, 9, 9, 14, 5, 3));
        assertEquals("grab_2026-09-09_14.05.03", name);
        assertEquals(name, GrabFiles.sanitize(name));
    }

    @Test
    void anExistingFileIsNeverOverwritten(@TempDir Path directory) throws IOException {
        assertEquals(directory.resolve("base.snbt"), GrabFiles.unique(directory, "base"));

        Files.createFile(directory.resolve("base.snbt"));
        assertEquals(directory.resolve("base_1.snbt"), GrabFiles.unique(directory, "base"));

        Files.createFile(directory.resolve("base_1.snbt"));
        assertEquals(directory.resolve("base_2.snbt"), GrabFiles.unique(directory, "base"));
        assertTrue(Files.exists(directory.resolve("base.snbt")));
    }
}
