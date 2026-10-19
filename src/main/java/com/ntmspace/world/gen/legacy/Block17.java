package com.ntmspace.world.gen.legacy;

/**
 * A block as 1.7 chunk generation saw it: an identity plus the two properties the generators test
 * ({@code isOpaqueCube()} and {@code getMaterial().isLiquid()}). Air is {@code null}, as in a 1.7 chunk primer.
 *
 * <p>The hbm-space generators run on {@code Block17[]} buffers exactly as they ran on {@code Block[]}; the modern chunk
 * generator maps each one to a block state only when it copies the finished buffer into the chunk
 * ({@code com.ntmspace.world.LegacyBlocks}). The {@link #id} is that block's registry id.
 */
public final class Block17 {

    public final String id;
    private final boolean opaque;
    private final boolean liquid;
    /** The block state each 1.7 metadata becomes, for blocks whose metadata chose a block or a state; else null. */
    private final java.util.function.IntFunction<String> metaIds;

    private Block17(String id, boolean opaque, boolean liquid) {
        this(id, opaque, liquid, null);
    }

    private Block17(String id, boolean opaque, boolean liquid, java.util.function.IntFunction<String> metaIds) {
        this.id = id;
        this.opaque = opaque;
        this.liquid = liquid;
        this.metaIds = metaIds;
    }

    /** A block for a body's own terrain code (its identity is the instance, as a 1.7 {@code Block} was). */
    public static Block17 of(String id, boolean opaque, boolean liquid) {
        return new Block17(id, opaque, liquid);
    }

    /**
     * A block whose 1.7 metadata picks the modern block or state: {@code idForMeta} gives a block id or a state string
     * ({@code "hbm:sellafield_slaked[shade=8]"}) for metadata 0 to 255.
     */
    public static Block17 of(String id, boolean opaque, boolean liquid, java.util.function.IntFunction<String> idForMeta) {
        return new Block17(id, opaque, liquid, idForMeta);
    }

    /** Whether the primer's metadata picks this block's modern state ({@link #id(int)}). */
    public boolean hasMetaIds() {
        return metaIds != null;
    }

    /** The block id or state string a primer entry of this block with metadata {@code meta} (0 to 255) becomes. */
    public String id(int meta) {
        return metaIds == null ? id : metaIds.apply(meta);
    }

    public boolean isOpaqueCube() {
        return opaque;
    }

    public boolean isLiquid() {
        return liquid;
    }

    @Override
    public String toString() {
        return id;
    }

    // Vanilla
    public static final Block17 STONE = new Block17("minecraft:stone", true, false);
    public static final Block17 GRASS = new Block17("minecraft:grass_block", true, false);
    public static final Block17 DIRT = new Block17("minecraft:dirt", true, false);
    public static final Block17 SAND = new Block17("minecraft:sand", true, false);
    public static final Block17 SANDSTONE = new Block17("minecraft:sandstone", true, false);
    public static final Block17 GRAVEL = new Block17("minecraft:gravel", true, false);
    public static final Block17 BEDROCK = new Block17("minecraft:bedrock", true, false);
    public static final Block17 OBSIDIAN = new Block17("minecraft:obsidian", true, false);
    public static final Block17 WATER = new Block17("minecraft:water", false, true);
    public static final Block17 FLOWING_WATER = new Block17("minecraft:water", false, true);
    public static final Block17 LAVA = new Block17("minecraft:lava", false, true);
    public static final Block17 FLOWING_LAVA = new Block17("minecraft:lava", false, true);
    /** 1.7 {@code Blocks.ice}: a {@code BlockBreakable}, not an opaque cube. */
    public static final Block17 ICE = new Block17("minecraft:ice", false, false);
    /** 1.7 {@code Blocks.snow}, the full snow block. */
    public static final Block17 SNOW = new Block17("minecraft:snow_block", true, false);
    public static final Block17 PACKED_ICE = new Block17("minecraft:packed_ice", true, false);
    /** 1.7 {@code Blocks.log} with metadata 0. */
    public static final Block17 OAK_LOG = new Block17("minecraft:oak_log", true, false);

