package com.ntmspace.bedrock;

import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.special.BedrockOreType;
import com.ntmspace.bedrock.CelestialBedrockOre.Spec;
import com.ntmspace.registry.BedrockOreItems;
import dev.golem.core.render.Sprites;

/**
 * The computed bedrock ore sprites (client only, before the first resource load). 1.7 hbm-space registered one mutable
 * atlas icon per grade prefix and type, recolouring the greyscale {@code bedrock_ore_<prefix>_<suffix>} through
 * {@code RGBMutatorInterpolatedComponentRemap(0xFFFFFF, 0x505050, type.light, type.dark)}: here one remap per prefix and
 * suffix with one permutation per body. Also the fragments of the materials HBM gives none, recoloured from HBM's
 * fragment plate the way HBM's {@code AutogenSprites} does its own. A material whose light and dark colours are equal
 * (gallium) gets no remap in 1.7 ({@code ItemAutogen.registerIcons}): the plain greyscale icon is drawn tinted with the
 * material's molten colour ({@code ItemAutogen.getColorFromItemStack}), so it is tinted here, not flattened.
 */
public final class BedrockOreClient {

    private BedrockOreClient() {}

    public static void install() {
        for (String prefix : BedrockOreItems.PREFIXES) {
            for (String suffix : BedrockOreItems.SUFFIXES) {
                Sprites.Remap remap = Sprites.remap(Sprites.Atlas.ITEMS).texture(BedrockOreItems.sprite(prefix, suffix));
                for (Spec spec : CelestialBedrockOre.SPECS) {
                    if (!spec.suffix().equals(suffix)) continue;
                    BedrockOreType type = CelestialBedrockOre.typeOf(spec);
                    remap.permutation(spec.body(), CelestialBedrockOre.averageColor(type, false),
                            CelestialBedrockOre.averageColor(type, true));
                }
                if (!remap.isEmpty()) remap.declare();
            }
        }

        Sprites.Remap fragments = Sprites.remap(Sprites.Atlas.ITEMS).texture("hbm:items/bedrock_ore_fragment");
        for (NTMMaterial material : BedrockOreItems.fragmentMaterials()) {
            if (material.solidColorLight == material.solidColorDark) fragments.tint(material.name, material.moltenColor);
            else fragments.permutation(material.name, material.solidColorLight, material.solidColorDark);
        }
        if (!fragments.isEmpty()) fragments.declare();
    }
}
