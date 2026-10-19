package com.ntmspace.world.gen.legacy;

/**
 * hbm-space's per-body ores ({@code BlockOre} and its fluid ores): one 1.7 block whose metadata is the celestial body
 * ({@code SolarSystem.Body.ordinal()}: 0 orbit, 1 Kerbin, 2 Mun, 3 Minmus, 4 Duna, 5 Moho, 6 Dres, 7 Eve, 8 Ike,
 * 9 Laythe, 10 Tekto), drawn as that body's stone with the ore on top. In the port each is one block with a {@code body}
 * state ({@code com.ntmspace.block.BlockBodyOre}), so a primer entry maps to {@code ntmspace:<ore>[body=<name>]}.
 */
public final class BodyOres17 {

    /** The body names by 1.7 metadata; orbit (0), which has no stone, draws as Kerbin. */
    public static final String[] BODIES = {"kerbin", "kerbin", "mun", "minmus", "duna", "moho", "dres", "eve", "ike", "laythe", "tekto"};

    /** The 1.7 metadata of the bodies with ported terrain. */
    public static final byte MUN = 2;
    public static final byte DUNA = 4;
    public static final byte DRES = 6;
    public static final byte IKE = 8;

    private BodyOres17() {}

    /** The body name of 1.7 ore metadata {@code meta}. */
    public static String body(int meta) {
        return meta >= 0 && meta < BODIES.length ? BODIES[meta] : "kerbin";
    }

    /**
     * The state of {@code ntmspace:<path>} for each 1.7 metadata; hbm-space's {@code ore_oil} on Kerbin is HBM's own
     * {@code hbm:ore_oil}.
     */
    public static java.util.function.IntFunction<String> form(String path) {
        return meta -> {
            String body = body(meta);
            if (path.equals("ore_oil") && body.equals("kerbin")) return "hbm:ore_oil";
            return "ntmspace:" + path + "[body=" + body + "]";
        };
    }
}
