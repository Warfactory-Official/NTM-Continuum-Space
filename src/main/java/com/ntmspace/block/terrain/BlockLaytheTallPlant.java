package com.ntmspace.block.terrain;

import com.hbm.blocks.generic.BlockTerrainPlant;
import com.ntmspace.block.flora.FloraDrops;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code plant_tall_laythe} ("Saltgrass", {@code BlockTallPlantWater}): a two-block water plant, 1.7's
 * metadata {@code +8} for the upper half here vanilla's {@code half}. The lower half stays on laythe silt, the upper on
 * the lower; both hold water.
 *
 * <p>Ported from the 1.7 class:
 * <ul>
 *   <li>placed only on laythe silt with still water above ({@code canReplace}: {@code Blocks.water}), the upper half
 *       then set into that water ({@code onBlockPlacedBy});</li>
 *   <li>each half drops zero to three saltleaves ({@link FloraDrops#saltleaf}); a half that loses its support turns into
 *       water, the lower one dropping first ({@code checkAndDropBlock}); no bonemeal;</li>
 *   <li><b>ported oddity</b> ({@code onBlockHarvested}): a player breaking either half also removes the other one
 *       (to air), and then, in survival, drops another zero to three saltleaves (1.7 dropped the block above's items,
 *       always saltleaf); in creative it sets the block above the broken half to air instead, which clears the water
 *       over a broken upper half.</li>
 * </ul>
 * The {@code detectCut} branch of 1.7's {@code checkAndDropBlock} asks whether a {@code plant_flower} could stay here,
 * which needs grass, dirt or farmland below and never happens on laythe silt; it is not ported.
 */
public class BlockLaytheTallPlant extends BlockTerrainPlant {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private final Supplier<? extends Block> silt;

    public BlockLaytheTallPlant(Properties properties, Supplier<? extends Block> silt) {
        super(properties);
        this.silt = silt;
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return below.is(silt.get());
    }

    /** 1.7 {@code checkAndDropBlock}: only the lower half ({@code meta < 8}) drops. */
    @Override
    protected boolean dropsWhenUnsupported(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    /** 1.7 {@code canReplace}: still water above, and the silt below. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState above = level.getBlockState(pos.above());
        if (!above.is(Blocks.WATER) || !above.getFluidState().isSource()) return null;
        if (!level.getBlockState(pos.below()).is(silt.get())) return null;
        return defaultBlockState();
    }

    /** 1.7 {@code onBlockPlacedBy}: the upper half goes into the water above. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), 2);
    }

    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide()) return;
        BlockPos other = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
        if (level.getBlockState(other).is(this)) level.setBlock(other, Blocks.AIR.defaultBlockState(), 3);
        if (player.isCreative()) {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        } else if (level instanceof ServerLevel server) {
            for (ItemStack drop : FloraDrops.saltleaf(server.getRandom())) popResource(level, pos.above(), drop);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return FloraDrops.saltleaf(FloraDrops.random(params));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return net.minecraft.world.level.material.Fluids.WATER.getSource(false);
    }
}
