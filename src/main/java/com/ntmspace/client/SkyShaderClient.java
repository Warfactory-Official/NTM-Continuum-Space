package com.ntmspace.client;

import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.NukeShocks;
import dev.golem.core.client.Fogs;
import dev.golem.core.client.Precipitation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Client start-up of the shader sky layers ({@link PlanetShaders}), the bodies' rain and snow ({@link CelestialWeather})
 * and the nuke shocks seen from space ({@link NukeShocks}), plus their harness query.
 */
public final class SkyShaderClient {

    private SkyShaderClient() {}

    public static void init() {
        NukeShocks.initClient();
        CelestialWeather.init();
        dev.golem.core.harness.HarnessQueryRegistry.register("ntmspace_sky_shaders", SkyShaderClient::query);
    }

    /**
     * Harness query {@code ntmspace_sky_shaders}: the draws of each shader layer so far ({@code draws}, by shader id), the
     * weather quads Golem drew ({@code weather_quads}) and whether it draws this level's weather ({@code weather_custom}),
     * this body's rain and snow colours, the far end of the linear fog Golem's {@code Fogs} applies ({@code fog_end},
     * absent for none), the nuke shocks of a body ({@code params.body}, default this one) and the body's light intensity.
     */
    private static com.google.gson.JsonObject query(Minecraft mc, com.google.gson.JsonObject params) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        ClientLevel level = mc.level;
        json.add("draws", PlanetShaders.draws());
        json.addProperty("weather_quads", Precipitation.quadsDrawn());
        if (level == null) return json;
        String body = CelestialTraits.bodyOf(level);
        json.addProperty("body", body);
        json.addProperty("weather_custom", Precipitation.has(level));
        com.ntmspace.trait.CBT_Water water = CelestialTraits.getTrait(body, com.ntmspace.trait.CBT_Water.class, true);
        float[] rain = water == null || water.fluid == null ? new float[] {1, 1, 1} : CelestialWeather.rainColor(water.fluid.getColor());
        com.google.gson.JsonArray rainJson = new com.google.gson.JsonArray();
        for (float c : rain) rainJson.add(c);
        json.add("rain_color", rainJson);
        float fogEnd = Fogs.far(mc.getCameraEntity(), Fogs.renderDistanceBlocks());
        if (!Float.isNaN(fogEnd)) json.addProperty("fog_end", fogEnd);
        String target = params.has("body") ? params.get("body").getAsString() : body;
        json.addProperty("nuke_shocks", NukeShocks.clientShocks(target, level.getGameTime()).size());
        com.ntmspace.trait.CBT_Lights lights = CelestialTraits.getTrait(target, com.ntmspace.trait.CBT_Lights.class, true);
        json.addProperty("light_intensity", lights == null ? 0 : lights.getIntensity());
        return json;
    }
}
