package com.ntmspace.world;

import com.ntmspace.config.SpaceConfig;
import com.ntmspace.orbit.Orbit;
import dev.golem.core.world.BlockChanges;
import dev.golem.core.world.Portals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Space's fire-placement portal rule on celestial levels and in orbit.
 */
public final class SpaceNetherPortals {
    static {
        BlockChanges.onChange(new BlockChanges.Handler() {
            @Override
            public boolean observesChunk(ServerLevel level, int chunkX, int chunkZ) {
                return SpaceConfig.allowNetherPortals;
            }

            @Override
            public void onChange(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
                if (!(after.getBlock() instanceof BaseFireBlock)) return;
                if (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER || level.dimension() == Level.END)
                    return;
                if (CelestialBodies.of(level) != null || Orbit.inOrbit(level)) {
                    Portals.tryCreate(level, pos, Direction.Axis.X);
                }
            }
        });
    }

    private SpaceNetherPortals() {
    }

    public static void init() {
    }
}
