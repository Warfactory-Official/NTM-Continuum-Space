import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * NTM: Space's progression machines, part C (magma drill, solar panel, steam radiator, gas giant harvester,
 * troll_disturber). None has a GUI. Each is placed by hand from the east (the machine extends west, its core
 * {@code offset} blocks from the clicked block and {@code heightOffset} up), photographed, and runs 1.7 steps read back
 * from the block entity's saved data:
 * <ul>
 *   <li>steam radiator (Kerbin): 300 mB spent steam become 300 mB water (1.7 converts as much as the 500 mB water tank
 *       takes, every tick); 30-radiator;</li>
 *   <li>solar panel (Kerbin, noon, clear): sky light 15 - 0 - 11 = 4, 100 HE per tick, so the 1,000 HE buffer is full
 *       after ten ticks; 31-solar;</li>
 *   <li>magma drill on Kerbin does nothing (1.7: Moho only) - its buffer keeps its power; on Moho over a 3x3 of lava
 *       four blocks below the core it draws 10,000 HE a tick and fills its molten buffer with slag and mineral-rich
 *       magma; 32-magma-kerbin, 33-magma-moho;</li>
 *   <li>gas giant harvester on Kerbin collects nothing (no gas giant); on Laythe (Jool's moon) 500 mB hydrogen and
 *       500 mB oxygen launch the scoop and bring 8,000 mB Jool gas; 34-gasdock-kerbin, 35-gasdock-laythe (rocket up);</li>
 *   <li>troll_disturber: placed, 36-troll.</li>
 * </ul>
 */
public final class MachinesC implements Scenario {

    private static final int Y = 100, L = 101;

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
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        c.cmd("weather clear");

        // ---------------------------------------------------------------- steam radiator (offset 0, height 1)
        int x = 0, z = 200;
        Fixtures.platform(c, x, Y, z, 6);
        Fixtures.placeOnTop(c, "ntmspace:machine_radiator", x, Y, z);
        int[] rad = core(c, x, L + 1, z, 0);
        Expect.block(c, rad[0], rad[1], rad[2], "ntmspace:machine_radiator");
        c.cmd("data merge block " + rad[0] + " " + rad[1] + " " + rad[2] + " {input:300}");
        c.waitTicks(10);
        String nbt = nbt(c, rad);
        Expect.that(nbt.contains("input:0") && nbt.contains("output:300"),
                "radiator: 300 mB spent steam -> 300 mB water (1.7 rate: as much as the water tank takes); nbt " + nbt);
        shoot(c, rad, 5, 2, "30-radiator");

        // ---------------------------------------------------------------- solar panel (offset 0)
        x = 30;
        Fixtures.platform(c, x, Y, z, 6);
        Fixtures.placeOnTop(c, "ntmspace:machine_solar", x, Y, z);
        int[] solar = {x, L, z};
        Expect.block(c, solar[0], solar[1], solar[2], "ntmspace:machine_solar");
        c.waitTicks(20);
        nbt = nbt(c, solar);
        Expect.that(nbt.contains("power:1000L") && nbt.contains("maxpwr:1000L"),
                "solar panel at noon on Kerbin: sun 4 -> 100 HE/t, buffer 1,000 HE full; nbt " + nbt);
        shoot(c, solar, 4, 3, "31-solar");

        // ---------------------------------------------------------------- magma drill on Kerbin (offset 3, height 3)
        x = 60;
        Fixtures.platform(c, x, Y, z, 8);
        Fixtures.placeOnTop(c, "ntmspace:machine_magma", x, Y, z);
        int[] magma = core(c, x, L + 3, z, 3);
        Expect.block(c, magma[0], magma[1], magma[2], "ntmspace:machine_magma");
        lavaUnder(c, magma);
        c.cmd("data merge block " + magma[0] + " " + magma[1] + " " + magma[2] + " {power:1000000L}");
        c.waitTicks(20);
        nbt = nbt(c, magma);
        Expect.that(nbt.contains("power:1000000L") && !nbt.contains("slag"),
                "magma drill on Kerbin must not run (1.7: Moho only); nbt " + nbt);
        shoot(c, magma, 9, 6, "32-magma-kerbin");

        // ---------------------------------------------------------------- gas dock on Kerbin (offset 1)
        x = 90;
        Fixtures.platform(c, x, Y, z, 6);
        Fixtures.placeOnTop(c, "ntmspace:gas_dock", x, Y, z);
        int[] dock = core(c, x, L, z, 1);
        Expect.block(c, dock[0], dock[1], dock[2], "ntmspace:gas_dock");
        c.cmd("data merge block " + dock[0] + " " + dock[1] + " " + dock[2] + " {f1:1000,f2:1000}");
        c.waitTicks(40);
        nbt = nbt(c, dock);
        Expect.that(nbt.contains("f1:1000") && nbt.contains("f2:1000") && !nbt.contains("gas:8000"),
                "gas dock on Kerbin collects nothing (no gas giant); nbt " + nbt);
        shoot(c, dock, 4, 2, "34-gasdock-kerbin");

        // ---------------------------------------------------------------- troll_disturber
        x = 120;
        Fixtures.platform(c, x, Y, z, 4);
        Fixtures.placeOnTop(c, "ntmspace:troll_disturber", x, Y, z);
        Expect.block(c, x, L, z, "ntmspace:troll_disturber");
        shoot(c, new int[] {x, L, z}, 3, 1, "36-troll");

        // ---------------------------------------------------------------- magma drill on Moho
        c.cmd("execute in ntmspace:moho run tp @s 0 " + (L + 10) + " " + z);
        c.waitTicks(40);
        Expect.equal("ntmspace:moho", c.status().get("dimension").getAsString(), "dimension after tp to Moho");
        x = 0;
        Fixtures.platform(c, x, Y, z, 8);
        Fixtures.placeOnTop(c, "ntmspace:machine_magma", x, Y, z);
        magma = core(c, x, L + 3, z, 3);
        Expect.block(c, magma[0], magma[1], magma[2], "ntmspace:machine_magma");
        lavaUnder(c, magma);
        c.cmd("data merge block " + magma[0] + " " + magma[1] + " " + magma[2] + " {power:1000000L}");
        c.waitTicks(30);
        nbt = nbt(c, magma);
        Expect.that(nbt.contains("slag") && nbt.contains("rich_magma") && !nbt.contains("power:1000000L"),
                "magma drill on Moho over lava draws power and melts slag + mineral-rich magma (saved by name); nbt " + nbt);
        shoot(c, magma, 9, 6, "33-magma-moho");

        // ---------------------------------------------------------------- gas dock on Laythe (Jool's moon)
        c.cmd("execute in ntmspace:laythe run tp @s 0 " + (L + 10) + " " + z);
        c.waitTicks(40);
        Expect.equal("ntmspace:laythe", c.status().get("dimension").getAsString(), "dimension after tp to Laythe");
        x = 0;
        Fixtures.platform(c, x, Y, z, 6);
        Fixtures.placeOnTop(c, "ntmspace:gas_dock", x, Y, z);
        dock = core(c, x, L, z, 1);
        Expect.block(c, dock[0], dock[1], dock[2], "ntmspace:gas_dock");
        c.waitTicks(25); // the empty dock counts launchTicks down to -20 and waits for fuel
        c.cmd("data merge block " + dock[0] + " " + dock[1] + " " + dock[2] + " {f1:1000,f2:1000}");
        c.waitTicks(25);
        shoot(c, dock, 6, 12, "35-gasdock-laythe");
        nbt = nbt(c, dock);
        Expect.that(nbt.contains("gas:8000") && nbt.contains("f1:500") && nbt.contains("f2:500")
                        && nbt.contains("gas_type:\"ntmspace:joolgas\""),
                "gas dock on Laythe: 500 + 500 mB fuel -> 8,000 mB Jool gas (one flight); nbt " + nbt);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem|magma|radiator|gas_dock|solar)");
    }

    /** The 3x3 of lava four blocks below the drill's core (the platform layer under the structure). */
    private static void lavaUnder(Client c, int[] core) {
        c.cmd("fill " + (core[0] - 1) + " " + (core[1] - 4) + " " + (core[2] - 1) + " " + (core[0] + 1) + " " + (core[1] - 4)
                + " " + (core[2] + 1) + " minecraft:lava");
        c.waitTicks(2);
    }

    private static void shoot(Client c, int[] at, double dist, double up, String name) {
        c.teleport(at[0] + dist + 0.5, at[1] + up, at[2] + dist + 0.5);
        c.lookAt(at[0] + 0.5, at[1] + 0.5, at[2] + 0.5);
        c.waitTicks(10);
        c.screenshot(name);
    }

    /** The core of a machine placed from the east: {@code offset} blocks west at {@code y}, else east. */
    private static int[] core(Client c, int x, int y, int z, int offset) {
        int[] west = {x - offset, y, z};
        JsonObject be = c.blockEntity(west[0], west[1], west[2]);
        if (be.has("nbt") && !be.get("nbt").isJsonNull()) return west;
        System.out.println("[machinesC] " + c.node() + " no core at " + west[0] + "," + y + "," + z + "; trying east");
        return new int[] {x + offset, y, z};
    }

    private static String nbt(Client c, int[] pos) {
        JsonObject be = c.blockEntity(pos[0], pos[1], pos[2]);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }
}
