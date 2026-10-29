package com.ntmspace.fork;

import com.hbm.blocks.generic.BlockImpactDirt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Space's BlockDirt keeps scorched soil beside grass and moves it beneath newly regrown grass. */
public final class SpaceImpactDirt extends BlockImpactDirt {
    public SpaceImpactDirt(Properties properties) {
        super(properties);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                     boolean movedByPiston) {
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        var below = pos.below();
        if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK) && level.getBlockState(below).is(Blocks.DIRT)) {
            level.setBlock(below, defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
