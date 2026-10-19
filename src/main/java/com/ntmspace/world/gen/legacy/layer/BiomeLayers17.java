package com.ntmspace.world.gen.legacy.layer;

/**
 * hbm-space's {@code WorldChunkManagerCelestial.BiomeGenLayers}: a body's two biome chains, both seeded from the world
 * seed. The {@code biome} chain (4-block cells) feeds the terrain noise ({@code getBiomesForGeneration}); the
 * {@code detail} chain (single blocks, the Voronoi zoom) is the biome a block column has ({@code getBiomeGenAt}), read by
 * the surface pass and the map generators.
 */
public final class BiomeLayers17 {

    public final GenLayer17 biome;
    public final GenLayer17 detail;

    public BiomeLayers17(GenLayer17 biome, GenLayer17 detail, long seed) {
        this.biome = biome;
        this.detail = detail;
        biome.initWorldGenSeed(seed);
        detail.initWorldGenSeed(seed);
    }
}
