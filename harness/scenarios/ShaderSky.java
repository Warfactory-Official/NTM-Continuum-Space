import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * Discord report: under a shader pack the Kerbin sky lost its sun and showed a black wedge. Screenshots of the sky at
 * sunrise, morning, noon and sunset, looking at the sun, and of the night sky; run once with Iris and a pack, once without, and compare.
 */
public final class ShaderSky implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("execute in minecraft:overworld run tp @s 0.5 120 0.5");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("weather clear");
        c.cmd("clear");
        c.waitTicks(40);
        shot(c, 23300, -90, -5, "01-sunrise");
        shot(c, 1500, -90, -25, "02-morning");
        shot(c, 6000, 0, -88, "03-noon");
        shot(c, 12500, 90, -5, "04-sunset");
        shot(c, 18000, 0, -88, "05-midnight");
        shot(c, 15000, -90, -30, "06-night-east");
        shot(c, 15000, 90, -30, "07-night-west");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(ntmspace|golem)");
    }

    private static void shot(Client c, int time, double yaw, double pitch, String name) throws Exception {
        c.cmd("time set " + time);
        c.look(yaw, pitch);
        c.waitTicks(40);
        c.screenshot(name);
    }
}
