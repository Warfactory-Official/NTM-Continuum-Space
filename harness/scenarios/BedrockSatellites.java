import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.List;

/**
 * NTM: Space's per-body bedrock ores and satellites across the bodies, on a real client:
 *
 * <ul>
 *   <li>Duna: the ore density scanner's HUD lists Duna's six types (Actinide among them, which Kerbin has not) with a
 *       tier line (00-duna-scanner-hud); {@code /ntmbedrock sample} collects raw bedrock ore in the column through
 *       HBM's per-level {@code ItemBedrockOreBase.setOreAmount} and logs {@code bedrock_sample ... body.duna} with six
 *       {@code ntmspace:duna_*} amounts; the same on Kerbin gives four {@code ntmspace:kerbin_*} and none of HBM's six;</li>
 *   <li>one processing step in the real centrifuge, through its menu: a Duna light-metal BASE ore gives a Duna
 *       light-metal primary fraction and gravel (10-duna-centrifuge-loaded, 11-duna-centrifuge-done);</li>
 *   <li>the Mun: the satellite ID manager copies a chip's frequency (20-mun-linker, the "linker reading"); a mapper
 *       launched on the Mun with {@code /ntmsatellites orbit <frequency>} (hbm-space's override) is listed by
 *       {@code /ntmsatellites list} on the Mun and not on Kerbin.</li>
 * </ul>
 */
public final class BedrockSatellites implements Scenario {

    private static final int Y = 150, G = Y + 1;

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
        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:resistance 1000 255 true");

        // ---------------------------------------------------------------- Duna: scanner and raw ore
        c.cmd("execute in ntmspace:duna run tp @s 0 " + G + " 0");
        c.waitTicks(40);
        Expect.equal("ntmspace:duna", c.status().get("dimension").getAsString(), "dimension after the Duna teleport");
        Fixtures.platform(c, 0, Y, 0, 8);
        Fixtures.giveOnly(c, "hbm:ore_density_scanner", 1);
        c.waitTicks(30);
        String info = info(c);
        Expect.that(info.contains("Tier"), "the scanner's tier line on Duna: " + info);
        Expect.that(info.contains("Actinide") && info.contains("Rare Earth") && info.contains("Crystalline"),
                "the scanner lists Duna's types (actinide, rare earth, crystalline): " + info);
        c.screenshot("00-duna-scanner-hud");

        c.markLog();
        c.cmd("ntmbedrock sample");
        c.waitTicks(10);
        List<String> sample = c.logSinceMark("bedrock_sample");
        Expect.that(!sample.isEmpty(), "/ntmbedrock sample logged a sample");
        String duna = sample.get(sample.size() - 1);
        Expect.that(duna.contains("ntmspace:duna body.duna") && count(duna, "ntmspace:duna_") == 6,
                "raw ore on Duna carries Duna's six types: " + duna);
        Expect.inventoryHolds(c, "hbm:bedrock_ore_base", 1);

        c.cmd("execute in minecraft:overworld run tp @s 0 " + G + " 0");
        c.waitTicks(40);
        Fixtures.platform(c, 0, Y, 0, 3);
        c.markLog();
        c.cmd("ntmbedrock sample");
        c.waitTicks(10);
        sample = c.logSinceMark("bedrock_sample");
        Expect.that(!sample.isEmpty(), "/ntmbedrock sample logged a sample on Kerbin");
        String kerbin = sample.get(sample.size() - 1);
        Expect.that(kerbin.contains("body.kerbin") && count(kerbin, "ntmspace:kerbin_") == 4 && !kerbin.contains(" light="),
                "raw ore on Kerbin carries Kerbin's four types, none of HBM's six: " + kerbin);

        // ---------------------------------------------------------------- Duna: one centrifuge step
        c.cmd("execute in ntmspace:duna run tp @s 0 " + G + " 0");
        c.waitTicks(40);
        int cx = 0, cz = 0;
        placeMachine(c, "hbm:machine_centrifuge", cx, cz);
        int[] cent = findCore(c, cx, G, cz, "TileEntityMachineCentrifuge");
        Expect.that(cent != null, "the centrifuge was placed on Duna");
        c.cmd("data merge block " + cent[0] + " " + cent[1] + " " + cent[2] + " {power:100000L}");
        c.cmd("clear");
        c.cmd("give @s ntmspace:bedrock_ore_base_duna_light 1");
        c.waitTicks(3);
        c.teleport(cx + 0.5, G, cz - 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, cent[0], cent[1], cent[2]);
        c.waitScreen("GUICentrifuge", 5000);
        Expect.screenStaysOpen(c, "GUICentrifuge", 5);
        clickFromInventory(c, "ntmspace:bedrock_ore_base_duna_light", 0);
        c.screenshot("10-duna-centrifuge-loaded");
        c.waitTicks(240);
        c.screenshot("11-duna-centrifuge-done");
        Expect.slotHolds(c, 2, "ntmspace:bedrock_ore_primary_duna_light", 1);
        Expect.slotHolds(c, 3, "minecraft:gravel", 1);
        close(c);

