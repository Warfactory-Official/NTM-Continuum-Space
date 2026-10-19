package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * 1.7 ore veins as hbm-space's world generators placed them: FML 1.7.10's per-chunk generator seeding
 * ({@code GameRegistry.generateWorld}), hbm-space's {@code DungeonToolbox.generateOre} and vanilla's
 * {@code WorldGenMinable} (constants checked against the 1.7.10 jar's bytecode).
 *
 * <p>A vein's random draws never depend on the world, so a vein whose ore is not ported yet is still drawn
 * ({@code placer} null): the veins after it land where 1.7 put them.
 */
public final class Ores17 {

    /** Writes one ore block where the world holds the vein's target block (1.7 {@code isReplaceableOreGen}). */
    @FunctionalInterface
    public interface Placer {
        void place(int x, int y, int z);
    }

    private Ores17() {}

    /**
     * The {@code Random} FML 1.7.10 handed every {@code IWorldGenerator} for chunk {@code (chunkX, chunkZ)}: seeded
     * afresh for each generator from the world seed and the chunk position.
     */
    public static Random generatorRandom(long worldSeed, int chunkX, int chunkZ) {
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
        fmlRandom.setSeed(chunkSeed);
        return fmlRandom;
    }

    /** hbm-space's {@code DungeonToolbox.generateOre}: {@code veinCount} veins of {@code amount}, block coordinates. */
    public static void generateOre(Random rand, int x, int z, int veinCount, int amount, int minHeight, int variance, Placer placer) {
        for (int i = 0; i < veinCount; i++) {
            int vx = x + rand.nextInt(16);
            int vy = minHeight + (variance > 0 ? rand.nextInt(variance) : 0);
            int vz = z + rand.nextInt(16);

            minable(rand, vx, vy, vz, amount, placer);
        }
    }

    /** 1.7 {@code WorldGenMinable.generate}: one vein of {@code numberOfBlocks} around {@code (x + 8, y, z + 8)}. */
    public static void minable(Random rand, int x, int y, int z, int numberOfBlocks, Placer placer) {
        float f = rand.nextFloat() * (float) Math.PI;
        double d0 = (double) ((float) (x + 8) + Math17.sin(f) * (float) numberOfBlocks / 8.0F);
        double d1 = (double) ((float) (x + 8) - Math17.sin(f) * (float) numberOfBlocks / 8.0F);
        double d2 = (double) ((float) (z + 8) + Math17.cos(f) * (float) numberOfBlocks / 8.0F);
        double d3 = (double) ((float) (z + 8) - Math17.cos(f) * (float) numberOfBlocks / 8.0F);
        double d4 = (double) (y + rand.nextInt(3) - 2);
        double d5 = (double) (y + rand.nextInt(3) - 2);

        for (int l = 0; l <= numberOfBlocks; ++l) {
            double d6 = d0 + (d1 - d0) * (double) l / (double) numberOfBlocks;
            double d7 = d4 + (d5 - d4) * (double) l / (double) numberOfBlocks;
            double d8 = d2 + (d3 - d2) * (double) l / (double) numberOfBlocks;
            double d9 = rand.nextDouble() * (double) numberOfBlocks / 16.0D;
            double d10 = (double) (Math17.sin((float) l * (float) Math.PI / (float) numberOfBlocks) + 1.0F) * d9 + 1.0D;
            double d11 = (double) (Math17.sin((float) l * (float) Math.PI / (float) numberOfBlocks) + 1.0F) * d9 + 1.0D;
            int i1 = Math17.floor_double(d6 - d10 / 2.0D);
            int j1 = Math17.floor_double(d7 - d11 / 2.0D);
            int k1 = Math17.floor_double(d8 - d10 / 2.0D);
            int l1 = Math17.floor_double(d6 + d10 / 2.0D);
            int i2 = Math17.floor_double(d7 + d11 / 2.0D);
            int j2 = Math17.floor_double(d8 + d10 / 2.0D);

            if (placer == null) continue;

            for (int k2 = i1; k2 <= l1; ++k2) {
                double d12 = ((double) k2 + 0.5D - d6) / (d10 / 2.0D);

                if (d12 * d12 < 1.0D) {
                    for (int l2 = j1; l2 <= i2; ++l2) {
                        double d13 = ((double) l2 + 0.5D - d7) / (d11 / 2.0D);

                        if (d12 * d12 + d13 * d13 < 1.0D) {
                            for (int i3 = k1; i3 <= j2; ++i3) {
                                double d14 = ((double) i3 + 0.5D - d8) / (d10 / 2.0D);

                                if (d12 * d12 + d13 * d13 + d14 * d14 < 1.0D) {
                                    placer.place(k2, l2, i3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
