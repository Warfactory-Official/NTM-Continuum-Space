package com.ntmspace.world.structure;

import com.hbm.main.StructureManager;
import com.hbm.world.gen.component.Component.BlockSelector;
import com.hbm.world.gen.component.LegacyBlock;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.JigsawPool;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import com.ntmspace.NtmSpace;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;

/**
 * The NBT structures hbm-space's planet generators registered in their constructors ({@code WorldGeneratorMoon},
 * {@code -Minmus}, {@code -Moho}, {@code -Dres}, {@code -Duna}, {@code -Ike}, {@code -Laythe}), as HBM
 * {@link SpawnCondition}s under their 1.7 names: piece, pools, height offsets, terrain alignment, size and range limits,
 * height clamps and block tables, verbatim. Where and how often they generate (the per-dimension weights, biome rules
 * and null weights) is {@link BodyStructures}; the meteor dungeon every body has is HBM's own {@code meteor_dungeon}.
 *
 * <p>The files are hbm-space's ({@code StructureManager}'s planet bases), shipped as {@code assets/ntmspace/structures}
 * with the blocks only hbm-space has named {@code ntmspace:<name>} in their palettes (HBM's palette reads any namespace);
 * everything else is the 1.7 file as written. The vertibirds on Laythe are HBM's own files.
 *
 * <p>Not here: Thatmo's {@code thatmotest}, {@code thatmo2}, {@code trenches} and {@code city}. hbm-space never
 * registered {@code WorldGeneratorThatmo} ({@code PlanetGen} lists every other body's generator), so its constructor,
 * the only place those spawns were registered, never ran and they never generated; Thatmo has the meteor dungeon only.
 * Duna's {@code martian} base belongs to the world type "The Martian" and is built by {@code world/MartianWorld}.
 */
public final class SpaceStructures {

    private SpaceStructures() {}

    /** 1.7 {@code StructureManager}'s planet files, loaded on first use. */
    public static final class Files {
        public static final NBTStructure MUN_BASE = file("munbase/base");
        public static final NBTStructure MUN_FLAG = file("munbase/flag");
        public static final NBTStructure MUN_FLAG_UK = file("munbase/flag-uk");
        public static final NBTStructure MUN_PANELS = file("munbase/panels");
        public static final NBTStructure MUN_STARDAR = file("munbase/stardar");
        public static final NBTStructure MUN_TANKS = file("munbase/tanks");
        public static final NBTStructure MUN_CONNECTOR_1 = file("munbase/connector-1");
        public static final NBTStructure MUN_CONNECTOR_2 = file("munbase/connector-2");
        public static final NBTStructure MUN_CONNECTOR_3 = file("munbase/connector-3");
        public static final NBTStructure MUN_CONNECTOR_S = file("munbase/connector-s");
        public static final NBTStructure MUN_CONNECTOR_L = file("munbase/connector-l");
        public static final NBTStructure MUN_CONNECTOR_T = file("munbase/connector-t");

        public static final NBTStructure MOHO_CORE = file("mohobase/moho-core");
        public static final NBTStructure MOHO_CORNER_EMPTY = file("mohobase/moho-corner-empty");
        public static final NBTStructure MOHO_CORNER_LAB = file("mohobase/moho-corner-lab");
        public static final NBTStructure MOHO_CORNER_CAP = file("mohobase/moho-corner-cap");
        public static final NBTStructure MOHO_CORNER_EXTENSION = file("mohobase/moho-corner-extension");
        public static final NBTStructure MOHO_CORNER_HEFFER = file("mohobase/moho-corner-heffer");
        public static final NBTStructure MOHO_ROOM_TAPE = file("mohobase/moho-room-tape");
        public static final NBTStructure MOHO_ROOM_RECEPTION = file("mohobase/moho-room-reception");
        public static final NBTStructure MOHO_ROOM_KITCHEN = file("mohobase/moho-room-kitchen");
        public static final NBTStructure MOHO_SNORKEL = file("mohobase/moho-snorkel");

        public static final NBTStructure DRES_CORE = file("dresbmk/core");
        public static final NBTStructure DRES_T = file("dresbmk/t");
        public static final NBTStructure DRES_DOME = file("dresbmk/dome");
        public static final NBTStructure DRES_POOL = file("dresbmk/pool");
        public static final NBTStructure DRES_AIRLOCK = file("dresbmk/airlock");
        public static final NBTStructure DRES_BALCONY = file("dresbmk/balcony");
        public static final NBTStructure DRES_PAD = file("dresbmk/pad");
        public static final NBTStructure DRES_INCAP = file("dresbmk/incap");
        public static final NBTStructure DRES_OUTCAP = file("dresbmk/outcap");
        public static final NBTStructure DRES_HALL_STARBMK = file("dresbmk/hall/starbmk");
        public static final NBTStructure DRES_HALL_BREEDER = file("dresbmk/hall/breeder");

