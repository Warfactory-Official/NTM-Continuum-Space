package com.ntmspace.block.terrain;

import com.hbm.blocks.generic.BlockTerrainPlant;
import com.ntmspace.block.flora.FloraDrops;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's Laythe water plants ({@code BlockWaterPlant}: {@code laythe_seagrass}, {@code laythe_glowgrass}), blocks
 * of {@code Material.water} drawn as crossed planes: here a plant that always holds water (like vanilla's seagrass), no
 * collision. {@code ground} is 1.7's {@code canBlockStay} (the water plants and coral had none, so they always stay).
 *
 * <p>1.7 {@code canPlaceBlockAt}: only with a liquid above (water, or another of these wet plants). The water plants
 * drop zero to three saltleaves ({@link FloraDrops#saltleaf}); the coral subclass keeps its loot table.
 */
public class BlockWaterPlant extends BlockTerrainPlant {

    private final Predicate<BlockState> ground;
    private final boolean saltleaf;

    public BlockWaterPlant(Properties properties) {
        this(properties, below -> true, true);
    }

    public BlockWaterPlant(Properties properties, Predicate<BlockState> ground, boolean saltleaf) {
        super(properties);
        this.ground = ground;
        this.saltleaf = saltleaf;
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return ground.test(level.getBlockState(pos.below()));
    }

    /** 1.7 {@code canPlaceBlockAt}: {@code world.getBlock(x, y + 1, z).getMaterial().isLiquid()}. */
    public static boolean liquidAbove(LevelReader level, BlockPos pos) {
        return !level.getFluidState(pos.above()).isEmpty();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!liquidAbove(context.getLevel(), context.getClickedPos())) return null;
        return super.getStateForPlacement(context);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (saltleaf) return FloraDrops.saltleaf(FloraDrops.random(params));
        return super.getDrops(state, params);
    }

    /** 1.7: {@code Material.water}; the block is wet. */
    @Override
    public FluidState getFluidState(BlockState state) {
        return net.minecraft.world.level.material.Fluids.WATER.getSource(false);
    }
}
