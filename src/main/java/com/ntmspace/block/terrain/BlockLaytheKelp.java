package com.ntmspace.block.terrain;

import com.hbm.blocks.generic.BlockTerrainPlant;
import dev.golem.core.world.BlockTickScheduler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;

/**
 * hbm-space's {@code laythe_kelp} ("Laythe Kelp", {@code BlockKelp}): a column of crossed planes on laythe silt that
 * holds water. 1.7 kept metadata 8 on the top segment (drawn with {@code laythe_kelp_top}) and 0 below it; here that is
 * {@link #TOP}, kept up to date as the column changes. Drops zero to three saltleaves; a segment that loses its support
 * turns into water without dropping anything (1.7 {@code onNeighborBlockChange}).
 */
public class BlockLaytheKelp extends BlockTerrainPlant {

    public static final BooleanProperty TOP = BooleanProperty.create("top");

    private final Supplier<? extends Block> silt;

    public BlockLaytheKelp(Properties properties, Supplier<? extends Block> silt) {
        super(properties);
        this.silt = silt;
        registerDefaultState(stateDefinition.any().setValue(TOP, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP);
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || below.is(silt.get());
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        BlockState updated = super.onUpdateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState);
        if (!updated.is(this)) return updated;
        return updated.setValue(TOP, !level.getBlockState(pos.above()).is(this));
    }

    @Override
    protected boolean dropsWhenUnsupported(BlockState state) {
        return false;
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
                                                                     net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return com.ntmspace.block.flora.FloraDrops.saltleaf(com.ntmspace.block.flora.FloraDrops.random(params));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return net.minecraft.world.level.material.Fluids.WATER.getSource(false);
    }
}