        public static final NBTStructure DUNA_COMMS = file("duna-comms");
        public static final NBTStructure NUKE_SUB = file("nuke-sub");
        public static final NBTStructure IKE_ARTIFACT = file("ike_artifact");
        /** hbm-space's {@code StructureManager.martian}: Duna's base in a "The Martian" world ({@code world/MartianWorld}). */
        public static final NBTStructure MARTIAN_BASE = file("martian-base");

        /** Every file, for the palette GameTest. */
        public static final NBTStructure[] ALL = {MUN_BASE, MUN_FLAG, MUN_FLAG_UK, MUN_PANELS, MUN_STARDAR, MUN_TANKS,
                MUN_CONNECTOR_1, MUN_CONNECTOR_2, MUN_CONNECTOR_3, MUN_CONNECTOR_S, MUN_CONNECTOR_L, MUN_CONNECTOR_T,
                MOHO_CORE, MOHO_CORNER_EMPTY, MOHO_CORNER_LAB, MOHO_CORNER_CAP, MOHO_CORNER_EXTENSION, MOHO_CORNER_HEFFER,
                MOHO_ROOM_TAPE, MOHO_ROOM_RECEPTION, MOHO_ROOM_KITCHEN, MOHO_SNORKEL,
                DRES_CORE, DRES_T, DRES_DOME, DRES_POOL, DRES_AIRLOCK, DRES_BALCONY, DRES_PAD, DRES_INCAP, DRES_OUTCAP,
                DRES_HALL_STARBMK, DRES_HALL_BREEDER, DUNA_COMMS, NUKE_SUB, IKE_ARTIFACT, MARTIAN_BASE};

