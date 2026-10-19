package com.ntmspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ntmspace.NtmSpace;
import com.ntmspace.world.LayeredChunkGenerator;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.world.Teleports;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /ntmspace tp <body>} and {@code /ntmspace check <body>} for Eve, Moho and Minmus (the Mun's are in
 * {@link SpaceCommands}; Brigadier merges both trees under {@code /ntmspace}). Operators only.
 */
public final class PlanetCommands {

    private PlanetCommands() {}

    public static void init() {
        CompatCommands.server(PlanetCommands::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> check = Commands.literal("check");
        LiteralArgumentBuilder<CommandSourceStack> tp = Commands.literal("tp");
        body(check, tp, "eve", SpaceDimensions.EVE);
        body(check, tp, "moho", SpaceDimensions.MOHO);
        body(check, tp, "minmus", SpaceDimensions.MINMUS);
        dispatcher.register(Commands.literal(NtmSpace.MOD_ID)
                .requires(dev.golem.core.command.Permissions.gamemasters())
                .then(check)
                .then(tp));
    }

    private static void body(LiteralArgumentBuilder<CommandSourceStack> check, LiteralArgumentBuilder<CommandSourceStack> tp,
                             String name, ResourceKey<Level> dimension) {
        check.then(Commands.literal(name).executes(context -> check(context.getSource(), name)));
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

    /**
     * Compares the chunk the player stands in with the generator's 1.7 primer, block for block, and logs
     * {@code <body>_check OK} or the first difference (the harness reads the log). Blocks a player changed differ, and so
     * do chunks generated in an earlier game session where the body has session randomness (Eve's cracks, Moho's
     * plateaus, as in 1.7), so run it on terrain generated in this session.
     */
    private static int check(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        ResourceKey<Level> wanted = Dimensions.of(name);
        if (!level.dimension().equals(wanted) || !(level.getChunkSource().getGenerator() instanceof LayeredChunkGenerator generator)) {
            source.sendFailure(Component.literal("not on " + name));
            return 0;
        }
        int chunkX = player.blockPosition().getX() >> 4;
        int chunkZ = player.blockPosition().getZ() >> 4;
        CelestialTerrain17.Primer primer = generator.primer(chunkX, chunkZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int checked = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    int index = (x * 16 + z) * 256 + y;
                    String expected = BuiltInRegistries.BLOCK.getKey(
                            LayeredChunkGenerator.state(primer.blocks[index], primer.metas[index]).getBlock()).toString();
                    String actual = BuiltInRegistries.BLOCK
                            .getKey(level.getBlockState(pos.set(chunkX * 16 + x, y, chunkZ * 16 + z)).getBlock()).toString();
                    if (!expected.equals(actual) && !(expected.equals("minecraft:air") && actual.equals("minecraft:cave_air"))) {
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

    private static final class Dimensions {
        static ResourceKey<Level> of(String name) {
            return switch (name) {
                case "eve" -> SpaceDimensions.EVE;
                case "moho" -> SpaceDimensions.MOHO;
                default -> SpaceDimensions.MINMUS;
            };
        }
    }
}
