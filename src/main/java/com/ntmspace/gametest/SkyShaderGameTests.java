package com.ntmspace.gametest;

import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.trait.CBT_Water;
import com.ntmspace.trait.CelestialTraits;
import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.NukeShocks;
import com.ntmspace.world.SkyMath;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The pure maths behind the shader sky layers, Eve's fog (1.7's exponential, the port's linear stand-in) and the tinted rain (hbm-space
 * {@code WorldProviderCelestial.fogDensity} / {@code getWeatherColor}, {@code AtmosphereRenderUtil},
 * {@code CelestialNukeShockHandler}). Expected values come from an independent restatement of the 1.7 code run in a
 * plain JVM (session scratch {@code m3shader/oracle/Oracle.java}), or by hand where noted.
 */
public final class SkyShaderGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /**
     * 1.7 fog densities (Eve 0.05, Duna's storm, Laythe's water), their {@code GL_EXP} fog (Eve 95 % at 60 blocks) and the
     * port's linear stand-in ending there ({@code ln 20 / density}: Eve 59.9 blocks).
     */
    public static final GameTests.Function FOG = TESTS.function("sky_fog_density_linear_end", 1, () -> SkyShaderGameTests::fog);

    /** {@code getWeatherColor}: water's and mercury's rain colours, and the bodies' default liquids. */
    public static final GameTests.Function RAIN = TESTS.function("sky_rain_tint_colour", 1, () -> SkyShaderGameTests::rain);

    /** {@code CelestialNukeShockHandler.trigger}: the hashed, grid-snapped centre and the strength of a blast's shock. */
    public static final GameTests.Function NUKE = TESTS.function("sky_nuke_shock_centre", 1, () -> SkyShaderGameTests::nuke);

    /** {@code getEveFlashStrength}: the strike schedule (window 6 is the first with a strike). */
    public static final GameTests.Function EVE = TESTS.function("sky_eve_flash_schedule", 1, () -> SkyShaderGameTests::eve);

    /** {@code getAtmosphereStyle} and the storm darkening. */
    public static final GameTests.Function STYLE = TESTS.function("sky_atmosphere_style", 1, () -> SkyShaderGameTests::style);

    private SkyShaderGameTests() {}

    public static void init() {}

    private static void fog(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // Eve's default air is 5 atm: 5 * 5 * 0.002
        near(errors, "eve fog density", SkyMath.fogDensity((float) Atmospheres.pressure("eve")), 0.05, 1e-7);
        near(errors, "2 atm fog density (none)", SkyMath.fogDensity(2.0F), 0.0, 0);
        near(errors, "kerbin fog density (none)", SkyMath.fogDensity((float) Atmospheres.pressure("kerbin")), 0.0, 0);
        // GL_EXP: 1 - e^(-0.05 * 20)
        near(errors, "eve fog at 20 blocks", SkyMath.exponentialFog(0.05, 20), 0.6321205588285577, 1e-12);
        near(errors, "eve fog at 60 blocks", SkyMath.exponentialFog(0.05, 60), 0.950212931632136, 1e-12);
        // the linear stand-in: from the eye to ln 20 / density, where GL_EXP is 95 % opaque
        near(errors, "eve linear fog end", SkyMath.linearFogEnd(0.05F), 59.914645471079815, 1e-4);
        near(errors, "GL_EXP at the linear end", SkyMath.exponentialFog(0.05, SkyMath.linearFogEnd(0.05F)), 0.95, 1e-6);
        near(errors, "eve linear fog at 20 blocks", SkyMath.linearFog(SkyMath.linearFogEnd(0.05F), 20), 0.3338082, 1e-6);
        near(errors, "duna storm 1.0 linear end", SkyMath.linearFogEnd(0.075F), 39.94309698071988, 1e-4);
        near(errors, "laythe water linear end", SkyMath.linearFogEnd(SkyMath.LAYTHE_WATER_FOG_DENSITY), 99.85774245179969, 1e-3);
        near(errors, "duna storm 0.24 (none)", SkyMath.dustStormFogDensity(0.24F), -1.0, 0);
        near(errors, "duna storm 1.0", SkyMath.dustStormFogDensity(1.0F), 0.075, 1e-7);
        near(errors, "duna storm 0.5", SkyMath.dustStormFogDensity(0.5F), 0.01875, 1e-7);
        near(errors, "laythe water", SkyMath.LAYTHE_WATER_FOG_DENSITY, 0.03, 1e-7);
        finish(helper, errors);
    }

    private static void rain(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // water 0x3333FF: luminance 0.2912, each channel 35 % of the way out from it
        colour(errors, "water", SkyMath.weatherColor(0x3333FF), 0.259280, 0.259280, 0.539280);
        colour(errors, "mercury", SkyMath.weatherColor(0x808080), 0.501961, 0.501961, 0.501961);
        colour(errors, "eve air", SkyMath.weatherColor(0xDCABF8), 0.797564, 0.730309, 0.835995);
        if (Fluids.WATER.getColor() != 0x3333FF) errors.add("water's colour is " + Integer.toHexString(Fluids.WATER.getColor()) + ", 1.7 0x3333FF");
        if (Fluids.MERCURY.getColor() != 0x808080) errors.add("mercury's colour is " + Integer.toHexString(Fluids.MERCURY.getColor()) + ", 1.7 0x808080");
        CBT_Water eve = CelestialTraits.getTrait("eve", CBT_Water.class, false);
        if (eve == null || eve.fluid != Fluids.MERCURY) errors.add("Eve's liquid is not mercury: " + (eve == null ? null : eve.fluid));
        CBT_Water kerbin = CelestialTraits.getTrait("kerbin", CBT_Water.class, false);
        if (kerbin == null || kerbin.fluid != Fluids.WATER) errors.add("Kerbin's liquid is not water: " + (kerbin == null ? null : kerbin.fluid));
        finish(helper, errors);
    }

    private static void nuke(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        shock(errors, NukeShocks.shock(100, -50, 1000, 70), 0.53125F, 0.28125F, 0.5F);
        shock(errors, NukeShocks.shock(0, 0, 24000, 175), 0.15625F, 0.21875F, 1.0F);
        shock(errors, NukeShocks.shock(-1234.5, 987.25, 123456, 10), 0.53125F, 0.59375F, 0.35F);
        finish(helper, errors);
    }

    private static void eve(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        near(errors, "before the strike", SkyMath.eveFlashStrength(49.134518F), 0.0, 0);
        near(errors, "rising", SkyMath.eveFlashStrength(49.254517F), 0.4999827, 1e-5);
        near(errors, "peak", SkyMath.eveFlashStrength(49.324516F), 1.0, 1e-6);
        near(errors, "fading", SkyMath.eveFlashStrength(51.184517F), 0.6763107, 1e-5);
        near(errors, "almost gone", SkyMath.eveFlashStrength(54.08452F), 0.0012630224, 1e-5);
        near(errors, "window 0 has no strike", SkyMath.eveFlashStrength(3.3F), 0.0, 0);
        near(errors, "window 12 has no strike", SkyMath.eveFlashStrength(100.25F), 0.0, 0);
        finish(helper, errors);
    }

    private static void style(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        int[][] styles = {{500, 2}, {301, 2}, {300, 1}, {100, 1}, {60, 1}, {50, 1}, {49, 0}, {10, 0}, {0, 0}};
        for (int[] s : styles) {
            int got = SkyMath.atmosphereStyle(s[0] / 100.0F);
            if (got != s[1]) errors.add(s[0] / 100.0F + " atm has style " + got + ", 1.7 " + s[1]);
        }
        if (SkyMath.atmosphereStyle((float) Atmospheres.pressure("eve")) != 2) errors.add("Eve is not haze");
        if (SkyMath.atmosphereStyle((float) Atmospheres.pressure("laythe")) != 1) errors.add("Laythe has no clouds");
        if (SkyMath.atmosphereStyle((float) Atmospheres.pressure("duna")) != 0) errors.add("Duna is not clear");
        // raining + thundering 0.22 + 0.48; animated 0.5 * 0.35; the 0.9 cap
        near(errors, "storm", SkyMath.stormDarkness(true, true, 1, 1), 0.70, 1e-6);
        near(errors, "drizzle", SkyMath.stormDarkness(false, false, 0.5F, 0), 0.175, 1e-6);
        near(errors, "rain only", SkyMath.stormDarkness(true, false, 0, 0), 0.22, 1e-6);
        finish(helper, errors);
    }

    private static void shock(List<String> errors, NukeShocks.Shock shock, float cx, float cy, float strength) {
        if (shock.centerX() != cx || shock.centerY() != cy || Math.abs(shock.strength() - strength) > 1e-6) {
            errors.add("shock " + shock + ", 1.7 (" + cx + ", " + cy + ", " + strength + ")");
        }
    }

    private static void colour(List<String> errors, String what, float[] got, double r, double g, double b) {
        if (Math.abs(got[0] - r) > 1e-5 || Math.abs(got[1] - g) > 1e-5 || Math.abs(got[2] - b) > 1e-5) {
            errors.add(what + ": (" + got[0] + ", " + got[1] + ", " + got[2] + "), 1.7 (" + r + ", " + g + ", " + b + ")");
        }
    }

    private static void near(List<String> errors, String what, double got, double expected, double tolerance) {
        if (Math.abs(got - expected) > tolerance) errors.add(what + ": " + got + ", 1.7 " + expected);
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (errors.isEmpty()) {
            helper.succeed();
        } else {
            helper.fail(String.join("; ", errors));
        }
    }
}
