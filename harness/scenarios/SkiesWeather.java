import dev.golem.harness.api.*;

/**
 * NTM: Space's skies and weather on a real client (hbm-space {@code SkyProviderCelestial}, {@code WorldProviderCelestial}
 * and its per-body providers, {@code CBT_Weather}, {@code WorldProviderDuna}'s dust storms):
 *
 * <ul>
 *   <li>Every body's sky at noon, just before sunset and at midnight ({@code /ntmspace skytime}), looking west at the
 *       horizon and, at night, at the zenith: {@code <body>-noon}, {@code <body>-sunset}, {@code <body>-night},
 *       {@code <body>-zenith}. A human checks the sun, the sunset fan (blue on Duna, green on Eve, none in vacuum but the
 *       airless charged-dust specks on the Mun, Minmus, Ike, Dres and Moho), the atmosphere glow around Kerbin, Duna,
 *       Eve, Laythe and Jool, Sarnus' rings from Tekto, and Dres' own rings over its horizon. {@code <body>-sunset-ground}
 *       looks down at the platform: its light is 1.7's light map from the body's sun brightness (Golem
 *       {@code Lightmaps.useClassicFormula} on 26.1). The body's weather is cleared first and Eve's shots are taken
 *       between lightning flashes, so every node shoots the same sky.</li>
 *   <li>Eve's weather ({@code /ntmspace weather thunder}): the level's rain and thunder follow the body's weather trait
 *       (query {@code ntmspace_sky}: rain and thunder above 0.6), then a lightning flash ({@code eve_flash} above 0.3, 1.7's
 *       hash schedule, up to a minute): {@code eve-lightning} (the rumble plays then). Then {@code /ntmspace weather clear}.</li>
 *   <li>Duna's dust storm ({@code /ntmspace duststorm 1}): the client's eased strength passes 0.7 in 200 ticks and the fog
 *       thickens (fog density above 0.03): {@code duna-storm-horizon}, {@code duna-storm-down}. Then calm again.</li>
 *   <li>Laythe: the camera in water uses 1.7's clearer water fog (density 0.03): {@code laythe-water}; at midnight the
 *       ground under Jool's light map tint: {@code laythe-night-ground}.</li>
 *   <li>Clouds ({@code CloudProviderCelestial} through Golem {@code Clouds}, fancy clouds switched on for the check):
 *       Eve's three purple layers, Laythe's and Tekto's one tinted layer, none on the Mun and on Duna (0.1 atm), one white
 *       layer on a terraformed Duna; query {@code ntmspace_clouds} plus {@code <body>-clouds-below/-horizon/-above}. Kerbin
 *       keeps vanilla's clouds.</li>
 *   <li>Kerbin at night: a mapper satellite launched on frequency 4711 and a station launched over Kerbin show in the
 *       client's sky cache (query {@code ntmspace_sky}: satellite 4711 at 100 km, at least one station) and in the sky with
 *       the "Bobmazon" delivery satellite: {@code kerbin-satellites-*}. Holding a satellite shows its orbit line:
 *       {@code kerbin-held-satellite}.</li>
 *   <li>The shader-drawn sky layers (hbm-space's GLSL through Golem {@code Sky.shaderQuads}), each with a positive
 *       signal - the layer's Golem draw counter grows while it should show (query {@code ntmspace_sky_shaders},
 *       {@code draws}) - and screenshots: from the Mun, Kerbin's clouds ({@code sky_atmosphere}) at
 *       {@code mun-kerbin-*}; its city lights and their glow ({@code /ntmspace skytrait lights kerbin 20000}:
 *       {@code sky_nightlights}, {@code sky_atmosphere_emissive}) at {@code mun-kerbin-lights-*}; lightning in its clouds
 *       while Kerbin thunders ({@code sky_lightning}) at {@code mun-kerbin-lightning-*}; a nuclear blast's flash and ring
 *       ({@code /ntmspace skytrait nuke kerbin 140}: one shock on the client, {@code sky_nuke}) at
 *       {@code mun-kerbin-nuke-*}; the black hole of a destroyed Kerbol ({@code sky_blackhole}) at {@code mun-blackhole*};
 *       the flesh sky of an infected Mun ({@code sky_flesh}) at {@code mun-flesh-*}; Jool's gas bands from Laythe at
 *       {@code laythe-jool-*}. Each trait is removed again.</li>
 *   <li>Tinted rain (hbm-space {@code WeatherProviderCelestial} through Golem {@code Precipitation}): on Eve in a
 *       thunderstorm Golem draws the level's weather ({@code weather_custom}, {@code weather_quads} growing) in mercury's
 *       grey (0.502, 0.502, 0.502): {@code eve-rain-tint}; on Kerbin in rain, water's desaturated blue
 *       (0.259, 0.259, 0.539): {@code kerbin-rain-tint}.</li>
 *   <li>Eve's fog: 1.7's {@code GL_EXP} of density 0.05 (5 atm) drawn as a linear fog from the eye to where the
 *       exponential one is 95 % opaque, {@code ln 20 / 0.05} = 59.9 blocks (query {@code fog_end}; Golem no longer
 *       overrides vanilla's fog include, user decision 2026-10-11): {@code eve-fog-linear-horizon},
 *       {@code eve-fog-linear-down}.</li>
 *   <li>Thatmo's own sky (hbm-space {@code SkyProviderThatmo}, query {@code ntmspace_thatmo}): the level reads Kerbin's
 *       traits (1.7 gave the body Thatmo no dimension), so it has no battlefield by default; with the battlefield trait on
 *       Kerbin ({@code /ntmspace skytrait battle kerbin}, cleared afterwards) the battle runs on the client (meteors and smoke pile up, the showers of orange beams fall) and the railgun's
 *       black silhouette is drawn ({@code model}, {@code frames}, {@code billboards} and {@code beam_quads} grow):
 *       {@code thatmo-battle-*}, {@code thatmo-railgun} (yaw 19, the silhouette) and {@code thatmo-ground} (the platform
 *       at a tenth of the sun's brightness, the cycle restarted so no flash lights it); the railgun fires (the query's
 *       {@code charge} test hook set to 1001): the flash grows and brightens the sky ({@code flashd},
 *       {@code light_add}, {@code flash}) at {@code thatmo-flash} (yaw -8, pitch -14) and {@code thatmo-flash-beam}; the
 *       shield hit (charge 299): the orange beam and the shield planes ({@code scale} 20, {@code shield_frames}) at
 *       {@code thatmo-shield} (yaw -142, pitch -43) and {@code thatmo-shield-beam} (yaw -96, pitch -27).</li>
 * </ul>
 *
 * Independent: clears Eve's and Kerbin's weather, Duna's storm and the sky traits it set, removes the frequency-4711
 * satellite's chip from the hand, and restores the GUI and time when done.
 */
