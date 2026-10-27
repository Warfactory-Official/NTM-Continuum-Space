package com.ntmspace.block;

import com.hbm.api.data.DepositDataMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A fluid deposit settles into its own drained family, preserving the full deposit's BODY value. */
public final class BlockBodyFluidOre extends BlockBodyOre {

    public BlockBodyFluidOre(Properties properties) {
        super(properties);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        var deposit = DepositDataMaps.deposit(state);
        if (deposit == null) return;
        var empty = deposit.emptyBlock();
        if (empty == null || !level.getBlockState(pos.below()).is(empty)) return;
        // BlockOreFluid.onNeighborBlockChange replaces the upper cell first, using the full cell's metadata twice.
        level.setBlock(pos, DepositDataMaps.emptyState(state, empty), Block.UPDATE_ALL);
        level.setBlock(pos.below(), state, Block.UPDATE_ALL);
    }
}