        /**
         * Read through this class: HBM's {@code NBTStructure(namespace, path)} opens the file through HBM's own class,
         * which on a module-layered loader (NeoForge) sees only HBM's jar, so an addon hands the stream in.
         */
        private static NBTStructure file(String path) {
            String name = "structures/" + path + ".nbt";
            java.io.InputStream stream = SpaceStructures.class.getResourceAsStream("/assets/" + NtmSpace.MOD_ID + "/" + name);
            if (stream == null) {
                NtmSpace.LOG.error("[structures] missing {}", name);
                stream = new java.io.ByteArrayInputStream(new byte[0]);
            }
            try (java.io.InputStream in = stream) {
                return new NBTStructure(name, in);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
    }

    public static final String MUN_BASE = "mun_base";
    public static final String MINMUS_BASE = "minmus_base";
    public static final String MOHO_BASE = "moho_base";
    public static final String DRES_RBMK = "dres_rbmk";
    public static final String DUNA_COMMS = "duna_comms";
    public static final String IKE_ARTIFACT = "ike_artifact";
    public static final String LAYTHE_NUKE_SUB = "laythe_nuke_sub";
    public static final String LAYTHE_VERTIBIRD = "laythe_vertibird";
    public static final String LAYTHE_CRASHED_VERTIBIRD = "laythe_crashed_vertibird";
    /** HBM's {@code meteor_dungeon} ({@code NTMComponentStructures}), which hbm-space registered on every body too. */
    public static final String METEOR_DUNGEON = "meteor_dungeon";

    private static boolean registered;

    /** The 1.7 constructors' registrations (weights and biome rules: {@link BodyStructures}). */
    public static synchronized void registerSpawns() {
        if (registered) return;
        registered = true;
        StructureTileData.register();
        moon();
        minmus();
        moho();
        dres();
        duna();
        ike();
        laythe();
    }

    /** {@code WorldGeneratorMoon}. */
    private static void moon() {
        JigsawPiece munBase = aligned("mun_base", Files.MUN_BASE);

        SpawnCondition spawn = new SpawnCondition(MUN_BASE);
        spawn.sizeLimit = 32;
        spawn.startPool = "start";
        spawn.pools = new HashMap<>();
        spawn.pools.put("start", pool(munBase, 1));
        JigsawPool def = new JigsawPool();
        def.add(munBase, 1);
        def.add(aligned("mun_flag", Files.MUN_FLAG), 2);
        def.add(aligned("mun_panels", Files.MUN_PANELS), 6);
        def.add(aligned("mun_stardar", Files.MUN_STARDAR), 1);
        def.add(aligned("mun_tanks", Files.MUN_TANKS), 6);
        spawn.pools.put("default", def);
        spawn.pools.put("connect", connectors("mun"));
        NBTStructure.registerStructure(spawn);
    }

    /** {@code WorldGeneratorMinmus}: the Mun base again, its coloured concrete swapped for sand-coloured concrete. */
    private static void minmus() {
        Map<String, BlockSelector> concrete = new HashMap<>();
        concrete.put("concrete_colored", new DesertConcrete());

        JigsawPiece minmusBase = aligned("minmus_base", Files.MUN_BASE);
        minmusBase.blockTable = concrete;

        SpawnCondition spawn = new SpawnCondition(MINMUS_BASE);
        spawn.sizeLimit = 32;
        spawn.startPool = "start";
        spawn.pools = new HashMap<>();
        spawn.pools.put("start", pool(minmusBase, 1));
        JigsawPool def = new JigsawPool();
        def.add(minmusBase, 1);
        def.add(aligned("minmus_flag", Files.MUN_FLAG_UK), 2);
        def.add(aligned("minmus_panels", Files.MUN_PANELS), 6);
        def.add(aligned("minmus_stardar", Files.MUN_STARDAR), 1);
        def.add(aligned("minmus_tanks", Files.MUN_TANKS), 6);
        spawn.pools.put("default", def);
        spawn.pools.put("connect", connectors("minmus"));
        NBTStructure.registerStructure(spawn);
    }

    /** {@code WorldGeneratorMoho}: a base dug into the crust, below sea level. */
    private static void moho() {
        SpawnCondition spawn = new SpawnCondition(MOHO_BASE);
        spawn.minHeight = 63 - 11;
        spawn.maxHeight = 63;
        spawn.sizeLimit = 64;
        spawn.rangeLimit = 64;
        spawn.startPool = "start";
        spawn.pools = new HashMap<>();
        spawn.pools.put("start", pool(new JigsawPiece("moho_core", Files.MOHO_CORE, -11), 1));
        JigsawPool def = new JigsawPool();
        def.add(new JigsawPiece("moho_corner_lab", Files.MOHO_CORNER_LAB, -14), 2);
        def.add(new JigsawPiece("moho_corner_heffer", Files.MOHO_CORNER_HEFFER, -17), 2);
        def.add(new JigsawPiece("moho_corner_extension", Files.MOHO_CORNER_EXTENSION, -11), 1);
        def.add(new JigsawPiece("moho_corner_empty", Files.MOHO_CORNER_EMPTY, -11), 1);
        def.fallback = "fallback";
        spawn.pools.put("default", def);
        JigsawPool room = new JigsawPool();
        room.add(new JigsawPiece("moho_room_tape", Files.MOHO_ROOM_TAPE, -11), 1);
        room.add(new JigsawPiece("moho_room_reception", Files.MOHO_ROOM_RECEPTION, -11), 1);
        room.add(new JigsawPiece("moho_room_kitchen", Files.MOHO_ROOM_KITCHEN, -11), 1);
        room.fallback = "room";
        spawn.pools.put("room", room);
        spawn.pools.put("fallback", pool(new JigsawPiece("moho_fall", Files.MOHO_CORNER_CAP, -11), 1));
        spawn.pools.put("snorkel", pool(new JigsawPiece("moho_snorkel", Files.MOHO_SNORKEL, -11), 1));
        NBTStructure.registerStructure(spawn);
    }

    /** {@code WorldGeneratorDres}: the abandoned RBMK station, its lab tiles four in five whole. */
    private static void dres() {
        Map<String, BlockSelector> tiles = new HashMap<>();
        tiles.put("tile_lab", new LabTiles(0.2F));

        SpawnCondition spawn = new SpawnCondition(DRES_RBMK);
        spawn.minHeight = 40;
        spawn.maxHeight = 40;
        spawn.sizeLimit = 128;
        spawn.rangeLimit = 64;
        spawn.startPool = "start";
        spawn.pools = new HashMap<>();
        spawn.pools.put("start", pool(tiled("dres_core", Files.DRES_CORE, tiles), 1));
        JigsawPool def = new JigsawPool();
        def.add(tiled("dres_t", Files.DRES_T, tiles), 1);
        def.add(tiled("dres_airlock", Files.DRES_AIRLOCK, tiles), 1);
        def.add(tiled("dres_dome", Files.DRES_DOME, tiles), 1);
        def.add(tiled("dres_pool", Files.DRES_POOL, tiles), 1);
        def.fallback = "inback";
        spawn.pools.put("default", def);
        JigsawPool outside = new JigsawPool();
        outside.add(tiled("dres_balcony", Files.DRES_BALCONY, tiles), 1);
        outside.add(tiled("dres_pad", Files.DRES_PAD, tiles), 1);
        outside.fallback = "outback";
        spawn.pools.put("outside", outside);
        JigsawPool reactor = new JigsawPool();
        reactor.add(tiled("dres_hall_starbmk", Files.DRES_HALL_STARBMK, tiles), 5);
        reactor.add(tiled("dres_hall_breeder", Files.DRES_HALL_BREEDER, tiles), 1);
        spawn.pools.put("reactor", reactor);
        spawn.pools.put("inback", pool(tiled("dres_incap", Files.DRES_INCAP, tiles), 1));
        spawn.pools.put("outback", pool(tiled("dres_outcap", Files.DRES_OUTCAP, tiles), 1));
        NBTStructure.registerStructure(spawn);
    }

    /** {@code WorldGeneratorDuna}. */
    private static void duna() {
        single(DUNA_COMMS, new JigsawPiece("duna_comms", Files.DUNA_COMMS, -1));
    }

    /** {@code WorldGeneratorIke}: the artifact, its pedestal re-stocked by Ike's populate ({@code PlanetFeatures17}). */
    private static void ike() {
        single(IKE_ARTIFACT, new JigsawPiece("ike_artifact", Files.IKE_ARTIFACT, -5));
    }

    /** {@code WorldGeneratorLaythe}: the sunk submarine (kept under y 54) and HBM's two vertibirds. */
    private static void laythe() {
        SpawnCondition sub = single(LAYTHE_NUKE_SUB, new JigsawPiece("laythe_nuke_sub", Files.NUKE_SUB));
        sub.maxHeight = 54;
        single(LAYTHE_VERTIBIRD, new JigsawPiece("laythe_vertibird", StructureManager.vertibird, -3));
        single(LAYTHE_CRASHED_VERTIBIRD, new JigsawPiece("laythe_crashed_vertibird", StructureManager.crashed_vertibird, -10));
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static SpawnCondition single(String name, JigsawPiece piece) {
        SpawnCondition spawn = new SpawnCondition(name);
        spawn.structure = piece;
        NBTStructure.registerStructure(spawn);
        return spawn;
    }

    /** {@code alignToTerrain = true; heightOffset = -1}. */
    private static JigsawPiece aligned(String name, NBTStructure file) {
        JigsawPiece piece = new JigsawPiece(name, file, -1);
        piece.alignToTerrain = true;
        return piece;
    }

    private static JigsawPiece tiled(String name, NBTStructure file, Map<String, BlockSelector> tiles) {
        JigsawPiece piece = new JigsawPiece(name, file);
        piece.blockTable = tiles;
        return piece;
    }

    private static JigsawPool pool(JigsawPiece piece, int weight) {
        JigsawPool pool = new JigsawPool();
        pool.add(piece, weight);
        return pool;
    }

    /** The base's six corridor connectors, weight 1 each, named {@code <prefix>_connector_<n>}. */
    private static JigsawPool connectors(String prefix) {
        JigsawPool pool = new JigsawPool();
        pool.add(new JigsawPiece(prefix + "_connector_1", Files.MUN_CONNECTOR_1), 1);
        pool.add(new JigsawPiece(prefix + "_connector_2", Files.MUN_CONNECTOR_2), 1);
        pool.add(new JigsawPiece(prefix + "_connector_3", Files.MUN_CONNECTOR_3), 1);
        pool.add(new JigsawPiece(prefix + "_connector_s", Files.MUN_CONNECTOR_S), 1);
        pool.add(new JigsawPiece(prefix + "_connector_l", Files.MUN_CONNECTOR_L), 1);
        pool.add(new JigsawPiece(prefix + "_connector_t", Files.MUN_CONNECTOR_T), 1);
        return pool;
    }

    /** hbm-space {@code WorldGeneratorMinmus.DesertConcrete}: always {@code concrete_colored_ext} metadata 6. */
    static final class DesertConcrete extends BlockSelector {

        private static final LegacyBlock CONCRETE_COLORED_EXT = LegacyBlock.hbm("concrete_colored_ext");

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            this.block = CONCRETE_COLORED_EXT;
            this.meta = 6;
        }
    }

    /**
     * hbm-space's {@code Component.LabTiles(crackFrequency)}: whole below {@code 1 - crackFrequency}, cracked below 0.9,
     * else broken (HBM's {@code LabTiles} is the 0.5 default; Dres asks 0.2, so four tiles in five are whole).
     */
    static final class LabTiles extends BlockSelector {

        private static final LegacyBlock TILE_LAB = LegacyBlock.hbm("tile_lab");
        private static final LegacyBlock TILE_LAB_CRACKED = LegacyBlock.hbm("tile_lab_cracked");
        private static final LegacyBlock TILE_LAB_BROKEN = LegacyBlock.hbm("tile_lab_broken");

        private final float crackFrequency;

        LabTiles(float crackFrequency) {
            this.crackFrequency = 1 - crackFrequency;
        }

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < crackFrequency) {
                this.block = TILE_LAB;
            } else if (chance < 0.9F) {
                this.block = TILE_LAB_CRACKED;
            } else {
                this.block = TILE_LAB_BROKEN;
            }
        }
    }
}
