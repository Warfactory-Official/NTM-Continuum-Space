package com.ntmspace.gametest;

import com.hbm.particle.helper.HbmEffectNT;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.tileentity.dyson.TileEntityDysonLauncher;
import com.ntmspace.world.MartianWorld;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.SpaceSkies;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.TimeOfDay;
import dev.golem.core.world.WorldTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * The Golem Core / HBM facades the M3 families waited for, as NTM: Space uses them: each body's own time of day (Golem
 * {@code TimeOfDay}, hbm-space {@code CelestialBodyWorldSavedData}), the world type "The Martian" (Golem
 * {@code WorldTypes}, first join through Golem {@code Lifecycle.onPlayerJoin}) and the Dyson launch burst as an addon
 * {@code HbmEffectNT}. The client-only ones (FOV, camera distance, rider tilt) are in the harness scenario
 * {@code FacadeClient}.
 */
public final class FacadeGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /**
     * Every body but Kerbin keeps its own clock; it is saved, read back and drives the sky's solar angle.
     */
    public static final GameTests.Function BODY_CLOCKS = TESTS.function("facade_body_time_of_day", 40,
            () -> FacadeGameTests::bodyClocks);

    /**
     * "The Martian": its preset, parameter list and tag are loaded, its overworld is stable, this test world is not one.
     */
    public static final GameTests.Function MARTIAN = TESTS.function("facade_martian_world_type", 20,
            () -> FacadeGameTests::martian);

    /**
     * The launch burst is an addon effect under its namespaced name.
     */
    public static final GameTests.Function EFFECT = TESTS.function("facade_dyson_launch_effect", 20,
            () -> FacadeGameTests::effect);

    private FacadeGameTests() {
    }

    public static void init() {
    }

    private static void bodyClocks(GameTestHelper helper) {
        for (var dimension : java.util.List.of(SpaceDimensions.MUN, SpaceDimensions.MINMUS, SpaceDimensions.DUNA,
                SpaceDimensions.IKE, SpaceDimensions.DRES, SpaceDimensions.EVE, SpaceDimensions.MOHO,
                SpaceDimensions.LAYTHE, SpaceDimensions.TEKTO, SpaceDimensions.THATMO)) {
            helper.assertTrue(TimeOfDay.hasOwnClock(dimension), dimension + " keeps its own time of day");
        }
        helper.assertTrue(!TimeOfDay.hasOwnClock(Level.OVERWORLD), "Kerbin keeps vanilla's time of day");
        helper.assertTrue(TimeOfDay.hasOwnClock(Orbit.ORBIT), "Orbit keeps its fixed noon clock");
        // the GameTest server has only the overworld: try a clock there for the length of this test
        ServerLevel level = helper.getLevel();
        long vanilla = TimeOfDay.vanilla(level);
        TimeOfDay.ownClock(level.dimension());
        long probe = vanilla + 77_777L;
        TimeOfDay.set(level, probe);
        boolean readBack = TimeOfDay.get(level) == probe && TimeOfDay.vanilla(level) == vanilla;
        float expected = SolarSystem.solarAngle(SolarSystem.body("mun"), probe, 0.0F);
        boolean advancing = TimeOfDay.advancing(level);
        helper.runAfterDelay(5, () -> {
            long now = TimeOfDay.get(level);
            TimeOfDay.release(level.dimension());
            helper.assertTrue(readBack, "an own clock reads back what was set, vanilla's day time untouched");
            helper.assertTrue(advancing ? now >= probe + 4 && now <= probe + 6 : now == probe,
                    "an own clock ticks with the daylight rule (" + advancing + "): " + probe + " -> " + now);
            helper.assertTrue(TimeOfDay.get(level) == TimeOfDay.vanilla(level),
                    "a released clock reads vanilla's again");
            helper.assertTrue(SolarSystem.solarAngle(SolarSystem.body("mun"), probe, 0.0F) == expected,
                    "the solar angle is a function of the clock");
            helper.succeed();
        });
    }

    private static void martian(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var registries = server.registryAccess();
        var listKey = ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
                dev.golem.core.Ids.of("ntmspace", MartianWorld.NAME));
        var list = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).get(listKey);
        helper.assertTrue(list.isPresent(), "parameter list ntmspace:martian is loaded");
        var presetKey = ResourceKey.create(Registries.WORLD_PRESET,
                dev.golem.core.Ids.of("ntmspace", MartianWorld.NAME));
        var preset = registries.lookupOrThrow(Registries.WORLD_PRESET).get(presetKey);
        helper.assertTrue(preset.isPresent(), "world preset ntmspace:martian is loaded");
        var normal = net.minecraft.tags.TagKey.create(Registries.WORLD_PRESET, dev.golem.core.Ids.minecraft("normal"));
        helper.assertTrue(preset.get().is(normal),
                "ntmspace:martian is listed on the create-world screen (#minecraft:normal)");
        // the preset's overworld names the parameter list, and Golem calls it stable (no experimental screen)
        var stem = preset.get().value().overworld();
        helper.assertTrue(stem.isPresent() && WorldTypes.isStableOverworld(LevelStem.OVERWORLD, stem.get()),
                "the martian overworld is stable");
        helper.assertTrue(!MartianWorld.is(server), "the test world is not a martian world");
        // a fresh player has never played here: the first-join signal hbm-space's hasWarped stood for
        net.minecraft.server.level.ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        var stats = player.getStats();
        var playTime = net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME);
        var leaveGame = net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.LEAVE_GAME);
        int savedPlay = stats.getValue(playTime), savedLeave = stats.getValue(leaveGame);
        stats.setValue(player, playTime, 0);
        stats.setValue(player, leaveGame, 0);
        boolean fresh = Lifecycle.isFirstJoin(player);
        stats.setValue(player, playTime, 20);
        boolean played = Lifecycle.isFirstJoin(player);
        stats.setValue(player, playTime, 0);
        stats.setValue(player, leaveGame, 1);
        boolean left = Lifecycle.isFirstJoin(player);
        stats.setValue(player, playTime, savedPlay);
        stats.setValue(player, leaveGame, savedLeave);
        helper.assertTrue(fresh && !played && !left,
                "first join = no play time and never left (" + fresh + ", " + played + ", " + left + ")");
        helper.succeed();
    }

    private static void effect(GameTestHelper helper) {
        // The launcher's class registers the effect when it initialises; read it first, or the lookup runs before
        // that on a node where no launcher was touched yet (26.1 Fabric's test order) and finds nothing.
        HbmEffectNT expected = TileEntityDysonLauncher.SPINLAUNCH;
        helper.assertTrue(HbmEffectNT.byName("ntmspace:spinlaunch") == expected,
                "the Dyson launch burst is HBM effect ntmspace:spinlaunch");
        helper.succeed();
    }
}
