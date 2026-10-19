package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * hbm-space's {@code MapGenRoots} ({@code com.hbm.dim.mapgen}), ported line for line: 1.7's cave tunnels, but instead
 * of digging they turn the stone they pass through into oak logs (obsidian below y 10) and grass where a root breaks the
 * surface on filler. Laythe's "roots".
 *
 * <p><b>Ported oddities:</b> the water test of the original tunnel code still runs but its result is ignored (roots grow
 * under the sea too); like 1.7's tunnels, each dig step writes the block one above the y it tested.
 */
public class MapGenRoots17 extends MapGen17 {

    protected void func_151542_a(long seed, int chunkX, int chunkZ, Block17[] blocks, double x, double y, double z) {
        this.func_151541_a(seed, chunkX, chunkZ, blocks, x, y, z, 1.0F + this.rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    protected void func_151541_a(long seed, int p_151541_3_, int p_151541_4_, Block17[] p_151541_5_,
                                 double p_151541_6_, double p_151541_8_, double p_151541_10_, float p_151541_12_, float p_151541_13_,
                                 float p_151541_14_, int p_151541_15_, int p_151541_16_, double p_151541_17_) {
        double d4 = (double) (p_151541_3_ * 16 + 8);
        double d5 = (double) (p_151541_4_ * 16 + 8);
        float f3 = 0.0F;
        float f4 = 0.0F;
        Random random = new Random(seed);

        if (p_151541_16_ <= 0) {
            int j1 = this.range * 16 - 16;
            p_151541_16_ = j1 - random.nextInt(j1 / 4);
        }

        boolean flag2 = false;

        if (p_151541_15_ == -1) {
            p_151541_15_ = p_151541_16_ / 2;
            flag2 = true;
        }

        int k1 = random.nextInt(p_151541_16_ / 2) + p_151541_16_ / 4;

        for (boolean flag = random.nextInt(6) == 0; p_151541_15_ < p_151541_16_; ++p_151541_15_) {
            double d6 = 1.5D + (double) (Math17.sin((float) p_151541_15_ * (float) Math.PI / (float) p_151541_16_) * p_151541_12_ * 1.0F);
            double d7 = d6 * p_151541_17_;
            float f5 = Math17.cos(p_151541_14_);
            float f6 = Math17.sin(p_151541_14_);
            p_151541_6_ += (double) (Math17.cos(p_151541_13_) * f5);
            p_151541_8_ += (double) f6;
            p_151541_10_ += (double) (Math17.sin(p_151541_13_) * f5);

            if (flag) {
                p_151541_14_ *= 0.92F;
            } else {
                p_151541_14_ *= 0.7F;
            }

            p_151541_14_ += f4 * 0.1F;
            p_151541_13_ += f3 * 0.1F;
            f4 *= 0.9F;
            f3 *= 0.75F;
            f4 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            f3 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (!flag2 && p_151541_15_ == k1 && p_151541_12_ > 1.0F && p_151541_16_ > 0) {
                this.func_151541_a(random.nextLong(), p_151541_3_, p_151541_4_, p_151541_5_, p_151541_6_, p_151541_8_,
                        p_151541_10_, random.nextFloat() * 0.25F + 0.5F, p_151541_13_ - ((float) Math.PI / 2F),
                        p_151541_14_ / 3.0F, p_151541_15_, p_151541_16_, 1.0D);
                this.func_151541_a(random.nextLong(), p_151541_3_, p_151541_4_, p_151541_5_, p_151541_6_, p_151541_8_,
                        p_151541_10_, random.nextFloat() * 0.25F + 0.5F, p_151541_13_ + ((float) Math.PI / 2F),
                        p_151541_14_ / 3.0F, p_151541_15_, p_151541_16_, 1.0D);
                return;
            }

            if (flag2 || random.nextInt(4) != 0) {
                double d8 = p_151541_6_ - d4;
                double d9 = p_151541_10_ - d5;
                double d10 = (double) (p_151541_16_ - p_151541_15_);
                double d11 = (double) (p_151541_12_ + 2.0F + 16.0F);

                if (d8 * d8 + d9 * d9 - d10 * d10 > d11 * d11) {
                    return;
                }

                if (p_151541_6_ >= d4 - 16.0D - d6 * 2.0D && p_151541_10_ >= d5 - 16.0D - d6 * 2.0D
                        && p_151541_6_ <= d4 + 16.0D + d6 * 2.0D && p_151541_10_ <= d5 + 16.0D + d6 * 2.0D) {
                    int i4 = Math17.floor_double(p_151541_6_ - d6) - p_151541_3_ * 16 - 1;
                    int l1 = Math17.floor_double(p_151541_6_ + d6) - p_151541_3_ * 16 + 1;
                    int j4 = Math17.floor_double(p_151541_8_ - d7) - 1;
                    int i2 = Math17.floor_double(p_151541_8_ + d7) + 1;
                    int k4 = Math17.floor_double(p_151541_10_ - d6) - p_151541_4_ * 16 - 1;
                    int j2 = Math17.floor_double(p_151541_10_ + d6) - p_151541_4_ * 16 + 1;

                    if (i4 < 0) i4 = 0;
                    if (l1 > 16) l1 = 16;
                    if (j4 < 1) j4 = 1;
                    if (i2 > 248) i2 = 248;
                    if (k4 < 0) k4 = 0;
                    if (j2 > 16) j2 = 16;

                    boolean flag3 = false;
                    int k2;
                    int j3;

                    for (k2 = i4; !flag3 && k2 < l1; ++k2) {
                        for (int l2 = k4; !flag3 && l2 < j2; ++l2) {
                            for (int i3 = i2 + 1; !flag3 && i3 >= j4 - 1; --i3) {
                                j3 = (k2 * 16 + l2) * 256 + i3;

                                if (i3 >= 0 && i3 < 256) {
                                    if (isOceanBlock(p_151541_5_, j3)) {
                                        flag3 = true;
                                    }

                                    if (i3 != j4 - 1 && k2 != i4 && k2 != l1 - 1 && l2 != k4 && l2 != j2 - 1) {
                                        i3 = j4;
                                    }
                                }
                            }
                        }
                    }

                    for (k2 = i4; k2 < l1; ++k2) {
                        double d13 = ((double) (k2 + p_151541_3_ * 16) + 0.5D - p_151541_6_) / d6;

                        for (j3 = k4; j3 < j2; ++j3) {
                            double d14 = ((double) (j3 + p_151541_4_ * 16) + 0.5D - p_151541_10_) / d6;
                            int k3 = (k2 * 16 + j3) * 256 + i2;
                            boolean flag1 = false;

                            if (d13 * d13 + d14 * d14 < 1.0D) {
                                for (int l3 = i2 - 1; l3 >= j4; --l3) {
                                    double d12 = ((double) l3 + 0.5D - p_151541_8_) / d7;

                                    if (d12 > -0.7D && d13 * d13 + d12 * d12 + d14 * d14 < 1.0D) {
                                        if (isTopBlock(p_151541_5_, k3, k2, j3, p_151541_3_, p_151541_4_)) {
                                            flag1 = true;
                                        }
                                        digBlock(p_151541_5_, k3, k2, l3, j3, p_151541_3_, p_151541_4_, flag1);
                                    }

                                    --k3;
                                }
                            }
                        }
                    }

                    if (flag2) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void recursiveGenerate(int p_151538_2_, int p_151538_3_, int p_151538_4_, int p_151538_5_, Block17[] p_151538_6_) {
        int i1 = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(15) + 1) + 1);

        if (this.rand.nextInt(7) != 0) {
            i1 = 0;
        }

        for (int j1 = 0; j1 < i1; ++j1) {
            double d0 = (double) (p_151538_2_ * 16 + this.rand.nextInt(16));
            double d1 = (double) this.rand.nextInt(this.rand.nextInt(120) + 8);
            double d2 = (double) (p_151538_3_ * 16 + this.rand.nextInt(16));
            int k1 = 1;

            if (this.rand.nextInt(4) == 0) {
                this.func_151542_a(this.rand.nextLong(), p_151538_4_, p_151538_5_, p_151538_6_, d0, d1, d2);
                k1 += this.rand.nextInt(4);
            }

            for (int l1 = 0; l1 < k1; ++l1) {
                float f = this.rand.nextFloat() * (float) Math.PI * 2.0F;
                float f1 = (this.rand.nextFloat() - 0.5F) * 2.0F / 16.0F;
                float f2 = this.rand.nextFloat() * 0.5F + this.rand.nextFloat();

                if (this.rand.nextInt(10) == 0) {
                    f2 *= this.rand.nextFloat() * this.rand.nextFloat() * 2.5F + 1.0F;
                }

                this.func_151541_a(this.rand.nextLong(), p_151538_4_, p_151538_5_, p_151538_6_, d0, d1, d2, f2, f, f1, 0, 0, 2.25D);
            }
        }
    }

    protected boolean isOceanBlock(Block17[] data, int index) {
        return data[index] == Block17.FLOWING_WATER || data[index] == Block17.WATER;
    }

    private boolean isTopBlock(Block17[] data, int index, int x, int z, int chunkX, int chunkZ) {
        Biome17 biome = world.biomeAt(x + chunkX * 16, z + chunkZ * 16);
        return data[index] == biome.topBlock;
    }

    /** Stone becomes obsidian below y 10, else an oak log, with grass on exposed filler above. */
    protected void digBlock(Block17[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        Biome17 biome = world.biomeAt(x + chunkX * 16, z + chunkZ * 16);
        Block17 filler = biome.fillerBlock;
        Block17 block = data[index];

        if (block == Block17.STONE) {
            if (y < 10) {
                data[index] = Block17.OBSIDIAN;
            } else {
                data[index] = Block17.OAK_LOG;

                if (foundTop && data[index + 1] == filler) {
                    data[index + 1] = Block17.GRASS;
                }
            }
        }
    }
}
