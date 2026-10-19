package com.ntmspace.block.generic;

import com.hbm.blocks.generic.BlockTerrainPlant;
import dev.golem.core.world.CompatBonemealableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The two {@code plant_flower} types hbm-space added to HBM's {@code BlockNTMFlower}: {@code STRAWBERRY} ("Wild
 * Strawberries") and {@code MINT} ("Wild Mint"), here {@code ntmspace:plant_flower_strawberry} and
 * {@code ntmspace:plant_flower_mint} (HBM's flowers are one block per type, {@code hbm:plant_flower_<type>}; its type
 * enum is closed, inventory E-094, so an addon registers its own blocks).
 *
 * <p>The rules are {@code BlockNTMFlower}'s for a type that is neither hemp nor a mustard willow: it stays on grass,
 * dirt, farmland, dead or oily dirt ({@link BlockTerrainPlant#isFlowerGround}); it never grows by itself; bone meal
 * always succeeds and "grows" it by dropping its harvest where it stands ({@code dropBlockAsItem}). Harvested, the
 * strawberries give one {@code ntmspace:strawberry} and the mint one {@code ntmspace:mint_leaves} (hbm-space's
 * {@code getItemDropped}; its mint kept the flower's metadata 7 as the leaves' damage, which a plain item has no use
 * for) - the loot tables say so. Earth generation: {@code world.WildFlowers}.
 */
public class BlockWildFlower extends BlockTerrainPlant implements CompatBonemealableBlock {

    public BlockWildFlower(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return isFlowerGround(level.getBlockState(pos.below()));
    }

    @Override
    public boolean canBonemeal(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        dropResources(state, level, pos);
    }
}
