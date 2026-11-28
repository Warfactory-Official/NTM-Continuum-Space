import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NTM: Space thrusters on Sable ships (1.21.1 NeoForge with {@code ORG_GRADLE_PROJECT_sable.run=true}). Each thruster is
 * placed by hand twice from the same spot and aim: once on an iron block that is then assembled into a ship, once onto
 * that iron block after it became a ship. Both ships must weigh the same: a thruster placed onto a ship lost every
 * block that fell in a plot chunk the ship did not have yet. Then the HTR-F NEO's plasma end against a fusion torus:
 * the torus draws the connector of an arm whose receiver it found.
 */
public final class SableThrusters implements Scenario {

    private static final int Y = 100, D = Y + 1;
    private static final Pattern MASS = Pattern.compile("Mass: (-?[0-9.]+)");
    private static final String[] THRUSTERS = {
            "ntmspace:machine_xenon_thruster", "ntmspace:machine_htr3", "hbm:machine_lpw2", "ntmspace:machine_htrf4neo"};

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.resetWorld();
        c.cmd("gamemode creative");
        c.cmd("sable remove @e");
        c.markLog();
        for (int i = 0; i < THRUSTERS.length; i++) {
            String id = THRUSTERS[i];
            String name = id.substring(id.indexOf(':') + 1);
            int x = i * 80;
            double assembled = assembledMass(c, id, x, -40, name);
            double placed = placedMass(c, id, x, 40, name);
            System.out.printf("[SableThrusters] %s: built then assembled %.2f, placed onto the ship %.2f%n", id, assembled, placed);
            Expect.that(Math.abs(assembled - placed) < 0.01,
                    id + " placed onto a ship weighs " + placed + ", built in the world and assembled " + assembled);
        }
        neoAgainstTorus(c);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /** Builds on an iron block in the world, then assembles the area. */
    private static double assembledMass(Client c, String id, int x, int z, String name) throws Exception {
        Fixtures.platform(c, x, Y, z, 30);
        Fixtures.setBlock(c, x, D, z, "minecraft:iron_block");
        place(c, id, x, z);
        c.cmd("sable assemble area " + (x - 26) + " " + D + " " + (z - 26) + " " + (x + 26) + " " + (D + 12) + " " + (z + 26));
        c.waitTicks(20);
        double mass = mass(c);
        view(c, x, z, name + "-assembled");
        c.cmd("sable remove @e");
        c.waitTicks(5);
        return mass;
    }

    /** Assembles the lone iron block, then builds on it. */
    private static double placedMass(Client c, String id, int x, int z, String name) throws Exception {
        Fixtures.platform(c, x, Y, z, 30);
        Fixtures.setBlock(c, x, D, z, "minecraft:iron_block");
        c.cmd("sable assemble area " + x + " " + D + " " + z + " " + x + " " + D + " " + z);
        c.waitTicks(20);
        double deck = mass(c);
        place(c, id, x, z);
        c.waitTicks(20);
        double mass = mass(c);
        Expect.that(mass > deck, id + " added no mass to the ship (" + deck + " -> " + mass + ")");
        view(c, x, z, name + "-placed");
        c.cmd("sable remove @e");
        c.waitTicks(5);
        return mass;
    }

    private static void place(Client c, String id, int x, int z) {
        Fixtures.giveOnly(c, id, 1);
        c.teleport(x + 2.5, D, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, D + 0.98, z + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(10);
        c.cmd("clear");
    }

    private static void view(Client c, int x, int z, String shot) {
        c.teleport(x + 18.5, D + 10, z + 18.5);
        c.lookAt(x + 0.5, D + 2, z + 0.5);
        c.waitTicks(40);
        c.screenshot(shot);
    }

    /** The one ship's mass, from {@code /sable info}. */
    private static double mass(Client c) throws Exception {
        c.markLog();
        c.cmd("sable info @e");
        c.waitTicks(5);
        List<String> lines = c.logSinceMark("Mass:");
        Expect.that(lines.size() == 1, "/sable info reported one ship: " + lines);
        Matcher m = MASS.matcher(lines.get(0));
        Expect.that(m.find(), "mass line parses: " + lines.get(0));
        return Double.parseDouble(m.group(1));
    }

    /**
     * 1.7's layout, placed by hand looking east: the torus core lands 7 ahead of its click, the NEO's core 11 ahead and
     * 2 up (facing south), so its plasma end (core + 9) touches the torus's west edge at core + 10.
     */
    private static void neoAgainstTorus(Client c) throws Exception {
        int px = 420, pz = 0;
        Fixtures.platform(c, px + 14, Y, pz, 30);
        Fixtures.giveOnly(c, "hbm:fusion_torus", 1);
        aimEast(c, px + 21, pz);
        Fixtures.giveOnly(c, "ntmspace:machine_htrf4neo", 1);
        aimEast(c, px, pz);
        c.cmd("clear");
        String torus = c.blockEntity(px + 28, D, pz).toString();
        String neo = c.blockEntity(px + 11, D + 2, pz).toString();
        Expect.that(torus.contains("fusion_torus"), "the torus core is at the expected spot: " + torus);
        Expect.that(neo.contains("machine_htrf4neo"), "the NEO core is at the expected spot: " + neo);
        c.teleport(px + 20.5, D + 12, pz + 18.5);
        c.lookAt(px + 21.5, D + 2, pz + 0.5);
        c.waitTicks(60);
        c.screenshot("neo-torus");
    }

    private static void aimEast(Client c, int x, int z) {
        c.teleport(x - 1.5, D, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(10);
    }
}
