import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * NTM: Space's items and blocks in HBM's category and convention tags (HBM GitHub issue 132), merged with HBM's own
 * members. Asked through {@code /clear @s <tag> 0} on a held item and {@code /execute if block} on a placed one. NTM: Space
 * adds no guns or armour, so an HBM gun stands in for the gun tag.
 */
public final class Gh132SpaceTags implements Scenario {

    private static final String[][] ITEMS = {
            {"hbm:food", "ntmspace:strawberry"},
            {"c:foods", "ntmspace:strawberry"},
            {"hbm:missiles", "ntmspace:rp_pod_20"},
            {"hbm:bombs", "ntmspace:nuke_antimatter"},
            {"hbm:machines", "ntmspace:machine_magma"},
            {"hbm:hazardous/radioactive", "ntmspace:ingot_cf252"},
            {"hbm:hazardous/neutron", "ntmspace:ingot_cf252"},
            {"hbm:hazardous/neutron", "hbm:ingot_pu240"},
            {"hbm:hazardous", "ntmspace:ingot_cf252"},
            {"c:ingots", "ntmspace:ingot_cf252"},
            {"hbm:blast_resistant/nuclear", "ntmspace:ore_schrabidium"},
            {"hbm:blast_resistant", "ntmspace:ore_schrabidium"},
            {"hbm:guns", "hbm:gun_light_revolver"},
            {"hbm:food", "hbm:apple_lead"},
    };

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 6);
        c.cmd("gamemode creative");
        String convention = c.node().startsWith("1.20.1-forge") ? "forge:" : "c:";
        for (String[] row : ITEMS) {
            String tag = row[0].startsWith("c:") ? convention + row[0].substring(2) : row[0];
            Expect.that(counts(c, tag, row[1]), tag + " holds " + row[1]);
        }
        Expect.that(!counts(c, "hbm:hazardous", "ntmspace:strawberry"), "a strawberry is harmless");

        Fixtures.setBlock(c, 0, G, 2, "ntmspace:ore_schrabidium");
        Expect.that(blockIs(c, "hbm:blast_resistant/nuclear"), "schrabidium ore is a nuclear-tier block");
        Expect.that(!blockIs(c, "hbm:blast_resistant/obsidian"), "schrabidium ore is below the obsidian tier");
        Fixtures.setBlock(c, 0, G, 2, "ntmspace:ore_bedrock_oil");
        Expect.that(blockIs(c, "hbm:blast_resistant/obsidian"), "bedrock oil is an obsidian-tier block");
        Fixtures.setBlock(c, 0, G, 2, "hbm:cmb_brick");
        Expect.that(blockIs(c, "hbm:blast_resistant/obsidian"), "HBM's CMB bricks keep their tier");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|ntmspace|golem|tag)");
        System.out.println("[Gh132Space] PASS");
    }

    /** Whether {@code id} in the hand is a member of {@code tag}: {@code /clear} with a zero count only counts. */
    private static boolean counts(Client c, String tag, String id) {
        c.cmd("clear");
        c.cmd("give @s " + id);
        c.waitTicks(3);
        c.markLog();
        c.cmd("clear @s #" + tag + " 0");
        c.waitTicks(5);
        List<String> lines = c.logSinceMark("matching item");
        boolean found = lines.stream().anyMatch(line -> line.contains("Found 1 matching item"));
        System.out.println("[Gh132Space] " + tag + " " + id + ": " + found + " " + lines);
        return found;
    }

    private static boolean blockIs(Client c, String tag) {
        c.markLog();
        c.cmd("execute if block 0 " + G + " 2 #" + tag);
        c.waitTicks(5);
        return !c.logSinceMark("Test passed").isEmpty();
    }
}
