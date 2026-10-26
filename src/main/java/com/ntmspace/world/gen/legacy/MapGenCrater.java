package com.ntmspace.world.gen.legacy;

/** hbm-space's {@code MapGenCrater} (com.hbm.dim.mapgen, not HBM's world-gen crater), ported line for line. */
public class MapGenCrater extends MapGen17 {

    private final int chancePerChunk;
    private int minSize = 8;
    private int maxSize = 64;

    public Block17 regolith;
    public Block17 rock;

    // Note that the chance is effectively squared, so make it lower than you normally would
    public MapGenCrater(int chancePerChunk) {
        this.chancePerChunk = chancePerChunk;
    }

    public void setSize(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.range = (maxSize / 8) + 1;
    }

    private double depthFunc(double x, double rad, double depth) {
        return CraterGeometry.depth(x, rad, depth, 4);
    }

    // This function is looped over from -this.range to +this.range on both XZ axes.
    @Override
    protected void recursiveGenerate(int offsetX, int offsetZ, int chunkX, int chunkZ, Block17[] blocks) {
        if (rand.nextInt(chancePerChunk) == Math.abs(offsetX) % chancePerChunk
                && rand.nextInt(chancePerChunk) == Math.abs(offsetZ) % chancePerChunk) {

            double radius = rand.nextInt(maxSize - minSize) + minSize;
            double depth = CraterGeometry.maximumDepth(radius);

            int xCoord = -offsetX + chunkX;
            int zCoord = -offsetZ + chunkZ;

            for (int bx = 15; bx >= 0; bx--) { // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
                for (int bz = 15; bz >= 0; bz--) {
                    for (int y = 254; y >= 0; y--) {
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
                                for (int i = 0; i < dep; i++) blocks[index - i] = null;

                                index -= dep;
                                y -= dep;

                                dep = Math.min(3, y - 1);

                                // Fill back in
                                if (r + rand.nextInt(3) <= radius / 3D) {
                                    for (int i = 0; i < dep; i++) blocks[index - i] = regolith;
                                } else {
                                    for (int i = 0; i < dep; i++) blocks[index - i] = rock;
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
