package com.ntmspace.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.CloudLayers17;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.SpaceSkies;
import dev.golem.core.Ids;
import dev.golem.core.client.Clouds;
import dev.golem.core.render.CloudRendering;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Every body level's clouds (hbm-space {@code WorldProviderCelestial.getCloudRenderer} = {@code CloudProviderCelestial}),
 * through Golem Core's {@link Clouds}: the layers {@link CloudLayers17} gives for the body's air, drawn instead of vanilla's
 * clouds on every node, and none at all on an airless body or in orbit (1.7 {@code getCloudHeight()} of -99999). The
 * overworld (Kerbin) keeps its native first layer and adds upper decks according to its live atmosphere.
 */
public final class SpaceClouds {

    private SpaceClouds() {}

    static void init() {
        // port: Kerbin keeps each node's native cloud geometry instead of 1.7's fixed base to retain native rendering.
        Clouds.registerAdditions(Level.OVERWORLD, new Clouds.AdditionalProvider() {
            @Override
            public boolean visible(Level level) {
                var body = SpaceSkies.airBody(level);
                return body != null && CloudLayers17.layerCount(Atmospheres.of(body, level.isClientSide())) > 0;
            }

            @Override
            public List<Clouds.Layer> layers(Level level, Clouds.NativeLayer base) {
                return CloudLayers17.additionalLayers(level, base);
            }
        });
        for (ResourceKey<Level> dimension : List.of(SpaceDimensions.MUN, SpaceDimensions.MINMUS, SpaceDimensions.DUNA,
                SpaceDimensions.IKE, SpaceDimensions.DRES, SpaceDimensions.EVE, SpaceDimensions.MOHO,
                SpaceDimensions.LAYTHE, SpaceDimensions.TEKTO, SpaceDimensions.THATMO)) {
            Clouds.register(dimension, (level, partialTick) -> CloudLayers17.layers(level, cloudTime(level, partialTick), partialTick));
        }
        Clouds.off(Orbit.ORBIT);
        dev.golem.core.harness.HarnessQueryRegistry.register("ntmspace_clouds", SpaceClouds::query);
    }

    /**
     * Harness query {@code ntmspace_clouds}: whether Golem draws the level's clouds ({@code custom}), its layers now
     * (height, colour, alpha, texture shift) and how many layers Golem has drawn since start ({@code drawn}: grows every
     * frame while a layer is on screen, the positive signal that the node's hook runs).
     */
    private static JsonObject query(Minecraft mc, JsonObject params) {
        var json = new JsonObject();
        if (mc.level == null) return json;
        json.addProperty("dimension", Ids.keyId(mc.level.dimension()).toString());
        json.addProperty("custom", Clouds.has(mc.level));
        var additive = Clouds.additive(mc.level);
        json.addProperty("mode", additive ? "additive" : Clouds.has(mc.level) ? "replacement" : "vanilla");
        json.addProperty("drawn", CloudRendering.layersDrawn());
        json.addProperty("native_drawn", CloudRendering.nativeLayersDrawn());
        json.addProperty("cloud_setting", String.valueOf(mc.options.cloudStatus().get()));
        var nativeLayers = CloudRendering.renderedNativeLayers(mc.level);
        List<Clouds.Layer> visible;
        if (additive) {
            visible = Clouds.nativeVisible(mc.level) ? nativeLayers : List.of();
            if (!nativeLayers.isEmpty()) {
                var base = nativeLayers.get(0);
                var argb = (Math.round(base.alpha() * 255) << 24) | (Math.round(base.red() * 255) << 16)
                        | (Math.round(base.green() * 255) << 8) | Math.round(base.blue() * 255);
                json.addProperty("native_color", argb);
                json.addProperty("native_height", base.height());
            }
        } else {
            visible = Clouds.layers(mc.level, 1.0F);
        }
        var layers = new JsonArray();
        for (Clouds.Layer layer : visible) {
            var o = new JsonObject();
            o.addProperty("height", layer.height());
            o.addProperty("red", layer.red());
            o.addProperty("green", layer.green());
            o.addProperty("blue", layer.blue());
            o.addProperty("alpha", layer.alpha());
            o.addProperty("shift_x", layer.shiftX());
            o.addProperty("shift_z", layer.shiftZ());
            layers.add(o);
        }
        json.add("layers", layers);
        return json;
    }

    /** {@code getCloudTime}: the player's age in ticks plus the partial tick, or the world's age without a player. */
    static double cloudTime(Level level, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        int ticks = player != null ? player.tickCount : (int) (level.getGameTime() & Integer.MAX_VALUE);
        return ticks + partialTick;
    }
}
