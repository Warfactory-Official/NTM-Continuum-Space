package com.ntmspace.block.flora;

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
 * hbm-space's {@code rubber_silt} ("Tekto Silt", {@code BlockGeneric(Material.sand)}): drops itself; a hoe turns it into
 * rubber farmland ({@link RubberSoil}).
 */
public class BlockRubberSilt extends CompatBlock {

    private final Supplier<? extends Block> farmland;

    public BlockRubberSilt(Properties properties, Supplier<? extends Block> farmland) {
        super(properties);
        this.farmland = farmland;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        return RubberSoil.till(level, pos, player, hand, stack, farmland);
    }
}
