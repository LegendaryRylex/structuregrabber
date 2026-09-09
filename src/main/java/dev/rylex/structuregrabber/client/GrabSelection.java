package dev.rylex.structuregrabber.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class GrabSelection {

    @Nullable
    private static ResourceKey<Level> dimension;

    @Nullable
    private static BlockPos first;

    @Nullable
    private static BlockPos second;

    private GrabSelection() {}

    static void setFirst(ResourceKey<Level> level, BlockPos pos) {
        enter(level);
        first = pos;
    }

    static void setSecond(ResourceKey<Level> level, BlockPos pos) {
        enter(level);
        second = pos;
    }

    @Nullable
    static BlockPos first() {
        return first;
    }

    @Nullable
    static BlockPos second() {
        return second;
    }

    static boolean inDimension(ResourceKey<Level> level) {
        return level.equals(dimension);
    }

    static void clear() {
        dimension = null;
        first = null;
        second = null;
    }

    private static void enter(ResourceKey<Level> level) {
        if (!level.equals(dimension)) {
            clear();
            dimension = level;
        }
    }
}
