import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/**
 * Frame cost across NTM: Space's skies: one {@code /golem profiler 15} capture per view (Kerbin ground and high
 * altitude, the Mun facing Kerbin, Eve, Duna, Laythe at sunset, a station in orbit), written to the run's
 * {@code golem} folder in that order, each after a screenshot named like it. Asserts nothing.
 */
public final class SkyProfileTour implements Scenario {

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
            c.cmd("gamemode spectator");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("weather clear");
            view(c, "minecraft:overworld", 120, 6000, 90, -10, "1-kerbin-ground");
            view(c, "minecraft:overworld", 420, 18000, 0, -30, "2-kerbin-y420");
            view(c, "ntmspace:mun", 160, 18000, 0, -40, "3-mun-night");
            view(c, "ntmspace:eve", 160, 6000, 90, -20, "4-eve-day");
            view(c, "ntmspace:duna", 160, 12500, 90, -10, "5-duna-dusk");
            view(c, "ntmspace:laythe", 160, 12500, 90, -10, "6-laythe-sunset");
            c.cmd("ntmstations launch 3 4 kerbin");
            c.cmd("ntmstations tp 3 4");
            c.waitTicks(200);
            c.look(0, 60);
            c.waitTicks(40);
            c.screenshot("7-orbit");
            c.cmd("golem profiler 15");
            c.waitTicks(20 * 20);
        } finally {
            c.cmd("execute in minecraft:overworld run tp @s 0.5 120 0.5");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (maxFps != null) c.setOption("max_fps", maxFps);
            if (vsync != null) c.setOption("vsync", vsync);
            if (inactivity != null) c.setOption("inactivity_fps", inactivity);
        }
    }

    private static void view(Client c, String dimension, int y, int time, double yaw, double pitch, String name)
            throws Exception {
        c.cmd("execute in " + dimension + " run tp @s 0.5 " + y + " 0.5");
        c.cmd("execute in " + dimension + " run time set " + time);
        c.look(yaw, pitch);
        c.waitTicks(200);
        c.screenshot(name);
        c.cmd("golem profiler 15");
        c.waitTicks(20 * 20);
    }
}
