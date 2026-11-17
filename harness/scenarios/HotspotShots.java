import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Before/after shots for the render hotspot fixes, with ticks frozen and every view shot twice (a, b) so a run proves
 * its own shots repeat: Kerbin, the Mun's charged dust at sunrise and sunset, Duna and Eve skies, a machine yard and a
 * glyphid crowd.
 * Asserts nothing; compare the shots across builds.
 */
public final class HotspotShots implements Scenario {

    private static final int SKY_TICKS = 13_600;
    private static final int X = 1500, Y = 100, Z = 1500, G = Y + 1;
    private static final String[] MACHINES = {
            "machine_chemical_plant", "machine_mixer", "machine_crystallizer", "machine_cyclotron", "machine_radgen",
            "machine_liquefactor", "machine_assembly_machine", "machine_pumpjack", "machine_solidifier"};
    private static final String[] MOBS = {
            "hbm:entity_glyphid", "hbm:entity_glyphid_brawler", "hbm:entity_glyphid_behemoth", "hbm:entity_glyphid_brenda",
            "hbm:entity_glyphid_bombardier", "hbm:entity_glyphid_blaster", "hbm:entity_glyphid_scout",
            "hbm:entity_glyphid_digger", "hbm:entity_glyphid_nuclear"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        String hideGui = null, maxFps = null, distance = null;
        String forceload = (X - 40) + " " + (Z - 40) + " " + (X + 40) + " " + (Z + 40);
        try {
            hideGui = c.setOption("hide_gui", "true");
            maxFps = c.setOption("max_fps", "60");
            distance = c.setOption("render_distance", "8");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("weather clear");
            c.cmd("gamemode spectator");
            c.query("ntmspace_sky", pin(SKY_TICKS));
            sky(c, "minecraft:overworld", 6000, 90, -20, "kerbin-noon");
            sky(c, "minecraft:overworld", 12500, 90, -10, "kerbin-dusk");
            sky(c, "minecraft:overworld", 18000, 0, -60, "kerbin-night");
            sky(c, "ntmspace:mun", 23300, -90, -5, "mun-sunrise");
            sky(c, "ntmspace:mun", 12700, 90, -5, "mun-sunset");
            sky(c, "ntmspace:duna", 12500, 90, -10, "duna-dusk");
            sky(c, "ntmspace:eve", 6000, 90, -20, "eve-day");
            c.query("ntmspace_sky", pin(-1));

            c.cmd("execute in minecraft:overworld run tp @s " + X + " " + (G + 10) + " " + Z);
            c.cmd("forceload add " + forceload);
            c.waitTicks(40);
            c.cmd("gamemode creative");
            Fixtures.platform(c, X, Y, Z, 36);
            c.cmd("gamemode spectator");
            c.cmd("time set 6000");
            for (int i = 0; i < MACHINES.length; i++) {
                Fixtures.setBlock(c, X - 32 + (i % 5) * 16, G, Z - 8 + (i / 5) * 16, "hbm:" + MACHINES[i] + "[facing=north]");
            }
            c.teleport(X + 0.5, G + 22, Z - 36.5);
            c.lookAt(X + 0.5, G, Z + 4.5);
            frozen(c, "machines");

            for (int y = G; y < G + 24; y += 4) {
                c.cmd("fill " + (X - 36) + " " + y + " " + (Z - 36) + " " + (X + 36) + " " + (y + 3) + " " + (Z + 36) + " minecraft:air");
            }
            c.cmd("difficulty normal");
            for (int i = 0; i < MOBS.length * 2; i++) {
                c.cmd("summon " + MOBS[i % MOBS.length] + " " + (X - 16 + (i % 6) * 6.5) + " " + G + " " + (Z - 6 + (i / 6) * 7)
                        + " {NoAI:1b,PersistenceRequired:1b,Silent:1b,Rotation:[" + (i * 40) + "f,0f]}");
            }
            c.teleport(X + 0.5, G + 10, Z - 22.5);
            c.lookAt(X + 0.5, G + 1, Z + 3.5);
            frozen(c, "glyphids");
            c.cmd("kill @e[type=!minecraft:player]");
        } finally {
            c.cmd("tick unfreeze");
            c.cmd("difficulty peaceful");
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback true");
            c.cmd("forceload remove " + forceload);
            c.cmd("execute in minecraft:overworld run tp @s 0.5 120 0.5");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (maxFps != null) c.setOption("max_fps", maxFps);
            if (distance != null) c.setOption("render_distance", distance);
        }
    }

    private static void sky(Client c, String dimension, int time, double yaw, double pitch, String name) throws Exception {
        c.cmd("tick unfreeze");
        c.cmd("execute in " + dimension + " run tp @s 0.5 160 0.5");
        c.cmd("time set " + time);
        c.look(yaw, pitch);
        c.waitTicks(120);
        frozen(c, name);
    }

    private static void frozen(Client c, String name) throws Exception {
        c.waitTicks(40);
        c.cmd("tick freeze");
        c.waitTicks(20);
        c.screenshot(name + "-a");
        c.waitTicks(20);
        c.screenshot(name + "-b");
        c.cmd("tick unfreeze");
    }

    private static JsonObject pin(int ticks) {
        JsonObject o = new JsonObject();
        o.addProperty("pin_ticks", ticks);
        return o;
    }
}
