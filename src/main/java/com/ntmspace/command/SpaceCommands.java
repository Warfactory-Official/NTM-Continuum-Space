package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.ntmspace.NtmSpace;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.world.Teleports;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /ntmspace tp <body>}: a debug teleport onto a celestial body's surface, standing in for the rockets until
 * they are ported. Operators only.
 */
public final class SpaceCommands {

    private SpaceCommands() {}

    public static void init() {
        CompatCommands.server(SpaceCommands::register);
    }

    /**
     * Compares the chunk the player stands in on the Mun with the generator's 1.7 primer, block for block, and logs
     * {@code mun_check OK} or the first difference (the harness reads the log). hbm-space's populate (ores, clusters,
     * conglomerate layers, sulfur caves, bedrock ores) runs after the primer and only ever replaces the body's stone and
     * bedrock, so a difference is accepted where the primer has moon rock or bedrock and the world now holds a solid
     * non-terrain block; every other difference (air where rock should be, rock in a cave, a changed surface) is a
     * mismatch. The populate is also replayed over the primers for this chunk and its eight neighbours, whose veins
     * reach into it, and the log says how many of the placed blocks the replay predicts: it cannot predict all, since
     * 1.7's bedrock ore drew from the unseeded world random and the neighbours populate in the modern pipeline's order,
     * which changes where a vein finds stone and so the draws after it. Blocks a player changed differ, and so do chunks
     * generated in an earlier game session (hbm-space's noise caves are seeded per session, see MapGenGreg), so run it on
     * terrain generated in this session.
     */
    private static int check(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        if (!(level.getChunkSource().getGenerator() instanceof com.ntmspace.world.MunChunkGenerator generator)) {
            source.sendFailure(Component.literal("not on the Mun"));
            return 0;
        }
        int chunkX = player.blockPosition().getX() >> 4;
        int chunkZ = player.blockPosition().getZ() >> 4;
        com.ntmspace.world.gen.legacy.CelestialTerrain17.Primer primer = generator.primer(chunkX, chunkZ);
        // position -> every block a replayed populate leaves there
        java.util.Map<Long, java.util.Set<net.minecraft.world.level.block.Block>> populated = new java.util.HashMap<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (java.util.Map.Entry<Long, net.minecraft.world.level.block.state.BlockState> entry
                        : generator.populateReplay(chunkX + dx, chunkZ + dz).entrySet()) {
                    long key = entry.getKey();
                    if (com.ntmspace.world.populate.PrimerPopulateWorld.x(key) >> 4 != chunkX
                            || com.ntmspace.world.populate.PrimerPopulateWorld.z(key) >> 4 != chunkZ) continue;
                    populated.computeIfAbsent(key, k -> new java.util.HashSet<>()).add(entry.getValue().getBlock());
                }
            }
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int checked = 0;
        int placed = 0;
        int predicted = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    com.ntmspace.world.gen.legacy.Block17 expected = primer.blocks[(x * 16 + z) * 256 + y];
                    String wanted = expected == null ? "minecraft:air" : expected.id;
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos.set(chunkX * 16 + x, y, chunkZ * 16 + z));
                    net.minecraft.world.level.block.Block block = state.getBlock();
                    String actual = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
                    boolean terrain = wanted.equals(actual) || expected == null && actual.equals("minecraft:cave_air");
                    if (!terrain) {
                        boolean replaceable = expected == com.ntmspace.world.gen.legacy.Block17.MOON_ROCK
                                || expected == com.ntmspace.world.gen.legacy.Block17.BEDROCK;
                        java.util.Set<net.minecraft.world.level.block.Block> replay =
                                populated.get(com.ntmspace.world.populate.PrimerPopulateWorld.key(chunkX * 16 + x, y, chunkZ * 16 + z));
                        // populate also writes into the primer's air (HBM's cave stalactites): accepted only where the
                        // replayed populate put exactly that block
                        boolean replayed = replay != null && replay.contains(block);
                        if (!replayed && (!replaceable || state.isAir() || !state.getFluidState().isEmpty() || TERRAIN.contains(actual))) {
                            String message = "mun_check MISMATCH chunk " + chunkX + "," + chunkZ + " at " + pos.toShortString()
                                    + ": " + actual + ", the 1.7 primer has " + wanted
                                    + (replay == null ? "" : " and the populate replay " + names(replay));
                            NtmSpace.LOG.info(message);
                            source.sendFailure(Component.literal(message));
                            return 0;
                        }
                        placed++;
                        if (replayed) predicted++;
                    }
                    checked++;
                }
            }
        }
        String message = "mun_check OK chunk " + chunkX + "," + chunkZ + ": " + checked
                + " blocks match the 1.7 primer (" + placed + " placed by populate, " + predicted + " of them as replayed)";
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /** The Mun primer's own blocks: populate never puts these where the primer has another block. */
    private static final java.util.Set<String> TERRAIN = java.util.Set.of("ntmspace:moon_rock", "hbm:moon_turf",
            "minecraft:bedrock", "minecraft:air", "minecraft:cave_air");

    private static String names(java.util.Set<net.minecraft.world.level.block.Block> blocks) {
        return blocks.stream().map(b -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString())
                .sorted().toList().toString();
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(Commands.literal("check").then(Commands.literal("mun").executes(context -> check(context.getSource()))))
                .then(Commands.literal("tp").then(Commands.literal("mun").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel mun = context.getSource().getServer().getLevel(com.ntmspace.world.SpaceDimensions.MUN);
                    if (mun == null) {
                        context.getSource().sendFailure(Component.literal("The Mun dimension is not loaded"));
                        return 0;
                    }
                    BlockPos column = BlockPos.containing(player.getX(), 0, player.getZ());
                    int y = mun.getChunk(column).getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX() & 15, column.getZ() & 15) + 1;
                    Teleports.toLevel(player, mun, player.getX(), y, player.getZ());
                    return 1;
                }))));
    }
}
