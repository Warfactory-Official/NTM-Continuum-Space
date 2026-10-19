package com.ntmspace.gametest;

import com.ntmspace.world.StardarMath;
import com.ntmspace.world.ThatmoBattlefield;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The pure maths of the StarDar planet layers ({@code GUIMachineStardar.drawBodyOverlays},
 * {@code AtmosphereRenderUtil.renderAtmosphereGlow2D}) and of Thatmo's battlefield sky ({@code WorldProviderThatmo}'s
 * client update). Expected values are hand-computed from the 1.7 code, the state machine's in IEEE single precision
 * (the session scratch {@code m3gui/sim.py} restates 1.7's float arithmetic with numpy float32).
 */
public final class StardarThatmoGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** {@code renderAtmosphereGlow2D}: the band's 32 vertices, sizes and alphas, Kerbin-air colour clamped at 1.15x. */
    public static final GameTests.Function GLOW = TESTS.function("stardar_glow_band", 1, () -> StardarThatmoGameTests::glow);

    /** The StarDar layer inputs: the body quad, the pattern offset, light intensity, blackouts and the lava fade. */
    public static final GameTests.Function LAYERS = TESTS.function("stardar_layer_values", 1, () -> StardarThatmoGameTests::layers);

    /** Thatmo's railgun flash: 61 ticks to 100, one fire-flash tick, then the charge restarts; the impact sound at 285. */
    public static final GameTests.Function FLASH = TESTS.function("thatmo_battlefield_flash", 1, () -> StardarThatmoGameTests::flash);

    /** Thatmo's shield hit between charge 300 and 430. */
    public static final GameTests.Function SHIELD = TESTS.function("thatmo_battlefield_shield", 1, () -> StardarThatmoGameTests::shield);

    /** Thatmo's meteors: one a tick, 74 in the sky, 4,366 smoke puffs once settled; the sky terms. */
    public static final GameTests.Function METEORS = TESTS.function("thatmo_battlefield_meteors", 1, () -> StardarThatmoGameTests::meteors);

    private StardarThatmoGameTests() {}

    public static void init() {}

    private static void glow(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // 1 atm: glow 0.08 + 1 * 0.16 = 0.24; size 20 at (100, 50): radius 10, inner 9.8, middle 10.75, outer 11.5 * 1.06
        float[] v = StardarMath.glowBand(100, 50, 20, 0.24F, 0.7529412, 0.84705883, 1.0);
        if (v == null || v.length != 32 * 7) {
            errors.add("glow band has " + (v == null ? "no" : v.length / 7) + " vertices, 1.7 32");
        } else {
            vertex(errors, "top 0", v, 0, 100 - 12.19, 50 - 12.19, 0);
            vertex(errors, "top 2", v, 2, 110.75, 39.25, 0.12);
            vertex(errors, "top 3", v, 3, 89.25, 39.25, 0.12);
            vertex(errors, "top 6", v, 6, 109.8, 40.2, 0.24);
            vertex(errors, "left 9", v, 9, 112.19, 62.19, 0);
            vertex(errors, "left 14", v, 14, 109.8, 59.8, 0.24);
            vertex(errors, "bottom 19", v, 19, 110.75, 60.75, 0.12);
            vertex(errors, "right 31", v, 31, 90.2, 59.8, 0.24);
            near(errors, "red", v[3], 0.7529412 * 1.15, 1e-6);
            near(errors, "green", v[4], 0.84705883 * 1.15, 1e-6);
            near(errors, "blue (clamped)", v[5], 1.0, 0);
        }
        if (StardarMath.glowBand(0, 0, 10, 0.001F, 1, 1, 1) != null) errors.add("a glow of 0.001 is drawn (1.7: none)");
        finish(helper, errors);
    }

    private static void vertex(List<String> errors, String what, float[] v, int i, double x, double y, double a) {
        near(errors, what + " x", v[i * 7], x, 1e-4);
        near(errors, what + " y", v[i * 7 + 1], y, 1e-4);
        near(errors, what + " alpha", v[i * 7 + 6], a, 1e-6);
    }

    private static void layers(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        float[] q = StardarMath.texturedQuad(10, 20, 8, 0.25F);
        float[] expected = {6, 24, 0, 0.25F, 1, 14, 24, 0, 1.25F, 1, 14, 16, 0, 1.25F, 0, 6, 16, 0, 0.25F, 0};
        if (!java.util.Arrays.equals(q, expected)) errors.add("drawTexturedQuad " + java.util.Arrays.toString(q));
        float[] p = StardarMath.partialTex(1, 2, 3, 4, 0.1F, 0.2F, 0.3F, 0.4F);
        float[] pe = {1, 6, 0, 0.1F, 0.4F, 4, 6, 0, 0.3F, 0.4F, 4, 2, 0, 0.3F, 0.2F, 1, 2, 0, 0.1F, 0.2F};
        if (!java.util.Arrays.equals(p, pe)) errors.add("drawPartialTex " + java.util.Arrays.toString(p));
        near(errors, "pattern offset of a drawn-offset body", StardarMath.patternOffset(false, 0.25F, 2400, 6000), 2.5, 1e-6);
        near(errors, "pattern offset of a turned body", StardarMath.patternOffset(true, 0.25F, 2400, 6000), 0.25, 0);
        near(errors, "pattern offset without a period", StardarMath.patternOffset(false, 0.25F, 0, 6000), 0.25, 0);
        near(errors, "pattern offset, infinite period", StardarMath.patternOffset(false, 0.25F, Double.POSITIVE_INFINITY, 6000), 0.25, 0);
        near(errors, "lights 7 of 4 levels", StardarMath.lightIntensity(true, 7, 0, 4), 3, 0);
        near(errors, "lights 39.9 ticks after an impact", StardarMath.lightIntensity(true, 2, 39.9, 4), 2, 0);
        near(errors, "lights 40 ticks after an impact", StardarMath.lightIntensity(true, 2, 40, 4), 0, 0);
        near(errors, "no lights trait", StardarMath.lightIntensity(false, 3, 0, 4), 0, 0);
        near(errors, "blackouts at 0", StardarMath.blackouts(0), 0, 0);
        near(errors, "blackouts at 7.9", StardarMath.blackouts(7.9), 0, 0);
        near(errors, "blackouts at 8", StardarMath.blackouts(8), 1, 0);
        near(errors, "blackouts at 100", StardarMath.blackouts(100), 5, 0);
        near(errors, "blackouts before the impact", StardarMath.blackouts(-20), 0, 0);
        near(errors, "lava at 5 ticks", StardarMath.lavaAlpha(5), 0.5, 1e-6);
        near(errors, "lava at 20 ticks", StardarMath.lavaAlpha(20), 1, 0);
        near(errors, "lava at 0 ticks", StardarMath.lavaAlpha(0), 0, 0);
        finish(helper, errors);
    }

    private static void flash(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ThatmoBattlefield b = new ThatmoBattlefield();
        b.chargetime = 1001;
        int sounds = b.tick(new Random(1), () -> 0.5, 0, 0);
        near(errors, "flash after 1 tick", b.flashd, 4.7865005, 1e-6);
        if (sounds != ThatmoBattlefield.SOUND_FIREFLASH) errors.add("first flash tick sounds " + sounds + ", 1.7 the fire flash");
        b.tick(new Random(1), () -> 0.5, 0, 0);
        near(errors, "flash after 2 ticks", b.flashd, 9.357609, 1e-5);
        near(errors, "flash alpha after 2 ticks", b.flashAlpha(), 1 - 9.357609 / 100, 1e-5);
        int ticks = 2, fireFlashes = 1;
        while (b.chargetime != 0 && ticks < 500) {
            if ((b.tick(new Random(1), () -> 0.5, 0, 0) & ThatmoBattlefield.SOUND_FIREFLASH) != 0) fireFlashes++;
            ticks++;
        }
        near(errors, "ticks until the flash ends", ticks, 62, 0);
        near(errors, "fire-flash sounds", fireFlashes, 1, 0);
        near(errors, "flash at its end", b.flashd, 100, 0);
        near(errors, "flash alpha at its end", b.flashAlpha(), 0, 0);
        b.tick(new Random(1), () -> 0.5, 0, 0);
        near(errors, "charge restarts", b.chargetime, 1, 0);
        near(errors, "flash cleared while charging", b.flashd, 0, 0);
        ThatmoBattlefield c = new ThatmoBattlefield();
        c.chargetime = 284;
        near(errors, "impact sound at charge 285", c.tick(new Random(1), () -> 0.5, 0, 0), ThatmoBattlefield.SOUND_IMPACT, 0);
        finish(helper, errors);
    }

    private static void shield(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ThatmoBattlefield b = new ThatmoBattlefield();
        b.chargetime = 299;
        for (int i = 1; i <= 20; i++) {
            b.tick(new Random(1), () -> 0.5, 0, 0);
            if (i == 13) {
                near(errors, "beam scale after 13 ticks", b.scale, 19.5, 0);
                near(errors, "no shield before the beam is grown", b.shield, 0, 0);
            }
            if (i == 14) {
                near(errors, "beam scale capped", b.scale, 20, 0);
                near(errors, "shield after 14 ticks", b.shield, 2.2450001, 1e-6);
                near(errors, "nmass after 14 ticks", b.nmass, 5.4, 1e-6);
                near(errors, "shielde after 14 ticks", b.shielde, 0.90000004, 1e-6);
                near(errors, "csyw after 14 ticks", b.csyw, 3.1940002, 1e-6);
                near(errors, "cooldown after 14 ticks", b.cooldown, 0.5, 0);
            }
        }
        near(errors, "charge after 20 ticks", b.chargetime, 319, 0);
        near(errors, "shield after 20 ticks", b.shield, 12.589415, 1e-4);
        near(errors, "nmass after 20 ticks", b.nmass, 34.56309, 1e-4);
        near(errors, "shielde after 20 ticks", b.shielde, 5.2728367, 1e-4);
        near(errors, "csyw after 20 ticks", b.csyw, 20.443426, 1e-4);
        near(errors, "cooldown after 20 ticks", b.cooldown, 3.5, 0);
        near(errors, "shield glow after 20 ticks", b.shieldAlpha(), 0.5 - 20.443426 / 100, 1e-5);
        b.chargetime = 431;
        b.tick(new Random(1), () -> 0.5, 0, 0);
        if (b.scale != 0 || b.shield != 0 || b.csyw != 0) errors.add("the shield did not reset after charge 430");
        finish(helper, errors);
    }

    private static void meteors(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ThatmoBattlefield b = new ThatmoBattlefield();
        Random rand = new Random(7);
        int[][] counts = {{1, 1, 0}, {2, 2, 1}, {3, 3, 3}, {74, 74, 2596}, {150, 74, 4366}};
        int tick = 0;
        for (int[] expect : counts) {
            while (tick < expect[0]) {
                b.tick(rand, () -> 0.25, 1000, -2000);
                tick++;
            }
            near(errors, "meteors after " + tick + " ticks", b.meteors.size(), expect[1], 0);
            near(errors, "smoke after " + tick + " ticks", b.smoke.size(), expect[2], 0);
        }
        ThatmoBattlefield.Meteor newest = b.meteors.get(b.meteors.size() - 1);
        if (newest.posY != 2017 || newest.posX < 1000 - 8000 || newest.posX >= 1000 + 8000 || newest.posZ < -2000 - 8000
                || newest.posZ >= -2000 + 8000) {
            errors.add("new meteor at " + newest.posX + ", " + newest.posY + ", " + newest.posZ);
        }
        ThatmoBattlefield.Meteor oldest = b.meteors.get(0);
        near(errors, "oldest meteor height", oldest.posY, 2017 - 20.8 * 73, 1e-6);
        if (!b.fragments.isEmpty()) errors.add("fragments are never made in 1.7");
        // altitude climbs 5 a tick and wraps at 400 with a new randPos
        near(errors, "altitude after 150 ticks", b.altitude, (150 % 80) * 5, 0);
        near(errors, "randPos", b.randPos, 0.25, 0);
        ThatmoBattlefield s = new ThatmoBattlefield();
        s.flashd = 25;
        near(errors, "flash term at 25", s.flashAlpha(), 0.75, 0);
        s.csyw = 10;
        near(errors, "shield term at 10", s.shieldAlpha(), 0.4, 1e-6);
        s.csyw = 60;
        near(errors, "shield term at 60", s.shieldAlpha(), 0, 0);
        finish(helper, errors);
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
