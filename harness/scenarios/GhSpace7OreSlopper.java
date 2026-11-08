import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * NTM: Space issue 7: with NTM: Space the ore slopper's JEI page listed every body's ore types in one recipe, a grid far
 * past the panel. hbm-space's {@code OreSlopperHandler} shows one recipe per body (the body's raw ore, its four to six
 * types and slop), which 1.7's layouts fit. Opens the category, pages through it and screenshots each page; with EMI in
 * the run (1.21.1 NeoForge only) opens EMI's page too. The centrifuge and crystallizer pages are filmed for the
 * Space types as well.
 */
public final class GhSpace7OreSlopper implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(40);
        c.markLog();
        // EMI takes JEI's runtime over, so a run with EMI checks EMI's page instead
        if (!has(c, "emi")) {
            long recipes = open(c, "hbm:ore_slopper", "01-jei-ore-slopper");
            System.out.println("[GhSpace7] JEI ore slopper recipes: " + recipes);
            Expect.that(recipes == 10, "one ore slopper recipe per celestial body with a table (10): " + recipes);
            for (int page = 2; page <= 4; page++) {
                // JEI's next-page arrow beside "1/4", at 813,140 on the 1280x720 window
                double scale = c.status().get("gui_scale").getAsDouble();
                c.click(813 / scale, 140 / scale);
                c.waitTicks(10);
                c.screenshot(String.format("%02d-jei-ore-slopper-page", page));
            }
            c.key("escape");
            c.waitTicks(10);
            open(c, "hbm:centrifuge", "05-jei-centrifuge");
            c.key("escape");
            c.waitTicks(10);
            open(c, "hbm:crystallizer", "06-jei-crystallizer");
            c.key("escape");
            c.waitTicks(10);
        } else {
            JsonObject p = new JsonObject();
            p.addProperty("op", "open");
            p.addProperty("category", "hbm:ore_slopper");
            JsonObject opened = c.query("emi", p);
            c.waitTicks(30);
            c.screenshot("07-emi-ore-slopper");
            System.out.println("[GhSpace7] EMI " + opened);
            Expect.that(opened.has("opened"), "EMI opened the ore slopper: " + opened);
            c.key("escape");
            c.waitTicks(10);
        }
        // HBM and NTM: Space both listed the patrol drone in the machines tab; 1.21.1 NeoForge then refused the tab
        c.cmd("gamemode creative");
        c.key("e");
        c.waitTicks(20);
        c.screenshot("08-creative-inventory");
        c.key("escape");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "(?i)(exception|already exists in the tab|\\[JEI\\].*error).*");
        System.out.println("[GhSpace7] PASS");
    }

    private static boolean has(Client c, String query) {
        try {
            c.query(query, new JsonObject());
            return true;
        } catch (RuntimeException e) {
            // "unknown query kind": that viewer is not in this run
            return false;
        }
    }

    private static long open(Client c, String category, String shot) {
        JsonObject p = new JsonObject();
        p.addProperty("category", category);
        JsonObject opened = c.query("jei", p);
        Expect.that(opened.has("opened") && opened.get("opened").getAsBoolean(), "JEI opened " + category + ": " + opened);
        c.waitTicks(30);
        c.screenshot(shot);
        return opened.get("recipes").getAsLong();
    }
}
