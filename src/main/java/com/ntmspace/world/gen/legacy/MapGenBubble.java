package com.ntmspace.world.gen.legacy;

import java.util.function.Predicate;

/**
 * hbm-space's {@code MapGenBubble} (com.hbm.world.gen.terrain), ported line for line onto the chunk primer: oblate
 * bubbles of a fluid ore (brine on the Mun) and, always, the "oil spot" at the surface above each.
 *
 * <p><b>Ported oddity:</b> the surface spot was written for oil under grass, but hbm-space reuses the generator for
 * brine on the Mun, so the spot's hole is dug there too: up to two blocks of the surface removed, an {@code oil_spill}
 * layer, and cracked stone around it. That is what a Mun brine bubble looked like in 1.7; it is kept.
 */
public class MapGenBubble extends MapGen17 {

    /** {@code EnumDeadPlantType.values().length} in 1.7 (generic, grass, flower, bigflower, fern). */
    private static final int DEAD_PLANT_TYPES = 5;

    private final int frequency;
    private int minSize = 8;
    private int maxSize = 64;

    public int minY = 15;
    public int rangeY = 25;

    public boolean fuzzy;

    public Block17 block;
    public byte meta = 0;
    public Block17 replace = Block17.STONE;

    public Predicate<Biome17> canSpawn;

    public MapGenBubble(int frequency) {
        this.frequency = frequency;
    }

    public void setSize(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.range = (maxSize / 8) + 1;
    }

    @Override
    protected void recursiveGenerate(int offsetX, int offsetZ, int chunkX, int chunkZ, Block17[] blocks) {
        int effecFreq = frequency;
        Biome17 biome = world.biomeAt(offsetX * 16, offsetZ * 16);
        if (biome.temperature >= 2 && biome.rainfall < 0.1) effecFreq /= 3;
        if (effecFreq <= 0) effecFreq = 1;

        if (rand.nextInt(effecFreq) == effecFreq - 1 && (canSpawn == null || canSpawn.test(biome))) {
            int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
            int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

            int yCoord = rand.nextInt(rangeY) + minY;

            double radius = rand.nextInt(maxSize - minSize) + minSize;
            double radiusSqr = (radius * radius) / 2; // original OilBubble implementation divided the square by 2 for some reason

            int yMin = Math.max(1, Math17.floor_double(yCoord - radius));
            int yMax = Math.min(127, Math17.ceiling_double_int(yCoord + radius));

            for (int bx = 15; bx >= 0; bx--) // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
                for (int bz = 15; bz >= 0; bz--)
                    for (int by = yMin; by < yMax; by++) {
                        int index = (bx * 16 + bz) * 256 + by;

                        if (blocks[index] == replace) {
                            // x, z are the coordinates relative to the target virtual chunk origin
                            int x = xCoord + bx;
                            int z = zCoord + bz;
                            int y = yCoord - by;

                            double rSqr = x * x + z * z + y * y * 3;
                            if (fuzzy) rSqr -= rand.nextDouble() * radiusSqr / 3;
                            if (rSqr < radiusSqr) {
                                blocks[index] = block;
                                metas[index] = meta;
                            }
                        }
                    }

            if (rand.nextInt(1) == 0) addSurfaceSpot(xCoord, zCoord, blocks);
        }
    }

    protected void addSurfaceSpot(int xCoord, int zCoord, Block17[] blocks) {
        int spotCount = 150;
        int spotWidth = 7;

        // Add oil spot damage
        for (int i = 0; i < spotCount; i++) {
            int offX = (int) (rand.nextGaussian() * spotWidth);
            int offZ = (int) (rand.nextGaussian() * spotWidth);
            int rx = offX - xCoord;
            int rz = offZ - zCoord;

            if (rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
                // find ground level
                for (int y = 127; y >= 4; y--) {
                    int index = (rx * 16 + rz) * 256 + y;

                    if (blocks[index] != null && blocks[index].isOpaqueCube()) {
                        for (int oy = 1; oy > -3; oy--) {
                            int subIndex = index + oy;

                            int distSq = offX * offX + offZ * offZ;
                            boolean inner = distSq < (spotWidth / 2) * (spotWidth / 2);

                            if (blocks[subIndex] == Block17.GRASS || blocks[subIndex] == Block17.DIRT) {
                                blocks[subIndex] = inner ? Block17.DIRT_OILY : Block17.DIRT_DEAD;

                                // this generation occurs BEFORE decoration, so we have no plants to modify
                                // so we'll instead just add some new ones right now
                                if (!inner && oy == 0 && rand.nextInt(20) == 0) {
                                    blocks[subIndex + 1] = Block17.PLANT_DEAD;
                                    metas[subIndex + 1] = (byte) rand.nextInt(DEAD_PLANT_TYPES);
                                }

                                break;
                            } else if (blocks[subIndex] == Block17.SAND || blocks[subIndex] == Block17.ORE_OIL_SAND) {
                                if (metas[subIndex] == 1) blocks[subIndex] = Block17.SAND_DIRTY_RED;
                                else blocks[subIndex] = Block17.SAND_DIRTY;
                                break;
                            } else if (blocks[subIndex] == Block17.STONE) {
                                blocks[subIndex] = Block17.STONE_CRACKED;
                                break;
                            }
                        }

                        break;
                    }
                }
            }
        }

        // and now for the hole(tm); 1.7 ForgeDirection 1..5 = UP, NORTH, SOUTH, WEST, EAST
        int[][] dirs = {{0, 0}, {0, 0}, {0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int i = 1; i < 6; i++) {
            int x = dirs[i][0] - xCoord;
            int z = dirs[i][1] - zCoord;

            if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                int solids = 0;

                for (int y = 127; y >= 0; y--) {
                    int index = (x * 16 + z) * 256 + y;
                    if (blocks[index] == null) continue;

                    if (blocks[index].isLiquid()) break;

                    if (blocks[index].isOpaqueCube()) {
                        solids++;

                        // this approach might break a little when the surface has holes and uneveness but i don't care lmao
                        if (i > 1) {
                            blocks[index] = Block17.STONE_CRACKED;
                            if (solids >= 4) break;
                        } else {
                            if (solids < 3) blocks[index] = null;
                            if (solids == 3) blocks[index] = Block17.OIL_SPILL;
                            if (solids > 3 && solids < 7) blocks[index] = Block17.STONE_CRACKED;
                            if (solids >= 7) break;
                        }
                    }
                }
            }
        }
    }
}
