package com.ntmspace.world.populate;

import static com.ntmspace.world.populate.Populate17.generateOre;
import static com.ntmspace.world.populate.Populate17.ore;

import com.hbm.config.WorldConfig;
import com.ntmspace.block.BlockBodyOre;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;

import java.util.Random;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's per-body {@code IWorldGenerator}s ({@code WorldGeneratorMoon}, {@code -Duna}, {@code -Ike}, {@code -Dres},
 * {@code -Eve}, {@code -Moho}, {@code -Minmus}, {@code -Laythe}; Tekto's is {@code LaytheTektoDecoration.tekto}; Thatmo's
 * was never registered), line for line, with the FML generator {@code Random}. Their ores replace the body's stone
 * ({@code WorldProviderCelestial.getStone}) and carry the body as their metadata (the {@code body} state).
 */
public final class BodyGenerators17 {

    private BodyGenerators17() {
    }

    public static void generate(Populate17.Body body, PopulateWorld world, Random rand, int i, int j,
                                @Nullable LayeredBodyTerrain17 terrain, Object lock) {
        Block stone = body.stoneBlock();
        BlockBodyOre.Body meta = body.ore;
        switch (body) {
            case MUN -> {
                generateOre(world, rand, i, j, WorldConfig.lithiumSpawn, 6, 4, 8, ore("ore_lithium", meta), stone);
                generateOre(world, rand, i, j, WorldConfig.aluminiumSpawn, 6, 5, 40, ore("ore_aluminium", meta), stone);
                generateOre(world, rand, i, j, WorldConfig.fluoriteSpawn, 4, 5, 45, ore("ore_fluorite", meta), stone);
                generateOre(world, rand, i, j, 10, 13, 5, 64, ore("ore_quartz", meta), stone);
                generateOre(world, rand, i, j, 1, 12, 8, 32, ore("ore_shale", meta), stone);
            }
            case DUNA -> {
                Populate17.duna(world, rand, i, j);
                // the martian world type's base (no draws): built once at server start by world/MartianWorld, being wider
                // than one chunk's population may write
                // then a broken behemoth lantern, one chunk in 1234 (draws its loot coin only where it stands)
                if (rand.nextInt(1234) == 0) {
                    int x = i + rand.nextInt(16);
                    int z = j + rand.nextInt(16);
                    world.brokenLantern(x, z, rand);
                }
            }
            case IKE -> {
                generateOre(world, rand, i, j, WorldConfig.asbestosSpawn, 8, 3, 22, ore("ore_asbestos", meta), stone);
                generateOre(world, rand, i, j, WorldConfig.copperSpawn, 9, 4, 27, ore("ore_copper", meta), stone);
                generateOre(world, rand, i, j, Populate17.IRON_SPAWN, 8, 1, 33, ore("ore_iron", meta), stone);
                generateOre(world, rand, i, j, WorldConfig.lithiumSpawn, 6, 4, 8, ore("ore_lithium", meta), stone);
                generateOre(world, rand, i, j, 2, 4, 15, 40, ore("ore_coltan", meta), stone);
                // "the HACKIEST hack i ever hacked": every pedestal in the chunk gets a divine shard (no draws)
                world.restockPedestals(i, j);
                generateOre(world, rand, i, j, Populate17.MINERAL_SPAWN, 10, 12, 32, ore("ore_mineral", meta), stone);
                // the ancient tomb, one chunk in pyramidStructure: built by the structure ntmspace:ancient_tomb_ike, which
                // replays these draws (world/structure/BodyDungeonStructure); the tomb's own draws come last
                if (com.hbm.config.DungeonConfig.pyramidStructure > 0 && rand.nextInt(
                        com.hbm.config.DungeonConfig.pyramidStructure) == 0) {
                    int x = i + rand.nextInt(16);
                    int z = j + rand.nextInt(16);
                    world.dungeonSite("ancient_tomb", x, z, rand);
                }
            }
            case DRES -> {
                generateOre(world, rand, i, j, WorldConfig.cobaltSpawn, 4, 3, 22, ore("ore_cobalt", meta), stone);
                // ported as written: hbm-space gave Dres's iron the copper vein count
                generateOre(world, rand, i, j, WorldConfig.copperSpawn, 9, 4, 27, ore("ore_iron", meta), stone);
                generateOre(world, rand, i, j, 12, 8, 1, 33, ore("ore_niobium", meta), stone);
                generateOre(world, rand, i, j, Populate17.coltanRate(), 4, 15, 40, ore("ore_coltan", meta), stone);
                generateOre(world, rand, i, j, 1, 6, 4, 64, ore("ore_lanthanium", meta), stone);
                generateOre(world, rand, i, j, 1, 12, 8, 32, ore("ore_shale", meta), stone);
            }
            case EVE -> eve(world, rand, i, j, stone, meta, terrain, lock);
            case MOHO -> moho(world, rand, i, j, stone, meta);
            case MINMUS -> {
                generateOre(world, rand, i, j, Populate17.NICKEL_SPAWN, 8, 1, 43, ore("ore_nickel", meta), stone);
                generateOre(world, rand, i, j, WorldConfig.titaniumSpawn, 12, 4, 27, ore("ore_titanium", meta), stone);
                generateOre(world, rand, i, j, 1, 16, 6, 40, PopulateWorld.state("hbm:stone_resource_malachite"),
                        stone);
                generateOre(world, rand, i, j, WorldConfig.copperSpawn * 3, 12, 8, 56, ore("ore_copper", meta), stone);
            }
            case LAYTHE -> {
                generateOre(world, rand, i, j, WorldConfig.asbestosSpawn, 4, 16, 16, ore("ore_asbestos", meta), stone);
                generateOre(world, rand, i, j, WorldConfig.berylliumSpawn, 4, 5, 30, ore("ore_beryllium", meta), stone);
                generateOre(world, rand, i, j, WorldConfig.rareSpawn, 5, 5, 20, ore("ore_rare", meta), stone);
            }
            default -> {
            }
        }
    }

