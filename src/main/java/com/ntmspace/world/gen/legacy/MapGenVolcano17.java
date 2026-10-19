package com.ntmspace.world.gen.legacy;

/**
 * hbm-space's {@code MapGenVolcano} ({@code com.hbm.dim.mapgen}), ported line for line: a rare cone of the material
 * block raised over the terrain with a crater, and the core block at its centre when {@code enableVolcanoGen}
 * (hbm-space's {@code SpaceConfig}, default on) is set.
 */
public class MapGenVolcano17 extends MapGen17 {

    /** hbm-space's {@code SpaceConfig.enableVolcanoGen} default. */
    public static boolean enableVolcanoGen = true;

    private final int chancePerChunk;
    private int minSize = 32;
    private int maxSize = 64;

    private Block17 coreBlock;
    private Block17 materialBlock;

    // Note that the chance is effectively squared, so make it lower than you normally would
    public MapGenVolcano17(int chancePerChunk) {
        this.chancePerChunk = chancePerChunk;
    }

    public void setSize(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.range = (maxSize / 8) + 1;
    }

    public void setMaterial(Block17 coreBlock, Block17 mateBlock) {
        this.coreBlock = coreBlock;
        this.materialBlock = mateBlock;
    }

    private double heightFunc(double x, double rad, double depth) {
        double xs = x / (rad * 2);
        double inner = (x * x * x) / (rad / 4) + 32;
        double outer = 1 / (xs * xs);
        return Math.min(inner, outer) * depth;
    }

    // This function is looped over from -this.range to +this.range on both XZ axes.
    @Override
    protected void recursiveGenerate(int offsetX, int offsetZ, int chunkX, int chunkZ, Block17[] blocks) {
        if (rand.nextInt(chancePerChunk) == Math.abs(offsetX) % chancePerChunk && rand.nextInt(chancePerChunk) == Math.abs(offsetZ) % chancePerChunk) {

            double radius = rand.nextInt(maxSize - minSize) + minSize;
            double depth = 0.75D;

            int xCoord = -offsetX + chunkX;
            int zCoord = -offsetZ + chunkZ;

            for (int bx = 15; bx >= 0; bx--) { // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
                for (int bz = 15; bz >= 0; bz--) {
                    for (int y = 254; y >= 0; y--) {
                        int index = (bx * 16 + bz) * 256 + y;

                        if (blocks[index] != null && blocks[index].isOpaqueCube()) {
                            // x, z are the coordinates relative to the target virtual chunk origin
                            int x = xCoord * 16 + bx;
                            int z = zCoord * 16 + bz;

                            // y is at the current height now
                            double r = Math.sqrt(x * x + z * z);

                            if (r - rand.nextInt(16) <= radius) {
                                // Carve out to intended depth
                                int height = (int) Math17.clamp_double(heightFunc(r, radius, depth), 0, y - 1);
                                if (height > 0) {
                                    for (int i = 0; i < height && i + y < 255; i++) {
                                        blocks[index + i] = materialBlock;
                                    }
                                } else {
                                    for (int i = 0; i > height && i + y > 1; i--) {
                                        blocks[index + i + 1] = null;
                                    }
                                }

                                index += height;
                                y += height;

                                if (x == 0 && z == 0 && enableVolcanoGen) blocks[index + 1] = coreBlock;
                            }

                            break;
                        }
                    }
                }
            }
        }
    }
}
