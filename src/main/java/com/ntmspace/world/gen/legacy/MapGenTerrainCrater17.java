package com.ntmspace.world.gen.legacy;

/**
 * hbm-space's {@code com.hbm.world.gen.terrain.MapGenCrater} (HBM's crater, not the {@code com.hbm.dim.mapgen} one in
 * {@link MapGenCrater}), ported line for line: one crater per {@code frequency} origin chunks (radius 8 to 63 unless
 * {@link #setSize} is called), a parabolic bowl dug into the first solid block at or below y 127, its centre third
 * lined with {@link #regolith} and the rest with {@link #rock}. Ike's "Sellafield" hot spots use it.
 */
public class MapGenTerrainCrater17 extends MapGen17 {

    private int frequency = 100;
    private int minSize = 8;
    private int maxSize = 64;

    public Block17 regolith;
    public Block17 rock;

    /** 1.7 {@code targetBiome}: the only biome an origin chunk may start a crater in, or null for any. */
    public Biome17 targetBiome;

    public MapGenTerrainCrater17(int frequency) {
        this.frequency = frequency;
    }

    public void setSize(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;

        this.range = (maxSize / 8) + 1;
    }

    private double depthFunc(double x, double rad, double depth) {
        return CraterGeometry.depth(x, rad, depth, 2);
    }

    // This function is looped over from -this.range to +this.range on both XZ axes.
    @Override
    protected void recursiveGenerate(int offsetX, int offsetZ, int chunkX, int chunkZ, Block17[] blocks) {
        if (rand.nextInt(frequency) == 0 && (targetBiome == null || targetBiome == world.biomeAt(offsetX * 16, offsetZ * 16))) {
            int xCoord = -offsetX + chunkX;
            int zCoord = -offsetZ + chunkZ;

            double radius = rand.nextInt(maxSize - minSize) + minSize;
            double depth = CraterGeometry.maximumDepth(radius);

            for (int bx = 15; bx >= 0; bx--) { // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
                for (int bz = 15; bz >= 0; bz--) {
                    for (int y = 127; y >= 0; y--) {
                        int index = (bx * 16 + bz) * 256 + y;

                        if (blocks[index] != null && (blocks[index].isOpaqueCube() || blocks[index].isLiquid())) {
                            // x, z are the coordinates relative to the target virtual chunk origin
                            int x = xCoord * 16 + bx;
                            int z = zCoord * 16 + bz;

                            // y is at the current height now
                            double r = Math.sqrt(x * x + z * z);

                            if (r - rand.nextInt(3) <= radius) {
                                // Carve out to intended depth
                                int dep = (int) Math17.clamp_double(depthFunc(r, radius, depth), 0, y - 1);
                                for (int i = 0; i < dep; i++) {
                                    blocks[index - i] = null;
                                }

                                index -= dep;
                                y -= dep;

                                dep = Math.min(3, y - 1);

                                // Fill back in
                                if (r + rand.nextInt(3) <= radius / 3D) {
                                    for (int i = 0; i < dep; i++) {
                                        blocks[index - i] = regolith;
                                    }
                                } else {
                                    for (int i = 0; i < dep; i++) {
                                        blocks[index - i] = rock;
                                    }
                                }
                            }

                            break;
                        }
                    }
                }
            }
        }
    }
}
