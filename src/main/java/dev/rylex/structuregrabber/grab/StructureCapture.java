package dev.rylex.structuregrabber.grab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class StructureCapture {

    private StructureCapture() {}

    public static CompoundTag capture(Level level, BlockPos origin, Vec3i size, boolean entities, boolean ignoreAir) {
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, origin, size, entities, List.of(ignoreAir ? Blocks.AIR : Blocks.STRUCTURE_VOID));
        return NbtUtils.addCurrentDataVersion(template.save(new CompoundTag()));
    }

    /** Consumes the tag: {@link NbtUtils#structureToSnbt} rewrites it in place into its own shape. */
    public static GrabResult write(Path directory, String name, CompoundTag structure) throws IOException {
        int blocks = structure.getListOrEmpty("blocks").size();
        int entities = structure.getListOrEmpty("entities").size();
        structure.put("count", countBlocks(structure));
        Files.createDirectories(directory);
        Path file = GrabFiles.unique(directory, name);
        Files.writeString(file, NbtUtils.structureToSnbt(structure) + "\n", StandardCharsets.UTF_8);
        return new GrabResult(file, blocks, entities);
    }

    static CompoundTag countBlocks(CompoundTag structure) {
        ListTag palette = structure.getListOrEmpty("palette");
        Map<String, Integer> counts = new TreeMap<>();
        for (Tag block : structure.getListOrEmpty("blocks")) {
            String name = palette.getCompoundOrEmpty(((CompoundTag) block).getIntOr("state", 0))
                    .getStringOr("Name", "");
            counts.merge(name, 1, Integer::sum);
        }
        CompoundTag count = new CompoundTag();
        counts.forEach(count::putInt);
        return count;
    }
}
