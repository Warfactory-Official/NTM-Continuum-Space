package com.ntmspace.inventory.material;

import com.hbm.items.ModAutogenItems;
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
 * {@code plate_welded_iridium}) without adding those recipe-only forms to the material's declared autogen set.
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
        CONGLOMERATE = mat(2993, "conglomerate", SmeltingBehavior.ADDITIVE, 0x797979, 0x797979, 0x797979, "Conglomerate").m();
        IRIDIUM = mat(7700, "iridium", SmeltingBehavior.SMELTABLE, 0xB8D0FF, 0xB8D0FF, 0xB8D0FF, "Iridium").m();
        IRIDIUM.setAutogen(MaterialShapes.INGOT);
        GAAS = mat(EX, "gaas", SmeltingBehavior.SMELTABLE, 0x6F4A57, 0x6F4A57, 0x6F4A57, "Gallium Arsenide").m();
        GAAS.setAutogen(MaterialShapes.NUGGET, MaterialShapes.BILLET);
        STAINLESS = mat(EX + 1, "stainless", SmeltingBehavior.SMELTABLE, 0xD8D8D8, 0x474747, 0x4A4A4A, "Stainless Steel").m();
        STAINLESS.setAutogen(MaterialShapes.PLATE, MaterialShapes.WELDEDPLATE, MaterialShapes.CASTPLATE);
        SpaceOreMaterials.ZINC.setAutogen(MaterialShapes.WIRE);
    }

    /** Space's additional forms, after the base autogen registry has retained its own ids. */
    public static void registerBaseShapes() {
        ModAutogenItems.init();
        for (var material : new NTMMaterial[] {Mats.MAT_U233, Mats.MAT_U235, Mats.MAT_PLUTONIUM, Mats.MAT_PU239,
                Mats.MAT_NEPTUNIUM, Mats.MAT_CO60, Mats.MAT_AU198, Mats.MAT_PB209, Mats.MAT_SCHRABIDIUM,
                Mats.MAT_SOLINIUM, Mats.MAT_GHIORSIUM, Mats.MAT_ARSENIC, Mats.MAT_CADMIUM, Mats.MAT_POLYMER}) {
            material.setAutogen(MaterialShapes.FRAGMENT);
        }
        Mats.MAT_RUBBER.setAutogen(MaterialShapes.FRAGMENT, MaterialShapes.DUST);
        Mats.MAT_PVC.setAutogen(MaterialShapes.FRAGMENT, MaterialShapes.DUST);
    }

    private static NTMMaterial mat(int id, String name, SmeltingBehavior smelting, int light, int dark, int molten,
                                   String display) {
        NTMMaterial existing = Mats.matByName.get(name);
        if (existing != null) return existing;
        return Mats.register(id, name).namespace("ntmspace").colors(light, dark, molten).smelting(smelting).displayName(display);
    }
}
