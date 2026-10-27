package com.ntmspace.world.populate;

import com.ntmspace.world.RandomSourceRandom;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Explicit feature placement uses the caller's loaded world height and random, without a world-random seed draw. */
public final class FeaturePopulateWorld extends PopulateWorld {
    private final WorldGenLevel level;
    private final Random random;
    private final int minY;
    private final int maxY;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public FeaturePopulateWorld(WorldGenLevel level, BlockPos origin, RandomSource random) {
        super(origin.getX() >> 4, origin.getZ() >> 4);
        this.level = level;
        this.random = new RandomSourceRandom(random);
        minY = level.getMinY();
        maxY = minY + level.getHeight();
    }
    @Override public boolean exists(int x, int y, int z) {
        return y >= minY && y < maxY && Math.abs((x >> 4) - centreX) <= 1 && Math.abs((z >> 4) - centreZ) <= 1;
    }
    @Override protected BlockState read(int x, int y, int z) { return level.getBlockState(pos.set(x, y, z)); }
    @Override protected void write(int x, int y, int z, BlockState state) { level.setBlock(pos.set(x, y, z), state, 2); }
    @Override public Random worldRandom() { return random; }
    @Override public void bedrockOre(int x, int y, int z, BedrockDeposit deposit, int shape) {
        throw new UnsupportedOperationException("This explicit feature does not place bedrock ore entities");
    }
}