    /**
     * {@code WorldGeneratorEve.generateEve}.
     */
    private static void eve(PopulateWorld world, Random rand, int i, int j, Block stone, BlockBodyOre.Body meta,
                            @Nullable LayeredBodyTerrain17 terrain, Object lock) {
        generateOre(world, rand, i, j, 12, 8, 1, 33, ore("ore_niobium", meta), stone);
        generateOre(world, rand, i, j, 8, 4, 5, 48, ore("ore_iodine", meta), stone);
        generateOre(world, rand, i, j, 1, 4, 1, 16, ore("ore_schrabidium", meta), stone);

        int x = i + rand.nextInt(16) + 8;
        int z = j + rand.nextInt(16) + 8;
        int y = world.heightValue(x, z);

        Block silt = PopulateWorld.block("ntmspace:eve_silt");
        Block rock = PopulateWorld.block("ntmspace:eve_rock");
        if (silt == null || rock == null) return;

        if (Populate17.eveSeismic(terrain, lock, x, z)) {
            EveFeatures17.spike(world, rand, x, y, z, silt, rock);
        }

        if (rand.nextInt(100) == 0) {
            // new WorldGenElectricVolcano(30, 22, eve_silt, eve_rock)
            EveFeatures17.volcano(world, rand, x, y, z, EveFeatures17.VOLCANO_HEIGHT, EveFeatures17.VOLCANO_WIDTH,
                    silt, rock, PopulateWorld.state("hbm:basalt"),
                    PopulateWorld.state("hbm:ore_depth_nether_neodymium"),
                    PopulateWorld.state("ntmspace:geysir_electric"));
        }
    }

