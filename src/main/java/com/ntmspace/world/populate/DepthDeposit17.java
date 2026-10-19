package com.ntmspace.world.populate;

import java.util.Random;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code DepthDeposit}: a ball of a depth ore wrapped in depth rock near bedrock, replacing the body's stone
 * and bedrock. Its random draws depend on the world (two per replaceable cell at most), so the veins after it in
 * {@code WorldGeneratorCelestial} land where 1.7 put them only where the terrain is 1.7's.
 */
public final class DepthDeposit17 {

    private DepthDeposit17() {}

    /** {@code generateCondition}: one deposit in {@code chance} chunks. */
    public static void generateCondition(PopulateWorld world, int x, int yMin, int yDev, int z, int size, double fill,
                                         BlockState block, Random rand, int chance, Block genTarget, BlockState filler) {
        if (rand.nextInt(chance) == 0) {
            int gx = x + rand.nextInt(16) + 8;
            int gy = yMin + rand.nextInt(yDev);
            int gz = z + rand.nextInt(16) + 8;
            generate(world, gx, gy, gz, size, fill, block, rand, genTarget, filler);
        }
    }

    /** {@code generate}; the distance is {@code Vec3.lengthVector}, a float square root. */
    public static void generate(PopulateWorld world, int x, int y, int z, int size, double fill, BlockState block,
                                Random rand, Block genTarget, BlockState filler) {
        for (int i = x - size; i <= x + size; i++) {
            for (int j = y - size; j <= y + size; j++) {
                for (int k = z - size; k <= z + size; k++) {
                    if (j < 1 || j > 126) continue;

                    double dx = x - i;
                    double dy = y - j;
                    double dz = z - k;
                    double len = (double) (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    BlockState target = world.get(i, j, k);

                    if (target.is(genTarget) || target.is(Blocks.BEDROCK)) {
                        if (len + rand.nextInt(2) < size * fill) {
                            world.set(i, j, k, block);
                        } else if (len + rand.nextInt(2) <= size) {
                            world.set(i, j, k, filler);
                        }
                    }
                }
            }
        }
    }
}
