package dev.rylex.structuregrabber.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.rylex.structuregrabber.StructureGrabber;
import dev.rylex.structuregrabber.config.GrabberConfig;
import dev.rylex.structuregrabber.grab.GrabFiles;
import dev.rylex.structuregrabber.grab.GrabResult;
import dev.rylex.structuregrabber.grab.StructureCapture;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.jetbrains.annotations.Nullable;

final class GrabCommands {

    private GrabCommands() {}

    static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher()
                .register(Commands.literal(StructureGrabber.MOD_ID)
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(context ->
                                                grab(context, corner(context, "from"), corner(context, "to"), null))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(context -> grab(
                                                        context,
                                                        corner(context, "from"),
                                                        corner(context, "to"),
                                                        StringArgumentType.getString(context, "name"))))))
                        .then(Commands.literal("pos1")
                                .executes(context -> setCorner(context, true, here(context)))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> setCorner(context, true, corner(context, "pos")))))
                        .then(Commands.literal("pos2")
                                .executes(context -> setCorner(context, false, here(context)))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> setCorner(context, false, corner(context, "pos")))))
                        .then(Commands.literal("grab")
                                .executes(context -> grabSelection(context, null))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context ->
                                                grabSelection(context, StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("clear").executes(GrabCommands::clear)));
    }

    private static BlockPos corner(CommandContext<CommandSourceStack> context, String argument) {
        return BlockPosArgument.getBlockPos(context, argument);
    }

    private static BlockPos here(CommandContext<CommandSourceStack> context) {
        return BlockPos.containing(context.getSource().getPosition());
    }

    private static int setCorner(CommandContext<CommandSourceStack> context, boolean firstCorner, BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return fail(context, "structuregrabber.command.no_level");
        }
        if (firstCorner) {
            GrabSelection.setFirst(level.dimension(), pos);
        } else {
            GrabSelection.setSecond(level.dimension(), pos);
        }
        succeed(
                context,
                Component.translatable(
                        firstCorner ? "structuregrabber.command.pos1" : "structuregrabber.command.pos2",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()));
        BlockPos first = GrabSelection.first();
        BlockPos second = GrabSelection.second();
        if (first != null && second != null) {
            Vec3i size = size(first, second);
            succeed(
                    context,
                    Component.translatable(
                            "structuregrabber.command.region",
                            size.getX(),
                            size.getY(),
                            size.getZ(),
                            (long) size.getX() * size.getY() * size.getZ()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        GrabSelection.clear();
        succeed(context, Component.translatable("structuregrabber.command.cleared"));
        return Command.SINGLE_SUCCESS;
    }

    private static int grabSelection(CommandContext<CommandSourceStack> context, @Nullable String name) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return fail(context, "structuregrabber.command.no_level");
        }
        BlockPos first = GrabSelection.first();
        BlockPos second = GrabSelection.second();
        if (first == null || second == null) {
            return fail(context, "structuregrabber.command.no_selection");
        }
        if (!GrabSelection.inDimension(level.dimension())) {
            return fail(context, "structuregrabber.command.wrong_dimension");
        }
        return grab(context, first, second, name);
    }

    private static int grab(
            CommandContext<CommandSourceStack> context, BlockPos from, BlockPos to, @Nullable String name) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return fail(context, "structuregrabber.command.no_level");
        }

        BlockPos min = new BlockPos(
                Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
        if (min.getY() < level.getMinBuildHeight() || Math.max(from.getY(), to.getY()) >= level.getMaxBuildHeight()) {
            context.getSource()
                    .sendFailure(Component.translatable(
                            "structuregrabber.command.out_of_bounds",
                            level.getMinBuildHeight(),
                            level.getMaxBuildHeight() - 1));
            return 0;
        }

        Vec3i size = size(from, to);
        long volume = (long) size.getX() * size.getY() * size.getZ();
        int limit = GrabberConfig.maxBlocks();
        if (volume > limit) {
            context.getSource().sendFailure(Component.translatable("structuregrabber.command.too_big", volume, limit));
            return 0;
        }

        IntegratedServer server = minecraft.getSingleplayerServer();
        ServerLevel hosted = server == null ? null : server.getLevel(level.dimension());
        boolean host = server != null && hosted != null;
        if (!host) {
            ChunkPos missing = firstUnloadedChunk(level, min, size);
            if (missing != null) {
                context.getSource()
                        .sendFailure(Component.translatable("structuregrabber.command.unloaded", missing.x, missing.z));
                return 0;
            }
            succeed(
                    context,
                    Component.translatable("structuregrabber.command.client_data")
                            .withStyle(ChatFormatting.YELLOW));
        }

        Level source = host ? hosted : level;
        Path directory = GrabFiles.directory(minecraft.gameDirectory.toPath(), GrabberConfig.outputDirectory());
        String fileName = GrabFiles.sanitize(name == null ? GrabFiles.timestampedName(LocalDateTime.now()) : name);
        boolean entities = GrabberConfig.includeEntities();
        boolean ignoreAir = GrabberConfig.ignoreAir();
        Runnable capture = () -> {
            CompoundTag structure = StructureCapture.capture(source, min, size, entities, ignoreAir);
            Util.ioPool().execute(() -> save(minecraft, directory, fileName, structure, size));
        };
        if (host) {
            server.execute(capture);
        } else {
            capture.run();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void save(Minecraft minecraft, Path directory, String name, CompoundTag structure, Vec3i size) {
        try {
            GrabResult result = StructureCapture.write(directory, name, structure);
            minecraft.execute(() -> tell(minecraft, grabbed(result, size)));
        } catch (IOException e) {
            StructureGrabber.LOGGER.error("Could not write the grabbed structure to {}", directory, e);
            minecraft.execute(() -> tell(
                    minecraft,
                    Component.translatable("structuregrabber.command.failed").withStyle(ChatFormatting.RED)));
        }
    }

    private static Component grabbed(GrabResult result, Vec3i size) {
        Component link = Component.literal(result.file().getFileName().toString())
                .withStyle(style -> style.withUnderlined(true)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.OPEN_FILE,
                                result.file().toAbsolutePath().toString()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT, Component.translatable("structuregrabber.command.open"))));
        return Component.translatable(
                "structuregrabber.command.grabbed",
                size.getX(),
                size.getY(),
                size.getZ(),
                link,
                result.blocks(),
                result.entities());
    }

    private static void tell(Minecraft minecraft, Component message) {
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(message);
        }
    }

    private static Vec3i size(BlockPos from, BlockPos to) {
        return new Vec3i(
                Math.abs(from.getX() - to.getX()) + 1,
                Math.abs(from.getY() - to.getY()) + 1,
                Math.abs(from.getZ() - to.getZ()) + 1);
    }

    @Nullable
    private static ChunkPos firstUnloadedChunk(ClientLevel level, BlockPos min, Vec3i size) {
        int lastX = SectionPos.blockToSectionCoord(min.getX() + size.getX() - 1);
        int lastZ = SectionPos.blockToSectionCoord(min.getZ() + size.getZ() - 1);
        for (int x = SectionPos.blockToSectionCoord(min.getX()); x <= lastX; x++) {
            for (int z = SectionPos.blockToSectionCoord(min.getZ()); z <= lastZ; z++) {
                if (!level.getChunkSource().hasChunk(x, z)) {
                    return new ChunkPos(x, z);
                }
            }
        }
        return null;
    }

    private static void succeed(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendSuccess(() -> message, false);
    }

    private static int fail(CommandContext<CommandSourceStack> context, String key) {
        context.getSource().sendFailure(Component.translatable(key));
        return 0;
    }
}
