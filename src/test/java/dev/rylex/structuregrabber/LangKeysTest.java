package dev.rylex.structuregrabber;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LangKeysTest {

    private static final Path LANG =
            Paths.get("src", "main", "resources", "assets", StructureGrabber.MOD_ID, "lang", "en_us.json");
    private static final Path SOURCE = Paths.get("src", "main", "java");

    private static final Pattern LITERAL = Pattern.compile("\"([^\"\\\\\\n]+)\"");
    private static final Pattern KEY_SHAPED = Pattern.compile("[a-z][A-Za-z]*(\\.[A-Za-z0-9_]+)+");

    private static JsonObject lang() {
        return JsonParser.parseString(ProjectFiles.read(LANG)).getAsJsonObject();
    }

    private static List<Path> sources() {
        try (Stream<Path> files = Files.walk(ProjectFiles.root().resolve(SOURCE))) {
            return files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void everyTranslationKeyInTheSourceExists() {
        JsonObject lang = lang();
        List<String> keys = lang.keySet().stream().toList();
        List<String> missing = new ArrayList<>();

        for (Path source : sources()) {
            Matcher literals = LITERAL.matcher(read(source));
            while (literals.find()) {
                String candidate = literals.group(1);
                if (!candidate.contains(StructureGrabber.MOD_ID)
                        || candidate.startsWith("dev.rylex")
                        || !KEY_SHAPED.matcher(candidate).matches()) {
                    continue;
                }
                boolean known = lang.has(candidate) || keys.stream().anyMatch(key -> key.startsWith(candidate + "."));
                if (!known) {
                    missing.add(candidate + " (" + ProjectFiles.root().relativize(source) + ")");
                }
            }
        }

        assertTrue(
                missing.isEmpty(),
                "these keys would render raw in game because en_us.json does not define them:\n  "
                        + String.join("\n  ", missing));
    }
}
