package com.ntmspace.world.populate;

import java.util.Random;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7's {@code WorldGenLakes} (checked against the 1.7.10 jar's bytecode, class {@code arx}: the same random draws in
 * the same order) and hbm-space's {@code WorldGenLiquidsCelestial} (vanilla's spring with the body's stone as its
 * target), as hbm-space's {@code BiomeDecoratorCelestial} ran them: Moho's and Eve's lava springs, Ike's bromine lakes.
 */
public final class Lakes17 {

    private Lakes17() {}

    /**
     * {@code WorldGenLakes.generate} with a liquid of water material (Ike's bromine; null while it is not registered:
     * the draws are made, nothing is placed). The rest of 1.7's method does not
     * apply to such a lake on a celestial body: grass over dirt needs dirt (none on Ike) and sky light, the lava rim
     * needs lava, and the freezing of its surface needs a biome colder than 0.15 (Ike's is vanilla's default 0.5).
     */
    public static boolean lake(PopulateWorld world, Random rand, int x, int y, int z, BlockState liquid) {
        x -= 8;
        for (z -= 8; y > 5 && world.isAir(x, y, z); --y) {
        }
        if (y <= 4) return false;
        y -= 4;

        boolean[] shape = new boolean[2048];
        int count = rand.nextInt(4) + 4;
        for (int i = 0; i < count; ++i) {
            double d0 = rand.nextDouble() * 6.0D + 3.0D;
            double d1 = rand.nextDouble() * 4.0D + 2.0D;
            double d2 = rand.nextDouble() * 6.0D + 3.0D;
            double d3 = rand.nextDouble() * (16.0D - d0 - 2.0D) + 1.0D + d0 / 2.0D;
            double d4 = rand.nextDouble() * (8.0D - d1 - 4.0D) + 2.0D + d1 / 2.0D;
            double d5 = rand.nextDouble() * (16.0D - d2 - 2.0D) + 1.0D + d2 / 2.0D;

            for (int k1 = 1; k1 < 15; ++k1) {
                for (int l1 = 1; l1 < 15; ++l1) {
                    for (int i2 = 1; i2 < 7; ++i2) {
                        double d6 = ((double) k1 - d3) / (d0 / 2.0D);
                        double d7 = ((double) i2 - d4) / (d1 / 2.0D);
                        double d8 = ((double) l1 - d5) / (d2 / 2.0D);
                        double d9 = d6 * d6 + d7 * d7 + d8 * d8;
                        if (d9 < 1.0D) shape[(k1 * 16 + l1) * 8 + i2] = true;
                    }
                }
            }
        }

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 8; ++k) {
                    boolean edge = !shape[(i * 16 + j) * 8 + k] && (i < 15 && shape[((i + 1) * 16 + j) * 8 + k]
                            || i > 0 && shape[((i - 1) * 16 + j) * 8 + k]
                            || j < 15 && shape[(i * 16 + j + 1) * 8 + k]
                            || j > 0 && shape[(i * 16 + (j - 1)) * 8 + k]
                            || k < 7 && shape[(i * 16 + j) * 8 + k + 1]
                            || k > 0 && shape[(i * 16 + j) * 8 + (k - 1)]);
                    if (edge) {
                        BlockState state = world.get(x + i, y + k, z + j);
                        if (k >= 4 && PopulateWorld.liquid(state)) return false;
                        if (k < 4 && !PopulateWorld.solid(state) && (liquid == null || !state.is(liquid.getBlock()))) return false;
                    }
                }
            }
        }

        if (liquid == null) return true;
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 8; ++k) {
                    if (shape[(i * 16 + j) * 8 + k]) {
                        world.set(x + i, y + k, z + j, k >= 4 ? Blocks.AIR.defaultBlockState() : liquid);
                    }
                }
            }
        }
        return true;
    }

    /**
     * hbm-space's {@code WorldGenLiquidsCelestial.generate}: a spring where the cell is enclosed by {@code target} above,
     * below and on three sides with one side open. 1.7 ran the liquid's {@code updateTick} at once; here it is
     * scheduled for the next tick.
     */
    public static boolean spring(PopulateWorld world, int x, int y, int z, BlockState liquid, net.minecraft.world.level.block.Block target) {
        if (!world.is(x, y + 1, z, target)) return false;
        if (!world.is(x, y - 1, z, target)) return false;
        if (!world.isAir(x, y, z) && !world.is(x, y, z, target)) return false;

        int stone = 0;
        if (world.is(x - 1, y, z, target)) ++stone;
        if (world.is(x + 1, y, z, target)) ++stone;
        if (world.is(x, y, z - 1, target)) ++stone;
        if (world.is(x, y, z + 1, target)) ++stone;

        int air = 0;
        if (world.isAir(x - 1, y, z)) ++air;
        if (world.isAir(x + 1, y, z)) ++air;
        if (world.isAir(x, y, z - 1)) ++air;
        if (world.isAir(x, y, z + 1)) ++air;

        if (stone == 3 && air == 1) {
            world.set(x, y, z, liquid);
            world.fluidTick(x, y, z);
        }
        return true;
    }
}
