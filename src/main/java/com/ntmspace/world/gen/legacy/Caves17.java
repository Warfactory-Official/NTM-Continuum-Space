package com.ntmspace.world.gen.legacy;

import java.util.Random;

/**
 * 1.7.10's {@code MapGenCaves} as Forge 1.7.10 patched it: the vanilla tunnel and room carver, with the water test,
 * the top-block test and the dig step behind the overridable hooks Forge added ({@link #isOceanBlock},
 * {@link #isTopBlock}, {@link #digBlock}). hbm-space's {@code MapGenGreg} extends it and overrides the dig step.
 * Constants and call order follow the 1.7.10 class (checked against its bytecode).
 */
public class Caves17 extends MapGen17 {

    protected void generateLargeCaveNode(long seed, int chunkX, int chunkZ, Block17[] blocks, double x, double y, double z) {
        generateCaveNode(seed, chunkX, chunkZ, blocks, x, y, z, 1.0F + this.rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    protected void generateCaveNode(long seed, int chunkX, int chunkZ, Block17[] blocks, double x, double y, double z,
                                    float width, float yaw, float pitch, int step, int maxSteps, double heightMul) {
        double centreX = chunkX * 16 + 8;
        double centreZ = chunkZ * 16 + 8;
        float yawChange = 0.0F;
        float pitchChange = 0.0F;
        Random random = new Random(seed);

        if (maxSteps <= 0) {
            int j1 = this.range * 16 - 16;
            maxSteps = j1 - random.nextInt(j1 / 4);
        }

        boolean room = false;
        if (step == -1) {
            step = maxSteps / 2;
            room = true;
        }

        int branchAt = random.nextInt(maxSteps / 2) + maxSteps / 4;

        for (boolean steep = random.nextInt(6) == 0; step < maxSteps; ++step) {
            double radius = 1.5D + (double) (Math17.sin((float) step * (float) Math.PI / (float) maxSteps) * width * 1.0F);
            double radiusY = radius * heightMul;
            float cosPitch = Math17.cos(pitch);
            float sinPitch = Math17.sin(pitch);
            x += (double) (Math17.cos(yaw) * cosPitch);
            y += (double) sinPitch;
            z += (double) (Math17.sin(yaw) * cosPitch);

            if (steep) pitch *= 0.92F;
            else pitch *= 0.7F;

            pitch += pitchChange * 0.1F;
            yaw += yawChange * 0.1F;
            pitchChange *= 0.9F;
            yawChange *= 0.75F;
            pitchChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (!room && step == branchAt && width > 1.0F && maxSteps > 0) {
                generateCaveNode(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F,
                        yaw - ((float) Math.PI / 2F), pitch / 3.0F, step, maxSteps, 1.0D);
                generateCaveNode(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F,
                        yaw + ((float) Math.PI / 2F), pitch / 3.0F, step, maxSteps, 1.0D);
                return;
            }

            if (room || random.nextInt(4) != 0) {
                double dx = x - centreX;
                double dz = z - centreZ;
                double remaining = (double) (maxSteps - step);
                double reach = (double) (width + 2.0F + 16.0F);

                if (dx * dx + dz * dz - remaining * remaining > reach * reach) return;

                if (x >= centreX - 16.0D - radius * 2.0D && z >= centreZ - 16.0D - radius * 2.0D
                        && x <= centreX + 16.0D + radius * 2.0D && z <= centreZ + 16.0D + radius * 2.0D) {
                    int minX = Math17.floor_double(x - radius) - chunkX * 16 - 1;
                    int maxX = Math17.floor_double(x + radius) - chunkX * 16 + 1;
                    int minY = Math17.floor_double(y - radiusY) - 1;
                    int maxY = Math17.floor_double(y + radiusY) + 1;
                    int minZ = Math17.floor_double(z - radius) - chunkZ * 16 - 1;
                    int maxZ = Math17.floor_double(z + radius) - chunkZ * 16 + 1;

                    if (minX < 0) minX = 0;
                    if (maxX > 16) maxX = 16;
                    if (minY < 1) minY = 1;
                    if (maxY > 248) maxY = 248;
                    if (minZ < 0) minZ = 0;
                    if (maxZ > 16) maxZ = 16;

                    boolean water = false;
                    for (int bx = minX; !water && bx < maxX; ++bx) {
                        for (int bz = minZ; !water && bz < maxZ; ++bz) {
                            for (int by = maxY + 1; !water && by >= minY - 1; --by) {
                                int index = (bx * 16 + bz) * 256 + by;
                                if (by >= 0 && by < 256) {
                                    if (isOceanBlock(blocks, index, bx, by, bz, chunkX, chunkZ)) water = true;
                                    if (by != minY - 1 && bx != minX && bx != maxX - 1 && bz != minZ && bz != maxZ - 1) {
                                        by = minY;
                                    }
                                }
                            }
                        }
                    }

                    if (!water) {
                        for (int bx = minX; bx < maxX; ++bx) {
                            double nx = ((double) (bx + chunkX * 16) + 0.5D - x) / radius;
                            for (int bz = minZ; bz < maxZ; ++bz) {
                                double nz = ((double) (bz + chunkZ * 16) + 0.5D - z) / radius;
                                int index = (bx * 16 + bz) * 256 + maxY;
                                boolean foundTop = false;
                                if (nx * nx + nz * nz < 1.0D) {
                                    for (int by = maxY - 1; by >= minY; --by) {
                                        double ny = ((double) by + 0.5D - y) / radiusY;
                                        if (ny > -0.7D && nx * nx + ny * ny + nz * nz < 1.0D) {
                                            if (isTopBlock(blocks, index, bx, by, bz, chunkX, chunkZ)) foundTop = true;
                                            digBlock(blocks, index, bx, by, bz, chunkX, chunkZ, foundTop);
                                        }
                                        --index;
                                    }
                                }
                            }
                        }
                        if (room) break;
                    }
                }
            }
        }
    }

    @Override
    protected void recursiveGenerate(int offsetX, int offsetZ, int chunkX, int chunkZ, Block17[] blocks) {
        int count = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(15) + 1) + 1);
        if (this.rand.nextInt(7) != 0) count = 0;

        for (int j1 = 0; j1 < count; ++j1) {
            double x = offsetX * 16 + this.rand.nextInt(16);
            double y = this.rand.nextInt(this.rand.nextInt(120) + 8);
            double z = offsetZ * 16 + this.rand.nextInt(16);
            int tunnels = 1;

            if (this.rand.nextInt(4) == 0) {
                generateLargeCaveNode(this.rand.nextLong(), chunkX, chunkZ, blocks, x, y, z);
                tunnels += this.rand.nextInt(4);
            }

            for (int l1 = 0; l1 < tunnels; ++l1) {
                float yaw = this.rand.nextFloat() * (float) Math.PI * 2.0F;
                float pitch = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float width = this.rand.nextFloat() * 2.0F + this.rand.nextFloat();
                if (this.rand.nextInt(10) == 0) width *= this.rand.nextFloat() * this.rand.nextFloat() * 3.0F + 1.0F;
                generateCaveNode(this.rand.nextLong(), chunkX, chunkZ, blocks, x, y, z, width, yaw, pitch, 0, 0, 1.0D);
            }
        }
    }

    /** Forge's hook: water stops a tunnel. */
    protected boolean isOceanBlock(Block17[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        return data[index] == Block17.FLOWING_WATER || data[index] == Block17.WATER;
    }

    /** Forge's hook: the biome's top block (grass in the three exception biomes, which no celestial body has). */
    protected boolean isTopBlock(Block17[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        Biome17 biome = world.biomeAt(x + chunkX * 16, z + chunkZ * 16);
        return data[index] == biome.topBlock;
    }

    /** Forge's default dig step (lava below y 10, the biome top block exposed on dirt). */
    protected void digBlock(Block17[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        Biome17 biome = world.biomeAt(x + chunkX * 16, z + chunkZ * 16);
        Block17 top = biome.topBlock;
        Block17 filler = biome.fillerBlock;
        Block17 block = data[index];
        if (block == Block17.STONE || block == filler || block == top) {
            if (y < 10) {
                data[index] = Block17.LAVA;
            } else {
                data[index] = null;
                if (foundTop && data[index - 1] == filler) data[index - 1] = top;
            }
        }
    }
}
