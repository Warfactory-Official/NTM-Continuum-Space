package com.ntmspace.gametest;

import com.ntmspace.config.SpaceConfig;
import com.ntmspace.world.SpaceDimensions;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.BlockPlaces;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class NativeNetherPortalGameTests {
    public static final GameTests.Function FLAG = SpaceGameTests.TESTS.function(
            "native_celestial_nether_portal_flag", 100, 0, () -> NativeNetherPortalGameTests::portalFlag);

    private NativeNetherPortalGameTests() {
    }

    static void init() {
    }

    private static void portalFlag(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(SpaceDimensions.LAYTHE);
        var top = Integer.MIN_VALUE;
        for (var x = 7; x <= 12; x++)
            for (var z = 7; z <= 9; z++) {
                top = Math.max(top, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
            }
        var origin = new BlockPos(8, top + 2, 8);
        var player = GameTestHelpers.mockServerPlayerInLevel(helper);
        var previous = SpaceConfig.allowNetherPortals;
        var saved = new BlockState[6 * 7 * 3];
        var cursor = new BlockPos.MutableBlockPos();
        var index = 0;
        for (var x = -1; x <= 4; x++)
            for (var y = -1; y <= 5; y++)
                for (var z = -1; z <= 1; z++) {
                    saved[index++] = level.getBlockState(
                            cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z));
                }
        try {
            SpaceConfig.allowNetherPortals = false;
            clear(level, origin, cursor);
            for (var x = 0; x <= 3; x++)
                for (var y = 0; y <= 4; y++) {
                    if (x == 0 || x == 3 || y == 0 || y == 4) {
                        level.setBlock(origin.offset(x, y, 0), Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            var ignition = origin.offset(1, 1, 0);
            helper.assertTrue(level.setBlock(ignition, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL),
                    "The disabled Laythe portal fixture did not place its ignition fire");
            BlockPlaces.placed(player, level, ignition, Blocks.FIRE.defaultBlockState());
            interior(helper, level, origin, false);

            for (var x = 1; x <= 2; x++)
                for (var y = 1; y <= 3; y++) {
                    level.setBlock(origin.offset(x, y, 0), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            SpaceConfig.allowNetherPortals = true;
            helper.assertTrue(level.setBlock(ignition, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL),
                    "The enabled Laythe portal fixture did not place its ignition fire");
            // 1.7 reacted only to fire a player placed
            BlockPlaces.placed(null, level, ignition, Blocks.FIRE.defaultBlockState());
            interior(helper, level, origin, false);
            BlockPlaces.placed(player, level, ignition, Blocks.FIRE.defaultBlockState());
            interior(helper, level, origin, true);
        } finally {
            SpaceConfig.allowNetherPortals = false;
            try {
                clear(level, origin, cursor);
                index = 0;
                for (var x = -1; x <= 4; x++)
                    for (var y = -1; y <= 5; y++)
                        for (var z = -1; z <= 1; z++) {
                            level.setBlock(cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z),
                                    saved[index++], Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        }
            } finally {
                SpaceConfig.allowNetherPortals = previous;
            }
        }
        helper.succeed();
    }

    private static void interior(GameTestHelper helper, ServerLevel level, BlockPos origin, boolean portal) {
        for (var x = 1; x <= 2; x++)
            for (var y = 1; y <= 3; y++) {
                var pos = origin.offset(x, y, 0);
                var state = level.getBlockState(pos);
                helper.assertTrue(state.is(Blocks.NETHER_PORTAL) == portal,
                        "Laythe portal flag=" + portal + " left " + state + " at " + pos);
            }
    }

    private static void clear(ServerLevel level, BlockPos origin, BlockPos.MutableBlockPos cursor) {
        for (var x = -1; x <= 4; x++)
            for (var y = -1; y <= 5; y++)
                for (var z = -1; z <= 1; z++) {
                    level.setBlock(cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z),
                            Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
    }
}
