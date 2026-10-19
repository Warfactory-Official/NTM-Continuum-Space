package com.ntmspace.inventory.material;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;

/**
 * The hbm-space {@code Mats} entries its per-body bedrock ore tables name and HBM does not have, registered into HBM's
 * material registry through {@link Mats#register} with the fork's ids, colours and smelting behaviour (1.7
 * {@code makeSmeltable} / {@code makeNonSmeltable}). Saved by name; the 1.7 number is only the runtime id packets carry.
 *
 * <p>Only the materials themselves and their bedrock ore fragments ({@code BedrockOreItems}) are here; their other 1.7
 * shapes (nickel ingots and so on) belong to whoever ports those items, who should reuse these entries
 * ({@link #NICKEL} etc. or {@code Mats.matByName}).
 */
public final class SpaceOreMaterials {

    private static final int EX = 24_000;

    public static NTMMaterial NICKEL;
    public static NTMMaterial ZINC;
    public static NTMMaterial GALLIUM;
    public static NTMMaterial BROMINE;
    public static NTMMaterial IODINE;
    public static NTMMaterial HAFNIUM;
    public static NTMMaterial LAPIS;
    public static NTMMaterial QUARTZ;
    public static NTMMaterial GLOWSTONE;
    public static NTMMaterial WHITE_PHOSPHORUS;
    public static NTMMaterial AUSTRALIUM;
    public static NTMMaterial TASMANITE;
    public static NTMMaterial AYERITE;
    public static NTMMaterial SEMTEX;

    private SpaceOreMaterials() {}

    /** Idempotent; from the registration class initialiser and from {@code declare()}. */
    public static synchronized void register() {
        if (NICKEL != null) return;
        // 1.7 hbm-space Mats, in its order of declaration
        GLOWSTONE = mat(5, "glowstone", false, 0xFFFF00, 0x535300, 0xFFFF00, "Glowstone");
        AUSTRALIUM = mat(13800, "australium", true, 0xFFFF00, 0x935B00, 0xFFFF00, "Australium");
        TASMANITE = mat(13895, "tasmanite", true, 0xFFFF00, 0x935B00, 0xFFFF00, "Tasmanite");
        AYERITE = mat(13851, "ayerite", true, 0xFFFF00, 0x935B00, 0xFFFF00, "Ayerite");
        LAPIS = mat(2701, "lapis", false, 0x567DF9, 0x001793, 0x264DC9, "Lapis Lazuli");
        WHITE_PHOSPHORUS = mat(1501, "white_phosphorus", false, 0xF5F5ED, 0xC4BD9A, 0xC4BD9A, "White Phosphorus");
        QUARTZ = mat(1402, "quartz", false, 0xF7F5F2, 0x6F5D5A, 0xF7F5F2, "Nether Quartz");
        NICKEL = mat(2800, "nickel", true, 0xE8D1C7, 0x87756E, 0xAE9572, "Nickel");
        GALLIUM = mat(3100, "gallium", true, 0x52687F, 0x52687F, 0x52687F, "Gallium");
        ZINC = mat(3000, "zinc", true, 0xD7CBDA, 0x7A7277, 0xA79DA8, "Zinc");
        BROMINE = mat(3500, "bromine", false, 0xFF642B, 0x720000, 0xFF642B, "Bromine");
        IODINE = mat(5300, "iodine", false, 0x7A8796, 0x3F3049, 0x7A8796, "Iodine");
        HAFNIUM = mat(7200, "hafnium", true, 0xFFF8C7, 0x2E1600, 0xFFF8C7, "Hafnium");
        SEMTEX = mat(EX + 3, "semtex", false, 0xEDAA28, 0x825D16, 0xF0B090, "Semtex");
    }

    /** All fourteen, in registration order. */
    public static NTMMaterial[] all() {
        register();
        return new NTMMaterial[] {GLOWSTONE, AUSTRALIUM, TASMANITE, AYERITE, LAPIS, WHITE_PHOSPHORUS, QUARTZ, NICKEL,
                GALLIUM, ZINC, BROMINE, IODINE, HAFNIUM, SEMTEX};
    }

    private static NTMMaterial mat(int id, String name, boolean smeltable, int light, int dark, int molten, String display) {
        NTMMaterial existing = Mats.matByName.get(name);
        if (existing != null) return existing;
        return Mats.register(id, name).colors(light, dark, molten)
                .smelting(smeltable ? SmeltingBehavior.SMELTABLE : SmeltingBehavior.NOT_SMELTABLE)
                .displayName(display);
    }
}