    /**
     * {@code WorldGeneratorMoho.generateMoho}.
     */
    private static void moho(PopulateWorld world, Random rand, int i, int j, Block stone, BlockBodyOre.Body meta) {
        generateOre(world, rand, i, j, Populate17.MINERAL_SPAWN, 10, 12, 32, ore("ore_mineral", meta), stone);

        generateOre(world, rand, i, j, 14, 12, 5, 30, ore("ore_glowstone", meta), stone);
        generateOre(world, rand, i, j, WorldConfig.netherPhosphorusSpawn, 6, 8, 64, ore("ore_fire", meta), stone);
        generateOre(world, rand, i, j, 8, 4, 0, 24, ore("ore_australium", meta), stone);

        generateOre(world, rand, i, j, 1, 12, 8, 32, ore("ore_shale", meta), stone);

        BlockState basalt = PopulateWorld.state("hbm:basalt");
        generateOre(world, rand, i, j, 10, 32, 0, 128, basalt, stone);

        // More basalt ores!
        Block basaltBlock = basalt == null ? null : basalt.getBlock();
        generateOre(world, rand, i, j, 16, 6, 16, 64, PopulateWorld.state("hbm:ore_basalt_sulfur"), basaltBlock);
        generateOre(world, rand, i, j, 12, 8, 8, 32, PopulateWorld.state("hbm:ore_basalt_fluorite"), basaltBlock);
        generateOre(world, rand, i, j, 8, 9, 8, 48, PopulateWorld.state("hbm:ore_basalt_asbestos"), basaltBlock);
        generateOre(world, rand, i, j, 2, 4, 0, 24, PopulateWorld.state("hbm:ore_basalt_gem"), basaltBlock);
        generateOre(world, rand, i, j, 8, 10, 16, 64, PopulateWorld.state("hbm:ore_basalt_molysite"), basaltBlock);

        Block mohoStone = PopulateWorld.block("ntmspace:moho_stone");
        Block mohoRegolith = PopulateWorld.block("ntmspace:moho_regolith");
        BlockState geyser = PopulateWorld.state("hbm:geysir_nether");
        BlockState netherrack = Blocks.NETHERRACK.defaultBlockState();
        for (int k = 0; k < 2; k++) {
            int x = i + rand.nextInt(16) + 8;
            int z = j + rand.nextInt(16) + 8;
            int d = 16 + rand.nextInt(96);

            for (int y = d - 5; y <= d; y++) {
                BlockState b = world.get(x, y, z);
                if (world.isAir(x, y + 1, z) && (b.is(mohoStone) || b.is(mohoRegolith))) {
                    world.set(x, y, z, geyser);
                    world.set(x + 1, y, z, netherrack);
                    world.set(x - 1, y, z, netherrack);
                    world.set(x, y, z + 1, netherrack);
                    world.set(x, y, z - 1, netherrack);
                    world.set(x + 1, y - 1, z, netherrack);
                    world.set(x - 1, y - 1, z, netherrack);
                    world.set(x, y - 1, z + 1, netherrack);
                    world.set(x, y - 1, z - 1, netherrack);
                }
            }
        }

        // Kick the volcanoes into action, and fix SOME floating lava: 1.7 put flowing lava (metadata 0, a source that
        // flows) under every lava block hanging over air, and set every volcano core to static-extinguishing
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockState extinguishing = PopulateWorld.state("hbm:volcano_core_static_extinguishing");
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 32; y < 128; y++) {
                    int ox = i + x + 8;
                    int oz = j + z + 8;
                    BlockState b = world.get(ox, y, oz);

                    if (b.is(Blocks.LAVA) && world.isAir(ox, y - 1, oz)) {
                        world.set(ox, y - 1, oz, lava);
                        world.fluidTick(ox, y - 1, oz);
                    } else if (volcanoCore(b) && extinguishing != null) {
                        world.set(ox, y, oz, extinguishing);
                    }
                }
            }
        }
    }

    /**
     * 1.7's {@code volcano_core}, any of its metadata states (separate blocks in HBM's port).
     */
    private static boolean volcanoCore(BlockState state) {
        return state.getBlock() instanceof com.hbm.blocks.bomb.BlockVolcano;
    }
}
