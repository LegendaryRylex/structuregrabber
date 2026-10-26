package dev.rylex.structuregrabber.grab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StructureCaptureTest {

    private static ListTag position(int x, int y, int z) {
        ListTag list = new ListTag();
        list.add(IntTag.valueOf(x));
        list.add(IntTag.valueOf(y));
        list.add(IntTag.valueOf(z));
        return list;
    }

    private static CompoundTag paletteEntry(String name) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Name", name);
        return entry;
    }

    private static CompoundTag block(ListTag pos, int state) {
        CompoundTag block = new CompoundTag();
        block.put("pos", pos);
        block.putInt("state", state);
        return block;
    }

    private static CompoundTag structure() {
        ListTag palette = new ListTag();
        palette.add(paletteEntry("minecraft:stone"));
        palette.add(paletteEntry("minecraft:oak_log"));

        ListTag blocks = new ListTag();
        blocks.add(block(position(0, 0, 0), 0));
        blocks.add(block(position(0, 1, 0), 1));
        blocks.add(block(position(0, 2, 0), 0));

        CompoundTag structure = new CompoundTag();
        structure.put("palette", palette);
        structure.put("blocks", blocks);
        structure.put("entities", new ListTag());
        structure.put("size", position(1, 3, 1));
        return NbtUtils.addCurrentDataVersion(structure);
    }

    @Test
    void theWrittenFileParsesBackIntoTheSameStructure(@TempDir Path directory)
            throws IOException, CommandSyntaxException {
        GrabResult result = StructureCapture.write(directory, "base", structure());

        assertEquals(directory.resolve("base.snbt"), result.file());
        assertEquals(3, result.blocks());
        assertEquals(0, result.entities());

        CompoundTag parsed = NbtUtils.snbtToStructure(Files.readString(result.file()));
        assertEquals(3, parsed.getListOrEmpty("blocks").size());
        assertEquals(
                "minecraft:oak_log",
                parsed.getListOrEmpty("palette").getCompoundOrEmpty(1).getStringOr("Name", ""));
        assertEquals(3, parsed.getListOrEmpty("size").getIntOr(1, 0));
        assertTrue(parsed.contains("DataVersion"));
    }

    @Test
    void theWrittenFileCountsEachBlock(@TempDir Path directory) throws IOException, CommandSyntaxException {
        GrabResult result = StructureCapture.write(directory, "base", structure());

        CompoundTag count =
                NbtUtils.snbtToStructure(Files.readString(result.file())).getCompoundOrEmpty("count");
        assertEquals(2, count.size());
        assertEquals(2, count.getIntOr("minecraft:stone", 0));
        assertEquals(1, count.getIntOr("minecraft:oak_log", 0));
    }

    @Test
    void aSecondGrabOfTheSameNameKeepsTheFirstFile(@TempDir Path directory) throws IOException {
        StructureCapture.write(directory, "base", structure());
        GrabResult second = StructureCapture.write(directory, "base", structure());

        assertEquals(directory.resolve("base_1.snbt"), second.file());
        assertTrue(Files.exists(directory.resolve("base.snbt")));
    }
}
