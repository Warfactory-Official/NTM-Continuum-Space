import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Lit ground on the bodies under the light map, per node. The bodies' light map is 1.7's formula on every node
 * ({@code Lightmaps.useClassicFormula}); where terrain samples it differs: 26.1 (and 1.7) at the texel centre, 1.20.1 and
 * 1.21.1 at a texel boundary (the average of two light levels, darker). Golem Core's overrides that centred the legacy
 * sampling (vanilla's {@code light.glsl} and Sodium's terrain shader, 2026-10-09) were removed by user decision on
 * 2026-10-11 (a vanilla shader include override risks clashing with other mods and shader packs), so the legacy nodes
 * draw vanilla's darker sampling and this scenario pins that instead of parity.
 *
 * <p>For Minmus, Duna, Moho and the Mun (no weather of their own; the Mun last, since right after the first
 * teleport a Fabric client sometimes still showed the platform half lit), at {@code /ntmspace skytime noon} and
 * {@code sunset}: {@code <body>-<phase>-ground} looks straight down at a smooth-stone platform (sky light 15). On the
 * Mun at noon {@code mun-noon-shade} looks at the floor under an 11 x 11 roof two blocks up (sky light 14 at the rim
 * down to about 9: the interpolated levels). The sky's orbits are pinned to one world time ({@code ntmspace_sky
 * pin_ticks}) as in {@code SkiesWeather}. Positive signals: the mean luma of the centre half of each ground shot
 * (logged as {@code lightmap-luma}, written to {@code lightmap-luma.txt}); each value is the era's measured one within
 * {@link #TOLERANCE} ({@link #NOON_26}/{@link #SUNSET_26}/{@link #MUN_SHADE_26} on 26.1, the {@code _LEGACY} tables on
 * 1.20.1/1.21.1; measured legacy/26.1: noon 1.007, sunset 0.950, shade 0.975); on each body sunset is darker than
 * noon, and the Mun's shade is darker than its open ground.
 */
public final class SpaceLightmapParity implements Scenario {

    private static final int Y = 200;
    private static final String[] BODIES = {"minmus", "duna", "moho", "mun"};

