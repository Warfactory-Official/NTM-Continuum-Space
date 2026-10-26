package com.ntmspace.world;

import dev.golem.core.world.Chunks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What 1.7 decoration ({@code populate}: a biome decorator and the {@code IWorldGenerator}s) read and wrote through its
 * {@code World}, on the level a modern chunk's FEATURES step gets: blocks by coordinate with 1.7's 0-255 height, its
 * {@code setBlock(..., 2)}, {@code blockExists}, {@code getHeightValue} and {@code getTopSolidOrLiquidBlock}.
 *
 * <p>1.7 decorated the 16 x 16 square half a chunk south-east of the chunk it populated, and could reach a little past
 * it; the modern step may write the chunk being decorated and its eight neighbours. A write or read beyond them does
 * what 1.7 did for an unloaded chunk: nothing, and air ({@link #exists} false).
 */
final class World17 {

    private final WorldGenLevel level;
    private final int centreX;
    private final int centreZ;
    private final int minY;
    private final int maxY;
    private final boolean legacyBounds;
    private int writes;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    World17(WorldGenLevel level, int chunkX, int chunkZ) {
        this(level, chunkX, chunkZ, 0, 256, true);
    }

    private World17(WorldGenLevel level, int chunkX, int chunkZ, int minY, int maxY, boolean legacyBounds) {
        this.level = level;
        this.centreX = chunkX;
        this.centreZ = chunkZ;
        this.minY = minY;
        this.maxY = maxY;
        this.legacyBounds = legacyBounds;
    }

    /** A sapling grows in the live world's height range; decoration keeps the authored 0..255 range. */
    static World17 live(WorldGenLevel level, int chunkX, int chunkZ) {
        var minY = Chunks.minY(level);
        return new World17(level, chunkX, chunkZ, minY, minY + level.getHeight(), false);
    }

    int minY() {
        return minY;
    }

    int maxY() {
        return maxY;
    }

    /** The level this world writes to (hbm-space's populate code in {@code world.populate} wraps it its own way). */
    WorldGenLevel level() {
        return level;
    }

    /** 1.7 {@code blockExists}: inside the height range and the decorated area. */
    boolean exists(int x, int y, int z) {
        return y >= minY && y < maxY && Math.abs((x >> 4) - centreX) <= 1 && Math.abs((z >> 4) - centreZ) <= 1;
    }

    BlockState get(int x, int y, int z) {
        if (!exists(x, y, z)) return Blocks.AIR.defaultBlockState();
        return level.getBlockState(pos.set(x, y, z));
    }

    boolean is(int x, int y, int z, Block block) {
        return get(x, y, z).is(block);
    }

    boolean isAir(int x, int y, int z) {
        return get(x, y, z).isAir();
    }

    /** 1.7 {@code setBlock(x, y, z, block, meta, 2)}. */
    void set(int x, int y, int z, BlockState state) {
        if (exists(x, y, z) && level.setBlock(pos.set(x, y, z), state, 2)) writes++;
    }

    int writes() { return writes; }

    /** Marks a block for its chunk's post-processing, which runs its shape update once the chunk is complete. */
    void postProcess(int x, int y, int z) {
        if (exists(x, y, z)) level.getChunk(pos.set(x, y, z)).markPosForPostprocessing(pos.immutable());
    }

    /**
     * 1.7 {@code getHeightValue}: one above the highest block that stops light, here the highest block that blocks
     * motion or holds a fluid (leaves and water count, plants and vines do not, as in 1.7).
     */
    int heightValue(int x, int z) {
        for (int y = maxY - 1; y >= minY; y--) {
            BlockState state = get(x, y, z);
            if (!state.isAir() && (state.blocksMotion() || !state.getFluidState().isEmpty())) return y + 1;
        }
        return 0;
    }

    /** 1.7 {@code getTopSolidOrLiquidBlock}: one above the highest block that blocks movement and is no leaves. */
    int topSolidOrLiquid(int x, int z) {
        for (int y = maxY - 1; y >= minY + (legacyBounds ? 1 : 0); y--) {
            BlockState state = get(x, y, z);
            if (state.blocksMotion() && !state.is(BlockTags.LEAVES)
                    && !state.is(com.ntmspace.registry.LaytheTektoBlocks.RUBBER_LEAVES.get())
                    && !state.is(com.ntmspace.registry.LaytheTektoBlocks.PET_LEAVES.get())) return y + 1;
        }
        return -1;
    }
}
