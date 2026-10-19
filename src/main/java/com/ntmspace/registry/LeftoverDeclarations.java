package com.ntmspace.registry;

import com.google.gson.JsonObject;
import com.hbm.items.MachineObjModel;
import com.ntmspace.block.generic.BlockSpacePlushie;
import dev.golem.core.dejson.BiomeModifiers;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Features;

/**
 * Build-time declarations of {@link LeftoverContent} (Dejson runs {@link #declare()}): the item models of its
 * renderer-drawn blocks, from hbm-space's item renderers, and the wild flowers' Earth generation.
 *
 * <p>Wild flowers: hbm-space's {@code HbmWorldGen.generateSurface} added, before Tom's impact and for
 * {@code biome instanceof BiomeGenPlains} (plains and its sunflower mutation), {@code rand.nextInt(32) == 0}
 * {@code DungeonToolbox.generateFlowers(... STRAWBERRY)} and, separately, the same for {@code MINT} - the same rule as
 * HBM's own foxglove, so the same shape as HBM's {@code ModBiomeModifiers.flowers}: HBM's {@code hbm:legacy_flowers}
 * feature (1.7 {@code WorldGenFlowers}; it does nothing after Tom's impact), {@code rarity_filter} 32,
 * {@code in_square}, {@code heightmap MOTION_BLOCKING}, {@code biome}, vegetal decoration. The configured and placed
 * features are authored files ({@code data/ntmspace/worldgen/configured_feature|placed_feature/wild_strawberry|
 * wild_mint.json}); declared here are the biome tag and the biome modifiers (NeoForge/Forge files; Fabric gets them
 * from Golem's {@code BiomeFeatures.install()}). They exist only with NTM: Space installed.
 */
public final class LeftoverDeclarations {

    private static final String NS = "ntmspace";
    private static boolean declared;

    private LeftoverDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // RenderOrrery item: inventory translate(0, 3, 0) scale(6); common: the star's unit cube, UV 0.375..0.625
        Dejson.item(NS, "orrery")
                .definition(MachineObjModel.of("ntmspace:models/orrery_cube.obj", "ntmspace:textures/misc/space/kerbol.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(6.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 3.0, 0.0))
                .oversizedInGui().register();
        // RenderAlgaeFilm.renderInventoryBlock: translate(0, -0.5, 0), the OBJ with the block icon
        Dejson.item(NS, "algae_film")
                .definition(MachineObjModel.of("ntmspace:models/blocks/algae_film.obj", "ntmspace:textures/blocks/algae_film.png")
                        .scale(1.0).ty(0.0).yaw(0.0).frame("block").rawLegacyTransform()
                        .itemTranslate(0.0, -0.5, 0.0).itemPreTranslate(0.0, 0.0, 0.0))
                .oversizedInGui().register();
        // RenderBarrel.renderInventoryBlock (BrownBarrel's render type): translate(0, -0.5, 0), the "Barrel" part
        Dejson.item(NS, "oil_barrel")
                .definition(MachineObjModel.of("hbm:models/blocks/barrel.obj", "ntmspace:textures/blocks/barrel_oily.png")
                        .scale(1.0).ty(0.0).yaw(0.0).frame("block").rawLegacyTransform().exclude("Connector")
                        .itemTranslate(0.0, -0.5, 0.0).itemPreTranslate(0.0, 0.0, 0.0))
                .oversizedInGui().register();
        // RenderPlushie's item renderer, per type (client SpacePlushieItemRenderer, "ntmspace:plushie")
        for (BlockSpacePlushie.Type type : BlockSpacePlushie.Type.VALUES) {
            Dejson.item(NS, type.id()).shares("hbm:item/machine_obj_context_base").register();
            JsonObject model = new JsonObject();
            model.addProperty("type", "hbm:custom_item");
            model.addProperty("renderer", NS + ":plushie");
            model.addProperty("frame", "machine");
            Dejson.itemDefinition(NS + ":item/" + type.id()).add("model", model);
            Dejson.itemDefinition(NS + ":item/" + type.id()).addProperty("oversized_in_gui", true);
        }

        declareWorldgen();
    }

    private static boolean worldgenDeclared;

    /**
     * The wild flowers' biome tag and biome modifier: at build time (Dejson) and, for Fabric's
     * {@code BiomeFeatures.install()}, at start-up ({@link LeftoverContent#init}). One modifier with strawberries first,
     * as 1.7 ran them.
     */
    public static synchronized void declareWorldgen() {
        if (worldgenDeclared) return;
        worldgenDeclared = true;
        Features.biomeTag(NS, "has_feature/wild_flowers", new String[] {"minecraft:plains", "minecraft:sunflower_plains"});
        BiomeModifiers.addFeatures(NS, "wild_flowers", "#" + NS + ":has_feature/wild_flowers",
                java.util.List.of(NS + ":wild_strawberry", NS + ":wild_mint"), BiomeModifiers.VEGETAL_DECORATION);
    }
}
