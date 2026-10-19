import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.ArrayList;
import java.util.List;

/**
 * HBM's chemical plant as a datapack table, with NTM: Space on top (M1):
 * <ul>
 *   <li>the client received the table in Golem Core's sync packet: 117 rows (HBM's 90, config-branch rows included,
 *       minus the four air recipes and the three {@code biogas}/{@code rubber} rows NTM: Space removes
 *       ({@code recipe_removals/hbm_chemical_plant.json}, {@code chemical_plant.json}), plus its 33 rows
 *       ({@code data/ntmspace/recipe/chemical_plant}, hbm-space's versions of those among them); minus
 *       {@code chem.cobble}, plus {@code chem.probe_gas} and one conditional row from the probe addon:
 *       90 - 7 + 33 - 1 + 2), with {@code ntmspace:chemical_plant/hcl} and without {@code hbm:chemical_plant/oxygen};</li>
 *   <li>the machine's recipe selector lists the addon's row: searching "hydrochloric" finds it, a click picks it and the
 *       machine saves {@code chem.hcl} (10-selector-search, 11-machine-tooltip);</li>
 *   <li>searching "oxygen" finds nothing in the chemical plant (12-oxygen-gone).</li>
 * </ul>
 */
public final class SpaceChemicalPlant implements Scenario {

    private static final int Y = 100, L = 101, CHEM_X = 0, Z = 0;

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

        JsonObject params = new JsonObject();
        params.addProperty("table", "hbm:chemical_plant");
        JsonObject table = c.query("recipe_table", params);
        List<String> ids = new ArrayList<>();
        for (JsonElement id : table.getAsJsonArray("ids")) ids.add(id.getAsString());
        System.out.println("[chem] " + c.node() + " client received " + ids.size() + " chemical plant rows");
        Expect.equal(117, ids.size(), "chemical plant rows the client received (90 HBM - 7 removed + 33 NTM: Space - 1 + 2 probe)");
        Expect.that(!ids.contains("hbm:chemical_plant/biogas") && ids.contains("ntmspace:chemical_plant/biogas")
                && !ids.contains("hbm:chemical_plant/rubber") && ids.contains("ntmspace:chemical_plant/rubber"),
                "NTM: Space's biogas and rubber rows replace HBM's");
        Expect.that(ids.contains("ntmprobe:chemical_plant/probe_gas") && !ids.contains("hbm:chemical_plant/cobble"),
                "the probe addon's row is on the client and the row it removes is not");
        Expect.that(ids.contains("ntmspace:chemical_plant/hcl"), "the client has NTM: Space's chem.hcl row");
        Expect.that(!ids.contains("hbm:chemical_plant/oxygen") && !ids.contains("hbm:chemical_plant/birkeland"),
                "the air recipes NTM: Space switches off are not on the client");
        Expect.that(ids.contains("hbm:chemical_plant/hydrogen"), "HBM's own rows are on the client");

        Fixtures.platform(c, CHEM_X, Y, Z, 8);
        c.cmd("fill -8 " + L + " -8 8 " + (L + 5) + " 8 minecraft:air");
        c.cmd("gamerule sendCommandFeedback false");
        Fixtures.giveOnly(c, "hbm:machine_chemical_plant", 1);
        standAt(c, CHEM_X + 0.5, L, Z + 1 + 2 + 0.5);
        c.lookAt(CHEM_X + 0.5, Y + 0.98, Z + 1 + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
        Expect.block(c, CHEM_X, L, Z, "hbm:machine_chemical_plant");

        open(c);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        c.click(left + 16, top + 134);             // the recipe button
        c.waitScreen("RecipeSelector", 5_000L);
        JsonObject search = editBox(c);
        int gl = search.get("x").getAsInt() - 28, gt = search.get("y").getAsInt() - 111;
        c.click(search.get("center_x").getAsDouble(), search.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type("hydrochloric");
        c.waitTicks(4);
        c.mouseMove(gl + 16, gt + 26);
        c.waitTicks(4);
        c.screenshot("10-selector-search");
        c.click(gl + 16, gt + 26);                 // the only match
        c.waitTicks(3);
        c.click(gl + 160, gt + 98);                // confirm and close
        c.waitTicks(6);
        if (c.screenClass() != null && c.screenClass().contains("RecipeSelector")) close(c);
        String nbt = nbt(c, CHEM_X, L, Z);
        Expect.that(nbt.contains("recipe0:\"chem.hcl\""), "the selector picked NTM: Space's chem.hcl; nbt " + nbt);

        if (c.screenClass() == null) open(c);
        s = c.screen();
        left = s.get("left").getAsInt();
        top = s.get("top").getAsInt();
        c.mouseMove(left + 16, top + 134);         // the selector button shows the active recipe
        c.waitTicks(4);
        c.screenshot("11-machine-tooltip");

        c.click(left + 16, top + 134);
        c.waitScreen("RecipeSelector", 5_000L);
        search = editBox(c);
        c.click(search.get("center_x").getAsDouble(), search.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type("oxygen");
        c.waitTicks(4);
        c.screenshot("12-oxygen-gone");
        closeScreens(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|couldn't parse|failed to load).*(recipe|chemical|ntmspace|golem)");
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static void standAt(Client c, double x, double y, double z) {
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 4.5");
        c.teleport(x, y, z);
        c.waitTicks(4);
    }

    private static void open(Client c) {
        c.hotbar(9);
        standAt(c, CHEM_X + 0.5, L + 1, Z + 0.5 + 2.5);
        Fixtures.interact(c, CHEM_X, L + 1, Z);
        c.waitScreen("GUIMachineChemicalPlant", 5_000L);
        Expect.screenStaysOpen(c, "GUIMachineChemicalPlant", 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static void closeScreens(Client c) {
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }
}
