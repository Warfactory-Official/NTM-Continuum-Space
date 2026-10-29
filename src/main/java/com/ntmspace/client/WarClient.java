package com.ntmspace.client;

import com.ntmspace.client.gui.war.GUIWarController;
import com.ntmspace.client.render.mob.RenderSiegeCraft;
import com.ntmspace.registry.WarContent;
import com.ntmspace.trait.CBT_Invasion;
import com.ntmspace.trait.CBT_War;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.war.WarSatellites;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.client.ClientTicks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The war and invasion family's client (hbm-space {@code ClientProxy}, {@code ModEventHandlerClient} and the client half
 * of {@code CBT_Invasion.update}):
 *
 * <ul>
 *   <li>the war controller's screen and the siege craft's renderer;</li>
 *   <li>the invasion's warning: in its last 5 countdown ticks the client plays {@code alarm.ping} (volume 10) and prints
 *       "Incoming Invasion!" in red for the currently viewed body;</li>
 *   <li>meteors while the viewed body's invasion runs: each client tick one in {@code max(1, 5 - wave)}, owned by that
 *       client level and cleared when the viewed world changes;</li>
 *   <li>the invasion's boss bar on its body while it invades and before the final wave: "Wave N", the kills still
 *       needed over the kill requirement (1.7 {@code BossStatus.setBossStatus(invasion)}, HBM's {@code BossStatus});</li>
 *   <li>{@code misc.impact} at the player (volume 10) the tick a war projectile of the player's body has 18 ticks of
 *       flight left.</li>
 * </ul>
 */
public final class WarClient {

    private WarClient() {
    }

    public static void init() {
        ClientRegistrations.screens(
                screens -> screens.register(WarContent.WAR_CONTROLLER_MENU.get(), GUIWarController::new));
        ClientRegistrations.renderers(r -> r.entity(WarContent.SIEGE_CRAFT.get(), RenderSiegeCraft::new));
        WarSatellites.initClient();
        WarSky.init();
        CBT_Invasion.CLIENT = new CBT_Invasion.ClientEffects() {
            @Override
            public void warning() {
                // Trait packets decode on the network thread; chat layout and sounds belong to the client thread.
                Minecraft.getInstance().execute(() -> {
                    var player = Minecraft.getInstance().player;
                    if (player == null) return;
                    player.playSound(WarContent.ALARM_PING.get(), 10F, 1F);
                    player.sendSystemMessage(
                            Component.translatable("invasion.hbmspace.incoming").withStyle(ChatFormatting.RED));
                });
            }

            @Override
            public void meteor() {
                SkyWeatherClient.addMeteor();
            }
        };
        ClientTicks.onEndTick(WarClient::tick);
        dev.golem.core.harness.HarnessQueryRegistry.register("ntmspace_war", WarClient::query);
    }

    /**
     * Harness query {@code ntmspace_war}: the client's view of the war family - the player's body, its synced war
     * (health, shield, projectiles with travel/anim/flash) and invasion, the meteors in the sky, the railguns the sky
     * draws, and an open war controller terminal's history.
     */
    private static com.google.gson.JsonObject query(Minecraft mc, com.google.gson.JsonObject params) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        ClientLevel level = mc.level;
        if (level == null) return json;
        json.addProperty("body", CelestialTraits.bodyOf(level));
        CBT_War war = CelestialTraits.getTrait(level, CBT_War.class);
        if (war != null) {
            json.addProperty("war_health", war.health);
            json.addProperty("war_shield", war.shield);
            com.google.gson.JsonArray projectiles = new com.google.gson.JsonArray();
            for (CBT_War.Projectile p : war.getProjectiles()) {
                com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                o.addProperty("type", p.getType().name());
                o.addProperty("travel", p.getTravel());
                o.addProperty("anim", p.getAnimtime());
                o.addProperty("flash", p.getFlashtime());
                projectiles.add(o);
            }
            json.add("projectiles", projectiles);
        }
        json.addProperty("sky_flash", WarSky.skyFlash(level));
        CBT_Invasion invasion = CelestialTraits.getTrait(level, CBT_Invasion.class);
        if (invasion != null) {
            json.addProperty("invasion_wave", invasion.wave);
            json.addProperty("invasion_invading", invasion.isInvading);
            json.addProperty("invasion_kills", invasion.kills);
            json.addProperty("invasion_killreq", invasion.killreq);
        }
        json.addProperty("meteors", WarSky.METEORS.size());
        com.google.gson.JsonArray railguns = new com.google.gson.JsonArray();
        for (WarSatellites.Railgun railgun : WarSatellites.clientRailguns()) {
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("freq", railgun.freq());
            o.addProperty("interp", railgun.interp());
            railguns.add(o);
        }
        json.add("railguns", railguns);
        if (mc.screen instanceof GUIWarController screen) {
            com.google.gson.JsonArray history = new com.google.gson.JsonArray();
            for (String line : screen.history()) history.add(line);
            json.add("terminal", history);
            json.addProperty("typed", screen.typed());
        }
        return json;
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;

        CBT_War war = CelestialTraits.getTrait(level, CBT_War.class);
        if (war != null) {
            for (CBT_War.Projectile projectile : war.getProjectiles()) {
                if (projectile != null && projectile.getTravel() >= 18 && projectile.getTravel() <= 18) {
                    player.playSound(WarContent.MISC_IMPACT.get(), 10F, 1F);
                }
            }
        }
    }
}
