package com.ntmspace.world.populate;

import java.util.Random;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Eve's {@code WorldGeneratorEve} features: hbm-space's {@code WorldGenEveSpike} (an ice-spike-shaped spire of Eve rock in
 * the seismic plains, vanilla's {@code WorldGenIceSpike} with Eve's blocks) and {@code WorldGenElectricVolcano} (a basalt
 * cone with a neodymium core and an electric geyser), line for line. Both reach well past the chunk; what falls outside
 * the area the modern step may write is dropped (1.7 generated the neighbouring chunks instead, the "cascade" its
 * {@code TODO} names).
 */
public final class EveFeatures17 {

    private EveFeatures17() {
    }

    public static final int VOLCANO_HEIGHT = 30;
    public static final int VOLCANO_WIDTH = 22;

    public record VolcanoShape(int y, int l, int i1) {
        public BoundingBox bounds(int x, int z) {
            return new BoundingBox(x - i1, y - l + 1, z - i1,
                    x + i1, y + Math.max(l - 1, 5), z + i1);
        }
    }

    private static int ceil(float value) {
        int i = (int) value;
        return value > (float) i ? i + 1 : i;
    }

    /**
     * {@code WorldGenEveSpike.generate}.
     */
    public static boolean spike(PopulateWorld world, Random rand, int x, int y, int z, Block silt, Block rock) {
        while (world.isAir(x, y, z) && y > 2) {
            --y;
        }

        if (!world.is(x, y, z, silt)) return false;

        BlockState rockState = rock.defaultBlockState();
        y += rand.nextInt(4);
        int l = rand.nextInt(4) + 10;
        int i1 = l / 4 + rand.nextInt(2);

        if (i1 > 1 && rand.nextInt(2) == 0) {
            y += 10 + rand.nextInt(30);
        }

        int j1;
        int k1;
        int l1;

        for (j1 = 0; j1 < l; ++j1) {
            float f = (1.0F - (float) j1 / (float) l) * (float) i1;
            k1 = ceil(f);

            for (l1 = -k1; l1 <= k1; ++l1) {
                float f1 = (float) Math.abs(l1) - 0.25F;

                for (int i2 = -k1; i2 <= k1; ++i2) {
                    float f2 = (float) Math.abs(i2) - 0.25F;

                    if ((l1 == 0 && i2 == 0 || f1 * f1 + f2 * f2 <= f * f) && (l1 != -k1 && l1 != k1 && i2 != -k1 && i2 != k1 || rand.nextFloat() <= 0.75F)) {
                        BlockState block = world.get(x + l1, y + j1, z + i2);

                        if (block.isAir() || block.is(silt) || block.is(rock)) {
                            world.set(x + l1, y + j1, z + i2, rockState);
                        }

                        if (j1 != 0 && k1 > 1) {
                            block = world.get(x + l1, y - j1, z + i2);

                            if (block.isAir() || block.is(silt) || block.is(rock)) {
                                world.set(x + l1, y - j1, z + i2, rockState);
                            }
                        }
                    }
                }
            }
        }

        j1 = i1 - 1;

        if (j1 < 0) {
            j1 = 0;
        } else if (j1 > 1) {
            j1 = 1;
        }

        for (int j2 = -j1; j2 <= j1; ++j2) {
            k1 = -j1;

            while (k1 <= j1) {
                l1 = y - 1;
                int k2 = 50;

                if (Math.abs(j2) == 1 && Math.abs(k1) == 1) {
                    k2 = rand.nextInt(5);
                }

                while (true) {
                    if (l1 > 50) {
                        BlockState block1 = world.get(x + j2, l1, z + k1);

                        if (block1.isAir() || block1.is(silt) || block1.is(rock)) {
                            world.set(x + j2, l1, z + k1, rockState);
                            --l1;
                            --k2;

                            if (k2 <= 0) {
                                l1 -= rand.nextInt(5) + 1;
                                k2 = rand.nextInt(5);
                            }

                            continue;
                        }
                    }

                    ++k1;
                    break;
                }
            }
        }

        return true;
    }

    /**
     * {@code WorldGenElectricVolcano.generate} (height 30, width 22, Eve silt surface, Eve rock stone). {@code geyser} is
     * hbm-space's {@code geysir_electric}; while it is not registered its cell is left as it was.
     */
    public static boolean volcano(PopulateWorld world, Random rand, int x, int y, int z, int height, int width,
                                  Block surface, Block stone, BlockState basalt, BlockState neodymium,
                                  BlockState geyser) {
        if (rand.nextInt(10) != 0) {
            return false;
        }
        while (world.isAir(x, y, z) && y > 2) {
            --y;
        }

        if (!world.is(x, y, z, surface)) return false;

        var shape = volcanoShape(rand, y, height, width);
        buildVolcano(world, rand, x, z, shape, surface, stone, basalt, neodymium, geyser,
                Blocks.AIR.defaultBlockState());
        return true;
    }

    public static VolcanoShape volcanoShape(Random rand, int y, int height, int width) {
        y += rand.nextInt(1);
        int l = rand.nextInt(4) + height;
        int i1 = l / 4 + rand.nextInt(width);
        if (i1 > 1 && rand.nextInt(5) == 0) rand.nextInt(30);
        return new VolcanoShape(y, l, i1);
    }

    public static void buildVolcano(PopulateWorld world, Random rand, int x, int z, VolcanoShape shape,
                                    Block surface, Block stone, BlockState basalt, BlockState neodymium,
                                    BlockState geyser, BlockState air) {
        int y = shape.y(), l = shape.l(), i1 = shape.i1();
        for (int j1 = 0; j1 < l; ++j1) {
            float f = (1.0F - (float) j1 / (float) l) * (float) i1;
            int k1 = ceil(f);

            for (int l1 = -k1; l1 <= k1; ++l1) {
                float f1 = (float) Math.abs(l1) - 0.25F;

                for (int i2 = -k1; i2 <= k1; ++i2) {
                    float f2 = (float) Math.abs(i2) - 0.75F;

                    if ((l1 == 0 && i2 == 0 || f1 * f1 + f2 * f2 <= f * f) && (l1 != -k1 && l1 != k1 && i2 != -k1 && i2 != k1 || rand.nextFloat() <= 0.75F)) {
                        BlockState block = world.get(x + l1, y + j1, z + i2);

                        if (block.isAir() || block.is(surface) || block.is(stone)) {
                            world.set(x + l1, y + j1, z + i2, basalt);
                        }

                        if (j1 != 0 && k1 > 1) {
                            block = world.get(x + l1, y - j1, z + i2);

                            if (block.isAir() || block.is(surface) || block.is(stone)) {
                                world.set(x + l1, y - j1, z + i2, neodymium);
                            }
                        }
                    }
                    if ((l1 == 0 && j1 == 0) || f1 * f1 + f2 * f2 > f * f) {
                        BlockState block = world.get(x + l1, y + j1, z + i2);

                        if (block.isAir() || block.is(surface) || block.is(stone)) {
                            world.set(x, y + j1, z, air);
                        }
                    }
                    if ((l1 == 0 && j1 == 0) || f1 * f1 + f2 * f2 > f * f) {
                        BlockState block = world.get(x + l1, y + j1, z + i2);

                        if (block.isAir() || block.is(surface) || block.is(stone)) {
                            world.set(x, y + 5, z, geyser);
                        }
                    }
                }
            }
        }

    }
}
