package com.ntmspace.block.terrain;

import com.ntmspace.block.flora.TypedItems;
import dev.golem.core.world.CompatBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * hbm-space's {@code laythe_coral_block} ("Block of Laythe Coral", {@code BlockEnumMulti(Material.coral, EnumCoral)}):
 * five types of coral block, each its own block item ({@link TypedItems}); drops itself, by type.
 */
public class BlockLaytheCoralBlock extends CompatBlock {

    public static final EnumProperty<PlantType.Coral> TYPE = BlockLaytheCoral.TYPE;

    public BlockLaytheCoralBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, PlantType.Coral.TUBE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return TypedItems.placement(this, TYPE, context);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return TypedItems.drops(state, TYPE, super.getDrops(state, params));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return TypedItems.stack(state, TYPE);
    }
}
