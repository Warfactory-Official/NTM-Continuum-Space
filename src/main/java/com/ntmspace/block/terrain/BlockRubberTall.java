package com.ntmspace.block.terrain;

import com.hbm.blocks.generic.BlockTerrainPlant;
import com.ntmspace.block.flora.TypedItems;
import com.ntmspace.registry.FloraBlocks;
import dev.golem.core.data.CompatBlockEntity;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * hbm-space's {@code rubber_tall} ("Tall Rubber Plant", 1.7's {@code spike_cacti}, {@code BlockRubberCacti}): cactus,
 * bush or flower (one block item each, {@link TypedItems}) on vinyl sand, rubber grass, rubber silt or rubber farmland;
 * no collision. Cactus and bush drop themselves, the flower drops chlorinated (paraffin) seeds; a plant that loses its
 * ground drops the same.
 *
 * <p>1.7 drew it with {@code RenderRubberCactus}: the OBJ {@code plant_spike} (two crossed planes three blocks tall
 * and four tilted leaves at the foot) with the type's block icon, drawn twice, turned half a turn apart. A block model
 * cannot reach three blocks up, so the block has an empty model and an entity ({@link Plant}) that NTM: Space's
 * client draws the OBJ for ({@code client/RenderRubberCactus}).
 */
public class BlockRubberTall extends BlockTerrainPlant implements EntityBlock {

    public static final EnumProperty<PlantType.RubberTall> TYPE = EnumProperty.create("type", PlantType.RubberTall.class);

    private final Predicate<BlockState> ground;

    public BlockRubberTall(Properties properties, Predicate<BlockState> ground) {
        super(properties);
        this.ground = ground;
        registerDefaultState(stateDefinition.any().setValue(TYPE, PlantType.RubberTall.CACT));
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

    /** 1.7 {@code getItemDropped}: the flower's chlorinated seeds, else the plant itself ({@code damageDropped}: its type). */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(TYPE) == PlantType.RubberTall.FLOWER) return List.of(new ItemStack(FloraBlocks.PARAFFIN_SEEDS.get()));
        return List.of(TypedItems.stack(state, TYPE));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return TypedItems.stack(state, TYPE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Plant(FloraBlocks.RUBBER_TALL_BE.get(), pos, state);
    }

    /** The tall rubber plant's renderer anchor; it holds no data. */
    public static final class Plant extends CompatBlockEntity {

        public Plant(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
    }
}
