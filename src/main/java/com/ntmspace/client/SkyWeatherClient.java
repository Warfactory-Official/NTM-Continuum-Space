package com.ntmspace.client;

import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceSkies;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.Ids;
import com.google.gson.JsonObject;
import com.ntmspace.orbit.Orbit;
import dev.golem.core.world.Skies;
import dev.golem.core.client.Lightmaps;
import com.ntmspace.registry.SkyWeatherContent;
import com.ntmspace.satellite.SatelliteSky;
import com.ntmspace.weather.DustStorm;
import com.ntmspace.world.SpaceDimensions;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.ClientSessions;
import dev.golem.core.render.Particles;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The client half of the bodies' weather (hbm-space's per-body {@code updateWeather} on the client):
 *
 * <ul>
 *   <li>Duna's dust storm: the strength the server sent ({@link DustStorm}) eased into at 0.8 % a tick
 *       ({@code dustStormSmoothed}, back to 0 on entering another level as 1.7's per-world provider was), and while it is
 *       at least 0.05, one dust cloud a tick with that chance ({@link ParticleDust}, scale 4, blowing at -4 along x) 50
 *       blocks out around the camera on a random bearing. Its fog is {@link CelestialSkyColors#fogDensity}.</li>
 *   <li>Eve's lightning: {@code ntmspace:misc.rumble} at the player, volume 10, the tick a flash begins (1.7
 *       {@code WorldProviderEve.updateWeather}, flash strength crossing 0.01 upwards).</li>
 *   <li>Laythe's and Ike's light maps (Golem {@code Lightmaps}): see {@link #lightmap}.</li>
 * </ul>
 * Plus the start-up of the client sky caches ({@link SatelliteSky}).
 */
public final class SkyWeatherClient {

    private SkyWeatherClient() {}

    /** {@code dustStormSmoothed}. */
    private static float dustStormSmoothed = 0;
    private static float lastFlashStrength;
    private static ClientLevel lastLevel;

    public static void init() {
        GolemNetwork.registerPlayToClient(DustStorm.CODEC_PAYLOAD, (payload, context) -> DustStorm.acceptSync(payload.intensity()));
        SatelliteSky.initClient();
        ClientTicks.onEndTick(SkyWeatherClient::clientTick);
        ClientSessions.START.register(session -> Minecraft.getInstance().execute(SkyWeatherClient::disconnected));
        ClientSessions.END.register(session -> Minecraft.getInstance().execute(SkyWeatherClient::disconnected));
        HarnessQueryRegistry.register("ntmspace_sky", SkyWeatherClient::query);
        // port: Each node keeps its native lightmap for renderer compatibility, including 26.1's departure from 1.7.
        Lightmaps.useClassicFormula(level -> Orbit.inOrbit(level)
                || (SpaceSkies.body(level) != null && !SpaceSkies.isKerbin(level)));
        Lightmaps.register(new Lightmaps.Modifier() {
            @Override
            public boolean active(ClientLevel level) {
                return level.dimension().equals(SpaceDimensions.LAYTHE) || level.dimension().equals(SpaceDimensions.IKE);
            }

            @Override
            public void add(ClientLevel level, int block, int sky, int[] rgb) {
                lightmap(level, sky, rgb);
            }
        });
    }

    /**
     * The light maps of the moons lit by their planet at night: Laythe's green from Jool ({@code WorldProviderLaythe
     * .updateLightmap}: {@code green += max(sky - sun, 0) * 60}) and Ike's red from Duna ({@code WorldProviderIke}:
     * {@code red += max(sky - (sun - 0.1), 0) * 20}), where {@code sky} is the sky-light row's brightness (1.7
     * {@code lightBrightnessTable}, {@code l / (4 - 3 l)} for these dimensions without ambient light) and {@code sun} the
     * body's sun brightness at partial tick 1. Truncated to whole steps, as 1.7's {@code int += float} was.
     */
    static void lightmap(ClientLevel level, int sky, int[] rgb) {
        float l = sky / 15.0F;
        float skyBrightness = l / (4.0F - 3.0F * l);
        float sun = Lightmaps.skyInput(level, 1.0F);
        if (level.dimension().equals(SpaceDimensions.LAYTHE)) {
            float jool = Math.max(skyBrightness - sun, 0);
            rgb[1] += (int) (jool * 60);
        } else if (level.dimension().equals(SpaceDimensions.IKE)) {
            float duna = Math.max(skyBrightness - (sun - 0.1F), 0);
            rgb[0] += (int) (duna * 20);
        }
    }

    /** {@code dustStormSmoothed} on Duna (0 elsewhere). */
    public static float dustStorm() {
        return dustStormSmoothed;
    }

    /** The viewed world's weather and meteors advance once per unpaused client tick. */
    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = useLevel(mc);
        if (level == null) {
            disconnected();
            return;
        }
        if (mc.isPaused()) return; // 1.7 ran this in the world tick, which pauses

        tickMeteors();

        if (level.dimension().equals(SpaceDimensions.DUNA)) {
            // BobMathUtil.lerp(0.008, smoothed, intensity)
            dustStormSmoothed = dustStormSmoothed + (DustStorm.clientIntensity - dustStormSmoothed) * 0.008F;
            Entity view = mc.getCameraEntity();
            if (view != null && dustStormSmoothed >= 0.05F && level.getRandom().nextFloat() < dustStormSmoothed) {
                Vec3 vec = new Vec3(20, 0, 50)
                        .zRot((float) (level.getRandom().nextDouble() * Math.PI * 10))
                        .yRot((float) (level.getRandom().nextDouble() * Math.PI * 2 * 5));
                Particles.add(new ParticleDust(level, view.getX() + vec.x, view.getY(), view.getZ() + vec.z, -4, 0, 0, 4F));
            }
        } else {
            dustStormSmoothed = 0;
        }

        if (level.dimension().equals(SpaceDimensions.EVE)) {
            float flashStrength = CelestialSkyColors.eveFlash(level, 0.0F);
            LocalPlayer player = mc.player;
            if (player != null && lastFlashStrength <= 0.01F && flashStrength > 0.01F) {
                player.playSound(SkyWeatherContent.MISC_RUMBLE.get(), 10F, 1F);
            }
            lastFlashStrength = flashStrength;
        }
    }

    private static boolean viewedBody(ClientLevel level) {
        return !Orbit.inOrbit(level) && SpaceSkies.body(level) != null;
    }

    private static ClientLevel useLevel(Minecraft mc) {
        var level = mc.level;
        if (level != lastLevel) {
            clearVisuals();
            lastLevel = level;
        }
        return level;
    }

    /** A meteor from any body's invasion, into the sky of the body the player is on. */
    public static void addMeteor() {
        var level = useLevel(Minecraft.getInstance());
        if (level != null && viewedBody(level)) WarSky.addMeteor();
    }

    public static void clearMeteors() {
        WarSky.METEORS.clear();
    }

    /** Meteors belong to the current celestial level instance and pause with that world. */
    public static void tickMeteors() {
        var mc = Minecraft.getInstance();
        var level = useLevel(mc);
        if (level == null || !viewedBody(level)) {
            clearMeteors();
            return;
        }
        if (!mc.isPaused()) WarSky.tickMeteors(level);
    }

    private static void clearVisuals() {
        clearMeteors();
        lastLevel = null;
        dustStormSmoothed = 0;
        lastFlashStrength = 0;
    }

    /** A real connection transition releases its visual objects and received celestial overrides. */
    public static void disconnected() {
        clearVisuals();
        CelestialTraits.receive(Map.of());
    }

    /**
     * Harness query {@code ntmspace_sky}: the client's level weather (rain, thunder), Duna's storm (sent and smoothed),
     * Eve's flash, the fog density NTM: Space applies, and the satellites and stations the sky draws.
     */
    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        ClientLevel level = mc.level;
        if (level == null) return json;
        json.addProperty("dimension", dev.golem.core.Ids.keyId(level.dimension()).toString());
        json.addProperty("rain", level.getRainLevel(1.0F));
        json.addProperty("thunder", level.getThunderLevel(1.0F));
        json.addProperty("dust_storm", DustStorm.clientIntensity);
        json.addProperty("dust_storm_smoothed", dustStormSmoothed);
        json.addProperty("eve_flash", CelestialSkyColors.eveFlash(level, 0.0F));
        json.addProperty("fog_density", CelestialSkyColors.fogDensity(level));
        json.addProperty("lightmap_input", Lightmaps.skyInput(level, 1.0F));
        json.addProperty("raw_sky_factor", Skies.rawSkyLightFactor(level));
        json.addProperty("classic_lightmap", Lightmaps.classic(level));
        json.addProperty("lightmap_additions", Lightmaps.additions() != null);
        if (Orbit.inOrbit(level)) {
            OrbitSky.appendState(json);
            json.add("server", serverSky(mc));
        }
        SatelliteSky.View view = SatelliteSky.client();
        com.google.gson.JsonArray sats = new com.google.gson.JsonArray();
        for (SatelliteSky.Sat sat : view.satellites()) {
            JsonObject o = new JsonObject();
            o.addProperty("freq", sat.freq());
            o.addProperty("type", sat.type());
            o.addProperty("altitude", sat.orbit().altitude());
            o.addProperty("inclination", sat.orbit().inclination());
            sats.add(o);
        }
        json.add("satellites", sats);
        json.addProperty("stations", view.stations().size());
        if (params != null && params.has("pin_ticks")) {
            double pin = params.get("pin_ticks").getAsDouble();
            MunSky.pinnedTicks = pin < 0 ? Double.NaN : pin;
            SpaceSkies.clientPinnedTicks = pin < 0 ? Long.MIN_VALUE : (long) pin;
        }
        if (SpaceSkies.body(level) != null) {
            json.addProperty("eclipse", SpaceSkies.eclipse(level));
            json.addProperty("sun_brightness", CelestialSkyColors.sunBrightness(level,
                    SpaceSkies.sunBrightness(level, 0.0F), 0.0F));
        }
        json.add("astro", astro(level));
        return json;
    }

    /** Samples the real integrated-server input on its owner thread, independently of the rendered orbital state. */
    private static JsonObject serverSky(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return new JsonObject();
        var playerId = mc.player.getUUID();
        return server.submit(() -> {
            var player = server.getPlayerList().getPlayer(playerId);
            var level = player.level();
            var json = new JsonObject();
            json.addProperty("dimension", Ids.keyId(level.dimension()).toString());
            var input = Skies.lightInput(level, player.blockPosition());
            json.addProperty("light_input_available", input.isPresent());
            input.ifPresent(value -> json.addProperty("light_input", value));
            json.addProperty("raw_sky_factor", Skies.rawSkyLightFactor(level));
            return json;
        }).join();
    }

    /**
     * The sky's astronomy as {@link MunSky} draws it at partial tick 0: the level's game time and time of day, the time
     * the orbits use ({@code sky_ticks}, pinned or the game time), the solar and sidereal angles, and every other body's
     * {@code AstroMetric} (angle, inclination, apparent size, phase, distance in km).
     */
    private static JsonObject astro(ClientLevel level) {
        JsonObject json = new JsonObject();
        json.addProperty("game_time", level.getGameTime());
        json.addProperty("time_of_day", dev.golem.core.world.TimeOfDay.get(level));
        SolarSystem.Body body = SpaceSkies.body(level);
        if (body == null) return json;
        double ticks = MunSky.skyTicks(level, 0.0F);
        float solarAngle = SpaceSkies.angle(level, 0.0F);
        json.addProperty("body", body.name);
        json.addProperty("pinned", !Double.isNaN(MunSky.pinnedTicks));
        json.addProperty("sky_ticks", ticks);
        json.addProperty("solar_angle", solarAngle);
        json.addProperty("axial_tilt", body.axialTilt);
        json.addProperty("sidereal_angle", com.ntmspace.world.SolarSystem.calculateSiderealAngle(body, ticks));
        com.google.gson.JsonArray bodies = new com.google.gson.JsonArray();
        for (SolarSystem.AstroMetric m : SolarSystem.calculateClientMetricsFromBody(ticks, body, solarAngle)) {
            if (m.distance == 0) continue;
            JsonObject o = new JsonObject();
            o.addProperty("body", m.body.name);
            o.addProperty("angle", m.angle);
            o.addProperty("inclination", m.inclination);
            o.addProperty("size", m.apparentSize);
            o.addProperty("phase", m.phase);
            o.addProperty("distance", m.distance);
            bodies.add(o);
        }
        json.add("bodies", bodies);
        return json;
    }
}