    // Mean luma (0..255) of the centre half of each ground shot, in BODIES order, measured 2026-10-11 with the overrides
    // gone (harness default Sodium/Embeddium on; 26.1 NeoForge and Fabric identical, 1.20.1 Forge and Fabric within
    // 0.1 %). Noon at full sky light barely differs (legacy/26.1 = 1.007: the light map is near white at 14 and 15);
    // sunset (0.950) and the shade (0.975), where the boundary sample mixes in a darker level, do. 1.21.1 was not
    // measured and is held to the 1.20.1 values (same vanilla sampling) - re-measure there if it fails.
    static final double[] NOON_26 = {154.912, 155.445, 154.912, 154.912};
    static final double[] NOON_LEGACY = {155.977, 155.977, 155.977, 155.977};
    static final double[] SUNSET_26 = {119.498, 119.974, 119.498, 119.498};
    static final double[] SUNSET_LEGACY = {113.567, 113.567, 113.567, 113.567};
    static final double MUN_SHADE_26 = 111.327;
    static final double MUN_SHADE_LEGACY = 108.592;
    /** Relative tolerance of the luma checks. */
    static final double TOLERANCE = 0.02;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:resistance 1000 255 true");
        c.query("ntmspace_sky", jsonOf("pin_ticks", 13_600));
        String hideGui = null;
        try {
            hideGui = c.setOption("hide_gui", "true");
            boolean legacy = !c.node().startsWith("26.");
            StringBuilder log = new StringBuilder();
            java.util.List<String> failures = new java.util.ArrayList<>();
            for (int b = 0; b < BODIES.length; b++) {
                String body = BODIES[b];
                double noon = Double.NaN;
                c.cmd("ntmspace tp " + body);
                c.waitTicks(40);
                Expect.equal("ntmspace:" + body, c.status().get("dimension").getAsString(), "dimension after tp " + body);
                c.teleport(0.5, Y + 1, 0.5);
                c.waitTicks(10);
                Fixtures.platform(c, 0, Y, 0, 12, "minecraft:smooth_stone");
                boolean roof = body.equals("mun");
                if (roof) c.cmd("fill 2 " + (Y + 3) + " -5 12 " + (Y + 3) + " 5 minecraft:stone");
                c.cmd("gamemode spectator");
                // the light engine and the chunk meshes settle (the first body's terrain is still lighting the platform)
                c.waitTicks(100);
                for (String phase : new String[] {"noon", "sunset"}) {
                    c.cmd("ntmspace skytime " + phase);
                    c.teleport(-4.5, Y + 6, 0.5);
                    c.look(0, 90);
                    c.waitTicks(30);
                    double luma = meanLuma(c.screenshot(body + "-" + phase + "-ground"));
                    String line = String.format(java.util.Locale.ROOT, "lightmap-luma %s %s-%s ground=%.3f", c.node(), body, phase, luma);
                    System.out.println(line);
                    log.append(line).append(System.lineSeparator());
                    boolean atNoon = phase.equals("noon");
                    double expected = atNoon ? (legacy ? NOON_LEGACY[b] : NOON_26[b]) : (legacy ? SUNSET_LEGACY[b] : SUNSET_26[b]);
                    check(failures, body + " " + phase + " ground", luma, expected);
                    if (atNoon) {
                        noon = luma;
                    } else if (luma >= noon) {
                        failures.add(body + " sunset ground " + luma + " not darker than noon " + noon);
                    }
                    if (roof && phase.equals("noon")) {
                        c.teleport(3.5, Y + 1.0, 0.5); // eye 1.62 above the floor, under the roof
                        c.lookAt(7.5, Y + 1.0, 0.5);
                        c.waitTicks(30);
                        double shade = meanLuma(c.screenshot("mun-noon-shade"));
                        log.append(String.format(java.util.Locale.ROOT, "lightmap-luma %s mun-noon-shade=%.3f%n", c.node(), shade));
                        if (shade >= noon) failures.add("mun shade " + shade + " not darker than its ground " + noon);
                        check(failures, "mun noon shade", shade, legacy ? MUN_SHADE_LEGACY : MUN_SHADE_26);
                    }
                }
                c.cmd("gamemode creative");
                if (roof) c.cmd("fill 2 " + (Y + 3) + " -5 12 " + (Y + 3) + " 5 minecraft:air");
            }
            java.nio.file.Files.writeString(c.screenshotDir().resolve("lightmap-luma.txt"), log.toString());
            Expect.that(failures.isEmpty(), "lit ground on " + c.node() + ": " + failures + System.lineSeparator() + log);
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.query("ntmspace_sky", jsonOf("pin_ticks", -1));
            c.cmd("execute in minecraft:overworld run tp @s 0.5 101 0.5");
        }
    }

    private static JsonObject jsonOf(String key, int value) {
        JsonObject o = new JsonObject();
        o.addProperty(key, value);
        return o;
    }

    private static void check(java.util.List<String> failures, String what, double luma, double expected) {
        if (Math.abs(luma / expected - 1.0) > TOLERANCE) {
            failures.add(what + " " + luma + ", expected " + expected + " +-" + TOLERANCE * 100 + " %");
        }
    }

    /** Mean Rec. 601 luma (0..255) of the centre half (both axes) of a screenshot. */
    static double meanLuma(java.nio.file.Path png) throws java.io.IOException {
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(png.toFile());
        int w = image.getWidth(), h = image.getHeight();
        double sum = 0;
        long n = 0;
        for (int y = h / 4; y < h * 3 / 4; y++) {
            for (int x = w / 4; x < w * 3 / 4; x++) {
                int rgb = image.getRGB(x, y);
                sum += 0.299 * ((rgb >> 16) & 255) + 0.587 * ((rgb >> 8) & 255) + 0.114 * (rgb & 255);
                n++;
            }
        }
        return sum / n;
    }
}
