import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/**
 * Space issue 9: frame cost of the Kerbin sky. First eleven screenshots with the orbits pinned (compare them across
 * builds for identical pixels), then one {@code /golem profiler 20} capture at night looking up (every body and its
 * crescent in view) and one at noon, written to the run's {@code golem} folder in that order. Asserts nothing.
 */
public final class GhSpace9SkyProfile implements Scenario {

    private static final int SKY_TICKS = 13_600;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        String hideGui = null, maxFps = null, vsync = null, inactivity = null;
        try {
            hideGui = c.setOption("hide_gui", "true");
            maxFps = c.setOption("max_fps", "260");
            vsync = c.setOption("vsync", "false");
            inactivity = c.setOption("inactivity_fps", "minimized");
            c.cmd("execute in minecraft:overworld run tp @s 0.5 120 0.5");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("weather clear");
            c.cmd("gamemode spectator");
            c.query("ntmspace_sky", pin(SKY_TICKS));
            try {
                for (int yaw = 0; yaw < 360; yaw += 90) shot(c, 18000, yaw, -40, "night-" + yaw);
                shot(c, 18000, 0, -88, "night-up");
                for (int yaw = 0; yaw < 360; yaw += 90) shot(c, 13000, yaw, -20, "dusk-" + yaw);
                shot(c, 23300, -90, -5, "sunrise");
                shot(c, 6000, 0, -88, "noon-up");
            } finally {
                c.query("ntmspace_sky", pin(-1));
            }
            capture(c, 18000, 0, -70);
            capture(c, 6000, 90, -30);
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (maxFps != null) c.setOption("max_fps", maxFps);
            if (vsync != null) c.setOption("vsync", vsync);
            if (inactivity != null) c.setOption("inactivity_fps", inactivity);
        }
    }

    private static void shot(Client c, int time, double yaw, double pitch, String name) throws Exception {
        c.cmd("time set " + time);
        c.look(yaw, pitch);
        c.waitTicks(40);
        c.screenshot(name);
    }

    private static void capture(Client c, int time, double yaw, double pitch) throws Exception {
        c.cmd("time set " + time);
        c.look(yaw, pitch);
        c.waitTicks(100);
        c.cmd("golem profiler 20");
        c.waitTicks(20 * 25);
    }

    private static JsonObject pin(int ticks) {
        JsonObject o = new JsonObject();
        o.addProperty("pin_ticks", ticks);
        return o;
    }
}
