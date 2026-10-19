package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.legacy.layer.BiomeLayers17;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongFunction;

/**
 * A celestial body whose biomes come from a 1.7 {@code GenLayer} stack (hbm-space's
 * {@code WorldChunkManagerCelestial}): the terrain noise reads the 4-block biome chain over the 10 x 10 cell window
 * 1.7's {@code getBiomesForGeneration} asked for, and every block-column lookup (the surface pass, the map generators,
 * {@code World.getBiomeGenForCoords}) reads the Voronoi detail chain, 16 x 16 blocks at a time as 1.7's
 * {@code BiomeCache} did.
 *
 * <p>Biome ids are 1.7 ids; each body maps the ids its chain produces to its {@link Biome17}s with {@link #register}.
 */
public abstract class LayeredBodyTerrain17 extends CelestialTerrain17 {

    private final BiomeLayers17 layers;
    private final Biome17[] byId = new Biome17[256];

    private int[] genWindow;
    private int windowX;
    private int windowZ;

    private final Map<Long, int[]> detail = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return size() > 64;
        }
    };

    protected LayeredBodyTerrain17(long seed, LongFunction<BiomeLayers17> layers) {
        super(seed);
        this.layers = layers.apply(seed);
    }

    /** Maps the 1.7 biome id {@code id} to {@code biome}. */
    protected final void register(int id, Biome17 biome) {
        byId[id] = biome;
    }

    /** The biome with 1.7 id {@code id}; every id a body's chain produces is registered. */
    public final Biome17 biome(int id) {
        Biome17 biome = id >= 0 && id < byId.length ? byId[id] : null;
        if (biome == null) throw new IllegalStateException("1.7 biome id " + id + " has no biome on " + getClass().getSimpleName());
        return biome;
    }

    /** The 1.7 biome id of block column {@code (x, z)} (the detail chain). */
    public final int biomeIdAt(int x, int z) {
        return chunkBiomeIds(x >> 4, z >> 4)[(x & 15) + (z & 15) * 16];
    }

    /**
     * The detail-chain ids of a chunk, index {@code x + z * 16}: 1.7's {@code loadBlockGeneratorData}, the
     * {@code biomesForGeneration} array a chunk provider holds after building its primer.
     */
    public final int[] chunkBiomeIds(int chunkX, int chunkZ) {
        long key = (long) chunkX << 32 ^ (chunkZ & 0xFFFFFFFFL);
        int[] ids = detail.get(key);
        if (ids == null) {
            ids = layers.detail.getInts(chunkX * 16, chunkZ * 16, 16, 16);
            detail.put(key, ids);
        }
        return ids;
    }

    @Override
    public final Biome17 biomeAt(int x, int z) {
        return biome(biomeIdAt(x, z));
    }

    @Override
    protected void generateBlocks(int x, int z, Block17[] blocks) {
        // 1.7: biomesForGeneration = getBiomesForGeneration(biomesForGeneration, x * 4 - 2, z * 4 - 2, 10, 10)
        windowX = x * 4 - 2;
        windowZ = z * 4 - 2;
        genWindow = layers.biome.getInts(windowX, windowZ, 10, 10);
        super.generateBlocks(x, z, blocks);
    }

    @Override
    protected final Biome17 generationBiome(int cellX, int cellZ) {
        return biome(genWindow[(cellX - windowX) + (cellZ - windowZ) * 10]);
    }

    /** Whether any column of the chunk has the biome {@code id} (the 1.7 bodies' scan of {@code biomesForGeneration}). */
    protected final boolean chunkHas(int chunkX, int chunkZ, int id) {
        for (int biome : chunkBiomeIds(chunkX, chunkZ)) {
            if (biome == id) return true;
        }
        return false;
    }
}