    // HBM
    public static final Block17 BASALT = new Block17("hbm:basalt", true, false);
    public static final Block17 STONE_CRACKED = new Block17("hbm:stone_cracked", true, false);
    public static final Block17 OIL_SPILL = new Block17("hbm:oil_spill", false, false);
    public static final Block17 MOON_TURF = new Block17("hbm:moon_turf", true, false);
    public static final Block17 DIRT_OILY = new Block17("hbm:dirt_oily", true, false);
    public static final Block17 DIRT_DEAD = new Block17("hbm:dirt_dead", true, false);
    public static final Block17 SAND_DIRTY = new Block17("hbm:sand_dirty", true, false);
    public static final Block17 SAND_DIRTY_RED = new Block17("hbm:sand_dirty_red", true, false);
    public static final Block17 ORE_OIL_SAND = new Block17("hbm:ore_oil_sand", true, false);
    /** {@code plant_dead}; its 1.7 metadata picks the type ({@code hbm:plant_dead_<type>} in the port). */
    public static final Block17 PLANT_DEAD = new Block17("hbm:plant_dead", false, false);
    /** {@code plant_flower} with metadata {@code CD0}, the mustard willow ({@code hbm:plant_flower_cd0} in the port). */
    public static final Block17 PLANT_FLOWER_CD0 = new Block17("hbm:plant_flower_cd0", false, false);
    /** {@code ore_oil}; hbm-space kept the celestial body in its metadata (the reservoir's fluid), see the primer metas. */
    public static final Block17 ORE_OIL = new Block17("hbm:ore_oil", true, false, BodyOres17.form("ore_oil"));
    /**
     * {@code ore_bedrock_oil}; the body in its metadata, as {@link #ORE_OIL}: Kerbin's is HBM's block, the other bodies'
     * {@code ntmspace:ore_bedrock_oil[body=...]} (fork content: Tekto's bubble is Tekto crude and hydrogen-rich gas).
     */
    public static final Block17 ORE_BEDROCK_OIL = new Block17("hbm:ore_bedrock_oil", true, false, meta -> {
        String body = BodyOres17.body(meta);
        return body.equals("kerbin") ? "hbm:ore_bedrock_oil" : "ntmspace:ore_bedrock_oil[body=" + body + "]";
    });
    public static final Block17 BASALT_SMOOTH = new Block17("hbm:basalt_smooth", true, false);
    /** {@code sellafield_slaked}; its metadata is the shade (0 to 9 used). */
    public static final Block17 SELLAFIELD_SLAKED = new Block17("hbm:sellafield_slaked", true, false,
            meta -> "hbm:sellafield_slaked[shade=" + Math.min(meta & 15, 9) + "]");

    // NTM: Space
    public static final Block17 MOON_ROCK = new Block17("ntmspace:moon_rock", true, false);
    /** {@code ore_brine}; its 1.7 metadata is the celestial body, kept in the chunk primer's meta buffer. */
    public static final Block17 ORE_BRINE = new Block17("ntmspace:ore_brine", true, false, BodyOres17.form("ore_brine"));

    // NTM: Space, Laythe and Tekto
    public static final Block17 LAYTHE_SILT = new Block17("ntmspace:laythe_silt", true, false);
    /** Tekto's sea: hbm-space's {@code ccl_block}, a {@code GenericFluidBlock} of {@code Material.water}. */
    public static final Block17 CCL_BLOCK = new Block17("ntmspace:ccl_block", false, true);
    public static final Block17 RUBBER_GRASS = new Block17("ntmspace:rubber_grass", true, false);
    public static final Block17 RUBBER_SILT = new Block17("ntmspace:rubber_silt", true, false);
    public static final Block17 VINYL_SAND = new Block17("ntmspace:vinyl_sand", true, false);
    /** {@code ore_tekto}, Tekto's oil ore (the body in its metadata). */
    public static final Block17 ORE_TEKTO = new Block17("ntmspace:ore_tekto", true, false, BodyOres17.form("ore_tekto"));
    /** {@code geysir_chloric}: a block container, not an opaque cube. */
    public static final Block17 GEYSIR_CHLORIC = new Block17("ntmspace:geysir_chloric", false, false);
    public static final Block17 DUNA_ROCK = new Block17("ntmspace:duna_rock", true, false);
    public static final Block17 DUNA_SANDS = new Block17("ntmspace:duna_sands", true, false);

    // NTM: Space, Eve, Moho and Minmus
    public static final Block17 EVE_ROCK = new Block17("ntmspace:eve_rock", true, false);
    /** {@code eve_silt}: a {@code BlockFalling} of {@code Material.sand}, an opaque cube. */
    public static final Block17 EVE_SILT = new Block17("ntmspace:eve_silt", true, false);
    /**
     * Eve's sea: hbm-space's {@code mercury_block}, a {@code GenericFluidBlock} of {@code Material.water}
     * ({@code com.ntmspace.registry.SpaceFluidBlocks#MERCURY_BLOCK}, a static fluid block).
     */
    public static final Block17 MERCURY = new Block17("ntmspace:mercury_block", false, true);
    /** {@code ore_gas}, the natural gas deposit; the body in its metadata, as {@link #ORE_BRINE} (Eve's is generated). */
    public static final Block17 ORE_GAS = new Block17("ntmspace:ore_gas", true, false, BodyOres17.form("ore_gas"));
    public static final Block17 MOHO_STONE = new Block17("ntmspace:moho_stone", true, false);
    public static final Block17 MOHO_REGOLITH = new Block17("ntmspace:moho_regolith", true, false);
    /** {@code volcano_core} with metadata 0 (static, active): HBM's {@code hbm:volcano_core}. */
    public static final Block17 VOLCANO_CORE = new Block17("hbm:volcano_core", true, false);
    public static final Block17 MINMUS_STONE = new Block17("ntmspace:minmus_stone", true, false);
    public static final Block17 MINMUS_REGOLITH = new Block17("ntmspace:minmus_regolith", true, false);
    public static final Block17 MINMUS_SMOOTH = new Block17("ntmspace:minmus_smooth", true, false);
}
