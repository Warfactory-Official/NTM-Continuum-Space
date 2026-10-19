package com.ntmspace.world.gen.legacy;

/**
 * What 1.7 chunk generation read from its {@code World}: the seed and the biome at a block column. A celestial body
 * with one biome (the Mun, through {@code WorldChunkManagerHell}) answers the same biome everywhere.
 */
public interface Legacy17World {

    long seed();

    Biome17 biomeAt(int x, int z);
}
