package com.ntmspace.gametest;

import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.StationData;
import com.ntmspace.satellite.SatelliteOrbit;
import com.ntmspace.satellite.SatelliteSky;
import com.ntmspace.trait.CBT_Weather;
import com.ntmspace.trait.CelestialBodyTrait;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.weather.BodyWeather;
import com.ntmspace.weather.DustStorm;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Weathers;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * The server half of the bodies' weather and sky objects: Golem {@link Weathers} replacing vanilla's weather cycle for a
 * level (on the GameTest level itself, through a test provider), each body's weather as its {@link CBT_Weather} gives
 * it ({@link BodyWeather}: Eve forced to thunder ramps up, airless or waterless bodies stay dry, the overworld keeps
 * vanilla's), Duna's dust storm cycle (1.7 {@code WorldProviderDuna.updateWeather}) and its save, and the satellites and
 * stations the client draws ({@link SatelliteSky}). The GameTest server has no body levels, so the body half is checked
 * on the traits and the level half on the test level. Tests that change saved state put it back.
 */
public final class SkyWeatherGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** Golem Weathers: a provided level's rain and thunder are the provider's, sent as vanilla's; vanilla resumes after. */
    public static final GameTests.Function PROVIDED = TESTS.function("sky_weather_provider_replaces_vanilla", 40,
            () -> SkyWeatherGameTests::provided);

    /** Eve forced to thunder: its weather ramps up 0.01 a tick in the trait, which is what Eve's level gets. */
    public static final GameTests.Function EVE = TESTS.function("sky_weather_eve_thunder_ramps", 200,
            () -> SkyWeatherGameTests::eve);

    /** Bodies without a weather cycle (Mun, Duna, Ike, Moho, Minmus, Dres, Thatmo) are dry; the overworld is vanilla's. */
    public static final GameTests.Function DRY = TESTS.function("sky_weather_dry_bodies", 20, () -> SkyWeatherGameTests::dry);

    /** Duna's storm cycle: the 1.7 durations and strengths, ticking, and the save's 1.7 key names. */
    public static final GameTests.Function STORM = TESTS.function("sky_weather_dust_storm_cycle", 20,
            () -> SkyWeatherGameTests::storm);

    /** The sky cache: a satellite with its orbit settings and a station over the level, the payload round trip. */
    public static final GameTests.Function OBJECTS = TESTS.function("sky_objects_satellites_stations", 20,
            () -> SkyWeatherGameTests::objects);

    /** The level a test forces its weather on, and the weather (test provider, below every real one). */
    private static volatile ServerLevel forcedLevel;
    private static volatile Weathers.State forced;

    private SkyWeatherGameTests() {}

    public static void init() {
        Weathers.register(level -> level == forcedLevel ? forced : null);
    }

    // ------------------------------------------------------------------------------------------------ weather

    private static void provided(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        forced = new Weathers.State(0.7F, 0.4F);
        forcedLevel = level;
        helper.runAfterDelay(5, () -> {
            List<String> errors = new ArrayList<>();
            float rain = level.getRainLevel(1.0F);
            float thunder = level.getThunderLevel(1.0F);
            if (Math.abs(rain - 0.7F) > 1e-6) errors.add("rain " + rain + " (provided 0.7)");
            // vanilla weighs the thunder by the rain
            if (Math.abs(thunder - 0.4F * 0.7F) > 1e-6) errors.add("thunder " + thunder + " (provided 0.4 x rain 0.7)");
            if (!level.isRaining()) errors.add("the level does not rain");
            // dry again for a tick, then vanilla's cycle runs again
            forced = new Weathers.State(0.0F, 0.0F);
            helper.runAfterDelay(2, () -> {
                if (level.getRainLevel(1.0F) != 0.0F) errors.add("rain did not stop: " + level.getRainLevel(1.0F));
                forcedLevel = null;
                forced = null;
                finish(helper, errors);
            });
        });
    }

    private static void eve(GameTestHelper helper) {
        CelestialTraits.attach(helper.getLevel());
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> before = copy(CelestialTraits.getTraits("eve"));
        CBT_Weather weather = CBT_Weather.ensureTrait("eve");
        if (weather == null) {
            helper.fail("Eve has no weather cycle (1.7: 5 atm and mercury)");
            return;
        }
        weather.forceClear(new java.util.Random(1), 100_000);
        weather.forceThunder(100_000);
        CelestialTraits.markChanged();
        float startRain = weather.rainStrength;
        long start = helper.getLevel().getGameTime();
        helper.succeedWhen(() -> {
            CBT_Weather live = CelestialTraits.getTrait("eve", CBT_Weather.class, false);
            List<String> errors = new ArrayList<>();
            if (live == null) errors.add("Eve lost its weather trait");
            else {
                Weathers.State state = BodyWeather.weatherOfBody("eve");
                if (state.rain() != live.rainStrength || state.thunder() != live.thunderStrength) {
                    errors.add("Eve's level weather " + state + " is not its trait's " + live.rainStrength + "/" + live.thunderStrength);
                }
                if (live.rainStrength < 0.6F || live.thunderStrength < 0.6F) {
                    errors.add("still ramping: rain " + live.rainStrength + " thunder " + live.thunderStrength);
                }
                long ticks = helper.getLevel().getGameTime() - start;
                // 0.01 a tick from a forced clear: never faster than the tick count allows
                if (live.rainStrength - startRain > ticks * 0.01F + 0.0101F) errors.add("rain rose faster than 0.01 a tick");
            }
            // retried each tick until it holds (GameTestHelper.fail throws the framework's assertion)
            if (!errors.isEmpty()) helper.fail(String.join("; ", errors));
            NtmSpace.LOG.info("sky_weather_eve ticks={} rain={} thunder={}", helper.getLevel().getGameTime() - start,
                    live.rainStrength, live.thunderStrength);
            CelestialTraits.setTraits("eve", before);
        });
    }

    private static void dry(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        for (String body : List.of("mun", "duna", "ike", "moho", "minmus", "dres", "thatmo")) {
            Weathers.State state = BodyWeather.weatherOfBody(body);
            if (state.rain() != 0 || state.thunder() != 0) errors.add(body + " weather " + state);
        }
        if (BodyWeather.weather(helper.getLevel().getServer().overworld()) != null) {
            errors.add("the overworld's weather must stay vanilla's");
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ dust storm

    private static void storm(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        RandomSource rand = RandomSource.create(4815162342L);
        DustStorm storm = new DustStorm();
        // a fresh save: strength 1 and an expired countdown, which the first tick turns into a long clear spell
        storm.tick(rand);
        if (storm.intensity != 0) errors.add("first tick should clear the sky: " + storm.intensity);
        if (storm.timer < 12000 - 1 || storm.timer >= 180000 - 1) errors.add("clear spell " + storm.timer);
        for (int round = 0; round < 20; round++) {
            storm.timer = 1;
            storm.tick(rand); // 1 -> 0
            storm.tick(rand); // expired: the weather changes
            boolean storming = storm.intensity >= 0.05F;
            if (storming) {
                if (storm.intensity < 0.25F || storm.intensity >= 1.0F) errors.add("storm strength " + storm.intensity);
                if (storm.timer < 12000 - 1 || storm.timer >= 24000 - 1) errors.add("storm length " + storm.timer);
            } else {
                if (storm.timer < 12000 - 1 || storm.timer >= 180000 - 1) errors.add("clear length " + storm.timer);
            }
            // the cycle alternates
            boolean expected = round % 2 == 0;
            if (storming != expected) errors.add("round " + round + " storming " + storming);
        }
        storm.intensity = 0.625F;
        storm.timer = 4321;
        Tag tag = DustStorm.CODEC.encodeStart(NbtOps.INSTANCE, storm).result().orElse(null);
        String nbt = String.valueOf(tag);
        if (!nbt.contains("dustStormTimer") || !nbt.contains("dustStormIntensity")) errors.add("1.7 keys missing: " + nbt);
        DustStorm back = tag == null ? null : DustStorm.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
        if (back == null || back.timer != 4321 || back.intensity != 0.625F) errors.add("save round trip " + nbt);
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ sky objects

    private static void objects(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var server = helper.getLevel().getServer();
        ServerLevel kerbin = server.overworld();
        int freq = 731_947;
        SatelliteSavedData data = SatelliteSavedData.get(kerbin);
        Satellite previous = data.sats.get(freq);
        Satellite sat = SatelliteType.MAPPER.create();
        SatelliteOrbit settings = new SatelliteOrbit(12.5F, 110F, 45F, true, 0.5F, "Tester", 0.25F, 0.5F, 0.75F);
        settings.write(sat.extra);
        StationData stations = StationData.get(server);
        int sx = 31, sz = -29, mx = 33, mz = -29;
        OrbitalStation station = stations.addStation(sx, sz, SolarSystem.body("kerbin"));
        OrbitalStation munStation = stations.addStation(mx, mz, SolarSystem.body("mun"));
        boolean hadStation = station.hasStation, hadMunStation = munStation.hasStation;
        try {
            data.sats.put(freq, sat);
            station.hasStation = true;
            munStation.hasStation = true;
            SatelliteSky.View view = SatelliteSky.view(kerbin);
            SatelliteSky.Sat found = null;
            for (SatelliteSky.Sat s : view.satellites()) if (s.freq() == freq) found = s;
            if (found == null) errors.add("Kerbin's satellite is not in its sky");
            else {
                if (!found.type().equals("mapper")) errors.add("type " + found.type());
                if (!found.orbit().equals(settings)) errors.add("settings " + found.orbit());
            }
            if (!view.stations().contains(new SatelliteSky.Station(sx, sz))) errors.add("Kerbin's station is not in its sky");
            if (view.stations().contains(new SatelliteSky.Station(mx, mz))) errors.add("a Mun station shows over Kerbin");

            // the payload round trip
            RegistryFriendlyByteBuf buf = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
            SatelliteSky.CODEC.encode(new SatelliteSky.Payload(view), buf);
            SatelliteSky.Payload back = SatelliteSky.CODEC.decode(buf);
            buf.release();
            if (!back.view().equals(view)) errors.add("payload round trip changed the view");
        } catch (RuntimeException e) {
            errors.add(e.toString());
        } finally {
            if (previous == null) data.sats.remove(freq);
            else data.sats.put(freq, previous);
            station.hasStation = hadStation;
            munStation.hasStation = hadMunStation;
            if (!hadStation) stations.removeStation(sx, sz);
            if (!hadMunStation) stations.removeStation(mx, mz);
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------

    private static Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> copy(
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> out = new HashMap<>();
        for (Map.Entry<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> e : traits.entrySet()) out.put(e.getKey(), e.getValue().copy());
        return out;
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (errors.isEmpty()) {
            helper.succeed();
        } else {
            String message = String.join("; ", errors);
            NtmSpace.LOG.error("GameTest failure: {}", message);
            helper.fail(message);
        }
    }
}
