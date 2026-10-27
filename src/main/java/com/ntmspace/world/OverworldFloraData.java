package com.ntmspace.world;

import com.google.gson.JsonObject;
import dev.golem.core.dejson.BiomeModifiers;
import dev.golem.core.dejson.Features;
import java.util.List;

/** Space HbmWorldGen: plains-only wild fruit, and eight six-block zinc veins at y 5..36 in Kerbin stone. */
public final class OverworldFloraData {
    static {
        var namespace = "ntmspace";
        Features.biomeTag(namespace, "has_feature/wild_fruit", new String[] {"minecraft:plains", "minecraft:sunflower_plains"});
        for (var flower : List.of("strawberry", "mint")) {
            var id = namespace + ":" + flower;
            Features.configured(namespace, flower, id, null);
            Features.placed(namespace, flower, id, Features.biome());
        }
        BiomeModifiers.addFeatures(namespace, "wild_fruit", "#ntmspace:has_feature/wild_fruit",
                List.of("ntmspace:strawberry", "ntmspace:mint"), BiomeModifiers.VEGETAL_DECORATION);

        Features.configured(namespace, "ore_zinc", "hbm:legacy_ore",
                Features.ore(Features.blockMatch("minecraft:stone"), Features.state("ntmspace:ore_zinc", "body", "kerbin"), 6));
        var gate = new JsonObject();
        gate.addProperty("type", "hbm:config");
        gate.addProperty("key", "2.D00_overworldOres");
        gate.addProperty("mode", "switch");
        Features.placed(namespace, "ore_zinc", "ntmspace:ore_zinc", gate, Features.count(8), Features.inSquare(),
                Features.uniformHeight(Features.absolute(5), Features.absolute(36)), Features.biome());
        BiomeModifiers.addFeatures(namespace, "ore_zinc", "#minecraft:is_overworld", "ntmspace:ore_zinc",
                BiomeModifiers.UNDERGROUND_ORES);
    }

    /** Pack overrides are generated separately from HBM's live declarations of the same paths. */
    public static void declare() {
        var count = new JsonObject();
        count.addProperty("type", "minecraft:count");
        count.addProperty("count", 0);
        for (var feature : List.of("australium", "ore_gneiss_lithium")) {
            Features.placed("hbm", feature, "hbm:" + feature, count);
        }
    }

    private OverworldFloraData() {}
    public static void init() {}
}
