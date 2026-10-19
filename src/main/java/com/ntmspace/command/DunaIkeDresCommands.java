package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ntmspace.NtmSpace;
import com.ntmspace.world.DresChunkGenerator;
import com.ntmspace.world.DunaChunkGenerator;
import com.ntmspace.world.IkeChunkGenerator;
import com.ntmspace.world.LayeredChunkGenerator;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.world.Teleports;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /ntmspace tp <body>} and {@code /ntmspace check <body>} for Duna, Ike and Dres (Brigadier merges this tree with
 * the Mun's in {@link SpaceCommands} and the other bodies' under {@code /ntmspace}). Operators only.
 *
 * <p>{@code check} compares the chunk the player stands in with the generator's 1.7 primer, block state for block state
 * (a body ore's body, a sellafite shade and a terracotta colour included), and logs {@code <body>_check OK} or the first
 * difference (the harness reads the log). Blocks a player changed differ, Duna sand that fell differs, and so do chunks
 * generated in an earlier game session where the body has session randomness (Duna's plateau steps and surface picks,
 * Dres's sellafite shades, as in 1.7), so run it on terrain generated in this session.
 */
public final class DunaIkeDresCommands {

    private DunaIkeDresCommands() {}

    public static void init() {
        CompatCommands.server(DunaIkeDresCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> check = Commands.literal("check");
        LiteralArgumentBuilder<CommandSourceStack> tp = Commands.literal("tp");
        body(check, tp, "duna", SpaceDimensions.DUNA);
        body(check, tp, "ike", SpaceDimensions.IKE);
        body(check, tp, "dres", SpaceDimensions.DRES);
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(check)
                .then(tp));
    }

    private static void body(LiteralArgumentBuilder<CommandSourceStack> check, LiteralArgumentBuilder<CommandSourceStack> tp,
                             String name, ResourceKey<Level> dimension) {
        check.then(Commands.literal(name).executes(context -> check(context.getSource(), name, dimension)));
        tp.then(Commands.literal(name).executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = context.getSource().getServer().getLevel(dimension);
            if (level == null) {
                context.getSource().sendFailure(Component.literal("The " + name + " dimension is not loaded"));
                return 0;
            }
            BlockPos column = BlockPos.containing(player.getX(), 0, player.getZ());
            int y = level.getChunk(column).getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX() & 15, column.getZ() & 15) + 1;
            Teleports.toLevel(player, level, player.getX(), y, player.getZ());
            return 1;
        }));
    }

    private static CelestialTerrain17.Primer primer(ChunkGenerator generator, int chunkX, int chunkZ) {
        if (generator instanceof DunaChunkGenerator duna) return duna.primer(chunkX, chunkZ);
        if (generator instanceof DresChunkGenerator dres) return dres.primer(chunkX, chunkZ);
        if (generator instanceof IkeChunkGenerator ike) return ike.primer(chunkX, chunkZ);
        return null;
    }

    private static int check(CommandSourceStack source, String name, ResourceKey<Level> dimension)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        CelestialTerrain17.Primer primer = null;
        int chunkX = player.blockPosition().getX() >> 4;
        int chunkZ = player.blockPosition().getZ() >> 4;
        if (level.dimension().equals(dimension)) primer = primer(level.getChunkSource().getGenerator(), chunkX, chunkZ);
        if (primer == null) {
            source.sendFailure(Component.literal("not on " + name));
            return 0;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int checked = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    int index = (x * 16 + z) * 256 + y;
                    BlockState expected = LayeredChunkGenerator.state(primer.blocks[index], primer.metas[index]);
                    BlockState actual = level.getBlockState(pos.set(chunkX * 16 + x, y, chunkZ * 16 + z));
                    if (actual != expected && !(expected.isAir() && actual.isAir())) {
                        String message = name + "_check MISMATCH chunk " + chunkX + "," + chunkZ + " at " + pos.toShortString()
                                + ": " + actual + ", the 1.7 primer has " + expected;
                        NtmSpace.LOG.info(message);
                        source.sendFailure(Component.literal(message));
                        return 0;
                    }
                    checked++;
                }
            }
        }
        String message = name + "_check OK chunk " + chunkX + "," + chunkZ + ": " + checked + " blocks match the 1.7 primer";
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
