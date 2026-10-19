import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.util.HashSet;
import java.util.Set;

/**
 * hbm-space's changes to base HBM recipes and its remaining items (M3 recipe-change family):
 * <ul>
 *   <li>the client received the changed machine tables in Golem Core's sync packet: the addon's rows are there, the HBM
 *       rows it removes are not (chemical plant air rows, the assembly lunar miner, the shredder nether fire ore);</li>
 *   <li>a hotbar of the new items (stainless, nickel, curium, drinks, the expensive parts, the coke powder) for a human to
 *       look at: every icon must be its hbm-space sprite, no magenta/black (01-new-items).</li>
 * </ul>
 * Removal ids that a sibling agent did not end up writing are reported, not failed: the generic GameTest
 * {@code recipe_change_removals_absent} is the authority on the server side.
 */
public final class RecipeChanges implements Scenario {

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

        Set<String> chem = ids(c, "hbm:chemical_plant");
        Expect.that(chem.contains("ntmspace:chemical_plant/hcl"), "the client has the addon's chem.hcl row");
        Expect.that(!chem.contains("hbm:chemical_plant/oxygen"), "chem.oxygen (supplied by the cryo distiller) is gone");

        Set<String> welder = ids(c, "hbm:arc_welder");
        Expect.that(welder.contains("ntmspace:arc_welder/rp_fuselage_20_12"), "the stainless 20 fuselage welds on the client");

        Set<String> assembly = ids(c, "hbm:assembly_machine");
        Expect.that(assembly.contains("ntmspace:assembly_machine/orbitalstationlauncher"),
                "the station launcher assembles on the client");
        Expect.that(assembly.contains("ntmspace:assembly_machine/rppod20"), "the drop pod assembles on the client");
        System.out.println("[recipes] " + c.node() + " lunar miner row present: "
                + assembly.contains("hbm:assembly_machine/sat_lunar_miner"));

        c.cmd("clear @p");
        String[] items = {"ntmspace:ingot_stainless", "ntmspace:plate_welded_stainless", "ntmspace:ingot_nickel",
                "ntmspace:billet_cm_mix", "ntmspace:item_expensive_avionics", "ntmspace:powder_coke_petroleum",
                "ntmspace:teacup", "ntmspace:glass_smilk", "ntmspace:mineral_fragment_nitric"};
        for (String id : items) c.cmd("give @p " + id + " 1");
        c.waitTicks(10);
        c.screenshot("01-new-items");
        for (String id : items) Expect.inventoryHolds(c, id, 1);
    }

    private static Set<String> ids(Client c, String table) throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("table", table);
        JsonObject result = c.query("recipe_table", params);
        Set<String> ids = new HashSet<>();
        for (JsonElement id : result.getAsJsonArray("ids")) ids.add(id.getAsString());
        System.out.println("[recipes] " + c.node() + " " + table + ": " + ids.size() + " rows");
        return ids;
    }
}