public final class SkiesWeather implements Scenario {

    private static final int Y = 200;
    private static final String[] BODIES = {"mun", "minmus", "duna", "ike", "dres", "eve", "moho", "laythe", "tekto"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        // independent of the scenarios before it: alive, creative, in the overworld, no invasion left running on the Mun
        if (c.screenClass() != null && c.screenClass().contains("DeathScreen")) {
            c.waitTicks(30);
            c.clickWidget("Respawn");
            c.waitTicks(40);
        }
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { // a screen an earlier failure left open
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode creative");
        c.cmd("ntmwar invasion stop mun");
        c.cmd("execute in minecraft:overworld run tp @s 0.5 101 0.5");
        c.waitTicks(20);
        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:resistance 1000 255 true");
        c.markLog();
        // The bodies' places in the sky follow the world's age (1.7 getTotalWorldTime), which differs between the
        // nodes' saves (about 300 ticks on a fresh 1.20.1 snapshot, 13,500 on 26.1's): pin the time the client's sky
        // orbits use, so every node draws the same sky (the Mun inside the Thatmo shield view, as on 26.1's age).
        com.google.gson.JsonObject astro = c.query("ntmspace_sky", jsonOf("pin_ticks", SKY_TICKS)).getAsJsonObject("astro");
        Expect.that(astro.has("game_time"), "the sky query reports its astronomy: " + astro);
        try {
            skies(c);
        } finally {
            c.query("ntmspace_sky", jsonOf("pin_ticks", -1));
        }
    }

    /** The total world time the sky's orbits are pinned to for the whole scenario (see {@link #run}). */
    private static final int SKY_TICKS = 13_600;
    /** The Mun's {@code AstroMetric.angle} from Kerbin at {@link #SKY_TICKS}, Thatmo's midnight (measured on all nodes). */
    private static final double MUN_ANGLE = 128.434;

    private static void skies(Client c) throws Exception {
        // ---- every body's sky at three times of day
        for (String body : BODIES) {
            c.cmd("ntmspace tp " + body);
            c.waitTicks(40);
            Expect.equal("ntmspace:" + body, c.status().get("dimension").getAsString(), "dimension after /ntmspace tp " + body);
            c.teleport(0.5, Y + 1, 0.5);
            c.waitTicks(10);
            Fixtures.platform(c, 0, Y, 0, 4, "minecraft:smooth_stone");
            // Eve, Laythe and Tekto have a weather cycle of their own (CBT_Weather): its rain and thunder darken and fog
            // the sky, at whatever point of the cycle the node's save happens to be. Calm it before the shots.
            calmWeather(c, body);
            for (String phase : new String[] {"noon", "sunset", "night"}) {
                c.markLog();
                c.cmd("ntmspace skytime " + phase);
                c.waitTicks(10);
                Expect.that(!c.logSinceMark("space_skytime dim=ntmspace:" + body).isEmpty(), "skytime " + phase + " on " + body);
                c.setOption("hide_gui", "true");
                c.look(90, -15); // west
                c.waitTicks(5);
                calmShot(c, body, body + "-" + phase);
                if (phase.equals("sunset")) {
                    // the platform under the sunset sky: the light map's sky light (1.7's formula on every node)
                    c.look(90, 35);
                    c.waitTicks(5);
                    calmShot(c, body, body + "-sunset-ground");
                }
                if (phase.equals("night")) {
                    c.look(0, -90);
                    c.waitTicks(5);
                    calmShot(c, body, body + "-zenith");
                }
                c.setOption("hide_gui", "false");
            }
            if (body.equals("dres")) {
                // Dres' own rings arch over its horizon: all four directions at noon
                c.cmd("ntmspace skytime noon");
                c.setOption("hide_gui", "true");
                String[] names = {"south", "west", "north", "east"};
                for (int i = 0; i < 4; i++) {
                    c.look(i * 90, -5);
                    c.waitTicks(5);
                    c.screenshot("dres-rings-" + names[i]);
                }
                c.setOption("hide_gui", "false");
            }
        }

        // ---- Eve: the body's weather on its level, and a lightning flash
        c.cmd("ntmspace tp eve");
        c.waitTicks(40);
        c.teleport(0.5, Y + 1, 0.5);
        c.cmd("ntmspace skytime noon");
        c.cmd("ntmspace weather thunder 600");
        com.google.gson.JsonObject sky = null;
        for (int i = 0; i < 30; i++) {
            c.waitTicks(10);
            sky = c.query("ntmspace_sky", new com.google.gson.JsonObject());
            if (sky.get("rain").getAsFloat() > 0.6F && sky.get("thunder").getAsFloat() > 0.6F) break;
        }
        Expect.that(sky != null && sky.get("rain").getAsFloat() > 0.6F, "Eve's level rains with its weather trait: " + sky);
        Expect.that(sky.get("thunder").getAsFloat() > 0.6F, "Eve's level thunders: " + sky);
        c.setOption("hide_gui", "true");
        c.look(90, -10);
        c.waitTicks(5);
        c.screenshot("eve-thunderstorm");
        float flash = 0;
        for (int i = 0; i < 400 && flash <= 0.3F; i++) {
            c.waitTicks(3);
            flash = c.query("ntmspace_sky", new com.google.gson.JsonObject()).get("eve_flash").getAsFloat();
        }
        c.screenshot("eve-lightning");
        c.setOption("hide_gui", "false");
        Expect.that(flash > 0.3F, "an Eve lightning flash within a minute: " + flash);
        c.cmd("ntmspace weather clear 600");

        // ---- Duna: a dust storm
        c.cmd("ntmspace tp duna");
        c.waitTicks(40);
        c.teleport(0.5, Y + 1, 0.5);
        c.cmd("ntmspace skytime noon");
        c.cmd("ntmspace duststorm 1 6000");
        c.waitTicks(200);
        sky = c.query("ntmspace_sky", new com.google.gson.JsonObject());
        Expect.that(sky.get("dust_storm").getAsFloat() == 1.0F, "the client got Duna's storm strength: " + sky);
        Expect.that(sky.get("dust_storm_smoothed").getAsFloat() > 0.7F, "the client eased into the storm: " + sky);
        Expect.that(sky.get("fog_density").getAsFloat() > 0.03F, "the storm thickens the fog: " + sky);
        c.setOption("hide_gui", "true");
        c.look(90, 0);
        c.waitTicks(5);
        c.screenshot("duna-storm-horizon");
        c.look(90, 45);
        c.waitTicks(5);
        c.screenshot("duna-storm-down");
        c.setOption("hide_gui", "false");
        c.cmd("ntmspace duststorm 0 200000");

        // ---- Laythe: clear water fog and Jool's light at night
        c.cmd("ntmspace tp laythe");
        c.waitTicks(40);
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(10);
        Fixtures.platform(c, 0, Y, 0, 6, "minecraft:smooth_stone");
        c.cmd("fill -3 " + (Y + 1) + " -3 3 " + (Y + 4) + " 3 minecraft:glass hollow");
        c.cmd("fill -2 " + (Y + 1) + " -2 2 " + (Y + 3) + " 2 minecraft:water");
        c.teleport(0.5, Y + 1.2, 0.5);
        c.waitTicks(20);
        sky = c.query("ntmspace_sky", new com.google.gson.JsonObject());
        Expect.that(Math.abs(sky.get("fog_density").getAsFloat() - 0.03F) < 1e-6, "Laythe's water fog density is 0.03: " + sky);
        c.setOption("hide_gui", "true");
        c.look(90, 0);
        c.waitTicks(5);
        c.screenshot("laythe-water");
        c.setOption("hide_gui", "false");
        c.cmd("fill -3 " + (Y + 1) + " -3 3 " + (Y + 4) + " 3 minecraft:air");
        c.teleport(0.5, Y + 1, 0.5);
        c.cmd("ntmspace skytime night");
        c.waitTicks(30);
        c.setOption("hide_gui", "true");
        c.look(0, 60);
        c.waitTicks(5);
        c.screenshot("laythe-night-ground");
        c.setOption("hide_gui", "false");

        // ---- clouds: hbm-space CloudProviderCelestial through Golem Clouds (one renderer path per node, same layers)
        clouds(c);

        // ---- Kerbin at night: satellites and a station in the sky
        c.cmd("execute in minecraft:overworld run tp @s 0 " + (Y + 1) + " 0");
        c.waitTicks(40);
        com.google.gson.JsonObject kerbinClouds = c.query("ntmspace_clouds", new com.google.gson.JsonObject());
        Expect.that(!kerbinClouds.get("custom").getAsBoolean(), "Kerbin (the overworld) keeps vanilla's clouds: " + kerbinClouds);
        Fixtures.platform(c, 0, Y, 0, 4, "minecraft:smooth_stone");
        c.cmd("time set 18000");
        c.cmd("weather clear");
        c.cmd("item replace entity @s weapon.mainhand with hbm:satellite_spy");
        c.cmd("ntmsatellites orbit 4711");
        c.cmd("ntmstations launch 3 -2 kerbin");
        c.waitTicks(20);
        sky = c.query("ntmspace_sky", new com.google.gson.JsonObject());
        boolean found = false;
        for (com.google.gson.JsonElement e : sky.getAsJsonArray("satellites")) {
            com.google.gson.JsonObject o = e.getAsJsonObject();
            if (o.get("freq").getAsInt() == 4711 && o.get("type").getAsString().equals("mapper")
                    && Math.abs(o.get("altitude").getAsFloat() - 100F) < 1e-3) found = true;
        }
        Expect.that(found, "the client's sky cache has the mapper on 4711 at 100 km: " + sky);
        Expect.that(sky.get("stations").getAsInt() >= 1, "the client's sky cache has Kerbin's station: " + sky);
        c.setOption("hide_gui", "true");
        String[] names = {"south", "west", "north", "east"};
        for (int i = 0; i < 4; i++) {
            c.look(i * 90, -45);
            c.waitTicks(5);
            c.screenshot("kerbin-satellites-" + names[i]);
        }
        c.look(0, -90);
        c.waitTicks(5);
        c.screenshot("kerbin-satellites-zenith");
        c.setOption("hide_gui", "false");
        c.cmd("item replace entity @s weapon.mainhand with hbm:satellite_spy");
        c.waitTicks(5);
        c.look(0, -60);
        c.waitTicks(5);
        c.screenshot("kerbin-held-satellite");
        c.cmd("item replace entity @s weapon.mainhand with minecraft:air");
        c.cmd("time set 6000");

        // ---- the shader-drawn sky layers, tinted weather and Eve's fog
        shaderLayers(c);
        weatherAndFog(c);

        // ---- Thatmo's battle in the sky (SkyProviderThatmo)
        thatmo(c);

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    /** Clears {@code body}'s weather and waits until its level's rain and thunder are gone. */
    private static void calmWeather(Client c, String body) throws Exception {
        c.cmd("ntmspace weather clear 6000");
        com.google.gson.JsonObject sky = null;
        for (int i = 0; i < 40; i++) {
            sky = c.query("ntmspace_sky", new com.google.gson.JsonObject());
            if (sky.get("rain").getAsFloat() == 0.0F && sky.get("thunder").getAsFloat() == 0.0F) break;
            c.waitTicks(10);
        }
        Expect.that(sky.get("rain").getAsFloat() == 0.0F && sky.get("thunder").getAsFloat() == 0.0F,
                body + ": no rain or thunder for the sky shots: " + sky);
    }

    /**
     * A screenshot outside Eve's lightning flashes: Eve's sky flashes on 1.7's hash schedule of the level's game time
     * whatever the weather ({@code WorldProviderEve}), and the game time differs between the nodes' saves, so a shot is
     * retaken until no flash was running before and after it.
     */
    private static void calmShot(Client c, String body, String name) throws Exception {
        if (!body.equals("eve")) {
            c.screenshot(name);
            return;
        }
        for (int attempt = 0; attempt < 40; attempt++) {
            if (c.query("ntmspace_sky", new com.google.gson.JsonObject()).get("eve_flash").getAsFloat() > 0) {
                c.waitTicks(5);
                continue;
            }
            c.screenshot(name);
            if (c.query("ntmspace_sky", new com.google.gson.JsonObject()).get("eve_flash").getAsFloat() == 0) return;
        }
        throw new AssertionError("no flash-free moment on Eve for " + name);
    }

    // ------------------------------------------------------------------------------------------------ Thatmo

    private static com.google.gson.JsonObject thatmoQuery(Client c) throws Exception {
        return c.query("ntmspace_thatmo", new com.google.gson.JsonObject());
    }

    /** One screenshot without the GUI, looking at yaw / pitch. */
    private static void shot(Client c, String name, float yaw, float pitch) throws Exception {
        c.setOption("hide_gui", "true");
        c.look(yaw, pitch);
        c.waitTicks(5);
        c.screenshot(name);
        c.setOption("hide_gui", "false");
    }

    private static void thatmo(Client c) throws Exception {
        c.cmd("ntmspace tp thatmo");
        c.waitTicks(40);
        Expect.equal("ntmspace:thatmo", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp thatmo");
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(10);
        Fixtures.platform(c, 0, Y, 0, 4, "minecraft:smooth_stone");
        c.cmd("ntmspace skytime night");
        c.cmd("ntmspace weather clear 6000");
        c.waitTicks(10);
        // Kerbin's sky on Thatmo at the pinned orbit time: the Mun at 1.7's metric angle for that time (the same on
        // every node; it stands south-east, inside the shield views below)
        com.google.gson.JsonObject astro = c.query("ntmspace_sky", new com.google.gson.JsonObject()).getAsJsonObject("astro");
        Expect.that(astro.get("pinned").getAsBoolean() && astro.get("sky_ticks").getAsDouble() == SKY_TICKS
                && astro.get("body").getAsString().equals("kerbin"), "Thatmo draws Kerbin's sky at the pinned time: " + astro);
        for (com.google.gson.JsonElement e : astro.getAsJsonArray("bodies")) {
            com.google.gson.JsonObject o = e.getAsJsonObject();
            if (o.get("body").getAsString().equals("mun")) {
                Expect.that(Math.abs(o.get("angle").getAsDouble() - MUN_ANGLE) < 0.01, "the Mun's angle from Thatmo: " + o);
            }
        }

        // hbm-space gave the body Thatmo no dimension, so on the Thatmo level CelestialBody.getBody (and every trait
        // lookup) falls back to Kerbin (CelestialBodies.of): Thatmo's own default CBT_BATTLEFIELD never reaches its level,
        // and the battle sky shows only while Kerbin has the trait
        com.google.gson.JsonObject q = thatmoQuery(c);
        Expect.that(q.get("thatmo").getAsBoolean() && !q.get("battlefield").getAsBoolean(),
                "Thatmo's level reads Kerbin's traits, without the battlefield by default (1.7): " + q);
        skytrait(c, "battle kerbin", "space_skytrait body=kerbin trait=battle");
        try {
            thatmoBattle(c);
        } finally {
            c.cmd("ntmspace skytrait clear kerbin battle");
        }
        c.waitTicks(10);
        q = thatmoQuery(c);
        Expect.that(!q.get("battlefield").getAsBoolean(), "Kerbin's battlefield trait is gone again: " + q);

        c.cmd("ntmspace skytime noon");
        c.cmd("execute in minecraft:overworld run tp @s 0.5 101 0.5");
        c.waitTicks(20);
    }

    private static void thatmoBattle(Client c) throws Exception {
        // the railgun's cycle from its start (a client that saw a battle before may be anywhere in it): no flash or
        // shield glow lights the sky in the battle and railgun shots
        c.query("ntmspace_thatmo", jsonOf("charge", 0));
        com.google.gson.JsonObject q = thatmoQuery(c);
        Expect.that(q.get("thatmo").getAsBoolean() && q.get("battlefield").getAsBoolean(),
                "Thatmo has the battlefield trait on the client (Kerbin's): " + q);
        long frames = q.get("frames").getAsLong(), model = q.get("model").getAsLong();
        long beams = q.get("beam_quads").getAsLong(), billboards = q.get("billboards").getAsLong();
        c.waitTicks(80);
        q = thatmoQuery(c);
        // one meteor a tick, each living 74 ticks; one smoke puff per meteor and tick, each living 60 ticks
        Expect.that(q.get("meteors").getAsInt() >= 60, "Thatmo's meteors pile up on the client: " + q);
        Expect.that(q.get("smoke").getAsInt() > 1000, "Thatmo's meteors leave smoke: " + q);
        Expect.that(q.get("frames").getAsLong() > frames, "Thatmo's special effects are drawn: " + q);
        Expect.that(q.get("model").getAsLong() > model, "the railgun silhouette is drawn: " + q);
        Expect.that(q.get("beam_quads").getAsLong() > beams, "the showers of orange beams are drawn: " + q);
        Expect.that(q.get("billboards").getAsLong() > billboards, "the meteors and their smoke are drawn: " + q);
        c.setOption("hide_gui", "true");
        String[] names = {"south", "west", "north", "east"};
        for (int i = 0; i < 4; i++) {
            c.look(i * 90, -30);
            c.waitTicks(5);
            c.screenshot("thatmo-battle-" + names[i]);
        }
        c.setOption("hide_gui", "false");
        q = thatmoQuery(c);
        Expect.that(q.get("light_add").getAsFloat() == 0.0F, "no flash or shield glow before the railgun fires: " + q);
        shot(c, "thatmo-railgun", 19.3F, -2.4F);
        shot(c, "thatmo-ground", 19.3F, 40F);

        // the railgun fires: flashd eases to 100 over 61 ticks (test hook: the charge counter at 1001)
        long flashes = q.get("flash").getAsLong();
        c.query("ntmspace_thatmo", jsonOf("charge", 1001));
        c.waitTicks(4);
        q = thatmoQuery(c);
        Expect.that(q.get("flashd").getAsFloat() > 0 && q.get("flashd").getAsFloat() < 100, "the railgun's flash runs: " + q);
        Expect.that(q.get("light_add").getAsFloat() > 0, "the flash brightens Thatmo's sky: " + q);
        Expect.that(q.get("flash").getAsLong() > flashes, "the flash is drawn: " + q);
        shot(c, "thatmo-flash", -8.1F, -14.5F);
        shot(c, "thatmo-flash-beam", -9.4F, -15.7F);

        // the shield hit: from charge 300 the orange beam grows (1.5 a tick) and the shield eases out
        long shields = q.get("shield_frames").getAsLong();
        c.query("ntmspace_thatmo", jsonOf("charge", 299));
        // Shoot early in the hit, keyed on the state, not on a fixed wait (client ticks per waited tick vary between
        // runs): the shockwave plane's alpha is 1 - nmass / 100 and nmass passes 100 about 26 client ticks after the
        // shield appears, so a late shot catches the outer ellipse at an alpha between 0.1 and 0 depending on frame
        // timing (the 2026-10-09 "26.1 draws an extra ellipse" report was that; it is the same on every node).
        q = thatmoQuery(c);
        for (int i = 0; i < 80 && !(q.get("scale").getAsFloat() == 20.0F && q.get("shield").getAsFloat() > 0); i++) {
            c.waitTicks(1);
            q = thatmoQuery(c);
        }
        c.waitTicks(2);
        q = thatmoQuery(c);
        Expect.that(q.get("scale").getAsFloat() == 20.0F && q.get("shield").getAsFloat() > 0, "the shield is hit: " + q);
        Expect.that(q.get("shield_frames").getAsLong() > shields, "the shield is drawn: " + q);
        shot(c, "thatmo-shield", -142.5F, -43.1F);
        shot(c, "thatmo-shield-beam", -95.9F, -27.4F);
    }

    // ------------------------------------------------------------------------------------------------ shader layers

    private static final String[] VIEWS = {"south", "west", "north", "east", "zenith"};

    /** How often Golem has drawn {@code shader} (an {@code ntmspace:sky_*} id) on this client. */
    private static long draws(Client c, String shader) throws Exception {
        com.google.gson.JsonObject q = c.query("ntmspace_sky_shaders", new com.google.gson.JsonObject());
        com.google.gson.JsonElement e = q.getAsJsonObject("draws").get("ntmspace:" + shader);
        return e == null ? 0 : e.getAsLong();
    }

    /** {@code shader} draws at least once within 10 ticks (it is drawn each frame while it shows). */
    private static void expectDrawn(Client c, String shader, String why) throws Exception {
        long before = draws(c, shader);
        c.waitTicks(10);
        long drawn = draws(c, shader) - before;
        Expect.that(drawn > 0, why + ": " + shader + " drew " + drawn + " times in 10 ticks");
    }

    /** {@code shader} does not draw within 10 ticks. */
    private static void expectNotDrawn(Client c, String shader, String why) throws Exception {
        long before = draws(c, shader);
        c.waitTicks(10);
        long drawn = draws(c, shader) - before;
        Expect.that(drawn == 0, why + ": " + shader + " drew " + drawn + " times in 10 ticks");
    }

    /** Screenshots in four directions slightly above the horizon and at the zenith. */
    private static void views(Client c, String label) throws Exception {
        c.setOption("hide_gui", "true");
        for (int i = 0; i < 4; i++) {
            c.look(i * 90, -30);
            c.waitTicks(5);
            c.screenshot(label + "-" + VIEWS[i]);
        }
        c.look(0, -90);
        c.waitTicks(5);
        c.screenshot(label + "-" + VIEWS[4]);
        c.setOption("hide_gui", "false");
    }

    private static void skytrait(Client c, String args, String logged) throws Exception {
        c.markLog();
        c.cmd("ntmspace skytrait " + args);
        c.waitTicks(10);
        Expect.that(!c.logSinceMark(logged).isEmpty(), "/ntmspace skytrait " + args + " logged " + logged);
    }

    private static com.google.gson.JsonObject jsonOf(String key, int value) {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty(key, value);
        return o;
    }

    private static com.google.gson.JsonObject jsonOf(String key, String value) {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty(key, value);
        return o;
    }

    private static void shaderLayers(Client c) throws Exception {
        c.cmd("ntmspace tp mun");
        c.waitTicks(40);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp mun");
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(10);
        Fixtures.platform(c, 0, Y, 0, 4, "minecraft:smooth_stone");
        c.cmd("ntmspace skytime night");
        c.waitTicks(10);
        try {
            // Kerbin seen from the Mun: 1 atm of air, clouds (atmosphere.frag style 1); no lights before any are set
            expectDrawn(c, "sky_atmosphere", "Kerbin's clouds from the Mun");
            expectNotDrawn(c, "sky_nightlights", "no city lights before any are set");
            views(c, "mun-kerbin");

            // city lights: CBT_Lights above 10,000 is citylights_3; the emissive glow needs Kerbin's clouds
            skytrait(c, "lights kerbin 20000", "space_skytrait body=kerbin trait=lights");
            com.google.gson.JsonObject q = c.query("ntmspace_sky_shaders", jsonOf("body", "kerbin"));
            Expect.equal(3, q.get("light_intensity").getAsInt(), "Kerbin's light intensity on the client: " + q);
            expectDrawn(c, "sky_nightlights", "Kerbin's city lights");
            expectDrawn(c, "sky_atmosphere_emissive", "the glow of Kerbin's city lights in its clouds");
            views(c, "mun-kerbin-lights");

            // lightning in Kerbin's clouds while Kerbin thunders (the thunder strength ramps up over some seconds)
            c.cmd("execute in minecraft:overworld run ntmspace weather thunder 600");
            for (int i = 0; i < 30; i++) {
                long before = draws(c, "sky_lightning");
                c.waitTicks(10);
                if (draws(c, "sky_lightning") > before) break;
            }
            expectDrawn(c, "sky_lightning", "lightning in Kerbin's thunderclouds");
            views(c, "mun-kerbin-lightning");
            c.cmd("execute in minecraft:overworld run ntmspace weather clear 600");

            // a nuclear blast on Kerbin: its flash and ring for 220 ticks
            skytrait(c, "nuke kerbin 140", "space_skytrait body=kerbin trait=nuke");
            q = c.query("ntmspace_sky_shaders", jsonOf("body", "kerbin"));
            Expect.that(q.get("nuke_shocks").getAsInt() >= 1, "the client has Kerbin's nuke shock: " + q);
            expectDrawn(c, "sky_nuke", "the nuke flash and ring on Kerbin");
            views(c, "mun-kerbin-nuke");

            // a destroyed Kerbol: the black hole at noon
            c.cmd("ntmspace skytime noon");
            skytrait(c, "destroyed kerbol", "space_skytrait body=kerbol trait=destroyed");
            expectDrawn(c, "sky_blackhole", "the black hole of a destroyed Kerbol");
            c.setOption("hide_gui", "true");
            c.look(0, -90);
            c.waitTicks(5);
            c.screenshot("mun-blackhole");
            c.look(90, -45);
            c.waitTicks(5);
            c.screenshot("mun-blackhole-west");
            c.setOption("hide_gui", "false");
            skytrait(c, "clear kerbol destroyed", "space_skytrait body=kerbol trait=destroyed cleared");
            expectNotDrawn(c, "sky_blackhole", "Kerbol is back");

            // an infected Mun: the flesh sky
            skytrait(c, "infected mun", "space_skytrait body=mun trait=infected");
            expectDrawn(c, "sky_flesh", "the flesh sky of an infected body");
            views(c, "mun-flesh");
            skytrait(c, "clear mun infected", "space_skytrait body=mun trait=infected cleared");
            expectNotDrawn(c, "sky_flesh", "the Mun is healed");
        } finally {
            c.cmd("ntmspace skytrait clear kerbin lights");
            c.cmd("ntmspace skytrait clear kerbol destroyed");
            c.cmd("ntmspace skytrait clear mun infected");
            c.cmd("execute in minecraft:overworld run ntmspace weather clear 600");
            c.setOption("hide_gui", "false");
        }

        // Jool from Laythe: a gas giant's bands (atmosphere.frag style 3)
        c.cmd("ntmspace tp laythe");
        c.waitTicks(40);
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(10);
        Fixtures.platform(c, 0, Y, 0, 4, "minecraft:smooth_stone");
        c.cmd("ntmspace skytime night");
        c.waitTicks(10);
        expectDrawn(c, "sky_atmosphere", "Jool's bands and Laythe's sky");
        views(c, "laythe-jool");
    }

    // ---------------------------------------------------------------------------------------------- weather and fog

    private static com.google.gson.JsonObject waitForRain(Client c, String body) throws Exception {
        com.google.gson.JsonObject sky = null;
        for (int i = 0; i < 30; i++) {
            c.waitTicks(10);
            sky = c.query("ntmspace_sky", new com.google.gson.JsonObject());
            if (sky.get("rain").getAsFloat() > 0.6F) break;
        }
        Expect.that(sky != null && sky.get("rain").getAsFloat() > 0.6F, body + " rains for the tint check: " + sky);
        return sky;
    }

    private static void weatherAndFog(Client c) throws Exception {
        // Eve: mercury rain in a thunderstorm, and its fog
        c.cmd("ntmspace tp eve");
        c.waitTicks(40);
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(10);
        Fixtures.platform(c, 0, Y, 0, 6, "minecraft:smooth_stone");
        c.cmd("ntmspace skytime noon");
        c.cmd("ntmspace weather thunder 600");
        try {
            waitForRain(c, "Eve");
            com.google.gson.JsonObject q = c.query("ntmspace_sky_shaders", new com.google.gson.JsonObject());
            Expect.that(q.get("weather_custom").getAsBoolean(), "Golem draws Eve's weather: " + q);
            expectColour(q, 0.501961F, 0.501961F, 0.501961F, "Eve's rain is mercury grey");
            long before = q.get("weather_quads").getAsLong();
            c.waitTicks(10);
            long drawn = c.query("ntmspace_sky_shaders", new com.google.gson.JsonObject()).get("weather_quads").getAsLong() - before;
            Expect.that(drawn > 0, "Golem drew Eve's rain columns (" + drawn + " quads in 10 ticks)");
            c.setOption("hide_gui", "true");
            c.look(90, 10);
            c.waitTicks(5);
            c.screenshot("eve-rain-tint");
            c.setOption("hide_gui", "false");

            // 1.7 GL_EXP of density 5 * 5 * 0.002, as a linear fog ending where it is 95 % opaque: ln 20 / 0.05
            Expect.that(q.has("fog_end") && Math.abs(q.get("fog_end").getAsFloat() - 59.914646F) < 1e-3,
                    "Eve's linear fog ends at 59.9 blocks: " + q);
            c.cmd("ntmspace weather clear 600");
            c.waitTicks(100);
            c.setOption("hide_gui", "true");
            c.look(90, 0);
            c.waitTicks(5);
            c.screenshot("eve-fog-linear-horizon");
            c.look(90, 40);
            c.waitTicks(5);
            c.screenshot("eve-fog-linear-down");
            c.setOption("hide_gui", "false");
        } finally {
            c.cmd("ntmspace weather clear 600");
        }

        // Kerbin: water's desaturated blue rain
        c.cmd("execute in minecraft:overworld run tp @s 0 " + (Y + 1) + " 0");
        c.waitTicks(40);
        Fixtures.platform(c, 0, Y, 0, 4, "minecraft:smooth_stone");
        c.cmd("time set 6000");
        // Kerbin is the overworld and keeps vanilla's weather (BodyWeather: /ntmspace weather only sets Kerbin's trait,
        // which its level does not follow), so the level is made to rain with vanilla's /weather
        c.cmd("weather rain 600");
        try {
            waitForRain(c, "Kerbin");
            com.google.gson.JsonObject q = c.query("ntmspace_sky_shaders", new com.google.gson.JsonObject());
            Expect.that(q.get("weather_custom").getAsBoolean(), "Golem draws Kerbin's weather: " + q);
            expectColour(q, 0.259280F, 0.259280F, 0.539280F, "Kerbin's rain is water's desaturated blue");
            c.setOption("hide_gui", "true");
            c.look(90, 10);
            c.waitTicks(5);
            c.screenshot("kerbin-rain-tint");
            c.setOption("hide_gui", "false");
        } finally {
            c.cmd("ntmspace weather clear 600");
            c.cmd("weather clear");
        }
    }

    private static void expectColour(com.google.gson.JsonObject q, float r, float g, float b, String why) {
        com.google.gson.JsonArray rgb = q.getAsJsonArray("rain_color");
        Expect.that(Math.abs(rgb.get(0).getAsFloat() - r) < 1e-4 && Math.abs(rgb.get(1).getAsFloat() - g) < 1e-4
                && Math.abs(rgb.get(2).getAsFloat() - b) < 1e-4, why + ": " + rgb);
    }

    /** Where the cloud views stand: below 1.7's cloud height 128, and above the top layer (152 + 4). */
    private static final int CLOUD_BELOW_Y = 110;
    private static final int CLOUD_ABOVE_Y = 175;

    /**
     * The bodies' clouds (hbm-space {@code CloudProviderCelestial}): the client's layers (query {@code ntmspace_clouds},
     * values from the 1.7 formulas at noon without rain), that Golem actually draws them on this node ({@code drawn}
     * grows by at least one per layer while they are on screen), and screenshots from below and above. Eve: three layers
     * at 128 / 140 / 152, alpha 0.8 / 0.672 / 0.608, purple (0.769, 0.603, 0.935). Laythe: one layer, xenon-tinted
     * (0.930, 0.817, 0.975). Tekto: one green layer (0.572, 0.935, 0.781). The Mun: none at all (the legacy nodes used to
     * draw vanilla's white clouds at y 192 there). Duna: none at its 0.1 atm, one white layer once terraformed to 1 atm of
     * Duna air (neutral, no tint), none again after the reset.
     */
    private static void clouds(Client c) throws Exception {
        String cloudsBefore = c.setOption("clouds", "fancy");
        try {
            cloudBody(c, "eve", "eve", new float[] {128, 140, 152}, new float[] {0.8F, 0.672F, 0.608F}, new float[] {0.769F, 0.603F, 0.935F});
            cloudBody(c, "laythe", "laythe", new float[] {128}, new float[] {0.8F}, new float[] {0.930F, 0.817F, 0.975F});
            cloudBody(c, "tekto", "tekto", new float[] {128}, new float[] {0.8F}, new float[] {0.572F, 0.935F, 0.781F});
            cloudBody(c, "mun", "mun", new float[0], new float[0], null);
            cloudBody(c, "duna", "duna", new float[0], new float[0], null);

            c.markLog();
            c.cmd("ntmspace clouds air duna 1.0 ntmspace:dunaair");
            c.waitTicks(20);
            Expect.that(!c.logSinceMark("space_clouds_air body=duna").isEmpty(), "Duna terraformed to 1 atm of Duna air");
            cloudBody(c, "duna", "duna-terraformed", new float[] {128}, new float[] {0.8F}, new float[] {1.0F, 1.0F, 1.0F});
            c.cmd("ntmspace clouds reset duna");
            c.waitTicks(20);
            com.google.gson.JsonObject after = c.query("ntmspace_clouds", new com.google.gson.JsonObject());
            Expect.that(after.getAsJsonArray("layers").isEmpty(), "Duna's clouds are gone after the reset: " + after);
        } finally {
            c.setOption("hide_gui", "false");
            c.setOption("clouds", cloudsBefore);
        }
    }

    /** One body's clouds at noon without rain: the layers, the draw count, and a view from below and from above. */
    private static void cloudBody(Client c, String body, String label, float[] heights, float[] alphas, float[] colour) throws Exception {
        c.cmd("ntmspace tp " + body);
        c.waitTicks(40);
        Expect.equal("ntmspace:" + body, c.status().get("dimension").getAsString(), "dimension after /ntmspace tp " + body);
        c.teleport(0.5, CLOUD_BELOW_Y + 1, 0.5);
        c.waitTicks(10);
        c.cmd("fill -3 " + (CLOUD_BELOW_Y + 1) + " -3 3 " + (CLOUD_BELOW_Y + 8) + " 3 minecraft:air");
        Fixtures.platform(c, 0, CLOUD_BELOW_Y, 0, 3, "minecraft:smooth_stone");
        c.teleport(0.5, CLOUD_BELOW_Y + 1, 0.5);
        c.cmd("ntmspace skytime noon");
        c.cmd("ntmspace weather clear 6000");
        com.google.gson.JsonObject sky = null;
        for (int i = 0; i < 30; i++) {
            c.waitTicks(10);
            sky = c.query("ntmspace_sky", new com.google.gson.JsonObject());
            if (sky.get("rain").getAsFloat() == 0.0F && sky.get("thunder").getAsFloat() == 0.0F) break;
        }
        Expect.that(sky != null && sky.get("rain").getAsFloat() == 0.0F, body + ": no rain for the cloud check: " + sky);

        com.google.gson.JsonObject clouds = c.query("ntmspace_clouds", new com.google.gson.JsonObject());
        Expect.that(clouds.get("custom").getAsBoolean(), body + ": Golem draws this level's clouds: " + clouds);
        com.google.gson.JsonArray layers = clouds.getAsJsonArray("layers");
        Expect.equal(heights.length, layers.size(), body + " cloud layers: " + clouds);
        for (int i = 0; i < Math.min(heights.length, layers.size()); i++) {
            com.google.gson.JsonObject layer = layers.get(i).getAsJsonObject();
            Expect.that(Math.abs(layer.get("height").getAsFloat() - heights[i]) < 1e-3, body + " layer " + i + " height: " + layer);
            Expect.that(Math.abs(layer.get("alpha").getAsFloat() - alphas[i]) < 1e-3, body + " layer " + i + " alpha: " + layer);
            Expect.that(Math.abs(layer.get("red").getAsFloat() - colour[0]) < 0.005F
                    && Math.abs(layer.get("green").getAsFloat() - colour[1]) < 0.005F
                    && Math.abs(layer.get("blue").getAsFloat() - colour[2]) < 0.005F, body + " layer " + i + " colour: " + layer);
        }

        c.setOption("hide_gui", "true");
        c.look(0, -35);
        long drawnBefore = c.query("ntmspace_clouds", new com.google.gson.JsonObject()).get("drawn").getAsLong();
        c.waitTicks(10);
        long drawn = c.query("ntmspace_clouds", new com.google.gson.JsonObject()).get("drawn").getAsLong() - drawnBefore;
        if (heights.length > 0) {
            Expect.that(drawn >= heights.length, body + ": Golem drew its cloud layers (" + drawn + " layer draws in 10 ticks)");
        } else {
            Expect.that(drawn == 0, body + ": no clouds drawn on an airless body (" + drawn + " layer draws)");
        }
        c.screenshot(label + "-clouds-below");
        c.look(90, -10);
        c.waitTicks(5);
        c.screenshot(label + "-clouds-horizon");
        if (heights.length > 0) {
            c.cmd("gamemode spectator"); // hovers above the clouds
            c.teleport(0.5, CLOUD_ABOVE_Y, 0.5);
            c.look(0, 35);
            c.waitTicks(10);
            c.screenshot(label + "-clouds-above");
            c.teleport(0.5, CLOUD_BELOW_Y + 1, 0.5);
            c.cmd("gamemode creative");
            c.waitTicks(5);
        }
        c.setOption("hide_gui", "false");
    }
}
