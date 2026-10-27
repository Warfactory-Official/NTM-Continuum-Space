package com.ntmspace.world;

import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import com.ntmspace.block.terrain.BlockLaytheCoral;
import com.ntmspace.block.terrain.BlockLaytheCoralBlock;
import com.ntmspace.block.terrain.BlockLaytheKelp;
import com.ntmspace.block.terrain.BlockLaytheTallPlant;
import com.ntmspace.block.terrain.PlantType;
import com.ntmspace.registry.LaytheTektoBlocks;
import com.ntmspace.world.gen.legacy.Biome17;
import com.ntmspace.world.gen.legacy.LaytheTerrain17;
import com.ntmspace.world.gen.legacy.Math17;
import com.ntmspace.world.gen.legacy.TektoTerrain17;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Laythe's, Tekto's and Thatmo's 1.7 decoration (hbm-space's {@code populate}), run at the modern FEATURES step of each
 * chunk with 1.7's own seeds, so a chunk is decorated as 1.7 decorated it as far as the terrain around it is the same
 * (1.7 decorated a chunk once its south-east neighbours existed; modern generation decorates in its own order, so where
 * two chunks' decorations overlap the later one may differ).
 *
 * <ul>
 *   <li><b>The biome decorator</b> ({@code ChunkProviderCelestial.populate} then {@code BiomeDecoratorCelestial} or
 *       vanilla's {@code BiomeDecorator}), seeded {@code x * a + z * b ^ seed}. hbm-space's {@code preventOrganicSpawn}
 *       denied the "organic" decoration types (trees, flowers, grass, mushrooms, reeds, pumpkins, cacti, ...) on every
 *       body without life, which is every body but Laythe. So:
 *       <ul>
 *         <li>Laythe: vanilla's ores in its vanilla stone (dirt, gravel, iron, gold, redstone, diamond, lapis; hbm-space
 *             denied coal on every celestial body), then 32 tries of water plants (16 on the reef) and, on the reef,
 *             32 tries of coral;</li>
 *         <li>Tekto: the vanilla ores find no stone, and its 64 rubber plants per chunk ask for the denied type (REED),
 *             so 1.7 placed none: nothing (ported oddity);</li>
 *         <li>Thatmo: vanilla's decorator, every organic type denied and its ores finding no stone: nothing.</li>
 *       </ul></li>
 *   <li><b>Tekto's {@code WorldGeneratorTekto}</b>, an {@code IWorldGenerator} with FML's seed
 *       {@code (xSeed * x + zSeed * z) ^ seed}: four cobalt veins in basalt (placed only once the per-body
 *       {@code ntmspace:ore_cobalt} exists; their random draws are always made, so the trees land where 1.7 put them),
 *       then by the biome at the chunk's south-east corner PVC and vinyl trees ({@code TTree}) with rubber and plastic
 *       leaves and, in the Tekto forest, vinyl vines. Its re-placing of chloric geysers (to restart their block
 *       entities) has nothing to do here.</li>
 * </ul>
 *
 * <p>hbm-space's other {@code IWorldGenerator}s on these bodies place ores, bedrock ores and NBT structures, which belong
 * to those ports.
 */
public final class LaytheTektoDecoration {

    private LaytheTektoDecoration() {}

    // ---------------------------------------------------------------- Laythe

    /** {@code BiomeDecoratorCelestial.seaLevel} (hbm-space left Laythe's at the default). */
    private static final int SEA_LEVEL = 63;

    static void laythe(LayeredChunkGenerator generator, LaytheTerrain17 terrain, long seed, World17 world, int chunkX, int chunkZ) {
        Random rand = populateRandom(seed, chunkX, chunkZ);
        int k = chunkX * 16;
        int l = chunkZ * 16;
        Biome17 biome;
        synchronized (generator) {
            biome = terrain.biomeAt(k + 16, l + 16);
        }

        // hbm-space's DecorateBiomeEvent.Pre subscribers (OreCave, OreLayer3D) on the decorator's random, then the ores
        com.ntmspace.world.populate.Populate17.pre(com.ntmspace.world.populate.Populate17.Body.LAYTHE,
                new com.ntmspace.world.populate.LevelPopulateWorld(world.level(), chunkX, chunkZ), seed, k, l, rand);
        vanillaOres(world, rand, k, l);

        // lakes: generateLakes is false and lakeChancePerChunk 0
        int waterPlants = biome == terrain.coast ? 16 : 32;
        int coral = biome == terrain.coast ? 32 : 0;
        for (int i = 0; i < waterPlants; ++i) {
            int x = k + rand.nextInt(16) + 8;
            int z = l + rand.nextInt(16) + 8;
            int y = rand.nextInt(SEA_LEVEL);
            waterPlant(world, rand, x, y, z);
        }
        // rubberPlantsPerChunk is 0 on Laythe
        for (int i = 0; i < coral; ++i) {
            int x = k + rand.nextInt(16) + 8;
            int z = l + rand.nextInt(16) + 8;
            int y = rand.nextInt(SEA_LEVEL);
            coral(world, rand, x, y, z);
        }
    }

    /** 1.7 {@code BiomeDecorator.generateOres} with hbm-space's coal denial ({@code WorldGeneratorCelestial.onGenerateOre}). */
    private static void vanillaOres(World17 world, Random rand, int k, int l) {
        standardOre(world, rand, k, l, 20, Blocks.DIRT, 32, 0, 256);
        standardOre(world, rand, k, l, 10, Blocks.GRAVEL, 32, 0, 256);
        // coal: denied on celestial bodies
        standardOre(world, rand, k, l, 20, Blocks.IRON_ORE, 8, 0, 64);
        standardOre(world, rand, k, l, 2, Blocks.GOLD_ORE, 8, 0, 32);
        standardOre(world, rand, k, l, 8, Blocks.REDSTONE_ORE, 7, 0, 16);
        standardOre(world, rand, k, l, 1, Blocks.DIAMOND_ORE, 7, 0, 16);
        // genStandardOre2(1, lapisGen, 16, 16)
        for (int i = 0; i < 1; ++i) {
            int x = k + rand.nextInt(16);
            int y = rand.nextInt(16) + rand.nextInt(16) + (16 - 16);
            int z = l + rand.nextInt(16);
            minable(world, rand, x, y, z, Blocks.LAPIS_ORE.defaultBlockState(), 6, Blocks.STONE);
        }
    }

    /** 1.7 {@code genStandardOre1}. */
    private static void standardOre(World17 world, Random rand, int k, int l, int count, Block ore, int size, int minY, int maxY) {
        for (int i = 0; i < count; ++i) {
            int x = k + rand.nextInt(16);
            int y = rand.nextInt(maxY - minY) + minY;
            int z = l + rand.nextInt(16);
            minable(world, rand, x, y, z, ore.defaultBlockState(), size, Blocks.STONE);
        }
    }

    /**
     * 1.7 {@code WorldGenMinable.generate} (checked against the 1.7.10 jar's bytecode for its random draws: a float, two
     * ints, then one double per step): a vein of {@code size} along a random line through {@code (x + 8, z + 8)},
     * replacing {@code target}. A null {@code ore} draws the same numbers and places nothing.
     */
    static void minable(World17 world, Random rand, int x, int y, int z, BlockState ore, int size, Block target) {
        float f = rand.nextFloat() * (float) Math.PI;
        double d0 = (double) ((float) (x + 8) + Math17.sin(f) * (float) size / 8.0F);
        double d1 = (double) ((float) (x + 8) - Math17.sin(f) * (float) size / 8.0F);
        double d2 = (double) ((float) (z + 8) + Math17.cos(f) * (float) size / 8.0F);
        double d3 = (double) ((float) (z + 8) - Math17.cos(f) * (float) size / 8.0F);
        double d4 = (double) (y + rand.nextInt(3) - 2);
        double d5 = (double) (y + rand.nextInt(3) - 2);

        for (int l = 0; l <= size; ++l) {
            double d6 = d0 + (d1 - d0) * (double) l / (double) size;
            double d7 = d4 + (d5 - d4) * (double) l / (double) size;
            double d8 = d2 + (d3 - d2) * (double) l / (double) size;
            double d9 = rand.nextDouble() * (double) size / 16.0D;
            double d10 = (double) (Math17.sin((float) l * (float) Math.PI / (float) size) + 1.0F) * d9 + 1.0D;
            double d11 = (double) (Math17.sin((float) l * (float) Math.PI / (float) size) + 1.0F) * d9 + 1.0D;
            if (ore == null) continue;
            int i1 = Math17.floor_double(d6 - d10 / 2.0D);
            int j1 = Math17.floor_double(d7 - d11 / 2.0D);
            int k1 = Math17.floor_double(d8 - d10 / 2.0D);
            int l1 = Math17.floor_double(d6 + d10 / 2.0D);
            int i2 = Math17.floor_double(d7 + d11 / 2.0D);
            int j2 = Math17.floor_double(d8 + d10 / 2.0D);

            for (int k2 = i1; k2 <= l1; ++k2) {
                double d12 = ((double) k2 + 0.5D - d6) / (d10 / 2.0D);
                if (d12 * d12 < 1.0D) {
                    for (int l2 = j1; l2 <= i2; ++l2) {
                        double d13 = ((double) l2 + 0.5D - d7) / (d11 / 2.0D);
                        if (d12 * d12 + d13 * d13 < 1.0D) {
                            for (int i3 = k1; i3 <= j2; ++i3) {
                                double d14 = ((double) i3 + 0.5D - d8) / (d10 / 2.0D);
                                if (d12 * d12 + d13 * d13 + d14 * d14 < 1.0D && world.is(k2, l2, i3, target)) {
                                    world.set(k2, l2, i3, ore);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** hbm-space's {@code WorldGenWaterPlant} with {@code seaLevel} 63. */
    private static void waterPlant(World17 world, Random rand, int x, int y, int z) {
        waterPlant(world, rand, x, y, z, SEA_LEVEL);
    }

    public static boolean waterPlant(WorldGenLevel level, RandomSource random,
                                  BlockPos origin, int seaLevel) {
        var world = World17.live(level, origin.getX() >> 4, origin.getZ() >> 4);
        waterPlant(world, new RandomSourceRandom(random), origin.getX(), origin.getY(), origin.getZ(), seaLevel);
        return world.writes() > 0;
    }

    private static void waterPlant(World17 world, Random rand, int x, int y, int z, int seaLevel) {
        Block silt = LaytheTektoBlocks.LAYTHE_SILT.get();
        for (int l = 0; l < 128; ++l) {
            int px = x + rand.nextInt(8) - rand.nextInt(8);
            int py = y + rand.nextInt(4) - rand.nextInt(4);
            int pz = z + rand.nextInt(8) - rand.nextInt(8);

            if (py < seaLevel - 1 && world.is(px, py, pz, Blocks.WATER) && world.is(px, py - 1, pz, silt)) {
                int type = rand.nextInt(7);

                switch (type) {
                    case 0, 1, 2 -> world.set(px, py, pz, LaytheTektoBlocks.LAYTHE_SEAGRASS.get().defaultBlockState());
                    case 3 -> world.set(px, py, pz, LaytheTektoBlocks.LAYTHE_GLOWGRASS.get().defaultBlockState());
                    case 4, 5 -> {
                        if (py < seaLevel - 2) {
                            BlockState tall = LaytheTektoBlocks.PLANT_TALL_LAYTHE.get().defaultBlockState();
                            world.set(px, py, pz, tall.setValue(BlockLaytheTallPlant.HALF, DoubleBlockHalf.LOWER));
                            world.set(px, py + 1, pz, tall.setValue(BlockLaytheTallPlant.HALF, DoubleBlockHalf.UPPER));
                        }
                    }
                    default -> {
                        if (py < seaLevel - 4) {
                            int height = 2 + rand.nextInt(Math.min(8, seaLevel - py - 2));
                            BlockState kelp = LaytheTektoBlocks.LAYTHE_KELP.get().defaultBlockState();
                            for (int h = 0; h < height; ++h) {
                                world.set(px, py + h, pz, kelp.setValue(BlockLaytheKelp.TOP, false));
                            }
                            // 1.7 kept metadata 8 on a kelp block with no kelp above it (onBlockAdded/neighbour change)
                            int top = py + height - 1;
                            if (!world.is(px, top + 1, pz, LaytheTektoBlocks.LAYTHE_KELP.get())) {
                                world.set(px, top, pz, kelp.setValue(BlockLaytheKelp.TOP, true));
                            }
                        }
                    }
                }
            }
        }
    }

    /** {@code Material.water} in 1.7: water and Laythe's wet plants (not the coral block). */
    private static boolean isWater(World17 world, int x, int y, int z) {
        BlockState state = world.get(x, y, z);
        return state.is(Blocks.WATER) || state.is(LaytheTektoBlocks.LAYTHE_SEAGRASS.get())
                || state.is(LaytheTektoBlocks.LAYTHE_GLOWGRASS.get()) || state.is(LaytheTektoBlocks.PLANT_TALL_LAYTHE.get())
                || state.is(LaytheTektoBlocks.LAYTHE_KELP.get()) || state.is(LaytheTektoBlocks.LAYTHE_CORAL.get());
    }

    /** hbm-space's {@code WorldGenWaterCoral} with {@code seaLevel} 63. */
    private static void coral(World17 world, Random rand, int x, int y, int z) {
        coral(world, rand, x, y, z, SEA_LEVEL);
    }

    public static boolean coral(WorldGenLevel level, RandomSource random,
                                  BlockPos origin, int seaLevel) {
        var world = World17.live(level, origin.getX() >> 4, origin.getZ() >> 4);
        coral(world, new RandomSourceRandom(random), origin.getX(), origin.getY(), origin.getZ(), seaLevel);
        return world.writes() > 0;
    }

    private static void coral(World17 world, Random rand, int x, int y, int z, int seaLevel) {
        Block silt = LaytheTektoBlocks.LAYTHE_SILT.get();
        for (int l = 0; l < 16; ++l) {
            int px = x + rand.nextInt(8) - rand.nextInt(8);
            int py = y + rand.nextInt(4) - rand.nextInt(4);
            int pz = z + rand.nextInt(8) - rand.nextInt(8);

            if (!world.exists(px, py, pz)) continue;

            if (py < seaLevel - 1 && isWater(world, px, py, pz) && world.is(px, py - 1, pz, silt)) {
                int meta = rand.nextInt(PlantType.Coral.values().length) + 8;
                PlantType.Coral type = PlantType.Coral.ofMeta(meta);
                BlockState block = LaytheTektoBlocks.LAYTHE_CORAL_BLOCK.get().defaultBlockState().setValue(BlockLaytheCoralBlock.TYPE, type);
                BlockState plant = LaytheTektoBlocks.LAYTHE_CORAL.get().defaultBlockState().setValue(BlockLaytheCoral.TYPE, type);

                world.set(px, py - 1, pz, block);

                int oy = 0;
                int ox = 0;
                int oz = 0;
                while (oy < 8) {
                    if (py + oy > seaLevel - 3) break;
                    if (rand.nextBoolean()) break;

                    if (rand.nextBoolean()) {
                        if (rand.nextBoolean()) {
                            ox += rand.nextBoolean() ? 1 : -1;
                        } else {
                            oz += rand.nextBoolean() ? 1 : -1;
                        }
                    }

                    if (world.exists(px + ox, py + oy, pz + oz)) world.set(px + ox, py + oy, pz + oz, block);
                    if (rand.nextBoolean() && world.exists(px + ox + 1, py + oy, pz + oz)) coralPlant(world, px + ox + 1, py + oy, pz + oz, plant);
                    if (rand.nextBoolean() && world.exists(px + ox - 1, py + oy, pz + oz)) coralPlant(world, px + ox - 1, py + oy, pz + oz, plant);
                    if (rand.nextBoolean() && world.exists(px + ox, py + oy, pz + oz + 1)) coralPlant(world, px + ox, py + oy, pz + oz + 1, plant);
                    if (rand.nextBoolean() && world.exists(px + ox, py + oy, pz + oz - 1)) coralPlant(world, px + ox, py + oy, pz + oz - 1, plant);

                    oy++;
                }

                if (world.exists(px + ox, py + oy, pz + oz)) coralPlant(world, px + ox, py + oy, pz + oz, plant);
            }
        }
    }

    /**
     * A coral plant, marked for the chunk's post-processing: 1.7 leaned it towards its first solid neighbour at render
     * time ({@code RenderCoral}); here the state's {@code facing} is computed once the chunk is complete
     * ({@code BlockLaytheCoral.onUpdateShape}), as its neighbours may be placed after it.
     */
    private static void coralPlant(World17 world, int x, int y, int z, BlockState plant) {
        world.set(x, y, z, plant);
        world.postProcess(x, y, z);
    }

    // ---------------------------------------------------------------- Tekto

    /** hbm-space's {@code WorldConfig.cobaltSpawn} default; Tekto generates twice as many veins. */
    private static final int COBALT_SPAWN = 2;

    static void tekto(LayeredChunkGenerator generator, TektoTerrain17 terrain, long seed, World17 world, int chunkX, int chunkZ) {
        // the biome decorator: vanilla ores find no stone, and the rubber plants are a denied type; draws only, no blocks
        Random rand = fmlRandom(seed, chunkX, chunkZ);
        int i = chunkX * 16;
        int j = chunkZ * 16;

        // DungeonToolbox.generateOre(world, rand, i, j, cobaltSpawn * 2, 6, 4, 8, ore_cobalt, meta, basalt)
        BlockState cobalt = optionalState("ntmspace:ore_cobalt[body=tekto]");
        Block basalt = BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse("hbm:basalt")).orElseThrow();
        for (int v = 0; v < COBALT_SPAWN * 2; v++) {
            int x = i + rand.nextInt(16);
            int y = 4 + rand.nextInt(8);
            int z = j + rand.nextInt(16);
            minable(world, rand, x, y, z, cobalt, 6, basalt);
        }

        Biome17 biome;
        synchronized (generator) {
            biome = terrain.biomeAt(i + 16, j + 16);
        }

        Block pvc = LaytheTektoBlocks.PVC_LOG.get();
        Block vinyl = LaytheTektoBlocks.VINYL_LOG.get();
        Block rubberLeaves = LaytheTektoBlocks.RUBBER_LEAVES.get();
        Block petLeaves = LaytheTektoBlocks.PET_LEAVES.get();

        if (biome == terrain.polyvinylPlains) {
            for (int o = 0; o < 2; o++) {
                if (rand.nextInt(10) == 0) {
                    int x = i + rand.nextInt(16) + 8;
                    int z = j + rand.nextInt(16) + 8;
                    int y = world.heightValue(x, z);
                    tree(world, rand, x, y, z, 4, 2, 10, 2, 4, false, pvc, rubberLeaves);
                }

                if (rand.nextInt(8) == 0) {
                    int x = i + rand.nextInt(16) + 8;
                    int z = j + rand.nextInt(16) + 8;
                    int y = world.heightValue(x, z);
                    tree(world, rand, x, y, z, 2, 4, 5, 3, 2, false, vinyl, petLeaves);
                }
            }
        }

        if (biome == terrain.halogenHills) {
            if (rand.nextInt(12) == 0) {
                for (int o = 0; o < 4; o++) {
                    int x = i + rand.nextInt(16) + 8;
                    int z = j + rand.nextInt(16) + 8;
                    int y = world.heightValue(x, z);
                    tree(world, rand, x, y, z, 3, 2, 14, 3, 3, false, pvc, rubberLeaves);
                }
            }
        }

        if (biome == terrain.forest) {
            for (int o = 0; o < 8; o++) {
                int x = i + rand.nextInt(16) + 8;
                int z = j + rand.nextInt(16) + 8;
                int y = world.heightValue(x, z);

                if (rand.nextInt(2) == 0) {
                    tree(world, rand, x, y, z, 3, 2, 20, 3, 5, true, pvc, rubberLeaves);
                } else {
                    tree(world, rand, x, y, z, 3, 1, 1, 3, 5, false, pvc, rubberLeaves);
                }
            }
        }
    }

    /** hbm-space's {@code TTree.generate}: a log column and an ellipsoid bulb of leaves, with optional hanging vines. */
    static boolean tree(World17 world, Random rand, int x, int y, int z, int offset, int smallest, int tallest,
                                int xz, int yRadius, boolean vines, Block logBlock, Block leafBlock) {
        return tree(world, rand::nextInt, x, y, z, offset, smallest, tallest, xz, yRadius, vines,
                logBlock.defaultBlockState(), leafBlock.defaultBlockState());
    }

    /** The same geometry and bounded draws for both the legacy decorator and configured sapling features. */
    static boolean tree(World17 world, IntUnaryOperator roll, int x, int y, int z,
                        int offset, int smallest, int tallest, int xz, int yRadius, boolean vines,
                        BlockState log, BlockState leaves) {
        int height = roll.applyAsInt(smallest) + tallest;

        if (y <= world.minY() || y + height + 1 > world.maxY()) {
            return false;
        }

        BlockState blockBelow = world.get(x, y - 1, z);
        if (!blockBelow.is(LaytheTektoBlocks.RUBBER_GRASS.get()) && !blockBelow.is(LaytheTektoBlocks.RUBBER_SILT.get())) {
            return false;
        }

        for (int i = 0; i < height; i++) {
            world.set(x, y + i, z, log);
        }

        int bulbStartY = y + height - offset;
        BlockState vine = LaytheTektoBlocks.VINYL_VINE.get().defaultBlockState();

        for (int dy = -yRadius; dy <= yRadius; dy++) {
            for (int dx = -xz; dx <= xz; dx++) {
                for (int dz = -xz; dz <= xz; dz++) {

                    if (Math.pow(dx / (double) xz, 2) + Math.pow(dy / (double) yRadius, 2) + Math.pow(dz / (double) xz, 2) <= 1) {
                        if (world.isAir(x + dx, bulbStartY + dy, z + dz)) {
                            world.set(x + dx, bulbStartY + dy, z + dz, leaves);
                            if (vines) {
                                for (int i = 0; i < 4; i++) {
                                    int vineX = x + dx + (i % 2 == 0 ? (i - 1) : 0);
                                    int vineZ = z + dz + (i % 2 == 1 ? (i - 2) : 0);

                                    if (roll.applyAsInt(12) == 0) {
                                        for (int k = 0; k < roll.applyAsInt(15); k++) {
                                            int vineY = bulbStartY + dy - k;
                                            if (world.isAir(vineX, vineY, vineZ)) {
                                                world.set(vineX, vineY, vineZ, vineFaces(vine, vineX, vineZ, x + dx, z + dz));
                                            } else {
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * The vine metadata {@code TTree} set, as vanilla's faces (1.7 vine bits: 1 south, 2 west, 4 north, 8 east): 2 west
     * of the leaves, 1 east, 4 south and 3 (south and west) north of them, exactly as hbm-space wrote it.
     */
    private static BlockState vineFaces(BlockState vine, int vineX, int vineZ, int leafX, int leafZ) {
        int meta;
        if (vineX > leafX) meta = 0x2;
        else if (vineX < leafX) meta = 0x1;
        else if (vineZ > leafZ) meta = 0x4;
        else if (vineZ < leafZ) meta = 0x3;
        else meta = 0;
        return vine.setValue(VineBlock.SOUTH, (meta & 1) != 0).setValue(VineBlock.WEST, (meta & 2) != 0)
                .setValue(VineBlock.NORTH, (meta & 4) != 0).setValue(VineBlock.EAST, (meta & 8) != 0)
                .setValue(VineBlock.UP, false);
    }

    // ---------------------------------------------------------------- seeds

    /** {@code ChunkProviderCelestial.populate}'s random, before {@code biome.decorate}. */
    static Random populateRandom(long seed, int chunkX, int chunkZ) {
        Random rand = new Random(seed);
        long i1 = rand.nextLong() / 2L * 2L + 1L;
        long j1 = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed((long) chunkX * i1 + (long) chunkZ * j1 ^ seed);
        return rand;
    }

    /** 1.7 FML {@code GameRegistry.generateWorld}'s random, as each {@code IWorldGenerator} gets it. */
    static Random fmlRandom(long seed, int chunkX, int chunkZ) {
        Random fmlRandom = new Random(seed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * chunkX + zSeed * chunkZ) ^ seed;
        fmlRandom.setSeed(chunkSeed);
        return fmlRandom;
    }

    /** A registered block state by state string, or null. */
    private static BlockState optionalState(String id) {
        int bracket = id.indexOf('[');
        String name = bracket < 0 ? id : id.substring(0, bracket);
        if (BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse(name)).isEmpty()) return null;
        return com.hbm.util.SavedIds.stateFromName(id, null);
    }
}
