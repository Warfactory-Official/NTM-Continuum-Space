package com.ntmspace.world.gen.legacy;

import com.ntmspace.world.gen.noise.Noise17Perlin;
import java.util.Random;

/**
 * hbm-space's {@code MapGenPlateau} ({@code com.hbm.dim.mapgen}), ported line for line: raises a biome's columns by a
 * stepped Perlin height, capped with surface rock (the fill block on flat step tops).
 *
 * <p><b>Ported oddity:</b> 1.7 built the noise from {@code world.rand}, an unseeded {@code Random}, so the plateau steps
 * differ from one game session to the next (and tear between chunks generated in different sessions). The port takes
 * that session {@code Random} from its owner. Air in the 1.7 primer was {@code Blocks.air} here; it is {@code null} in
 * the port's primer, which only this class compared, so the comparison reads {@code null}.
 */
public class MapGenPlateau17 {

    public Block17 stoneBlock;
    public Block17 surfrock;
    public Block17 fillblock;

    public Noise17Perlin plateauNoise;
    public double noiseScale = 0.05;
    public int maxPlateauAddition = 12;
    public int stepHeight = 6;
    public int topsoilThickness = 2;

    /** 1.7 {@code applyToBiome}. */
    public Biome17 applyToBiome;

    public MapGenPlateau17(Random worldRand) {
        this.plateauNoise = new Noise17Perlin(worldRand, 4);
    }

    /** 1.7 {@code func_151539_a} (it overrode the whole map-generator loop; only this chunk is touched). */
    public void generate(Legacy17World world, int chunkX, int chunkZ, Block17[] blocks) {
        int[][] plateauTops = new int[16][16];
        int[][] baseHeights = new int[16][16];
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int baseHeight = getSurfaceHeight(blocks, localX, localZ);
                baseHeights[localX][localZ] = baseHeight;
                double noiseVal = plateauNoise.value((chunkX * 16 + localX) * noiseScale, (chunkZ * 16 + localZ) * noiseScale);
                int plateauAddition = (int) (((noiseVal + 1) / 2.0) * maxPlateauAddition);
                plateauAddition = (plateauAddition / stepHeight) * stepHeight;
                plateauTops[localX][localZ] = baseHeight + plateauAddition;
            }
        }

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                if (applyToBiome != null) {
                    Biome17 biome = world.biomeAt(localX + chunkX * 16, localZ + chunkZ * 16);
                    if (biome != applyToBiome) continue;
                }
                int baseHeight = baseHeights[localX][localZ];
                int plateauTop = plateauTops[localX][localZ];
                for (int y = baseHeight + 1; y < 256; y++) {
                    int index = (localX * 16 + localZ) * 256 + y;
                    if (y < plateauTop - topsoilThickness) {
                        blocks[index] = stoneBlock;
                    } else if (y < plateauTop) {
                        boolean sameLeft = (localX - 1 < 0) || (plateauTops[localX - 1][localZ] == plateauTop);
                        boolean sameRight = (localX + 1 >= 16) || (plateauTops[localX + 1][localZ] == plateauTop);
                        boolean sameFront = (localZ - 1 < 0) || (plateauTops[localX][localZ - 1] == plateauTop);
                        boolean sameBack = (localZ + 1 >= 16) || (plateauTops[localX][localZ + 1] == plateauTop);

                        if (y == plateauTop - 1 && sameLeft && sameRight && sameFront && sameBack) {
                            blocks[index] = fillblock;
                        } else {
                            blocks[index] = surfrock;
                        }
                    } else {
                        blocks[index] = null;
                    }
                }
            }
        }
    }

    private int getSurfaceHeight(Block17[] blocks, int localX, int localZ) {
        int baseHeight = 0;
        for (int y = 255; y >= 0; y--) {
            int index = (localX * 16 + localZ) * 256 + y;
            if (blocks[index] != null) {
                baseHeight = y;
                break;
            }
        }
        return baseHeight;
    }
}
