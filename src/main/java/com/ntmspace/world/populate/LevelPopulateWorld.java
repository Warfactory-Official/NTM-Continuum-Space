package com.ntmspace.world.populate;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.tileentity.BedrockOreBlockEntity;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** {@link PopulateWorld} on the level a modern chunk's FEATURES step gets. */
public final class LevelPopulateWorld extends PopulateWorld {

    private final WorldGenLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final Random worldRandom;

    public LevelPopulateWorld(WorldGenLevel level, int chunkX, int chunkZ) {
        super(chunkX, chunkZ);
        this.level = level;
        this.worldRandom = new Random(level.getRandom().nextLong());
    }

    @Override
    protected BlockState read(int x, int y, int z) {
        return level.getBlockState(pos.set(x, y, z));
    }

    @Override
    protected void write(int x, int y, int z, BlockState state) {
        level.setBlock(pos.set(x, y, z), state, 2);
    }

    @Override
    public void fluidTick(int x, int y, int z) {
        if (!exists(x, y, z)) return;
        BlockPos at = new BlockPos(x, y, z);
        FluidState fluid = level.getBlockState(at).getFluidState();
        if (!fluid.isEmpty()) level.scheduleTick(at, fluid.getType(), 0);
    }

    @Override
    public void bedrockOre(int x, int y, int z, BedrockDeposit deposit, int shape) {
        if (!exists(x, y, z)) return;
        BlockPos at = new BlockPos(x, y, z);
        level.setBlock(at, ModOreBlocks.ore_bedrock_block.get().defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof BedrockOreBlockEntity ore) {
            ore.setDeposit(deposit.resource(), deposit.acid(), deposit.tier(), deposit.color(), shape);
        }
    }

    @Override
    public void meteorite(int x, int y, int z, net.minecraft.util.RandomSource rand) {
        new com.hbm.world.feature.Meteorite().generate(level, rand, new BlockPos(x, y, z), false, false, false);
    }

    @Override
    public void brokenLantern(int x, int z, Random rand) {
        com.ntmspace.world.structure.PlanetFeatures17.brokenLantern(level, this, x, z, rand);
    }

    @Override
    public void restockPedestals(int i, int j) {
        com.ntmspace.world.structure.PlanetFeatures17.restockPedestals(level, i, j);
    }

    @Override
    public Random worldRandom() {
        return worldRandom;
    }
}
