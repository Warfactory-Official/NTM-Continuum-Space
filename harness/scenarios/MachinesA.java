import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * NTM: Space's progression machines, part one (cryogenic distiller, milk reformer, alkylation unit):
 * <ul>
 *   <li>each is placed by hand on top of a platform block, from the east (the machine extends west, away from the player,
 *       its core {@code offset} blocks from the clicked block and {@code heightOffset} up, as 1.7's BlockDummyable);</li>
 *   <li>the cryo distiller's and the milk reformer's screens open from a cell (20-cryo-gui, 22-milk-gui) and the machines
 *       are photographed from outside (21-cryo, 23-milk, 24-alkylation);</li>
 *   <li>one 1.7 step each, fed through the block entity's saved data: Earth air 300 mB + 20,000 HE -> 60/25/10/5
 *       (200 left); milk 100 + 10,000 HE -> 50/35/15; petroleum 100 + sulfuric acid 50 + 4,000 HE -> light oil 50 +
 *       peroxide 20. Exactly one step's power each, so the result does not depend on how many ticks passed.</li>
 * </ul>
 */
public final class MachinesA implements Scenario {

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

        // ---------------------------------------------------------------- cryogenic distiller (offset 3, height 2)
        int x = 0, z = 0;
        Fixtures.platform(c, x, Y, z, 10);
        c.cmd("gamerule sendCommandFeedback false");
        Fixtures.placeOnTop(c, "ntmspace:machine_cryo_distill", x, Y, z);
        int[] cryo = core(c, x, L + 2, z, 3);
        Expect.block(c, cryo[0], cryo[1], cryo[2], "ntmspace:machine_cryo_distill");
        // the front-bottom cell the player clicked on sits where the item went
        open(c, x, L, z, "GUIMachineCryoDistill");
        c.screenshot("20-cryo-gui");
        close(c);
        c.cmd("data merge block " + cryo[0] + " " + cryo[1] + " " + cryo[2] + " {power:20000L,input:300}");
        c.waitTicks(10);
        String nbt = nbt(c, cryo);
        Expect.that(nbt.contains("input:200") && nbt.contains("o1:60") && nbt.contains("o2:25") && nbt.contains("o3:10")
                && nbt.contains("o4:5") && nbt.contains("power:0L"), "one cryo step (1.7: 100 mB air -> 60/25/10/5); nbt " + nbt);
        Expect.that(nbt.contains("input_type:\"ntmspace:earthair\"") && nbt.contains("o1_type:\"ntmspace:nitrogen\""),
                "cryo tanks saved by fluid name; nbt " + nbt);
        c.teleport(x + 8.5, L + 4, z + 8.5);
        c.lookAt(cryo[0] + 0.5, cryo[1], cryo[2] + 0.5);
        c.waitTicks(10);
        c.screenshot("21-cryo");

        // ---------------------------------------------------------------- milk reformer (offset 1)
        x = 40;
        Fixtures.platform(c, x, Y, z, 8);
        Fixtures.placeOnTop(c, "ntmspace:machine_milk_reformer", x, Y, z);
        int[] milk = core(c, x, L, z, 1);
        Expect.block(c, milk[0], milk[1], milk[2], "ntmspace:machine_milk_reformer");
        open(c, x, L, z, "GUIMilkReformer");
        c.screenshot("22-milk-gui");
        close(c);
        c.cmd("data merge block " + milk[0] + " " + milk[1] + " " + milk[2] + " {power:10000L,input:100}");
        c.waitTicks(10);
        nbt = nbt(c, milk);
        Expect.that(nbt.contains("input:0") && nbt.contains("m1:50") && nbt.contains("m2:35") && nbt.contains("m3:15"),
                "one milk step (1.7: 100 mB -> 50/35/15); nbt " + nbt);
        c.teleport(x + 6.5, L + 3, z + 6.5);
        c.lookAt(milk[0] + 0.5, milk[1] + 3, milk[2] + 0.5);
        c.waitTicks(10);
        c.screenshot("23-milk");

        // ---------------------------------------------------------------- alkylation unit (offset 2, no GUI)
        x = 80;
        Fixtures.platform(c, x, Y, z, 8);
        Fixtures.placeOnTop(c, "ntmspace:machine_alkylation", x, Y, z);
        int[] alk = core(c, x, L, z, 2);
        Expect.block(c, alk[0], alk[1], alk[2], "ntmspace:machine_alkylation");
        c.cmd("data merge block " + alk[0] + " " + alk[1] + " " + alk[2]
                + " {power:4000L,t0:100,t0_type:\"hbm:petroleum\",t1:50,t1_type:\"hbm:sulfuric_acid\"}");
        c.waitTicks(10);
        nbt = nbt(c, alk);
        Expect.that(nbt.contains("t2:50") && nbt.contains("t3:20") && nbt.contains("t0:0") && nbt.contains("t1:0"),
                "one alkylation step (1.7: petroleum 100 + sulfuric acid 50 -> light oil 50 + peroxide 20); nbt " + nbt);
        Expect.that(nbt.contains("t2_type:\"hbm:lightoil\"") && nbt.contains("t3_type:\"hbm:peroxide\""), "alkylation outputs; nbt " + nbt);
        c.teleport(x + 6.5, L + 3, z + 6.5);
        c.lookAt(alk[0] + 0.5, alk[1] + 1, alk[2] + 0.5);
        c.waitTicks(10);
        c.screenshot("24-alkylation");

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|couldn't parse|failed to load).*(recipe|ntmspace|golem|cryo|alkylation)");
    }

    /**
     * The core of a machine placed from the east onto {@code (x, ?, z)}: {@code offset} blocks west, at {@code y}. Falls
     * back to east (the other convention) so a wrong guess fails on the block check with both positions named.
     */
    private static int[] core(Client c, int x, int y, int z, int offset) {
        int[] west = {x - offset, y, z};
        JsonObject be = c.blockEntity(west[0], west[1], west[2]);
        if (be.has("nbt") && !be.get("nbt").isJsonNull()) return west;
        System.out.println("[machinesA] " + c.node() + " no core at " + west[0] + "," + y + "," + z + "; trying east");
        return new int[] {x + offset, y, z};
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
