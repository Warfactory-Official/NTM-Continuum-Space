package com.ntmspace.world.gen.legacy;

/**
 * The 1.7 blocks Duna's, Ike's and Dres's terrain code places, besides the shared ones in {@link Block17}
 * ({@code DUNA_ROCK}, {@code DUNA_SANDS}, {@code SELLAFIELD_SLAKED}, {@code ORE_OIL}, {@code ORE_BRINE}, {@code SNOW},
 * {@code BASALT}, ...). Every 1.7 block here is an opaque cube and none is a liquid.
 */
public final class DunaIkeDresBlocks17 {

    private DunaIkeDresBlocks17() {}

    private static final String[] DYES = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] STONE_TYPES = {"sulfur", "asbestos", "hematite", "malachite", "limestone", "bauxite",
            "conglomerate", "calcium"};

    public static final Block17 DUNA_COBBLE = Block17.of("ntmspace:duna_cobble", true, false);
    public static final Block17 FERRIC_CLAY = Block17.of("ntmspace:ferric_clay", true, false);
    /** {@code dry_ice}: {@code BlockGeneric(Material.ice)}, an opaque cube unlike vanilla ice. */
    public static final Block17 DRY_ICE = Block17.of("ntmspace:dry_ice", true, false);
    /** 1.7 {@code Blocks.stained_hardened_clay}: its metadata is the dye colour. */
    public static final Block17 STAINED_HARDENED_CLAY = Block17.of("minecraft:terracotta", true, false,
            meta -> "minecraft:" + DYES[meta & 15] + "_terracotta");
    /**
     * HBM's {@code stone_resource}, its metadata the stone type ({@code EnumStoneType}); hbm-space added
     * {@code CALCIUM} (7), {@code ntmspace:stone_resource_calcium}. Conglomerate (6) is hbm-space's too and is never
     * generated here.
     */
    public static final Block17 STONE_RESOURCE = Block17.of("hbm:stone_resource", true, false, meta -> {
        int type = meta & 15;
        if (type == 7) return "ntmspace:stone_resource_calcium";
        return "hbm:stone_resource_" + STONE_TYPES[type < 6 ? type : 0];
    });

    public static final Block17 IKE_STONE = Block17.of("ntmspace:ike_stone", true, false);
    public static final Block17 IKE_REGOLITH = Block17.of("ntmspace:ike_regolith", true, false);
    /**
     * HBM's {@code sellafield}, its metadata the heat level (0 to 5, {@code hbm:sellafield_<level>} in the port); a
     * metadata above 5 (left in the primer by an ore written there before) reads as level 5.
     */
    public static final Block17 SELLAFIELD = Block17.of("hbm:sellafield", true, false,
            meta -> "hbm:sellafield_" + Math.min(meta & 15, 5));

    public static final Block17 DRES_ROCK = Block17.of("ntmspace:dres_rock", true, false);
}
