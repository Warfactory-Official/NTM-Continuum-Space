package com.ntmspace.client;

import com.google.gson.JsonObject;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import dev.golem.core.client.Fov;
import dev.golem.core.client.PlayerRenderHooks;
import dev.golem.core.world.TimeOfDay;
import net.minecraft.client.Minecraft;

/**
 * The harness query {@code ntmspace_view} for the client-only facades (scenario {@code FacadeClient}): the frame's
 * dynamic FOV (Golem {@code Fov.current}, the Dyson swarm's point size), the camera's distance from the player
 * (Golem {@code CameraHooks.DISTANCE}: 12 blocks in a rocket), the rocket's pitch the rider is tilted by, and the
 * client's time of day (Golem {@code TimeOfDay}, a body's own clock). Registration is a no-op without the harness.
 */
public final class FacadeClient {

    private FacadeClient() {}

    public static void init() {
        dev.golem.core.harness.HarnessQueryRegistry.register("ntmspace_view", FacadeClient::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject out = new JsonObject();
        if (mc.player == null || mc.level == null) return out;
        out.addProperty("fov", Fov.current());
        out.addProperty("options_fov", mc.options.fov().get());
        out.addProperty("detached", !mc.options.getCameraType().isFirstPerson());
        out.addProperty("camera_distance", PlayerRenderHooks.renderOffset(mc.player, 1.0F).length());
        out.addProperty("riding_rocket", mc.player.getVehicle() instanceof EntityRideableRocket);
        out.addProperty("rocket_pitch", mc.player.getVehicle() instanceof EntityRideableRocket rocket ? rocket.getPitch(1.0F) : 0.0F);
        out.addProperty("time_of_day", TimeOfDay.get(mc.level));
        out.addProperty("own_clock", TimeOfDay.hasOwnClock(mc.level));
        return out;
    }
}
