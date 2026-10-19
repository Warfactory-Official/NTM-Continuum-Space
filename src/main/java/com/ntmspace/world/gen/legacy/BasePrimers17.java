package com.ntmspace.world.gen.legacy;

/**
 * A chunk's 1.7 primer before its map generators: {@code provideChunk}'s reseed, the density field filled with the
 * body's stone and sea, and each column's biome surface. Unlike the full primer it is the same in every game session
 * (hbm-space's Greg noise caves were seeded per session, and the surface passes' {@code Math.random()} choices pick
 * between solid blocks only), so GameTests pin its column heights.
 *
 * <p>Call it while holding the terrain's generator lock: it reuses the terrain's buffers and {@code Random}, as
 * {@code provide} does.
 */
public final class BasePrimers17 {

    private BasePrimers17() {}

    public static CelestialTerrain17.Primer of(CelestialTerrain17 terrain, int chunkX, int chunkZ) {
        terrain.rand.setSeed((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);
        CelestialTerrain17.Primer buffer = new CelestialTerrain17.Primer();
        terrain.generateBlocks(chunkX, chunkZ, buffer.blocks);
        terrain.replaceBlocksForBiome(chunkX, chunkZ, buffer.blocks, buffer.metas);
        return buffer;
    }

    /**
     * The y of each column's highest opaque block (-1 for none), folded into one checksum, columns in primer order:
     * {@code sum = sum * 31 + y}.
     */
    public static long heightChecksum(CelestialTerrain17.Primer primer) {
        long sum = 0;
        for (int column = 0; column < 256; column++) {
            int top = -1;
            for (int y = 255; y >= 0; y--) {
                Block17 block = primer.blocks[column * 256 + y];
                if (block != null && block.isOpaqueCube()) {
                    top = y;
                    break;
                }
            }
            sum = sum * 31 + top;
        }
        return sum;
    }
}
