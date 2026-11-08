import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NTM: Space issue 6: a held item soaking up neutrons replayed the re-equip animation at every dose. A po210be billet
 * in the hotbar irradiates the inventory; the held iron ingot's activation must grow while the hand stays raised
 * (the raise is read every tick through {@code hbm_gapsc}). A hot meteorite ingot, which cools every tick, must stay
 * raised too. Switching to another item must still lower and raise the hand.
 */
public final class GhSpace6NeutronReequip implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.resetWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        // creative: the billets would otherwise kill the player by radiation within seconds
        c.cmd("gamemode creative");
        c.cmd("clear");
        c.cmd("effect clear @s");
        c.cmd("ntmspace neutron on");
        c.cmd("item replace entity @s hotbar.0 with minecraft:iron_ingot");
        c.cmd("item replace entity @s hotbar.1 with minecraft:gold_ingot");
        c.cmd("item replace entity @s hotbar.8 with hbm:billet_po210be 1");
        c.hotbar(1);
        c.waitTicks(40);

        float before = activation(c);
        float steady = lowestRaise(c, 100);
        float after = activation(c);
        c.screenshot("01-irradiated-ingot-held");
        System.out.println("[GhSpace6] iron ingot activation " + before + " -> " + after + ", lowest raise " + steady);
        Expect.that(after > before, "the held ingot soaked up neutrons: " + before + " -> " + after);
        Expect.that(steady > 0.95F, "the hand stays raised while only the dose changes: lowest " + steady);

        c.cmd("item replace entity @s hotbar.8 with minecraft:air");
        String hot = c.node().startsWith("1.20.1") ? "hbm:ingot_meteorite{\"hbm:heat\":100}" : "hbm:ingot_meteorite[hbm:heat=100]";
        c.cmd("item replace entity @s hotbar.2 with " + hot);
        c.hotbar(3);
        c.waitTicks(30);
        float hotRaise = lowestRaise(c, 40);
        System.out.println("[GhSpace6] hot meteorite ingot, lowest raise " + hotRaise);
        Expect.that(hotRaise > 0.95F, "a cooling hot ingot stays raised: lowest " + hotRaise);

        c.hotbar(2);
        float switched = lowestRaise(c, 12);
        c.screenshot("02-after-switch");
        System.out.println("[GhSpace6] switch to the gold ingot, lowest raise " + switched);
        Expect.that(switched < 0.5F, "a real item switch still lowers the hand: lowest " + switched);

        c.cmd("ntmspace neutron off");
        c.cmd("clear");
        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(ntmspace|hbm|golem)");
        System.out.println("[GhSpace6] PASS");
    }

    /** The lowest main-hand raise seen over {@code ticks} client ticks. */
    private static float lowestRaise(Client c, int ticks) {
        float lowest = 1F;
        for (int i = 0; i < ticks; i++) {
            JsonObject q = c.query("hbm_gapsc", new JsonObject());
            lowest = Math.min(lowest, Math.min(q.get("equip").getAsFloat(), q.get("equip_prev").getAsFloat()));
            c.waitTicks(1);
        }
        return lowest;
    }

    /** The iron ingot's activation from {@code /ntmspace neutron check}. */
    private static float activation(Client c) {
        c.markLog();
        c.cmd("ntmspace neutron check");
        c.waitTicks(5);
        List<String> lines = c.logSinceMark("space_neutron_check");
        Matcher m = Pattern.compile("minecraft:iron_ingotx1=([0-9.Ee-]+)").matcher(lines.isEmpty() ? "" : lines.get(lines.size() - 1));
        return m.find() ? Float.parseFloat(m.group(1)) : -1F;
    }
}
