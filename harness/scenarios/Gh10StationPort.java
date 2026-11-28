import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NTM: Space issue 10: on 1.20.1 Forge the orbital station port's glass input was the empty {@code c:glass_blocks/colorless}
 * tag ("0x air"). Picks {@code ass.orbitalstationport} in an assembly machine by clicks, loads the inputs in 1.7's slot
 * order (vanilla glass in the third) and waits for the port.
 */
public final class Gh10StationPort implements Scenario {

    private static final int Y = 100, L = 101, X = 0, Z = 0;
    private static final String SCREEN = "GUIMachineAssemblyMachine", RECIPE = "ass.orbitalstationport";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode creative");
        c.cmd("execute in minecraft:overworld run tp @s 0.5 101 0.5");
        c.waitTicks(20);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("fill -8 " + L + " -8 8 " + (L + 5) + " 8 minecraft:air");
        c.cmd("gamerule sendCommandFeedback false");

        Fixtures.giveOnly(c, "hbm:machine_assembly_machine", 1);
        standAt(c, X + 0.5, L, Z + 3.5);
        c.lookAt(X + 0.5, Y + 0.98, Z + 1.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
        Expect.block(c, X, L, Z, "hbm:machine_assembly_machine");

        selectRecipe(c, "station port");
        c.cmd("data merge block " + X + " " + L + " " + Z + " {power:100000L}");
        put(c, 4, "hbm:plate_welded_titanium", 6);
        put(c, 5, "hbm:motor", 4);
        put(c, 6, "minecraft:glass", 8);
        put(c, 7, "hbm:ingot_polymer", 8);
        put(c, 8, "hbm:circuit_basic", 4);
        put(c, 9, "ntmspace:plate_stainless", 4);
        open(c);
        c.waitTicks(20);
        c.screenshot("01-station-port-running");
        int made = 0;
        for (int t = 0; t <= 600 && made < 1; t += 10) {
            made = count(nbt(c), "ntmspace:orbital_station_port");
            if (made < 1) c.waitTicks(10);
        }
        c.screenshot("02-station-port-done");
        close(c);
        Expect.that(made >= 1, "the assembly machine made an orbital station port from vanilla glass; nbt " + nbt(c));
        Expect.that(count(nbt(c), "minecraft:glass") == 0, "the eight glass were consumed; nbt " + nbt(c));

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|couldn't parse|failed to load).*(recipe|assembly|ntmspace|golem)");
        System.out.println("[Gh10] PASS on " + c.node());
    }

    private static void selectRecipe(Client c, String term) {
        open(c);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        c.click(left + 16, top + 134);
        c.waitScreen("RecipeSelector", 5_000L);
        String want = "recipe0:\"" + RECIPE + "\"";
        for (int i = 0; i < 8 && !nbt(c).contains(want); i++) {
            if (c.screenClass() == null) open(c);
            if (!c.screenClass().contains("RecipeSelector")) {
                c.click(left + 16, top + 134);
                c.waitScreen("RecipeSelector", 5_000L);
            }
            JsonObject box = editBox(c);
            int gl = box.get("x").getAsInt() - 28, gt = box.get("y").getAsInt() - 111;
            c.click(box.get("center_x").getAsDouble(), box.get("center_y").getAsDouble());
            c.waitTicks(2);
            c.type(term);
            c.waitTicks(3);
            if (i == 0) c.screenshot("00-selector-search");
            c.click(gl + 16 + 18 * i, gt + 26);
            c.waitTicks(2);
            c.click(gl + 160, gt + 98);
            c.waitTicks(4);
        }
        if (c.screenClass() != null) close(c);
        Expect.that(nbt(c).contains(want), RECIPE + " picked in the selector; " + nbt(c));
    }

    private static void put(Client c, int slot, String item, int n) {
        c.cmd("item replace block " + X + " " + L + " " + Z + " container." + slot + " with " + item + " " + n);
        c.waitTicks(1);
    }

    private static String nbt(Client c) {
        JsonObject be = c.blockEntity(X, L, Z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    /** Items of exactly {@code id} in an SNBT inventory (26.1 {@code count:3}, 1.20.1 {@code Count:3b}). */
    private static int count(String nbt, String id) {
        int total = 0;
        Matcher m = Pattern.compile("\\{[^{}]*id:\"" + Pattern.quote(id) + "\"[^{}]*}").matcher(nbt);
        while (m.find()) {
            Matcher n = Pattern.compile("(?i)count:([0-9]+)").matcher(m.group());
            total += n.find() ? Integer.parseInt(n.group(1)) : 1;
        }
        return total;
    }

    private static void standAt(Client c, double x, double y, double z) {
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 4.5");
        c.teleport(x, y, z);
        c.waitTicks(4);
    }

    private static void open(Client c) {
        c.hotbar(9);
        standAt(c, X + 0.5, L + 1, Z + 3.0);
        Fixtures.interact(c, X, L + 1, Z + 1);
        c.waitScreen(SCREEN, 5_000L);
        Expect.screenStaysOpen(c, SCREEN, 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }
}
