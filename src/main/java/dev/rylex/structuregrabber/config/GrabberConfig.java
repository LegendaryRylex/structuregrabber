package dev.rylex.structuregrabber.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GrabberConfig {

    private GrabberConfig() {}

    public static final ModConfigSpec SPEC;

    private static final String DEFAULT_OUTPUT_DIRECTORY = "structuregrabber";
    private static final boolean DEFAULT_INCLUDE_ENTITIES = false;
    private static final boolean DEFAULT_IGNORE_AIR = false;
    private static final int DEFAULT_MAX_BLOCKS = 262144;

    private static final ModConfigSpec.ConfigValue<String> OUTPUT_DIRECTORY;
    private static final ModConfigSpec.BooleanValue INCLUDE_ENTITIES;
    private static final ModConfigSpec.BooleanValue IGNORE_AIR;
    private static final ModConfigSpec.IntValue MAX_BLOCKS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("output");
        OUTPUT_DIRECTORY = builder.comment("Folder the .snbt files are written to, relative to the game directory.")
                .translation("structuregrabber.configuration.output.directory")
                .define(
                        "directory",
                        DEFAULT_OUTPUT_DIRECTORY,
                        value -> value instanceof String path && !path.isBlank());
        builder.pop();
        builder.push("capture");
        INCLUDE_ENTITIES = builder.comment("Save the entities standing in the region alongside its blocks.")
                .translation("structuregrabber.configuration.capture.include_entities")
                .define("include_entities", DEFAULT_INCLUDE_ENTITIES);
        IGNORE_AIR = builder.comment(
                        "Leave air out of the saved structure, so placing it does not clear what is already there. Structure voids are saved as ordinary blocks while this is on.")
                .translation("structuregrabber.configuration.capture.ignore_air")
                .define("ignore_air", DEFAULT_IGNORE_AIR);
        MAX_BLOCKS = builder.comment("Largest region the command will grab, counted in blocks.")
                .translation("structuregrabber.configuration.capture.max_blocks")
                .defineInRange("max_blocks", DEFAULT_MAX_BLOCKS, 1, 16777216);
        builder.pop();
        SPEC = builder.build();
    }

    public static String outputDirectory() {
        return SPEC.isLoaded() ? OUTPUT_DIRECTORY.get() : DEFAULT_OUTPUT_DIRECTORY;
    }

    public static boolean includeEntities() {
        return SPEC.isLoaded() ? INCLUDE_ENTITIES.get() : DEFAULT_INCLUDE_ENTITIES;
    }

    public static boolean ignoreAir() {
        return SPEC.isLoaded() ? IGNORE_AIR.get() : DEFAULT_IGNORE_AIR;
    }

    public static int maxBlocks() {
        return SPEC.isLoaded() ? MAX_BLOCKS.get() : DEFAULT_MAX_BLOCKS;
    }
}
