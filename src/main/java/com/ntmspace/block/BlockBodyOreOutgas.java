package com.ntmspace.block;

import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.generic.BlockOutgas;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * hbm-space's {@code BlockOreOutgas} ({@code ore_uranium}, {@code ore_asbestos}): a per-body ore ({@link BodyOre}) that
 * also lets out gas, HBM's {@link BlockOutgas} behaviour (random tick into a neighbouring air block, and the broken
 * block's place, at the rate hbm-space gave: {@code BlockOreOutgas(rock, true, 5, true)}). hbm-space's {@code getGas}
 * named radon for uranium and asbestos dust for asbestos, with no config switch.
 */
public class BlockBodyOreOutgas extends BlockOutgas implements BodyOre {

    private final Supplier<? extends Block> gas;

    public BlockBodyOreOutgas(Properties properties, Supplier<? extends Block> gas) {
        super(properties, true, 5, true);
        this.gas = gas;
        registerDefaultState(stateDefinition.any().setValue(BlockBodyOre.BODY, BlockBodyOre.Body.KERBIN));
    }

    /** {@code ore_uranium}'s gas. */
    public static Block radon() {
        return ModNuclearBlocks.gas_radon.get();
    }

    /** {@code ore_asbestos}'s gas. */
    public static Block asbestos() {
        return ModNuclearBlocks.gas_asbestos.get();
    }

    @Override
    public Block getGas() {
        return gas.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockBodyOre.BODY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BodyOre.placement(this, context);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return BodyOre.drops(this, state, super.getDrops(state, params));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return BodyOre.stack(this, state);
    }
}
