package com.ntmspace.world.gen.legacy;

/**
 * hbm-space's {@code MapGenBedrockOil} ({@code com.hbm.world.gen.terrain}), ported line for line onto the chunk primer:
 * a small diamond of bedrock oil ore in the bottom five layers and, around the spot above it, oily or dead dirt (with
 * the odd mustard willow and dead plant), dirty sand, cracked stone, and slaked sellafield where the ground is Tekto's
 * rubber silt, rubber grass or vinyl sand.
 */
public class MapGenBedrockOil17 extends MapGen17 {

    /** {@code EnumDeadPlantType.values().length} in 1.7 (generic, grass, flower, bigflower, fern). */
    private static final int DEAD_PLANT_TYPES = 5;

    private final int frequency;

    public Block17 block = Block17.ORE_BEDROCK_OIL;
    public Block17 replace = Block17.STONE;
    public byte meta = 0;

    public int spotWidth = 5;
    public int spotCount = 50;
    public boolean addWillows = true;

    public MapGenBedrockOil17(int frequency) {
        this.frequency = frequency;
        this.range = 4;
    }

    @Override
    protected void recursiveGenerate(int offsetX, int offsetZ, int chunkX, int chunkZ, Block17[] blocks) {
        if (rand.nextInt(frequency) == frequency - 2) {
            int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
            int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

            // Add the bedrock oil spot
            for (int bx = 15; bx >= 0; bx--)
                for (int bz = 15; bz >= 0; bz--)
                    for (int y = 0; y < 5; y++) {
                        int index = (bx * 16 + bz) * 256 + y;

                        if (blocks[index] == replace || blocks[index] == Block17.BEDROCK) {
                            // x, z are the coordinates relative to the target virtual chunk origin
                            int x = xCoord + bx;
                            int z = zCoord + bz;

                            if (Math.abs(x) < 5 && Math.abs(z) < 5 && Math.abs(x) + Math.abs(y) + Math.abs(z) <= 6) {
                                blocks[index] = block;
                                metas[index] = meta;
                            }
                        }
                    }

            // Add oil spot damage
            for (int i = 0; i < spotCount; i++) {
                int rx = (int) (rand.nextGaussian() * spotWidth) - xCoord;
                int rz = (int) (rand.nextGaussian() * spotWidth) - zCoord;

                if (rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
                    // find ground level
                    for (int y = 127; y >= 4; y--) {
                        int index = (rx * 16 + rz) * 256 + y;

                        if (blocks[index] != null && blocks[index].isOpaqueCube()) {
                            for (int oy = 1; oy > -3; oy--) {
                                int subIndex = index + oy;

                                if (blocks[subIndex] == Block17.GRASS || blocks[subIndex] == Block17.DIRT) {
                                    blocks[subIndex] = rand.nextInt(10) == 0 ? Block17.DIRT_OILY : Block17.DIRT_DEAD;

                                    if (addWillows && oy == 0 && rand.nextInt(50) == 0) {
                                        blocks[subIndex + 1] = Block17.PLANT_FLOWER_CD0;
                                        metas[subIndex + 1] = 0;
                                    }

                                    // this generation occurs BEFORE decoration, so we have no plants to modify
                                    // so we'll instead just add some new ones right now
                                    if (oy == 0 && rand.nextInt(20) == 0) {
                                        blocks[subIndex + 1] = Block17.PLANT_DEAD;
                                        metas[subIndex + 1] = (byte) rand.nextInt(DEAD_PLANT_TYPES);
                                    }

                                    break;
                                } else if (blocks[subIndex] == Block17.SAND || blocks[subIndex] == Block17.ORE_OIL_SAND) {
                                    if (metas[subIndex] == 1) {
                                        blocks[subIndex] = Block17.SAND_DIRTY_RED;
                                    } else {
                                        blocks[subIndex] = Block17.SAND_DIRTY;
                                    }
                                    break;
                                } else if (blocks[subIndex] == Block17.STONE) {
                                    blocks[subIndex] = Block17.STONE_CRACKED;
                                    break;
                                } else if (blocks[subIndex] == Block17.RUBBER_SILT || blocks[subIndex] == Block17.RUBBER_GRASS
                                        || blocks[subIndex] == Block17.VINYL_SAND) {
                                    blocks[subIndex] = Block17.SELLAFIELD_SLAKED;
                                    break;
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
