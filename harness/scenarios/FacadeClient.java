import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * The client halves of the M3 facades (Golem Core {@code Fov.current}, {@code CameraHooks.DISTANCE},
 * {@code PlayerRenderHooks.POSE}, {@code TimeOfDay}), read through the addon's harness query {@code ntmspace_view}:
 * <ul>
 *   <li>FOV: the frame's FOV is the options' while standing, and wider with Speed II (vanilla's dynamic modifier, 1.7's
 *       {@code getFOVModifier}, which the Dyson swarm's point size reads);</li>
 *   <li>a body's own time of day: on the Mun, {@code /ntmspace time 0} and {@code /ntmspace time 38700} reach the client
 *       (its {@code time_of_day} within a second of the set value) and move the sun: 60-mun-clock-0 and
 *       60-mun-clock-38700 look east at the horizon; the overworld (Kerbin) keeps vanilla's clock;</li>
 *   <li>the rocket camera: riding a summoned rideable rocket in third person, the camera stands about 12 blocks from the
 *       player (hbm-space's {@code thirdPersonDistance}), against about 4 on foot: 61-rocket-third-person.</li>
 * </ul>
 * The rider's tilt needs a launch (the pitch only rises while launching): {@code RocketFlight}'s 04-launch shows it,
 * and that scenario asserts the pitch and the 12-block camera there.
 */
public final class FacadeClient implements Scenario {

    private static final int Y = 100;

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
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("execute in minecraft:overworld run tp @s 0 " + (Y + 1) + " 0");
        c.waitTicks(20);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.setOption("fov", "70");
        c.cmd("effect clear @s");
        c.waitTicks(20);

        // ---- FOV
        double standing = view(c).get("fov").getAsDouble();
        Expect.that(Math.abs(standing - 70) < 1.5, "the frame's FOV is the options' while standing: " + standing);
        c.cmd("effect give @s minecraft:speed 30 1 true");
        c.waitTicks(30);
        double fast = view(c).get("fov").getAsDouble();
        Expect.that(fast > standing + 2, "Speed II widens the frame's FOV (" + standing + " -> " + fast + ")");
        c.cmd("effect clear @s");

        // ---- Kerbin keeps vanilla's clock
        c.cmd("ntmspace time");
        c.waitTicks(5);
        Expect.that(!c.logSinceMark("space_time dim=minecraft:overworld .* own=false").isEmpty(),
                "the overworld has no own clock: " + c.logSinceMark("space_time"));

        // ---- the Mun's own clock
        c.cmd("ntmspace tp mun");
        c.waitTicks(60);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp mun");
        c.cmd("gamerule doDaylightCycle false");
        c.setOption("hide_gui", "true");
        for (long time : new long[] {0L, 38_700L}) {
            c.markLog();
            c.cmd("ntmspace time " + time);
            c.waitTicks(10);
            Expect.that(!c.logSinceMark("space_time dim=ntmspace:mun time=" + time + " own=true").isEmpty(),
                    "the Mun's clock is set to " + time + ": " + c.logSinceMark("space_time"));
            JsonObject v = view(c);
            Expect.that(v.get("own_clock").getAsBoolean() && Math.abs(v.get("time_of_day").getAsLong() - time) <= 20,
                    "the client's Mun clock follows the server's (" + time + "): " + v);
            c.look(-90, -10);
            c.waitTicks(10);
            c.screenshot("60-mun-clock-" + time);
        }
        c.setOption("hide_gui", "false");
        c.cmd("gamerule doDaylightCycle true");

        // ---- the rocket camera
        c.cmd("execute in minecraft:overworld run tp @s 0 " + (Y + 1) + " 0");
        c.waitTicks(40);
        double onFoot;
        String previous = c.setOption("camera", "third_person_back");
        c.waitTicks(10);
        onFoot = view(c).get("camera_distance").getAsDouble();
        c.cmd("kill @e[type=ntmspace:entity_rideable_rocket]");
        c.cmd("summon ntmspace:entity_rideable_rocket 4.5 " + (Y + 1) + " 0.5");
        c.waitTicks(10);
        c.cmd("ride @s mount @e[type=ntmspace:entity_rideable_rocket,limit=1,sort=nearest]");
        c.waitTicks(20);
        JsonObject riding = view(c);
        Expect.that(riding.get("riding_rocket").getAsBoolean(), "the player rides the rocket: " + riding);
        double inRocket = riding.get("camera_distance").getAsDouble();
        Expect.that(onFoot < 6 && inRocket > 10, "the third-person camera stands about 12 blocks off in a rocket ("
                + onFoot + " on foot, " + inRocket + " riding)");
        c.look(0, 10);
        c.waitTicks(5);
        c.screenshot("61-rocket-third-person");
        c.cmd("ride @s dismount");
        c.cmd("kill @e[type=ntmspace:entity_rideable_rocket]");
        c.setOption("camera", previous == null ? "first_person" : previous);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    private static JsonObject view(Client c) {
        return c.query("ntmspace_view", new JsonObject());
    }
}
