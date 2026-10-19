import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * NTM: Space's progression machines, part B (Industrial Greenhouse, Vacuum Solderer):
 * <ul>
 *   <li>each is placed by hand on top of a platform block from the east (offset 0: the core is the placed block);</li>
 *   <li>both screens open from a cell (30-hydrobay-gui, 32-vacuum-gui) and both machines are photographed (31-hydrobay
 *       lit with its wheat, 33-vacuum with the recipe output on its plate);</li>
 *   <li>greenhouse: 2,000 HE, 1,000 mB CO2 and three wheat seeds through the block entity: the three plant positions get
 *       wheat, the seeds are used up, both lamp cells are {@code lit=true} and light the plant below them, 25 HE are
 *       drained per tick; CO2 + O2 stay 1,000 mB (15 mB per stage moved). Then 100 HE: the lamps go out;</li>
 *   <li>solderer: 8 chips, 2 PCBs and 4 capacitors on Kerbin (Earth air): the look-overlay state "cannot operate" holds
 *       and no progress is made ({@code progress:0}), the inputs stay (1.7: needs a near vacuum).</li>
 * </ul>
 * Inputs go in with {@code /item replace block}, so nothing depends on the save format of the inventory.
 */
public final class MachinesB implements Scenario {

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
        c.cmd("time set midnight"); // so the light at the plants is the lamps', not the sky's

        // ---------------------------------------------------------------- industrial greenhouse
        int x = 120, z = 0;
        Fixtures.platform(c, x, Y, z, 8);
        Fixtures.placeOnTop(c, "ntmspace:hydrobay", x, Y, z);
        int[] hydro = {x, L, z};
        Expect.block(c, hydro[0], hydro[1], hydro[2], "ntmspace:hydrobay");
        open(c, x, L, z, "GUIHydroponic");
        c.screenshot("30-hydrobay-gui");
        close(c);

        // the plant positions: the core and its two lateral neighbours, one up; the machine was placed facing west, so
        // the lateral axis is z (the lamps sit above the outer two, two up)
        int[][] plants = {{x, L + 1, z - 1}, {x, L + 1, z}, {x, L + 1, z + 1}};
        int[][] lamps = {{x, L + 2, z - 1}, {x, L + 2, z + 1}};
        if (!c.block(x, L + 1, z + 1).get("block").getAsString().equals("minecraft:air")) {
            // a wall north and south of the middle plant: the lateral axis is x
            plants = new int[][] {{x - 1, L + 1, z}, {x, L + 1, z}, {x + 1, L + 1, z}};
            lamps = new int[][] {{x - 1, L + 2, z}, {x + 1, L + 2, z}};
        }
        c.cmd("item replace block " + x + " " + L + " " + z + " container.0 with minecraft:wheat_seeds 3");
        c.cmd("data merge block " + x + " " + L + " " + z + " {power:2000L,t0:1000}");
        c.waitTicks(10);
        String nbt = nbt(c, hydro);
        for (int[] p : plants) Expect.block(c, p[0], p[1], p[2], "minecraft:wheat");
        for (int[] p : lamps) {
            JsonObject lamp = c.block(p[0], p[1], p[2]);
            Expect.that(lamp.get("state").getAsString().contains("lit=true"), "lamp cell lit while powered: " + lamp);
        }
        JsonObject under = c.block(plants[0][0], plants[0][1], plants[0][2]);
        Expect.that(under.get("light").getAsInt() >= 14, "the lamp lights the plant under it (1.7 dummy_beam, light 15): " + under);
        Expect.that(!nbt.contains("wheat_seeds"), "the seeds were planted; nbt " + nbt);
        Expect.that(nbt.contains("lights:1b"), "lights saved on; nbt " + nbt);
        int co2 = intField(nbt, "t0"), o2 = intField(nbt, "t1");
        Expect.that(co2 + o2 == 1000 && (1000 - co2) % 15 == 0, "CO2 -> O2 at 15 mB per growth stage: CO2 " + co2 + ", O2 " + o2 + "; nbt " + nbt);
        long power = longField(nbt, "power");
        Expect.that(power < 2000 && (2000 - power) % 25 == 0, "25 HE per tick: " + power + "; nbt " + nbt);
        c.teleport(x + 6.5, L + 3, z + 4.5);
        c.lookAt(x + 0.5, L + 1.5, z + 0.5);
        c.waitTicks(10);
        c.screenshot("31-hydrobay");
        c.cmd("data merge block " + x + " " + L + " " + z + " {power:100L}");
        c.waitTicks(5);
        for (int[] p : lamps) {
            JsonObject lamp = c.block(p[0], p[1], p[2]);
            Expect.that(lamp.get("state").getAsString().contains("lit=false"), "lamp cell dark without power: " + lamp);
        }

        // ---------------------------------------------------------------- vacuum solderer
        x = 160;
        Fixtures.platform(c, x, Y, z, 6);
        Fixtures.placeOnTop(c, "ntmspace:machine_vacuum_circuit", x, Y, z);
        int[] vac = {x, L, z};
        Expect.block(c, vac[0], vac[1], vac[2], "ntmspace:machine_vacuum_circuit");
        c.cmd("item replace block " + x + " " + L + " " + z + " container.0 with hbm:circuit_chip 8");
        c.cmd("item replace block " + x + " " + L + " " + z + " container.2 with hbm:circuit_pcb 2");
        c.cmd("item replace block " + x + " " + L + " " + z + " container.3 with hbm:circuit_capacitor 4");
        c.cmd("data merge block " + x + " " + L + " " + z + " {power:5000L}");
        open(c, x + 1, L, z, "GUIVacuumCircuit");
        c.screenshot("32-vacuum-gui");
        close(c);
        c.waitTicks(20);
        nbt = nbt(c, vac);
        Expect.that(nbt.contains("progress:0") && nbt.contains("circuit_chip"),
                "no soldering in Kerbin's air (1.7: near vacuum only); nbt " + nbt);
        c.teleport(x + 4.5, L + 2, z + 2.5);
        c.lookAt(x + 0.5, L + 1, z + 0.5);
        c.waitTicks(10);
        c.screenshot("33-vacuum");

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|couldn't parse|failed to load).*(recipe|ntmspace|golem|hydro|vacuum)");
    }

    private static int intField(String nbt, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[{,]" + name + ":(-?\\d+)[,}]").matcher(nbt);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static long longField(String nbt, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[{,]" + name + ":(-?\\d+)L").matcher(nbt);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    private static String nbt(Client c, int[] pos) {
        JsonObject be = c.blockEntity(pos[0], pos[1], pos[2]);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static void open(Client c, int x, int y, int z, String screen) {
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 6");
        c.teleport(x + 2.5, y, z + 0.5);
        c.waitTicks(4);
        Fixtures.interact(c, x, y, z);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    private static void close(Client c) {
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
    }
}
