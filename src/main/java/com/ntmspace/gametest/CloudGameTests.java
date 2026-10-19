package com.ntmspace.gametest;

import com.ntmspace.world.Atmospheres;
import com.ntmspace.world.CloudLayers17;
import dev.golem.core.client.Clouds;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The bodies' clouds (hbm-space {@code CloudProviderCelestial} / {@code WorldProviderCelestial.getCloudLayerCount}), the
 * pure part that {@code client/SpaceClouds} hands to Golem Core's {@link Clouds}. Expected values are computed by hand
 * from the 1.7 formulas (the GameTest server has no body levels and no client, so the air is each body's default).
 */
public final class CloudGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** Layers per body (default air) and the pressure thresholds 0.5 / 2.5 / 5 atm. */
    public static final GameTests.Function COUNTS = TESTS.function("clouds_layer_counts", 1, () -> CloudGameTests::counts);

    /** The air's tint on white clouds: Eve purple, Laythe's xenon, Tekto green; earth and Duna air neutral. */
    public static final GameTests.Function TINT = TESTS.function("clouds_atmosphere_tint", 1, () -> CloudGameTests::tint);

    /** 1.7 {@code drawCloudsBody}: noon, midnight, rain and thunder. */
    public static final GameTests.Function BASE = TESTS.function("clouds_base_colour", 1, () -> CloudGameTests::base);

    /** Eve's three layers: heights, alphas, colour, per-layer offsets and drift. */
    public static final GameTests.Function LAYERS = TESTS.function("clouds_eve_layers", 1, () -> CloudGameTests::layers);

    private CloudGameTests() {}

    public static void init() {}

    private static void counts(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> expected = Map.of("eve", 3, "kerbin", 1, "laythe", 1, "tekto", 1, "duna", 0, "mun", 0,
                "minmus", 0, "ike", 0, "dres", 0, "moho", 0);
        expected.forEach((body, count) -> {
            int got = CloudLayers17.layerCount(Atmospheres.of(body));
            if (got != count) errors.add(body + " has " + got + " layer(s), 1.7 gives " + count);
        });
        double[][] thresholds = {{0.49, 0}, {0.5, 1}, {2.49, 1}, {2.5, 2}, {4.99, 2}, {5.0, 3}, {40.0, 3}};
        for (double[] t : thresholds) {
            int got = CloudLayers17.layerCount(List.of(new Atmospheres.Entry(Atmospheres.DUNAAIR, t[0])));
            if (got != (int) t[1]) errors.add(t[0] + " atm gives " + got + " layer(s), expected " + (int) t[1]);
        }
        finish(helper, errors);
    }

    private static void tint(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // Eve air (53, 32, 74) normalised to blue, x 0.9 (white's luma), 65 % of the way from white
        check(errors, "eve", CloudLayers17.tinted(Atmospheres.of("eve"), 1, 1, 1), 0.768986, 0.602973, 0.935);
        // Laythe: xenon 0.15 of 0.6 atm -> 25 %, xenon (186, 69, 232)
        check(errors, "laythe", CloudLayers17.tinted(Atmospheres.of("laythe"), 1, 1, 1), 0.930388, 0.816918, 0.975);
        // Tekto air (36, 95, 70), all of the air -> capped at 65 %
        check(errors, "tekto", CloudLayers17.tinted(Atmospheres.of("tekto"), 1, 1, 1), 0.571684, 0.935, 0.781053);
        check(errors, "kerbin", CloudLayers17.tinted(Atmospheres.of("kerbin"), 1, 1, 1), 1, 1, 1);
        check(errors, "duna air", CloudLayers17.tinted(List.of(new Atmospheres.Entry(Atmospheres.DUNAAIR, 1.0)), 1, 1, 1), 1, 1, 1);
        if (CloudLayers17.tintData(Atmospheres.of("mun")) != null) errors.add("the Mun has a cloud tint");
        finish(helper, errors);
    }

    private static void base(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        check(errors, "noon", toDouble(CloudLayers17.baseColor(0.0F, 0, 0)), 1, 1, 1);
        // cos(pi) * 2 + 0.5 < 0 -> 0: r, g * 0.1, b * 0.15
        check(errors, "midnight", toDouble(CloudLayers17.baseColor(0.5F, 0, 0)), 0.1, 0.1, 0.15);
        // rain 1: grey 0.6, keep 0.05 -> 0.05 + 0.6 * 0.95 = 0.62
        check(errors, "rain", toDouble(CloudLayers17.baseColor(0.0F, 1, 0)), 0.62, 0.62, 0.62);
        // thunder 1 after that: grey 0.62 * 0.2 = 0.124 -> 0.62 * 0.05 + 0.124 * 0.95 = 0.1488
        check(errors, "thunder", toDouble(CloudLayers17.baseColor(0.0F, 1, 1)), 0.1488, 0.1488, 0.1488);
        finish(helper, errors);
    }

    private static void layers(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        float[] white = {1, 1, 1};
        List<Clouds.Layer> at0 = CloudLayers17.layers(Atmospheres.of("eve"), "eve", 0.0, white);
        List<Clouds.Layer> at100 = CloudLayers17.layers(Atmospheres.of("eve"), "eve", 100.0, white);
        if (at0.size() != 3 || at100.size() != 3) {
            errors.add("Eve has " + at0.size() + " layer(s), expected 3");
            finish(helper, errors);
            return;
        }
        float[] heights = {128, 140, 152};
        float[] alphas = {0.8F, 0.8F * 0.84F, 0.8F * 0.76F};
        // getLayerPositionOffset with seed 0 (1.7 client) and Eve's dimension id 413018
        double[][] offsets = {{-2828.461786, 5914.320492}, {454.620177, -3331.173449}, {-4726.941925, -1910.818781}};
        double[] scroll = {1.0, 0.84, 0.7};
        for (int i = 0; i < 3; i++) {
            Clouds.Layer layer = at0.get(i);
            if (layer.height() != heights[i]) errors.add("layer " + i + " height " + layer.height());
            if (Math.abs(layer.alpha() - alphas[i]) > 1e-6) errors.add("layer " + i + " alpha " + layer.alpha());
            check(errors, "layer " + i, new double[] {layer.red(), layer.green(), layer.blue()}, 0.768986, 0.602973, 0.935);
            if (Math.abs(layer.shiftX() - offsets[i][0]) > 1e-5) errors.add("layer " + i + " x offset " + layer.shiftX());
            if (Math.abs(layer.shiftZ() - offsets[i][1]) > 1e-5) errors.add("layer " + i + " z offset " + layer.shiftZ());
            double drift = at100.get(i).shiftX() - layer.shiftX();
            double expected = 100.0 * (double) 0.03F * scroll[i];
            if (Math.abs(drift - expected) > 1e-6) errors.add("layer " + i + " drift " + drift + ", expected " + expected);
            if (at100.get(i).shiftZ() != layer.shiftZ()) errors.add("layer " + i + " drifts along z");
        }
        if (!CloudLayers17.layers(Atmospheres.of("mun"), "mun", 0.0, white).isEmpty()) errors.add("the Mun has clouds");
        finish(helper, errors);
    }

    private static double[] toDouble(float[] f) {
        return new double[] {f[0], f[1], f[2]};
    }

    private static void check(List<String> errors, String what, double[] got, double r, double g, double b) {
        if (Math.abs(got[0] - r) > 1e-5 || Math.abs(got[1] - g) > 1e-5 || Math.abs(got[2] - b) > 1e-5) {
            errors.add(what + ": (" + got[0] + ", " + got[1] + ", " + got[2] + "), expected (" + r + ", " + g + ", " + b + ")");
        }
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (errors.isEmpty()) {
            helper.succeed();
        } else {
            helper.fail(String.join("; ", errors));
        }
    }
}
