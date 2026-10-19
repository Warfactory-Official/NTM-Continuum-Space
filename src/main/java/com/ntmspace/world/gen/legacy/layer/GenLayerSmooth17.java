package com.ntmspace.world.gen.legacy.layer;

/** Minecraft 1.7.10's {@code GenLayerSmooth}: a cell between two equal opposite neighbours takes their value. */
public class GenLayerSmooth17 extends GenLayer17 {

    public GenLayerSmooth17(long seed, GenLayer17 parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int width, int depth) {
        int i1 = x - 1;
        int j1 = z - 1;
        int k1 = width + 2;
        int l1 = depth + 2;
        int[] aint = this.parent.getInts(i1, j1, k1, l1);
        int[] aint1 = new int[width * depth];

        for (int i2 = 0; i2 < depth; ++i2) {
            for (int j2 = 0; j2 < width; ++j2) {
                int k2 = aint[j2 + 0 + (i2 + 1) * k1];
                int l2 = aint[j2 + 2 + (i2 + 1) * k1];
                int i3 = aint[j2 + 1 + (i2 + 0) * k1];
                int j3 = aint[j2 + 1 + (i2 + 2) * k1];
                int k3 = aint[j2 + 1 + (i2 + 1) * k1];

                if (k2 == l2 && i3 == j3) {
                    this.initChunkSeed((long) (j2 + x), (long) (i2 + z));
                    if (this.nextInt(2) == 0) k3 = k2;
                    else k3 = i3;
                } else {
                    if (k2 == l2) k3 = k2;
                    if (i3 == j3) k3 = i3;
                }

                aint1[j2 + i2 * width] = k3;
            }
        }
        return aint1;
    }
}
