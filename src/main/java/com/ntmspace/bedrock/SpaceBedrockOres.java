package com.ntmspace.bedrock;

import com.hbm.items.special.BedrockOreSet;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.BedrockOreChainRecipes;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.items.special.ItemBedrockOreBase.OreField;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.populate.Populate17;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's per-body raw bedrock ore, on HBM's {@link ItemBedrockOreBase#SOURCE} hook: what the excavator's raw ore,
 * the ore density scanner and an ore deposit's tier read in a column.
 *
 * <p>1.7 {@code ItemBedrockOreBase.getOreLevel(world, x, z, type)}: the noise is seeded with {@code world seed +
 * dimension id} and {@code seed - 4096 + type.index}, so the composition depends on the world and the body. The body is
 * {@code CelestialBody.getEnum(world)}: the overworld, the Nether, the End, Thatmo and any level that is no body answer
 * Kerbin (only Kerbin's four types, never HBM's six). The bore fluid is the body's {@code getBedrockAcid()} where it
 * names one (Dres nitric acid, Eve permanganate), else HBM's tier rule.
 */
public final class SpaceBedrockOres {

    private SpaceBedrockOres() {}

    private static boolean installed;

    /** At mod construction, after the types are registered. */
    public static synchronized void init() {
        if (installed) return;
        installed = true;
        ItemBedrockOreBase.SOURCE.register(SpaceBedrockOres::field);
        BedrockOreSet.OF.set(level -> CelestialBedrockOre.getSet(bodyOf(level.dimension())));
        BedrockOreSet.SAMPLES.set(() -> CelestialBedrockOre.BODIES.stream()
                .map(CelestialBedrockOre::sample).toList());
        ModTabs.displayStacks(HbmTab.MATERIALS, ModOreBlocks.bedrock_ore_base,
                () -> BedrockOreSet.SAMPLES.get().get());
        // hbm-space's pyrolysis roasting loop (still code in HBM) runs over every celestial type instead of HBM's six
        for (BedrockOreType type : BedrockOreType.VALUES) BedrockOreChainRecipes.removeChainType(type);
        for (BedrockOreType type : CelestialBedrockOre.getAllTypes()) BedrockOreChainRecipes.addChainType(type);
    }

    /** The raw ore field of a column of {@code dimension}; never null (every level answers a body). */
    public static OreField field(ResourceKey<Level> dimension, long seed, int x, int z) {
        String body = bodyOf(dimension);
        List<BedrockOreType> types = CelestialBedrockOre.get(body);
        if (types == null) {
            body = "kerbin";
            types = CelestialBedrockOre.get(body);
        }
        int salt = dimensionId(dimension);
        Map<BedrockOreType, Double> levels = new LinkedHashMap<>();
        for (BedrockOreType type : types) {
            levels.put(type, Populate17.oreLevel(seed, salt, x, z, CelestialBedrockOre.index(type)));
        }
        return new OreField("body." + body, levels, acid(dimension));
    }

    /** 1.7 {@code CelestialBody.getEnum(world).name}: the body a level is the surface of, Kerbin for anything else. */
    public static String bodyOf(ResourceKey<Level> dimension) {
        for (Populate17.Body body : Populate17.Body.values()) {
            if (body == Populate17.Body.THATMO) continue;
            if (levelOf(body).equals(dimension)) return body.name().toLowerCase(java.util.Locale.ROOT);
        }
        return "kerbin";
    }

    /**
     * The 1.7 dimension id that salted the noise ({@code world.provider.dimensionId}): hbm-space's {@code SpaceConfig}
     * defaults for the bodies and Thatmo, 0 for the overworld, -1 / 1 for the Nether / End, 0 for any other level.
     */
    public static int dimensionId(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.NETHER)) return -1;
        if (dimension.equals(Level.END)) return 1;
        for (Populate17.Body body : Populate17.Body.values()) {
            if (levelOf(body).equals(dimension)) return body.dimension;
        }
        return 0;
    }

    /** {@code WorldProviderCelestial.getBedrockAcid()} of the level, or null. */
    public static @Nullable FluidStack acid(ResourceKey<Level> dimension) {
        for (Populate17.Body body : Populate17.Body.values()) {
            if (levelOf(body).equals(dimension)) return body.bedrockAcid();
        }
        return null;
    }

    private static ResourceKey<Level> levelOf(Populate17.Body body) {
        return switch (body) {
            case MUN -> SpaceDimensions.MUN;
            case MINMUS -> SpaceDimensions.MINMUS;
            case DUNA -> SpaceDimensions.DUNA;
            case IKE -> SpaceDimensions.IKE;
            case DRES -> SpaceDimensions.DRES;
            case EVE -> SpaceDimensions.EVE;
            case MOHO -> SpaceDimensions.MOHO;
            case LAYTHE -> SpaceDimensions.LAYTHE;
            case TEKTO -> SpaceDimensions.TEKTO;
            case THATMO -> SpaceDimensions.THATMO;
        };
    }
}
