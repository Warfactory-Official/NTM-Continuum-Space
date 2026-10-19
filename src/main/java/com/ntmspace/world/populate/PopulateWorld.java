package com.ntmspace.world.populate;

import dev.golem.core.world.Compat;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The world 1.7 decoration ({@code populate}: a biome decorator and the FML {@code IWorldGenerator}s) read and wrote, as
 * hbm-space's ore and populate code used it: blocks by coordinate with 1.7's 0-255 height, {@code setBlock},
 * {@code blockExists}, {@code getHeightValue}, and the 1.7 material questions those generators asked. A modern chunk's
 * FEATURES step gives {@link LevelPopulateWorld}; GameTests give a world of 1.7 primers ({@link PrimerPopulateWorld}).
 *
 * <p>1.7 populated the 16 x 16 square half a chunk south-east of the chunk it decorated and could reach past it; the
 * modern step may touch the chunk being decorated and its eight neighbours. Beyond them a write does nothing and a read
 * is air ({@link #exists} false), as an unloaded chunk was in 1.7.
 */
public abstract class PopulateWorld {

    protected final int centreX;
    protected final int centreZ;

    protected PopulateWorld(int chunkX, int chunkZ) {
        this.centreX = chunkX;
        this.centreZ = chunkZ;
    }

    /** 1.7 {@code blockExists}: inside the height range and the decorated area. */
    public boolean exists(int x, int y, int z) {
        return y >= 0 && y < 256 && Math.abs((x >> 4) - centreX) <= 1 && Math.abs((z >> 4) - centreZ) <= 1;
    }

    public final BlockState get(int x, int y, int z) {
        if (!exists(x, y, z)) return Blocks.AIR.defaultBlockState();
        return read(x, y, z);
    }

    /** 1.7 {@code setBlock} (flag 2, or 3: generation makes no difference). Outside the area: nothing. */
    public final void set(int x, int y, int z, BlockState state) {
        if (state != null && exists(x, y, z)) write(x, y, z, state);
    }

    protected abstract BlockState read(int x, int y, int z);

    protected abstract void write(int x, int y, int z, BlockState state);

    /** Makes a just-placed fluid at a position flow (1.7 ran its {@code updateTick} at once). */
    public void fluidTick(int x, int y, int z) {}

    /**
     * Places HBM's bedrock ore block with its deposit ({@code ore_bedrock}'s tile entity in 1.7) at a position that
     * {@link #exists}.
     */
    public abstract void bedrockOre(int x, int y, int z, BedrockDeposit deposit, int shape);

    /**
     * HBM's fallen meteorite ({@code new Meteorite().generate(world, rand, x, y, z, false, false, false)}), drawing from
     * {@code rand}. A world without a level (a primer world) places none.
     */
    public void meteorite(int x, int y, int z, net.minecraft.util.RandomSource rand) {}

    /**
     * Duna's broken behemoth lantern ({@code WorldGeneratorDuna}) at column {@code (x, z)}: where the ground takes a torch
     * and the spot is free, the lantern with its 50% loot booklet two blocks north, drawing that coin from {@code rand}.
     * A world without a level places none (and so draws nothing, as 1.7 drew nothing where it did not stand).
     */
    public void brokenLantern(int x, int z, Random rand) {}

    /** Ike's pedestal re-stocking ({@code WorldGeneratorIke}): every pedestal in the chunk at {@code (i, j)} holds a divine shard. */
    public void restockPedestals(int i, int j) {}

    /**
     * A structure 1.7 built from this generator's random at {@code (x, z)} (the Ike tomb). The port builds it as a
     * structure that replays the generator's draws ({@code world/structure/BodyDungeonStructure}); a level places
     * nothing here, the replay records the site and the random's state.
     */
    public void dungeonSite(String kind, int x, int z, Random rand) {}

    /** 1.7's {@code world.rand}: a random no seed pins (hbm-space's bedrock ores drew their spread from it). */
    public abstract Random worldRandom();

    // ---------------------------------------------------------------- 1.7 questions

    public final boolean is(int x, int y, int z, Block block) {
        return block != null && get(x, y, z).is(block);
    }

    /** {@code getMaterial() == Material.air}. */
    public final boolean isAir(int x, int y, int z) {
        return get(x, y, z).isAir();
    }

    /** {@code getMaterial().isLiquid()}. */
    public static boolean liquid(BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    /** {@code getMaterial() == Material.water} (water, and 1.7's water-material fluids). */
    public static boolean water(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) || (liquid(state) && !state.getFluidState().is(FluidTags.LAVA));
    }

    /** {@code getMaterial().isSolid()}: anything that is neither air nor a liquid nor a plant-like non-blocking block. */
    public static boolean solid(BlockState state) {
        return !state.isAir() && !liquid(state) && state.blocksMotion();
    }

    /** {@code Block.isNormalCube()}: an opaque full cube. */
    public static boolean normalCube(BlockState state) {
        return Compat.isSolidRender(state);
    }

    /**
     * 1.7 {@code getHeightValue}: one above the highest block that stops light, here the highest block that blocks
     * motion or holds a fluid (leaves and water count, plants do not, as in 1.7).
     */
    public int heightValue(int x, int z) {
        for (int y = 255; y >= 0; y--) {
            BlockState state = get(x, y, z);
            if (!state.isAir() && (state.blocksMotion() || liquid(state))) return y + 1;
        }
        return 0;
    }

    /** A registered block by id, or null (a block another workstream has not registered yet). */
    public static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse(id)).orElse(null);
    }

    /** A registered block's default state by id, or null. */
    public static BlockState state(String id) {
        Block block = block(id);
        return block == null ? null : block.defaultBlockState();
    }
}
