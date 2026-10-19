import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The milk reformer on 1.7's connection points: a cable on a face that is not one of its points gives it no power, a
 * cable on a point does, and a providing barrel of milk on another point fills its input tank, the two ports facing each
 * other (10-ports with the debug port view).
 */
public final class SpacePorts implements Scenario {

    private static final int Y = 100, L = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode creative");
        c.cmd("execute in minecraft:overworld run tp @s 0.5 101 0.5");
        c.waitTicks(20);
        c.markLog();

        int x = 120, z = 0;
        Fixtures.platform(c, x, Y, z, 8);
        Fixtures.placeOnTop(c, "ntmspace:machine_milk_reformer", x, Y, z);
        int cx = x - 1, cy = L, cz = z;
        Expect.block(c, cx, cy, cz, "ntmspace:machine_milk_reformer");

        // the middle of the east face is no 1.7 point
        Fixtures.setBlock(c, cx + 3, cy, cz, "hbm:creative_generator");
        Fixtures.setBlock(c, cx + 2, cy, cz, "hbm:red_cable");
        c.waitTicks(40);
        long off = number(c, cx, cy, cz, "power");
        Expect.that(off == 0, "a cable off the 1.7 points must not power the reformer, power " + off);

        // (x + 2, z + 1) is one
        Fixtures.setBlock(c, cx + 2, cy, cz + 1, "hbm:red_cable");
        Fixtures.setBlock(c, cx - 2, cy, cz + 1, "hbm:barrel_steel{mode:2s,tank:16000,tank_max:16000,tank_type:\"ntmspace:milk\"}");
        c.cmd("ntmserver set DEBUG_PORT_VIEW true");
        c.teleport(cx + 6.5, cy + 3, cz + 6.5);
        c.lookAt(cx + 0.5, cy, cz + 0.5);
        c.waitTicks(60);
        c.screenshot("10-ports");
        c.cmd("ntmserver set DEBUG_PORT_VIEW false");

        long power = number(c, cx, cy, cz, "power");
        long milk = number(c, cx, cy, cz, "input");
        long cream = number(c, cx, cy, cz, "m1");
        Expect.that(power > 0 || cream > 0, "a cable on a 1.7 point powers the reformer: power " + power + ", m1 " + cream);
        Expect.that(milk > 0 || cream > 0, "a milk barrel on a 1.7 point fills the input: input " + milk + ", m1 " + cream);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static long number(Client c, int x, int y, int z, String key) {
        JsonObject be = c.blockEntity(x, y, z);
        if (!be.has("nbt") || be.get("nbt").isJsonNull()) return -1;
        Matcher m = Pattern.compile("(?<![\\w_])" + key + "\\s*:\\s*(-?\\d+)").matcher(be.get("nbt").getAsString());
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }
}
