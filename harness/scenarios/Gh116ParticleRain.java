import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 116: with Particle Rain in the client's mods folder (set {@code GH116_WITH_MOD=true} for the harness)
 * Space leaves the overworld's weather to vanilla's method, which that mod hooks: Golem draws no quads there. Without it
 * Space draws 1.7's rain. Eve keeps Space's weather either way.
 */
public final class Gh116ParticleRain implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        boolean withMod = "true".equals(System.getenv("GH116_WITH_MOD"));
        c.waitInWorld(180_000L);
        c.cmd("gamemode creative");
        c.cmd("execute in minecraft:overworld run tp @s 0 120 0");
        c.waitTicks(100);
        c.cmd("weather rain 900");
        c.setOption("hide_gui", "true");
        c.look(90, 10);
        c.waitTicks(300);
        JsonObject first = sky(c);
        c.waitTicks(40);
        JsonObject second = sky(c);
        long drawn = second.get("weather_quads").getAsLong() - first.get("weather_quads").getAsLong();
        System.out.println("[Gh116] " + c.node() + " with mod " + withMod + ": " + second.get("body") + " custom "
                + second.get("weather_custom") + ", " + drawn + " quads in 40 ticks");
        c.screenshot(withMod ? "overworld-rain-particlerain" : "overworld-rain-space");
        Expect.that(second.get("weather_custom").getAsBoolean() != withMod, "overworld weather owner: " + second);
        Expect.that(withMod ? drawn == 0 : drawn > 0, "Golem drew " + drawn + " quads in the overworld");

        c.cmd("execute in ntmspace:eve run tp @s 0 200 0");
        c.waitTicks(100);
        JsonObject eve = sky(c);
        System.out.println("[Gh116] " + c.node() + " body after tp: " + eve.get("body") + " custom " + eve.get("weather_custom"));
        Expect.that(eve.get("weather_custom").getAsBoolean(), "Space keeps Eve's weather: " + eve);
        c.setOption("hide_gui", "false");
        c.cmd("execute in minecraft:overworld run tp @s 0 101 0");
        c.cmd("weather clear");
    }

    private static JsonObject sky(Client c) {
        return c.query("ntmspace_sky_shaders", new JsonObject());
    }
}
