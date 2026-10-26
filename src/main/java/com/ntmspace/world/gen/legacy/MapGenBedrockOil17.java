package com.ntmspace.world.gen.legacy;

import java.util.List;
import java.util.Random;
/**
 * hbm-space's {@code MapGenBedrockOil} ({@code com.hbm.world.gen.terrain}), ported line for line onto the chunk primer:
 * a small diamond of bedrock oil ore in the bottom five layers and, around the spot above it, oily or dead dirt (with
 * the odd mustard willow and dead plant), dirty sand, cracked stone, and slaked sellafield where the ground is Tekto's
 * rubber silt, rubber grass or vinyl sand.
 */
public class MapGenBedrockOil17 extends MapGen17 {

    /** {@code EnumDeadPlantType.values().length} in 1.7 (generic, grass, flower, bigflower, fern). */
    private static final int DEAD_PLANT_TYPES = 5;

    public record SurfaceScar(Block17 from, Block17 to) {}
    public static final List<SurfaceScar> TEKTO_SCARS = List.of(
            new SurfaceScar(Block17.RUBBER_SILT, Block17.SELLAFIELD_SLAKED),
            new SurfaceScar(Block17.RUBBER_GRASS, Block17.SELLAFIELD_SLAKED),
            new SurfaceScar(Block17.VINYL_SAND, Block17.SELLAFIELD_SLAKED));

    /** The specification consumed by the terrain factory, without generating a second deposit. */
    public record Config(Block17 core, byte meta, Block17 replace, List<SurfaceScar> surfaceScars) {
        public Config { surfaceScars = List.copyOf(surfaceScars); }
        public MapGenBedrockOil17 create(int frequency) {
            var generator = new MapGenBedrockOil17(frequency);
            generator.block = core;
            generator.meta = meta;
            generator.replace = replace;
            generator.surfaceScars = surfaceScars;
            return generator;
        }
    }

    /** The operations the bedrock-oil column dressing needs, shared by a primer and an explicit live placement. */
    public interface Surface {
        boolean fullCube(int x, int y, int z);
        boolean is(int x, int y, int z, Block17 block);
        int meta(int x, int y, int z);
        void set(int x, int y, int z, Block17 block, int meta);
        void replace(int x, int y, int z, Block17 block);
        boolean scar(int x, int y, int z);
    }

    public static void scarColumn(Surface surface, Random random, int x, int z, int top, int bottom, boolean willows) {
        for (int y = top; y >= bottom; y--) {
            if (!surface.fullCube(x, y, z)) continue;
            for (int oy = 1; oy > -3; oy--) {
                int sy = y + oy;
                if (surface.is(x, sy, z, Block17.GRASS) || surface.is(x, sy, z, Block17.DIRT)) {
                    surface.replace(x, sy, z, random.nextInt(10) == 0 ? Block17.DIRT_OILY : Block17.DIRT_DEAD);
                    if (willows && oy == 0 && random.nextInt(50) == 0) surface.set(x, sy + 1, z, Block17.PLANT_FLOWER_CD0, 0);
                    if (oy == 0 && random.nextInt(20) == 0) surface.set(x, sy + 1, z, Block17.PLANT_DEAD, random.nextInt(DEAD_PLANT_TYPES));
                    break;
                } else if (surface.is(x, sy, z, Block17.SAND) || surface.is(x, sy, z, Block17.ORE_OIL_SAND)) {
                    surface.replace(x, sy, z, surface.meta(x, sy, z) == 1 ? Block17.SAND_DIRTY_RED : Block17.SAND_DIRTY);
                    break;
                } else if (surface.is(x, sy, z, Block17.STONE)) {
                    surface.replace(x, sy, z, Block17.STONE_CRACKED);
                    break;
                } else if (surface.scar(x, sy, z)) break;
            }
            break;
        }
    }

    private final int frequency;

    public Block17 block = Block17.ORE_BEDROCK_OIL;
    public Block17 replace = Block17.STONE;
    public byte meta = 0;

    public int spotWidth = 5;
    public int spotCount = 50;
    public boolean addWillows = true;
    public List<SurfaceScar> surfaceScars = TEKTO_SCARS;

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

            // The primer and explicit placements share the same column dressing and target order.
            Surface surface = new Surface() {
                private int index(int x, int y, int z) { return (x * 16 + z) * 256 + y; }
                @Override public boolean fullCube(int x, int y, int z) {
                    var block = blocks[index(x, y, z)];
                    return block != null && block.isOpaqueCube();
                }
                @Override public boolean is(int x, int y, int z, Block17 block) { return blocks[index(x, y, z)] == block; }
                @Override public int meta(int x, int y, int z) { return metas[index(x, y, z)]; }
                @Override public void set(int x, int y, int z, Block17 block, int meta) {
                    int index = index(x, y, z);
                    blocks[index] = block;
                    metas[index] = (byte) meta;
                }
                @Override public void replace(int x, int y, int z, Block17 block) { blocks[index(x, y, z)] = block; }
                @Override public boolean scar(int x, int y, int z) {
                    for (var scar : surfaceScars) {
                        if (is(x, y, z, scar.from())) { set(x, y, z, scar.to(), meta(x, y, z)); return true; }
                    }
                    return false;
                }
            };
            for (int i = 0; i < spotCount; i++) {
                int rx = (int) (rand.nextGaussian() * spotWidth) - xCoord;
                int rz = (int) (rand.nextGaussian() * spotWidth) - zCoord;
                if (rx >= 0 && rx < 16 && rz >= 0 && rz < 16) scarColumn(surface, rand, rx, rz, 127, 4, addWillows);
            }
        }
    }
}
