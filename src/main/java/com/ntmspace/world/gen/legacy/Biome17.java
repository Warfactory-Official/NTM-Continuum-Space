package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * The part of a 1.7 {@code BiomeGenBase} chunk generation reads: the height parameters the noise field blends, the
 * surface blocks, and the per-column surface pass ({@code genTerrainBlocks}).
 */
public abstract class Biome17 {

    public float rootHeight = 0.1F;
    public float heightVariation = 0.2F;
    public float temperature = 0.5F;
    public float rainfall = 0.5F;
    /** Shared authored tint of BiomeGenBaseEve, BiomeGenBaseLaythe and BiomeGenBaseTekto. */
    public static final int PURPLE_WATER_MULTIPLIER = 0x5B009A;
    /** BiomeGenBase's original RGB multiplier, before applying the vanilla blue water sheet. */
    public int waterColorMultiplier = 0xFFFFFF;
    public Block17 topBlock = Block17.GRASS;
    public byte topMeta = 0;
    public Block17 fillerBlock = Block17.DIRT;

    /** SpaceDimensionProvider.waterColor: convert the old multiplier to a modern final water colour. */
    public final int waterColor(int vanillaColor) {
        var color = 0;
        for (var shift = 0; shift <= 16; shift += 8) {
            color |= Math.round((vanillaColor >> shift & 255) * (waterColorMultiplier >> shift & 255) / 255.0F) << shift;
        }
        return color;
    }

    /** 1.7 {@code getFloatTemperature}: the biome temperature, reduced above y 64 as 1.7 did. */
    public float getFloatTemperature(int x, int y, int z) {
        // hbm-space's celestial biomes all sit below 0.15 here whatever the height reduction (see BiomeMun), so
        // only the biome temperature matters to them.
        return temperature;
    }

    /** 1.7 {@code genTerrainBlocks}: replaces the column's stone with the biome surface. */
    public abstract void genTerrainBlocks(Random rand, Block17[] blocks, byte[] metas, int x, int z, double noise);
}
