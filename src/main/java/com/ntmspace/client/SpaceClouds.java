package com.ntmspace.client;

import com.ntmspace.orbit.Orbit;
import com.ntmspace.world.CloudLayers17;
import com.ntmspace.world.SpaceDimensions;
import dev.golem.core.client.Clouds;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Every body level's clouds (hbm-space {@code WorldProviderCelestial.getCloudRenderer} = {@code CloudProviderCelestial}),
 * through Golem Core's {@link Clouds}: the layers {@link CloudLayers17} gives for the body's air, drawn instead of vanilla's
 * clouds on every node, and none at all on an airless body or in orbit (1.7 {@code getCloudHeight()} of -99999). The
 * overworld (Kerbin) keeps vanilla's; see {@link CloudLayers17}.
 */
public final class SpaceClouds {

    private SpaceClouds() {}

    static void init() {
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
    private static com.google.gson.JsonObject query(Minecraft mc, com.google.gson.JsonObject params) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        if (mc.level == null) return json;
        json.addProperty("dimension", dev.golem.core.Ids.keyId(mc.level.dimension()).toString());
        json.addProperty("custom", Clouds.has(mc.level));
        json.addProperty("drawn", dev.golem.core.render.CloudRendering.layersDrawn());
        json.addProperty("cloud_setting", String.valueOf(mc.options.cloudStatus().get()));
        com.google.gson.JsonArray layers = new com.google.gson.JsonArray();
        for (Clouds.Layer layer : Clouds.layers(mc.level, 1.0F)) {
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
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
