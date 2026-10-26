package com.ntmspace.inventory.material;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;

/**
 * hbm-space's own foundry materials ({@code Mats} entries the fork added), registered into HBM's material registry
 * through {@link Mats#register}. Saved by name; the 1.7 number is only the runtime id packets carry.
 */
public final class SpaceMaterials {

    /** 1.7 {@code Mats._EX}, the base of the fork's extension ids. */
    private static final int EX = 24_000;

    /**
     * 1.7 {@code MAT_RICH_MAGMA = makeSmeltable(_EX + 02, RICHMAGMA, 0x7F7F7F, 0x353555, 0xFF6212)} ("Mineral-Rich Magma",
     * the magma drill's product). Null until {@link #register()} ran.
     */
    public static NTMMaterial RICH_MAGMA;

    private SpaceMaterials() {}

    /** From {@code declare()}: HBM's materials exist by then. Idempotent. */
    public static synchronized void register() {
        if (RICH_MAGMA != null) return;
        SpaceAlloyMaterials.register();
        NTMMaterial existing = Mats.matByName.get("rich_magma");
        RICH_MAGMA = existing != null ? existing : Mats.register(EX + 2, "rich_magma").namespace("ntmspace")
                .colors(0x7F7F7F, 0x353555, 0xFF6212)
                .smelting(SmeltingBehavior.SMELTABLE).n()
                .displayName("Mineral-Rich Magma");
        // The Space Mats declarations insert these materials beside their 1.7 predecessors.
        SpaceOreMaterials.GLOWSTONE.after(Mats.MAT_OBSIDIAN);
        SpaceAlloyMaterials.CONGLOMERATE.after(Mats.MAT_CRYOLITE);
        SpaceOreMaterials.AUSTRALIUM.after(Mats.MAT_GHIORSIUM);
        SpaceOreMaterials.TASMANITE.after(SpaceOreMaterials.AUSTRALIUM);
        SpaceOreMaterials.AYERITE.after(SpaceOreMaterials.TASMANITE);
        SpaceOreMaterials.LAPIS.after(Mats.MAT_COBALT);
        SpaceOreMaterials.WHITE_PHOSPHORUS.after(Mats.MAT_PHOSPHORUS);
        SpaceOreMaterials.QUARTZ.after(Mats.MAT_ASBESTOS);
        SpaceOreMaterials.NICKEL.after(Mats.MAT_OSMIRIDIUM);
        SpaceOreMaterials.GALLIUM.after(SpaceOreMaterials.NICKEL);
        SpaceOreMaterials.ZINC.after(SpaceOreMaterials.GALLIUM);
        SpaceOreMaterials.BROMINE.after(SpaceOreMaterials.ZINC);
        SpaceOreMaterials.IODINE.after(SpaceOreMaterials.BROMINE);
        SpaceOreMaterials.HAFNIUM.after(SpaceOreMaterials.IODINE);
        SpaceAlloyMaterials.IRIDIUM.after(SpaceOreMaterials.HAFNIUM);
        SpaceAlloyMaterials.GAAS.after(Mats.MAT_SATURNITE);
        SpaceAlloyMaterials.STAINLESS.after(SpaceAlloyMaterials.GAAS);
        RICH_MAGMA.after(SpaceAlloyMaterials.STAINLESS);
        SpaceOreMaterials.SEMTEX.after(RICH_MAGMA);
    }
}
