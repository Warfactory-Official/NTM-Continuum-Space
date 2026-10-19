package com.ntmspace.block.terrain;

import com.ntmspace.block.flora.RubberSoil;
import dev.golem.core.fluid.FluidBlock;
import dev.golem.core.world.CompatBlock;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code RubberGrass} ({@code rubber_grass}, "Rubber Grass"), Tekto's grass: when a neighbour changes and
 * the block above is a liquid or a full cube, it turns into rubber silt. It never spreads and sustains no vanilla plant.
 * A hoe turns it into rubber farmland ({@link RubberSoil}).
 */
public class BlockRubberGrass extends CompatBlock {

    private final Supplier<? extends Block> silt;
    private final Supplier<? extends Block> farmland;

    public BlockRubberGrass(Properties properties, Supplier<? extends Block> silt, Supplier<? extends Block> farmland) {
        super(properties);
        this.silt = silt;
        this.farmland = farmland;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        BlockPos up = pos.above();
        BlockState above = level.getBlockState(up);
        // 1.7: BlockLiquid, BlockFluidBase or isNormalCube()
        if (FluidBlock.isLiquid(above)
                || above.isCollisionShapeFullBlock(level, up) && !above.isAir()) {
            level.setBlock(pos, silt.get().defaultBlockState(), 3);
        }
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        return RubberSoil.till(level, pos, player, hand, stack, farmland);
    }
}
