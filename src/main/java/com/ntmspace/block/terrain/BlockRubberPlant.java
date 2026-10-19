package com.ntmspace.block.terrain;

import com.hbm.blocks.generic.BlockTerrainPlant;
import com.ntmspace.block.flora.TypedItems;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * hbm-space's {@code rubber_plant} ("Short Rubber Plant", {@code BlockRubberPlant}): grass, fern or shrub (one block item
 * each, {@link TypedItems}) on Tekto's rubber grass, rubber silt or rubber farmland, drawn as crossed planes. It drops
 * nothing ({@code getItemDropped} null), also when it loses its ground.
 */
public class BlockRubberPlant extends BlockTerrainPlant {

    public static final EnumProperty<PlantType.RubberPlant> TYPE = EnumProperty.create("type", PlantType.RubberPlant.class);

    private final Predicate<BlockState> ground;

    public BlockRubberPlant(Properties properties, Predicate<BlockState> ground) {
        super(properties);
        this.ground = ground;
        registerDefaultState(stateDefinition.any().setValue(TYPE, PlantType.RubberPlant.GRASS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return ground.test(level.getBlockState(pos.below()));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return TypedItems.placement(this, TYPE, context);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return TypedItems.stack(state, TYPE);
    }
}
