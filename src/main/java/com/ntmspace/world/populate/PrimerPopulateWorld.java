package com.ntmspace.world.populate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@link PopulateWorld} over 1.7 chunk primers (GameTests and checks): the base terrain comes from {@code base}, every
 * write is kept in order of position, so a test can count what decoration placed.
 */
public final class PrimerPopulateWorld extends PopulateWorld {

    /** The undecorated terrain: the block a primer holds at a position. */
    @FunctionalInterface
    public interface Base {
        BlockState at(int x, int y, int z);
    }

    private final Base base;
    private final Map<Long, BlockState> written = new HashMap<>();
    private final Map<Long, BedrockDeposit> deposits = new LinkedHashMap<>();
    private final Random worldRandom;

    public PrimerPopulateWorld(Base base, int chunkX, int chunkZ, long worldRandomSeed) {
        super(chunkX, chunkZ);
        this.base = base;
        this.worldRandom = new Random(worldRandomSeed);
    }

    public static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
    }

    public static int x(long key) {
        return (int) (key >> 38);
    }

    public static int z(long key) {
        return (int) (key << 26 >> 38);
    }

    public static int y(long key) {
        return (int) (key & 0xFFFL);
    }

    @Override
    protected BlockState read(int x, int y, int z) {
        BlockState state = written.get(key(x, y, z));
        return state != null ? state : base.at(x, y, z);
    }

    @Override
    protected void write(int x, int y, int z, BlockState state) {
        written.put(key(x, y, z), state);
    }

    @Override
    public void bedrockOre(int x, int y, int z, BedrockDeposit deposit, int shape) {
        if (!exists(x, y, z)) return;
        write(x, y, z, com.hbm.blocks.ModOreBlocks.ore_bedrock_block.get().defaultBlockState());
        deposits.put(key(x, y, z), deposit);
    }

    @Override
    public Random worldRandom() {
        return worldRandom;
    }

    /** Every position written, with the state it holds now. */
    public Map<Long, BlockState> written() {
        return written;
    }

    /** The bedrock ore deposits placed. */
    public Map<Long, BedrockDeposit> deposits() {
        return deposits;
    }
}
