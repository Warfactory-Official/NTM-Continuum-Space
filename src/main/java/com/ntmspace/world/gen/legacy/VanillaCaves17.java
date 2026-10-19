package com.ntmspace.world.gen.legacy;

/**
 * hbm-space's {@code MapGenVanillaCaves} ({@code com.hbm.dim.mapgen}): Forge 1.7.10's cave carver ({@link Caves17})
 * digging the body's stone instead of vanilla stone, with a body-specific block instead of lava below y 10.
 */
public class VanillaCaves17 extends Caves17 {

    private final Block17 stoneBlock;
    private Block17 lavaBlock = Block17.LAVA;

    public VanillaCaves17(Block17 stoneBlock) {
        this.stoneBlock = stoneBlock;
    }

    public VanillaCaves17 withLava(Block17 lavaBlock) {
        this.lavaBlock = lavaBlock;
        return this;
    }

    @Override
    protected void digBlock(Block17[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        Biome17 biome = world.biomeAt(x + chunkX * 16, z + chunkZ * 16);
        Block17 block = data[index];

        if (block == stoneBlock || block == biome.fillerBlock || block == biome.topBlock) {
            if (y < 10) {
                data[index] = lavaBlock;
            } else {
                data[index] = null;

                if (foundTop && data[index - 1] == biome.fillerBlock) {
                    data[index - 1] = biome.topBlock;
                }
            }
        }
    }
}
