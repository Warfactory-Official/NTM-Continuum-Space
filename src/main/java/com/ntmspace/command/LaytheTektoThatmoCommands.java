package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.LaytheTektoBlocks;
import com.ntmspace.world.LayeredChunkGenerator;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.ThatmoChunkGenerator;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.world.Teleports;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /ntmspace tp laythe|tekto|thatmo} and {@code /ntmspace check laythe|tekto|thatmo}, merged into
 * {@link SpaceCommands}' {@code /ntmspace} (operators only).
 *
 * <p>{@code check} compares the chunk the player stands in with the generator's 1.7 primer, block for block, and logs
 * {@code <body>_check OK} or the first difference. Decoration runs after the primer and may put plants, trees, vines
 * and coral anywhere in a chunk, so a difference where the world now holds one of those is counted as decorated, not as
 * a mismatch. As for the Mun, run it on terrain generated in this game session (the noise caves are per session).
 */
public final class LaytheTektoThatmoCommands {

    private LaytheTektoThatmoCommands() {}

    public static void init() {
        CompatCommands.server(LaytheTektoThatmoCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> check = Commands.literal("check");
        LiteralArgumentBuilder<CommandSourceStack> tp = Commands.literal("tp");
        for (String body : new String[] {"laythe", "tekto", "thatmo"}) {
            check.then(Commands.literal(body).executes(context -> check(context.getSource(), body)));
            tp.then(Commands.literal(body).executes(context -> teleport(context.getSource(), body)));
        }
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(check).then(tp));
    }

    private static ResourceKey<Level> level(String body) {
        return switch (body) {
            case "laythe" -> SpaceDimensions.LAYTHE;
            case "tekto" -> SpaceDimensions.TEKTO;
            default -> SpaceDimensions.THATMO;
        };
    }

    private static int teleport(CommandSourceStack source, String body) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getServer().getLevel(level(body));
        if (level == null) {
            source.sendFailure(Component.literal("The " + body + " dimension is not loaded"));
            return 0;
        }
        BlockPos column = BlockPos.containing(player.getX(), 0, player.getZ());
        int y = level.getChunk(column).getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX() & 15, column.getZ() & 15) + 1;
        Teleports.toLevel(player, level, player.getX(), y, player.getZ());
        return 1;
    }

    /** The blocks decoration places (and may put over any primer block). */
    private static Set<Block> decoration() {
        return Set.of(LaytheTektoBlocks.LAYTHE_SEAGRASS.get(), LaytheTektoBlocks.LAYTHE_GLOWGRASS.get(),
                LaytheTektoBlocks.PLANT_TALL_LAYTHE.get(), LaytheTektoBlocks.LAYTHE_KELP.get(),
                LaytheTektoBlocks.LAYTHE_CORAL.get(), LaytheTektoBlocks.LAYTHE_CORAL_BLOCK.get(),
                LaytheTektoBlocks.RUBBER_PLANT.get(), LaytheTektoBlocks.RUBBER_TALL.get(), LaytheTektoBlocks.PVC_LOG.get(),
                LaytheTektoBlocks.VINYL_LOG.get(), LaytheTektoBlocks.RUBBER_LEAVES.get(), LaytheTektoBlocks.PET_LEAVES.get(),
                LaytheTektoBlocks.VINYL_VINE.get(), Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.IRON_ORE, Blocks.GOLD_ORE,
                Blocks.REDSTONE_ORE, Blocks.DIAMOND_ORE, Blocks.LAPIS_ORE, Blocks.DIRT, Blocks.GRAVEL);
    }

    private static int check(CommandSourceStack source, String body) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        int chunkX = player.blockPosition().getX() >> 4;
        int chunkZ = player.blockPosition().getZ() >> 4;
        CelestialTerrain17.Primer primer;
        if (level.dimension().equals(level(body)) && level.getChunkSource().getGenerator() instanceof LayeredChunkGenerator layered) {
            primer = layered.primer(chunkX, chunkZ);
        } else if (level.dimension().equals(level(body)) && level.getChunkSource().getGenerator() instanceof ThatmoChunkGenerator thatmo) {
            primer = thatmo.primer(chunkX, chunkZ);
        } else {
            source.sendFailure(Component.literal("not on " + body));
            return 0;
        }
        Set<Block> decoration = decoration();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int checked = 0;
        int decorated = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    int index = (x * 16 + z) * 256 + y;
                    Block17 expected = primer.blocks[index];
                    Block wanted = LayeredChunkGenerator.state(expected, primer.metas[index]).getBlock();
                    Block actual = level.getBlockState(pos.set(chunkX * 16 + x, y, chunkZ * 16 + z)).getBlock();
                    if (wanted != actual && !(expected == null && actual == Blocks.CAVE_AIR)) {
                        if (decoration.contains(actual)) {
                            decorated++;
                        } else {
                            String message = body + "_check MISMATCH chunk " + chunkX + "," + chunkZ + " at " + pos.toShortString()
                                    + ": " + BuiltInRegistries.BLOCK.getKey(actual) + ", the 1.7 primer has "
                                    + BuiltInRegistries.BLOCK.getKey(wanted);
                            NtmSpace.LOG.info(message);
                            source.sendFailure(Component.literal(message));
                            return 0;
                        }
                    }
                    checked++;
                }
            }
        }
        String message = body + "_check OK chunk " + chunkX + "," + chunkZ + ": " + checked + " blocks match the 1.7 primer ("
                + decorated + " decorated since)";
        NtmSpace.LOG.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
