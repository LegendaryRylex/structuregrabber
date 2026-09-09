package dev.rylex.structuregrabber.grab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class StructureCapture {

    private StructureCapture() {}

    public static CompoundTag capture(Level level, BlockPos origin, Vec3i size, boolean entities, boolean ignoreAir) {
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, origin, size, entities, ignoreAir ? Blocks.AIR : Blocks.STRUCTURE_VOID);
        return NbtUtils.addCurrentDataVersion(template.save(new CompoundTag()));
    }

    /** Consumes the tag: {@link NbtUtils#structureToSnbt} rewrites it in place into its own shape. */
    public static GrabResult write(Path directory, String name, CompoundTag structure) throws IOException {
        int blocks = structure.getList("blocks", Tag.TAG_COMPOUND).size();
        int entities = structure.getList("entities", Tag.TAG_COMPOUND).size();
        Files.createDirectories(directory);
        Path file = GrabFiles.unique(directory, name);
        Files.writeString(file, NbtUtils.structureToSnbt(structure) + "\n", StandardCharsets.UTF_8);
        return new GrabResult(file, blocks, entities);
    }
}
