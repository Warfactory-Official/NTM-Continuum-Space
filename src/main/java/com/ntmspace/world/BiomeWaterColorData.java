package com.ntmspace.world;

import com.google.gson.JsonParser;
import com.ntmspace.world.gen.legacy.EveTerrain17;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import com.ntmspace.world.gen.legacy.LaytheTerrain17;
import com.ntmspace.world.gen.legacy.TektoTerrain17;
import dev.golem.core.dejson.Data;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Emits biome water colours from the same authored descriptors the production terrain uses. */
public final class BiomeWaterColorData {

    // Vanilla's untinted blue sheet; SpaceDimensionProvider.waterColor applies the source RGB multiplier to it.
    private static final int VANILLA_WATER_COLOR = 0x3F76E4;

    private BiomeWaterColorData() {}

    /** Dejson's ordinary declare/verify/write path; terrain noise and chunks are never generated here. */
    public static void declare() {
        var templates = Path.of(System.getProperty("ntmspace.biomeTemplates"));
        declare(templates, LayeredBodies.EVE, new EveTerrain17(0L, new Random(0L)));
        declare(templates, LayeredBodies.LAYTHE, new LaytheTerrain17(0L, new Random(0L)));
        declare(templates, LayeredBodies.TEKTO, new TektoTerrain17(0L, new Random(0L)));
    }

    private static void declare(Path templates, LayeredBodies.Body body, LayeredBodyTerrain17 terrain) {
        for (var entry : body.biomes().entrySet()) {
            var name = body.name() + "_" + entry.getKey();
            var template = templates.resolve(name + ".json");
            try (var reader = Files.newBufferedReader(template, StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                var effects = json.getAsJsonObject("effects");
                if (effects.has("water_color")) {
                    throw new IllegalStateException(template + " must leave water_color to its production descriptor");
                }
                effects.addProperty("water_color", String.format("#%06x", terrain.biome(entry.getValue()).waterColor(VANILLA_WATER_COLOR)));
                Data.declare("data/ntmspace/worldgen/biome/" + name + ".json", json);
            } catch (IOException failure) {
                throw new UncheckedIOException("Cannot read biome template " + template, failure);
            }
        }
    }
}
