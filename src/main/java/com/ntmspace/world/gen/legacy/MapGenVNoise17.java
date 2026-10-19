package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code MapGenVNoise} ({@code com.hbm.dim.noise}), ported line for line: Voronoi plates of rock floated on
 * a body's sea (Eve's mercury, Moho's lava) in one biome, thickest at the cell centres, with cracks along the edges.
 *
 * <p>1.7 compared with {@code Blocks.air}, which is {@code null} in the port's primer.
 */
public class MapGenVNoise17 {

    public Block17 fluidBlock;
    public Block17 surfBlock;
    public Block17 rockBlock;
    public int cellSize = 32;
    public double crackSize = 2.0;
    public int plateStartY = 55;
    public int plateThickness = 35;
    public double shapeExponent = 2.0;

    /** 1.7 {@code applyToBiome}. */
    public Biome17 applyToBiome;

    /** 1.7 {@code func_151539_a} (it overrode the whole map-generator loop; only this chunk is touched). */
    public void generate(Legacy17World world, int chunkX, int chunkZ, Block17[] blocks) {
        long seed = world.seed();
        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {

                if (applyToBiome != null) {
                    Biome17 biome = world.biomeAt(lx + chunkX * 16, lz + chunkZ * 16);
                    if (biome != applyToBiome) continue;
                }

                int gx = startX + lx;
                int gz = startZ + lz;
                double d0 = Double.MAX_VALUE, d1 = Double.MAX_VALUE;
                int cellX = gx / cellSize;
                int cellZ = gz / cellSize;

                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        double[] center = getCellCenter(cellX + dx, cellZ + dz, seed);
                        double dist = distance(gx, gz, center[0], center[1]);
                        if (dist < d0) {
                            d1 = d0;
                            d0 = dist;
                        } else if (dist < d1) {
                            d1 = dist;
                        }
                    }
                }

                double edge = d1 - d0;
                if (edge > crackSize) {
                    double alpha = (edge - crackSize) / (cellSize - crackSize);
                    alpha = Math.max(0, Math.min(1, alpha));
                    alpha = Math.pow(alpha, shapeExponent);
                    int topY = plateStartY + (int) (plateThickness * alpha);

                    for (int y = plateStartY; y < topY && y < 256; y++) {
                        int index = (lx * 16 + lz) * 256 + y;

                        if (blocks[index] == null || blocks[index] == fluidBlock) {
                            blocks[index] = rockBlock;
                        }
                    }
                    if (topY > plateStartY) {
                        int topIndex = (lx * 16 + lz) * 256 + (topY - 1);
                        blocks[topIndex] = surfBlock;
                    }
                }
            }
        }
    }

    private double[] getCellCenter(int cellX, int cellZ, long worldSeed) {
        long hash = (long) cellX * 341873128712L + (long) cellZ * 132897987541L + worldSeed;
        Random rand = new Random(hash);
        double offsetX = (rand.nextDouble() - 0.5) * cellSize;
        double offsetZ = (rand.nextDouble() - 0.5) * cellSize;
        double centerX = cellX * (double) cellSize + cellSize / 2.0 + offsetX;
        double centerZ = cellZ * (double) cellSize + cellSize / 2.0 + offsetZ;
        return new double[] {centerX, centerZ};
    }

    private static double distance(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