        // ---------------------------------------------------------------- the Mun: linker and a launched satellite
        c.cmd("execute in ntmspace:mun run tp @s 0 " + G + " 0");
        c.waitTicks(40);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "dimension after the Mun teleport");
        Fixtures.platform(c, 0, Y, 0, 8);
        placeMachine(c, "hbm:machine_satlinker", 0, 0);
        Expect.block(c, 0, G, 0, "hbm:machine_satlinker");
        c.cmd("clear");
        c.cmd("give @s hbm:satellite_spy 1");
        c.cmd("give @s hbm:sat_coord 1");
        c.waitTicks(3);
        c.teleport(0.5, G, -2.5);
        c.waitTicks(2);
        Fixtures.interact(c, 0, G, 0);
        c.waitScreen("SatLinker", 5000);
        clickFromInventory(c, "hbm:satellite_spy", 0);
        clickFromInventory(c, "hbm:sat_coord", 1);
        c.waitTicks(10);
        c.screenshot("20-mun-linker");
        Expect.slotHolds(c, 0, "hbm:satellite_spy", 1);
        Expect.slotHolds(c, 1, "hbm:sat_coord", 1);
        close(c);

        int freq = 700_000 + (int) (System.currentTimeMillis() % 100_000);
        Fixtures.giveOnly(c, "hbm:satellite_spy", 1);
        c.markLog();
        c.cmd("ntmsatellites orbit " + freq);
        c.waitTicks(10);
        c.cmd("ntmsatellites list");
        c.waitTicks(10);
        List<String> munList = c.logSinceMark(freq + " - ");
        c.screenshot("21-mun-satellite-list");

        c.cmd("execute in minecraft:overworld run tp @s 0 " + G + " 0");
        c.waitTicks(40);
        c.markLog();
        c.cmd("ntmsatellites list");
        c.waitTicks(10);
        List<String> kerbinList = c.logSinceMark(freq + " - ");

        c.cmd("execute in ntmspace:mun run tp @s 0 " + G + " 0");
        c.waitTicks(40);
        c.cmd("ntmsatellites descend " + freq);
        c.waitTicks(5);

        System.out.println("[BedrockSatellites] " + c.node() + " duna=" + duna + " kerbin=" + kerbin + " mun list=" + munList
                + " kerbin list=" + kerbinList);
        Expect.that(!munList.isEmpty() && munList.get(0).contains("SatelliteMapper"),
                "the mapper launched on the Mun is listed on the Mun: " + munList);
        Expect.that(kerbinList.isEmpty(), "the Mun's mapper is not listed on Kerbin: " + kerbinList);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + 1)) n++;
        return n;
    }

    /** The info HUD, polled for up to five seconds until it shows anything (HBM's {@code hbm_info} query). */
    private static String info(Client c) {
        String info = "";
        for (int i = 0; i < 25; i++) {
            info = c.query("hbm_info", new JsonObject()).toString();
            if (!info.contains("\"messages\":[]")) break;
            c.waitTicks(4);
        }
        return info;
    }

    /** A multiblock placed by hand from four blocks north, clear of its footprint. */
    private static void placeMachine(Client c, String id, int x, int z) {
        Fixtures.giveOnly(c, id, 1);
        c.teleport(x + 0.5, G, z - 4.0);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
    }

    private static int[] findCore(Client c, int x, int y, int z, String cls) {
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    JsonObject be = c.blockEntity(x + dx, y + dy, z + dz);
                    if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                            && be.get("block_entity").getAsString().endsWith(cls)) {
                        return new int[]{x + dx, y + dy, z + dz};
                    }
                }
            }
        }
        return null;
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject from = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(id)) from = s;
        }
        Expect.that(from != null, "a player slot holds " + id);
        c.clickSlot(from.get("index").getAsInt());
        c.waitTicks(2);
        c.clickSlot(machineSlot);
        c.waitTicks(3);
    }
}
