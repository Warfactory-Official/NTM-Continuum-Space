package com.ntmspace.world.populate;

import com.ntmspace.block.terrain.BlockLaytheSilt;
import com.ntmspace.registry.LaytheTektoBlocks;

import java.util.Collections;
import java.util.LinkedHashMap;

import com.hbm.config.GeneralConfig;
import com.hbm.config.WorldConfig;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.block.BlockBodyOre;
import com.ntmspace.registry.BodyOreBlocks;
import com.ntmspace.world.gen.legacy.DunaTerrain17;
import com.ntmspace.world.gen.legacy.EveTerrain17;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import com.ntmspace.world.gen.legacy.Ores17;
import com.ntmspace.world.gen.noise.Noise17Perlin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's populate of a celestial body, 1:1, run at the modern FEATURES step of each chunk with 1.7's own seeds:
 *
 * <ol>
 *   <li><b>The biome decorator</b> ({@code ChunkProviderCelestial.populate} seeds a {@code Random} with
 *       {@code x * a + z * b ^ seed} and calls {@code biome.decorate}): first the {@code DecorateBiomeEvent.Pre}
 *       subscribers hbm-space's {@code MainRegistry} registered, in registration order (the global {@code OreLayer3D}
 *       hematite, bauxite and malachite layers, then each body's {@code OreCave}s and {@code OreLayer3D}s; the caves draw
 *       from the decorator's {@code Random}), then the decorator: vanilla's {@code generateOres} (coal denied by
 *       {@code WorldGeneratorCelestial.onGenerateOre}; its veins only replace vanilla stone), then
 *       {@code BiomeDecoratorCelestial}'s lava springs ({@code WorldGenLiquidsCelestial}: Eve 20, Moho 50) and lakes
 *       (Ike's bromine, one chunk in eight). Organic decoration is denied on every lifeless body
 *       ({@code ModEventHandler.preventOrganicSpawn}). Laythe's decorator is {@code LaytheTektoDecoration}'s, which calls
 *       {@link #pre} first.</li>
 *   <li><b>The FML world generators</b>, each with a {@code Random} FML seeded afresh per generator
 *       ({@link Ores17#generatorRandom}), in FML's weight order: the body's {@code WorldGenerator<Body>} (weight 1;
 *       Tekto's is {@code LaytheTektoDecoration}'s), HBM's {@code HbmWorldGen} surface pass (weight 1: fallen meteorites
 *       on every celestial body), then {@code WorldGeneratorCelestial} (weight 2: vanilla ores in the body's stone, HBM's
 *       ores, depth deposits, keyholes, bedrock ores).</li>
 * </ol>
 *
 * <p>1.7 decorated a chunk once its south-east neighbours existed and could write a little past it; modern generation
 * decorates in its own order and only within the chunk and its neighbours ({@link PopulateWorld}), so where two chunks'
 * decorations overlap the later one may differ, and a feature reaching further (Eve's volcanoes) is cut off.
 *
 * <p>Not here: hbm-space's NBT structures (registered by these generators' constructors, built by
 * {@code NTMWorldGenerator}), Ike's pedestal re-stocking, Ike's ancient tomb, Duna's broken behemoth lantern and HBM's
 * spaceship (their random draws come last in their generators, so nothing else moves).
 */
public final class Populate17 {

    /**
     * The 1.7 body a chunk generator populates, with what its {@code WorldProviderCelestial} answered.
     */
    public enum Body {
        MUN(BlockBodyOre.Body.MUN, "ntmspace:moon_rock", 413_015, true, false, 0, 0),
        MINMUS(BlockBodyOre.Body.MINMUS, "ntmspace:minmus_stone", 413_021, true, true, 0, 0),
        DUNA(BlockBodyOre.Body.DUNA, "ntmspace:duna_rock", 413_016, true, true, 0, 0),
        IKE(BlockBodyOre.Body.IKE, "ntmspace:ike_stone", 413_017, true, true, 0, 8),
        DRES(BlockBodyOre.Body.DRES, "ntmspace:dres_rock", 413_019, true, true, 0, 0),
        EVE(BlockBodyOre.Body.EVE, "ntmspace:eve_rock", 413_018, false, true, 20, 0),
        MOHO(BlockBodyOre.Body.MOHO, "ntmspace:moho_stone", 413_020, false, true, 50, 0),
        LAYTHE(BlockBodyOre.Body.LAYTHE, "minecraft:stone", 413_022, false, true, 0, 0),
        TEKTO(BlockBodyOre.Body.TEKTO, "hbm:basalt", 413_024, true, true, 0, 0),
        /**
         * Thatmo is no {@code SolarSystem.Body}: {@code CelestialBody.getBody} answered Kerbin for it.
         */
        THATMO(BlockBodyOre.Body.KERBIN, "hbm:sellafield_slaked", 413_025, false, false, 0, 0);

        /**
         * The ore metadata ({@code CelestialBody.getMeta}).
         */
        public final BlockBodyOre.Body ore;
        /**
         * {@code WorldProviderCelestial.getStone}.
         */
        public final String stone;
        /**
         * The 1.7 dimension id ({@code SpaceConfig} default), which salted the bedrock ore noise.
         */
        public final int dimension;
        /**
         * {@code hasIce}: bedrock ice deposits.
         */
        public final boolean ice;
        /**
         * The decorator is hbm-space's {@code BiomeDecoratorCelestial} (else vanilla's).
         */
        public final boolean celestialDecorator;
        /**
         * {@code generateLakes ? lavaCount : 0}.
         */
        public final int lavaSprings;
        /**
         * {@code lakeChancePerChunk}.
         */
        public final int lakeChance;

        Body(BlockBodyOre.Body ore, String stone, int dimension, boolean ice, boolean celestialDecorator,
             int lavaSprings, int lakeChance) {
            this.ore = ore;
            this.stone = stone;
            this.dimension = dimension;
            this.ice = ice;
            this.celestialDecorator = celestialDecorator;
            this.lavaSprings = lavaSprings;
            this.lakeChance = lakeChance;
        }

        public Block stoneBlock() {
            return PopulateWorld.block(stone);
        }

        /**
         * {@code WorldProviderCelestial.getBedrockAcid}: Dres nitric acid, Eve permanganate, else none.
         */
        public @Nullable FluidStack bedrockAcid() {
            return switch (this) {
                case DRES -> new FluidStack(Fluids.NITRIC_ACID, 500);
                case EVE -> new FluidStack(com.ntmspace.fluid.SpaceFluids.KMnO4, 500);
                default -> null;
            };
        }

        /**
         * The global indices of the body's {@code CelestialBedrockOre} types, in {@code CelestialBedrockOre}'s
         * registration order (Kerbin 4, Mun 5, Minmus 5, Duna 6, Moho 6, Dres 6, Eve 6, Ike 4, Laythe 6, Tekto 6).
         */
        public int[] bedrockOreTypes() {
            int[] start = {0, 4, 9, 14, 20, 26, 32, 38, 42, 48};
            int[] count = {4, 5, 5, 6, 6, 6, 6, 4, 6, 6};
            int index = ore.ordinal();
            int[] types = new int[count[index]];
            for (int i = 0; i < types.length; i++) types[i] = start[index] + i;
            return types;
        }
    }

    /**
     * hbm-space's {@code WorldConfig} defaults for keys HBM does not have ({@code ironSpawn} had no config key).
     */
    public static final int IRON_SPAWN = 9;
    public static final int NICKEL_SPAWN = 12;
    public static final int ZINC_SPAWN = 8;
    public static final int MINERAL_SPAWN = 4;

    private Populate17() {
    }

    // ---------------------------------------------------------------- entry points

    /**
     * Populates a chunk of {@code body} at the FEATURES step: decorator, then the FML generators. Laythe and Tekto are
     * populated in part by {@code LaytheTektoDecoration} (use {@link #decorator}/{@link #generators} around it).
     */
    public static void populate(Body body, WorldGenLevel level, long seed, int chunkX, int chunkZ,
                                @Nullable LayeredBodyTerrain17 terrain, Object lock) {
        PopulateWorld world = new LevelPopulateWorld(level, chunkX, chunkZ);
        decorator(body, world, seed, chunkX, chunkZ);
        generators(body, world, seed, chunkX, chunkZ, terrain, lock, true);
    }

    /**
     * {@code ChunkProviderCelestial.populate}'s random, before {@code biome.decorate}.
     */
    public static Random populateRandom(long seed, int chunkX, int chunkZ) {
        Random rand = new Random(seed);
        long i1 = rand.nextLong() / 2L * 2L + 1L;
        long j1 = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed((long) chunkX * i1 + (long) chunkZ * j1 ^ seed);
        return rand;
    }

    /**
     * FML's per-generator chunk seed ({@code GameRegistry.generateWorld}); {@code new Random(it)} is the generator's random.
     */
    public static long generatorSeed(long worldSeed, int chunkX, int chunkZ) {
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        return (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
    }

    // ---------------------------------------------------------------- the decorator

    /**
     * The biome decorator of a chunk of {@code body} (not Laythe's, see {@code LaytheTektoDecoration}).
     */
    public static void decorator(Body body, PopulateWorld world, long seed, int chunkX, int chunkZ) {
        decorator(body, world, seed, chunkX, chunkZ, populateRandom(seed, chunkX, chunkZ));
    }

    /**
     * {@link #decorator(Body, PopulateWorld, long, int, int)} drawing from {@code rand}.
     */
    public static void decorator(Body body, PopulateWorld world, long seed, int chunkX, int chunkZ, Random rand) {
        int k = chunkX * 16;
        int l = chunkZ * 16;
        pre(body, world, seed, k, l, rand);
        vanillaOres(world, rand, k, l);
        if (!body.celestialDecorator) return; // vanilla's decorator: sand and clay need water, the rest is denied

        Block stone = body.stoneBlock();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        for (int i = 0; i < body.lavaSprings; ++i) {
            int x = k + rand.nextInt(16) + 8;
            int y = rand.nextInt(rand.nextInt(rand.nextInt(240) + 8) + 8);
            int z = l + rand.nextInt(16) + 8;
            Lakes17.spring(world, x, y, z, lava, stone);
        }

        if (body.lakeChance > 0 && rand.nextInt(body.lakeChance) == 0) {
            int x = k + rand.nextInt(16) + 8;
            int y = rand.nextInt(256);
            int z = l + rand.nextInt(16) + 8;
            // ntmspace:bromine_block, once the fluids port registers it (the lake's draws are made either way)
            Lakes17.lake(world, rand, x, y, z, PopulateWorld.state("ntmspace:bromine_block"));
        }
        // water plants, rubber plants (REED, denied) and coral: none on these bodies
    }

    /**
     * The {@code DecorateBiomeEvent.Pre} subscribers for a chunk of {@code body} at block origin {@code (k, l)}, in
     * hbm-space's registration order, drawing from the decorator's {@code rand}.
     */
    public static void pre(Body body, PopulateWorld world, long seed, int k, int l, Random rand) {
        Block stone = body.stoneBlock();
        Features features = Features.get();
        for (OreLayer3D17 layer : features.global) layer.generate(world, seed, k, l, stone);
        switch (body) {
            case MUN -> {
                if (features.sulfurCave != null) features.sulfurCave.generate(world, seed, k, l, stone, rand);
                features.munConglomerate.generate(world, seed, k, l, stone);
            }
            case DUNA, MOHO -> {
                if (features.sulfurCave != null) features.sulfurCave.generate(world, seed, k, l, stone, rand);
            }
            case IKE -> {
                if (features.sulfurCave != null) features.sulfurCave.generate(world, seed, k, l, stone, rand);
                features.ikeConglomerate.generate(world, seed, k, l, stone);
            }
            case EVE -> {
                if (features.acidSulfurCave != null) features.acidSulfurCave.generate(world, seed, k, l, stone, rand);
                features.eveBasaltCave.generate(world, seed, k, l, stone, rand);
            }
            case MINMUS -> {
                if (features.sulfurCave != null) features.sulfurCave.generate(world, seed, k, l, stone, rand);
                features.minmusRegolith.generate(world, seed, k, l, stone);
                features.minmusSmooth.generate(world, seed, k, l, stone);
                features.minmusConglomerate.generate(world, seed, k, l, stone);
            }
            case LAYTHE -> {
                for (var cave : laytheCaves().values()) cave.generate(world, seed, k, l, stone, rand);
            }
            default -> {
            }
        }
    }

    /**
     * Vanilla 1.7 {@code BiomeDecorator.generateOres} with hbm-space's coal denial: dirt, gravel, iron, gold, redstone,
     * diamond and lapis veins in vanilla stone (which most bodies do not have: the draws are made all the same).
     */
    public static void vanillaOres(PopulateWorld world, Random rand, int k, int l) {
        Block stone = Blocks.STONE;
        standardOre(world, rand, k, l, 20, Blocks.DIRT.defaultBlockState(), 32, 0, 256, stone);
        standardOre(world, rand, k, l, 10, Blocks.GRAVEL.defaultBlockState(), 32, 0, 256, stone);
        // coal: denied on celestial bodies
        standardOre(world, rand, k, l, 20, Blocks.IRON_ORE.defaultBlockState(), 8, 0, 64, stone);
        standardOre(world, rand, k, l, 2, Blocks.GOLD_ORE.defaultBlockState(), 8, 0, 32, stone);
        standardOre(world, rand, k, l, 8, Blocks.REDSTONE_ORE.defaultBlockState(), 7, 0, 16, stone);
        standardOre(world, rand, k, l, 1, Blocks.DIAMOND_ORE.defaultBlockState(), 7, 0, 16, stone);
        // genStandardOre2(1, lapisGen, 16, 16)
        int x = k + rand.nextInt(16);
        int y = rand.nextInt(16) + rand.nextInt(16) + (16 - 16);
        int z = l + rand.nextInt(16);
        Ores17.minable(rand, x, y, z, 6, placer(world, Blocks.LAPIS_ORE.defaultBlockState(), stone));
    }

    /**
     * 1.7 {@code genStandardOre1} (and {@code WorldGeneratorCelestial.genVanillaOre}, the same draws).
     */
    private static void standardOre(PopulateWorld world, Random rand, int k, int l, int count, BlockState ore, int size,
                                    int minY, int maxY, Block target) {
        Ores17.Placer placer = placer(world, ore, target);
        for (int i = 0; i < count; ++i) {
            int x = k + rand.nextInt(16);
            int y = rand.nextInt(maxY - minY) + minY;
            int z = l + rand.nextInt(16);
            Ores17.minable(rand, x, y, z, size, placer);
        }
    }

    /**
     * Ordered native producers used by Laythe's live pre-decoration pass; disabled caves are absent.
     */
    public static Map<String, OreCave17> laytheCaves() {
        return Features.get().laytheCaves;
    }

    /**
     * The acid/sulfur producer used by Eve's pre-decoration pass.
     */
    public static OreCave17 eveSulfurCave() {
        return Features.get().acidSulfurCave;
    }

    // ---------------------------------------------------------------- the FML world generators

    /**
     * The FML generators of a chunk of {@code body}: its own {@code WorldGenerator<Body>} (unless
     * {@code bodyGenerator} is false: Tekto's runs in {@code LaytheTektoDecoration}), HBM's surface pass, then
     * {@code WorldGeneratorCelestial}.
     */
    public static void generators(Body body, PopulateWorld world, long seed, int chunkX, int chunkZ,
                                  @Nullable LayeredBodyTerrain17 terrain, Object lock, boolean bodyGenerator) {
        long chunkSeed = generatorSeed(seed, chunkX, chunkZ);
        int i = chunkX * 16;
        int j = chunkZ * 16;
        if (bodyGenerator) BodyGenerators17.generate(body, world, new Random(chunkSeed), i, j, terrain, lock);
        hbmSurface(world, chunkSeed, i, j);
        celestial(body, world, seed, new Random(chunkSeed), i, j);
    }

    /**
     * HBM's {@code HbmWorldGen.generateSurface} on a celestial body (it returned before its ores there): a fallen
     * meteorite one chunk in {@code meteoriteSpawn}, then a spaceship one chunk in 1000 (not built: HBM builds its
     * spaceship as a structure). The meteorite is HBM's own, given 1.7's random as a {@code RandomSource} (the same
     * generator, seeded alike).
     */
    private static void hbmSurface(PopulateWorld world, long chunkSeed, int i, int j) {
        LegacyRandomSource rand = new LegacyRandomSource(chunkSeed);
        if (WorldConfig.meteoriteSpawn > 0 && rand.nextInt(WorldConfig.meteoriteSpawn) == 0) {
            int x = i + rand.nextInt(16) + 8;
            int z = j + rand.nextInt(16) + 8;
            int y = world.heightValue(x, z) - rand.nextInt(10);
            BlockState b = world.get(x, y - 2, z);
            if (!b.isAir() && !PopulateWorld.liquid(b) && y > 1) world.meteorite(x, y, z, rand);
        }
    }

    /**
     * {@code WorldGeneratorCelestial.generate}.
     */
    public static void celestial(Body body, PopulateWorld world, long seed, Random rand, int x, int z) {
        Block stone = body.stoneBlock();
        if (stone == null) return;
        BlockBodyOre.Body meta = body.ore;

        // generateStructures: empty
        if (stone != Blocks.STONE) {
            // generateVanillaOres: genVanillaOre(world, rand, x, z, yMin, yMax, count, size, ore, stone, meta)
            standardOre(world, rand, x, z, 20, ore("ore_iron", meta), 8, 0, 64, stone);
            standardOre(world, rand, x, z, 2, ore("ore_gold", meta), 8, 0, 32, stone);
            standardOre(world, rand, x, z, 8, ore("ore_redstone", meta), 7, 0, 16, stone);
            standardOre(world, rand, x, z, 1, ore("ore_diamond", meta), 7, 0, 16, stone);
        }

        ntmOres(world, rand, x, z, stone, meta);
        bedrockOres(body, world, seed, rand, x, z, stone);
    }

    /**
     * {@code WorldGeneratorCelestial.generateNTMOres}.
     */
    private static void ntmOres(PopulateWorld world, Random rand, int x, int z, Block stone, BlockBodyOre.Body meta) {
        if (WorldConfig.alexandriteChance > 0 && rand.nextInt(WorldConfig.alexandriteChance) == 0) {
            generateOre(world, rand, x, z, 1, 3, 10, 5, PopulateWorld.state("hbm:ore_alexandrite"), stone);
        }

        BlockState depth = PopulateWorld.state("hbm:stone_depth");
        DepthDeposit17.generateCondition(world, x, 0, 3, z, 5, 0.6D, PopulateWorld.state("hbm:cluster_depth_iron"),
                rand, 24, stone, depth);
        DepthDeposit17.generateCondition(world, x, 0, 3, z, 5, 0.6D, PopulateWorld.state("hbm:cluster_depth_titanium"),
                rand, 32, stone, depth);
        DepthDeposit17.generateCondition(world, x, 0, 3, z, 5, 0.6D, PopulateWorld.state("hbm:cluster_depth_tungsten"),
                rand, 32, stone, depth);
        DepthDeposit17.generateCondition(world, x, 0, 3, z, 5, 0.8D, PopulateWorld.state("hbm:ore_depth_cinnabar"),
                rand, 16, stone, depth);
        DepthDeposit17.generateCondition(world, x, 0, 3, z, 5, 0.8D, PopulateWorld.state("hbm:ore_depth_zirconium"),
                rand, 16, stone, depth);
        DepthDeposit17.generateCondition(world, x, 0, 3, z, 5, 0.8D, PopulateWorld.state("hbm:ore_depth_borax"), rand,
                16, stone, depth);

        generateOre(world, rand, x, z, WorldConfig.uraniumSpawn, 5, 5, 20, ore("ore_uranium", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.thoriumSpawn, 5, 5, 25, ore("ore_thorium", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.titaniumSpawn, 6, 5, 30, ore("ore_titanium", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.sulfurSpawn, 8, 5, 30, ore("ore_sulfur", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.aluminiumSpawn, 6, 5, 40, ore("ore_aluminium", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.copperSpawn, 6, 5, 45, ore("ore_copper", meta), stone);
        generateOre(world, rand, x, z, NICKEL_SPAWN, 6, 5, 10, ore("ore_nickel", meta), stone);
        generateOre(world, rand, x, z, ZINC_SPAWN, 6, 5, 32, ore("ore_zinc", meta), stone);
        // mineralSpawn: commented out in hbm-space
        generateOre(world, rand, x, z, WorldConfig.fluoriteSpawn, 4, 5, 45, ore("ore_fluorite", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.niterSpawn, 6, 5, 30, ore("ore_niter", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.tungstenSpawn, 8, 5, 30, ore("ore_tungsten", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.leadSpawn, 9, 5, 30, ore("ore_lead", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.berylliumSpawn, 4, 5, 30, ore("ore_beryllium", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.rareSpawn, 5, 5, 20, ore("ore_rare", meta), stone);
        // asbestosSpawn: commented out in hbm-space
        generateOre(world, rand, x, z, WorldConfig.cinnabarSpawn, 4, 8, 16, ore("ore_cinnebar", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.cobaltSpawn, 4, 4, 8, ore("ore_cobalt", meta), stone);

        generateOre(world, rand, x, z, WorldConfig.ironClusterSpawn, 6, 15, 45, ore("cluster_iron", meta), stone);
        generateOre(world, rand, x, z, WorldConfig.titaniumClusterSpawn, 6, 15, 30, ore("cluster_titanium", meta),
                stone);
        generateOre(world, rand, x, z, WorldConfig.aluminiumClusterSpawn, 6, 15, 35, ore("cluster_aluminium", meta),
                stone);
        generateOre(world, rand, x, z, WorldConfig.copperClusterSpawn, 6, 15, 20, ore("cluster_copper", meta), stone);

        // malachite: commented out in hbm-space
        generateOre(world, rand, x, z, WorldConfig.limestoneSpawn, 12, 25, 30,
                PopulateWorld.state("ntmspace:stone_resource_calcium"), stone);

        if (rand.nextInt(4) == 0) {
            int rx = x + rand.nextInt(16) + 8;
            int ry = 6 + rand.nextInt(13);
            int rz = z + rand.nextInt(16) + 8;
            if (world.is(rx, ry, rz, stone)) world.set(rx, ry, rz, PopulateWorld.state("hbm:stone_keyhole"));
        }
    }

    /**
     * {@code WorldGeneratorCelestial.generateBedrockOres}.
     */
    private static void bedrockOres(Body body, PopulateWorld world, long seed, Random rand, int x, int z, Block stone) {
        if (rand.nextInt(10) == 0) {
            int randPosX = x + rand.nextInt(2) + 8;
            int randPosZ = z + rand.nextInt(2) + 8;
            BedrockOre17.generateAuto(body, world, seed, randPosX, randPosZ, stone);
        } else if (body.ice && rand.nextInt(3) == 0) {
            int randPosX = x + rand.nextInt(2) + 8;
            int randPosZ = z + rand.nextInt(2) + 8;
            BedrockOre17.generate(world, randPosX, randPosZ,
                    new BedrockDeposit(new ItemStack(Items.PACKED_ICE, 8 * 4), null, 0xAAFFFF, 1), Blocks.STONE);
        }
    }

    // ---------------------------------------------------------------- shared helpers

    /**
     * The state of per-body ore {@code path} on {@code body}, or null while it is not registered.
     */
    public static BlockState ore(String path, BlockBodyOre.Body body) {
        return BodyOreBlocks.state(path, body);
    }

    /**
     * {@code DungeonToolbox.generateOre(world, rand, x, z, veins, amount, minHeight, variance, ore, meta, target)}.
     */
    public static void generateOre(PopulateWorld world, Random rand, int x, int z, int veinCount, int amount,
                                   int minHeight, int variance, BlockState ore, Block target) {
        Ores17.generateOre(rand, x, z, veinCount, amount, minHeight, variance, placer(world, ore, target));
    }

    /**
     * {@code WorldGenMinable}'s write: the vein's block where the world holds {@code target}; null to draw only.
     */
    public static Ores17.Placer placer(PopulateWorld world, BlockState ore, Block target) {
        if (ore == null || target == null) return null;
        return (x, y, z) -> {
            if (world.get(x, y, z).is(target)) world.set(x, y, z, ore);
        };
    }

    // ---------------------------------------------------------------- the Pre-event features

    /**
     * hbm-space's {@code MainRegistry} caves and layers, built once from HBM's config as 1.7 built them.
     */
    static final class Features {

        private static volatile Features instance;

        final List<OreLayer3D17> global = new ArrayList<>();
        final OreCave17 sulfurCave;
        final OreCave17 acidSulfurCave;
        final OreCave17 asbestosCave;
        final OreLayer3D17 munConglomerate;
        final OreLayer3D17 ikeConglomerate;
        final OreCave17 eveBasaltCave;
        final OreLayer3D17 minmusRegolith;
        final OreLayer3D17 minmusSmooth;
        final OreLayer3D17 minmusConglomerate;
        final OreCave17 laytheTumor;
        final OreCave17 laytheSilt6;
        final OreCave17 laytheSilt3;
        final Map<String, OreCave17> laytheCaves;

        static Features get() {
            Features f = instance;
            if (f == null) {
                synchronized (Features.class) {
                    if (instance == null) instance = new Features();
                    f = instance;
                }
            }
            return f;
        }

        private static Supplier<BlockState> s(String id) {
            return () -> PopulateWorld.state(id);
        }

        private Features() {
            String resource = "hbm:tile.stone_resource";
            int id = 0;
            // EnumStoneType: SULFUR 0, ASBESTOS 1, HEMATITE 2, MALACHITE 3, LIMESTONE 4, BAUXITE 5, CONGLOMERATE 6, CALCIUM 7
            if (WorldConfig.enableHematite)
                global.add(new OreLayer3D17(id++, s("hbm:stone_resource_hematite"), 0.04D, 0.25D, 230));
            if (WorldConfig.enableBauxite)
                global.add(new OreLayer3D17(id++, s("hbm:stone_resource_bauxite"), 0.03D, 0.15D, 300));
            if (WorldConfig.enableMalachite)
                global.add(new OreLayer3D17(id++, s("hbm:stone_resource_malachite"), 0.1D, 0.15D, 275));

            Supplier<BlockState> sulfur = s("hbm:stone_resource_sulfur");
            Supplier<BlockState> asbestos = s("hbm:stone_resource_asbestos");
            Supplier<BlockState> acid = s("hbm:sulfuric_acid_block");
            sulfurCave = WorldConfig.enableSulfurCave
                    ? new OreCave17(resource, 0, sulfur, 1.5D, 20, 20, 30).spikes(s("hbm:stalactite_sulfur"),
                    s("hbm:stalagmite_sulfur"))
                    : null;
            acidSulfurCave = WorldConfig.enableSulfurCave
                    ? new OreCave17(resource, 0, sulfur, 1.5D, 20, 20, 30).withFluid(acid)
                                                                          .spikes(s("hbm:stalactite_sulfur"),
                                                                                  s("hbm:stalagmite_sulfur"))
                    : null;
            asbestosCave = WorldConfig.enableAsbestosCave
                    ? new OreCave17(resource, 1, asbestos, 1.75D, 20, 20, 25).spikes(s("hbm:stalactite_asbestos"),
                    s("hbm:stalagmite_asbestos"))
                    : null;

            Supplier<BlockState> conglomerate = s("ntmspace:stone_resource_conglomerate");
            munConglomerate = new OreLayer3D17(id++, conglomerate, 0.04D, 0.25D, 220);
            ikeConglomerate = new OreLayer3D17(id++, conglomerate, 0.04D, 0.25D, 220);
            eveBasaltCave = new OreCave17("hbm:tile.basalt", 0, s("hbm:basalt"), 0.15D, 40, 24, 54)
                    .blockOverride(() -> PopulateWorld.block("ntmspace:eve_silt")).ignoreWater().noStalagmites();
            minmusRegolith = new OreLayer3D17(id++, s("ntmspace:minmus_regolith"), 0.06D, 0.25D, 220);
            minmusSmooth = new OreLayer3D17(id++, s("ntmspace:minmus_smooth"), 0.05D, 0.15D, 280);
            minmusConglomerate = new OreLayer3D17(id++, conglomerate, 0.04D, 0.25D, 220);
            // tumor: BlockStalagmite.getMetaFromResource(tumor) is FLESH
            laytheTumor = new OreCave17("hbm:tile.tumor", 0, s("ntmspace:tumor"), 0.3D, 20, 70, 25)
                    .spikes(s("ntmspace:stalactite_flesh"), s("ntmspace:stalagmite_flesh"));
            // Authored metadata 6 and 3 map to the real dark and light silt states; random draws are unchanged.
            laytheSilt6 = new OreCave17("hbm:tile.laythe_silt", 6,
                    () -> LaytheTektoBlocks.LAYTHE_SILT.get().defaultBlockState()
                                                       .setValue(BlockLaytheSilt.SHADE, 2), 0.25D, 80, 24, 54)
                    .blockOverride(() -> PopulateWorld.block("ntmspace:laythe_silt")).ignoreWater().noStalagmites();
            laytheSilt3 = new OreCave17("hbm:tile.laythe_silt", 3,
                    () -> LaytheTektoBlocks.LAYTHE_SILT.get().defaultBlockState()
                                                       .setValue(BlockLaytheSilt.SHADE, 1), 0.2D, 60, 14, 58)
                    .blockOverride(() -> PopulateWorld.block("ntmspace:laythe_silt")).ignoreWater().noStalagmites();
            var caves = new LinkedHashMap<String, OreCave17>();
            if (acidSulfurCave != null) caves.put("ore_cave_sulfur", acidSulfurCave);
            if (asbestosCave != null) caves.put("ore_cave_asbestos", asbestosCave);
            caves.put("ore_cave_tumor", laytheTumor);
            caves.put("ore_cave_silt_dark", laytheSilt6);
            caves.put("ore_cave_silt", laytheSilt3);
            laytheCaves = Collections.unmodifiableMap(caves);
        }
    }

    // ---------------------------------------------------------------- bedrock ore noise

    private static final Map<Long, Noise17Perlin> BEDROCK_NOISE = new ConcurrentHashMap<>();

    /**
     * {@code ItemBedrockOreBase.getOreLevel} for the type with global index {@code index}.
     */
    public static double oreLevel(long worldSeed, int dimension, int x, int z, int index) {
        long seed = worldSeed + dimension;
        Noise17Perlin level = BEDROCK_NOISE.computeIfAbsent(seed, key -> new Noise17Perlin(new Random(key), 4));
        Noise17Perlin ore = BEDROCK_NOISE.computeIfAbsent(seed - 4096 + index,
                key -> new Noise17Perlin(new Random(key), 4));
        double scale = 0.01D;
        double value = Math.abs(level.value(x * scale, z * scale) * ore.value(x * scale, z * scale)) * 0.05;
        return value < 0 ? 0 : (value > 2 ? 2 : value);
    }

    /**
     * {@code GeneralConfig.coltanRate}.
     */
    static int coltanRate() {
        return GeneralConfig.coltanRate;
    }

    /**
     * {@code EveTerrain17}'s seismic plains at a block, read under the generator's lock.
     */
    static boolean eveSeismic(@Nullable LayeredBodyTerrain17 terrain, Object lock, int x, int z) {
        if (!(terrain instanceof EveTerrain17 eve)) return false;
        synchronized (lock) {
            return eve.biomeAt(x, z) == eve.seismicPlains;
        }
    }

    /**
     * {@code DunaTerrain17.populateOres}, Duna's ore veins.
     */
    static void duna(PopulateWorld world, Random rand, int i, int j) {
        Block stone = Body.DUNA.stoneBlock();
        Block basalt = PopulateWorld.block("hbm:basalt");
        DunaTerrain17.populateOres(rand, i, j, ore -> switch (ore) {
            case "ore_iron", "ore_zinc" -> placer(world, ore(ore, BlockBodyOre.Body.DUNA), stone);
            default -> placer(world, PopulateWorld.state("hbm:" + ore), basalt);
        });
    }
}
