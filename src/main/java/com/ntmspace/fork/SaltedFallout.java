package com.ntmspace.fork;

import com.hbm.blocks.generic.BlockFallout;
import com.hbm.handler.radiation.ChunkRadiationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** hbm-space {@code BlockFallout} as {@code salted_fallout}: HBM's fallout layer that adds 50 RAD to its chunk per random tick. */
public class SaltedFallout extends BlockFallout {

    public SaltedFallout(Properties props) {
        super(props);
    }

    /** 1.7 {@code updateTick}: {@code ChunkRadiationManager.proxy.incrementRad(world, x, y, z, 50)}. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChunkRadiationManager.proxy.incrementRad(level, pos, 50);
    }
}
