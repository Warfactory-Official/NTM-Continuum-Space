package com.ntmspace.inventory.material;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;

/**
 * The hbm-space {@code Mats} entries its recipes need beyond {@link SpaceOreMaterials} and {@link SpaceMaterials}: the
 * fork's alloys gallium arsenide and stainless steel, iridium and the conglomerate additive. Ids, colours, smelting
 * behaviour and autogen shapes are hbm-space's ({@code Mats}: {@code MAT_GAAS = makeSmeltable(_EX + 00, ...)},
 * {@code MAT_STAINLESS = makeSmeltable(_EX + 01, ...).setAutogen(PLATE, WELDEDPLATE, CASTPLATE)},
 * {@code MAT_IRIDIUM = makeSmeltable(7700, ...).setAutogen(INGOT)}, {@code MAT_CONGLOMERATE = makeAdditive(2993, ...)}).
 * The material name is the {@code c:} tag name ({@code c:ingots/stainless}), as for HBM's own materials.
 *
 * <p>Odd thing kept: hbm-space's recipes ask for {@code IRIDIUM.plateCast()} / {@code plateWelded()} although the
 * material only autogenerates {@code INGOT}; 1.7's {@code ItemAutogen} made those stacks anyway (the meta exists for
 * every material). Here the two shapes are registered as items ({@code ntmspace:plate_cast_iridium},
 * {@code plate_welded_iridium}) and so also listed in {@code setAutogen}, which only adds them to the creative tab.
 * Zinc likewise gains its 1.7 {@code WIRE} shape ({@code MAT_ZINC.setAutogen(FRAGMENT, NUGGET, DUST, WIRE)}).
 */
public final class SpaceAlloyMaterials {

    private static final int EX = 24_000;

    public static NTMMaterial GAAS;
    public static NTMMaterial STAINLESS;
    public static NTMMaterial IRIDIUM;
    public static NTMMaterial CONGLOMERATE;

    private SpaceAlloyMaterials() {}

    /** Idempotent; from the registration class initialiser and from {@code declare()}. */
    public static synchronized void register() {
        if (GAAS != null) return;
        SpaceOreMaterials.register();
        CONGLOMERATE = mat(2993, "conglomerate", SmeltingBehavior.ADDITIVE, 0x797979, 0x797979, 0x797979, "Conglomerate");
        IRIDIUM = mat(7700, "iridium", SmeltingBehavior.SMELTABLE, 0xB8D0FF, 0xB8D0FF, 0xB8D0FF, "Iridium");
        IRIDIUM.setAutogen(MaterialShapes.INGOT, MaterialShapes.CASTPLATE, MaterialShapes.WELDEDPLATE);
        GAAS = mat(EX, "gaas", SmeltingBehavior.SMELTABLE, 0x6F4A57, 0x6F4A57, 0x6F4A57, "Gallium Arsenide");
        GAAS.setAutogen(MaterialShapes.NUGGET, MaterialShapes.BILLET);
        STAINLESS = mat(EX + 1, "stainless", SmeltingBehavior.SMELTABLE, 0xD8D8D8, 0x474747, 0x4A4A4A, "Stainless Steel");
        STAINLESS.setAutogen(MaterialShapes.PLATE, MaterialShapes.WELDEDPLATE, MaterialShapes.CASTPLATE);
        SpaceOreMaterials.ZINC.setAutogen(MaterialShapes.WIRE);
    }

    private static NTMMaterial mat(int id, String name, SmeltingBehavior smelting, int light, int dark, int molten,
                                   String display) {
        NTMMaterial existing = Mats.matByName.get(name);
        if (existing != null) return existing;
        return Mats.register(id, name).colors(light, dark, molten).smelting(smelting).displayName(display);
    }
}
