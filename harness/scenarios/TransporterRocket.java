import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * NTM: Space's cargo transporter (hbm-space {@code BlockTransporterRocket} + {@code ItemTransporterLinker}), on a real
 * client:
 *
 * <ul>
 *   <li>two Rocket Transporter Pads placed by hand (facing east: the player looks west), 12 blocks apart; each becomes
 *       a core with a block entity;</li>
 *   <li>the pad's screen ({@code GUITransporterRocket}) opens with an empty hand and stays open; typing renames the pad
 *       (its NBT {@code name}); screenshot 00-pad-gui;</li>
 *   <li>the linker: sneak-use on both pads saves them ("Added transporter to linker" in chat), plain use on the first
 *       opens {@code GUITransporterLinker} listing the second (screenshot 01-linker-gui); clicking its row links the
 *       pads: both save {@code linkedTo} and the level by id ({@code dimension:"minecraft:overworld"});</li>
 *   <li>a shipment: 10 iron ingots in the first pad's input, both fuel tanks full; within 200 ticks the rocket flies
 *       (screenshot 02-rocket-launch while it rises) and the ingots arrive in the second pad's output slots;</li>
 *   <li>no exception or error naming ntmspace or golem in the log.</li>
 * </ul>
 */
public final class TransporterRocket implements Scenario {

    private static final int Y = 100, L = Y + 1;
    /** Click targets: the blocks the pads are placed on. */
    private static final int AX = 0, BX = 12, Z = 0;

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
        c.cmd("gamerule sendCommandFeedback false");
        Fixtures.platform(c, 6, Y, Z, 14);
        c.cmd("fill -8 " + L + " -8 20 " + (L + 8) + " 8 minecraft:air");
        c.waitTicks(5);

        int[] a = place(c, AX);
        int[] b = place(c, BX);
        System.out.println("[transporter] " + c.node() + " cores " + a[0] + "," + a[1] + "," + a[2] + " / " + b[0] + "," + b[1] + "," + b[2]);

        // the pad's screen
        c.cmd("clear @s");
        c.teleport(a[0] + 0.5, L, a[2] + 3.5);
        c.waitTicks(3);
        Fixtures.interact(c, a[0], a[1], a[2]);
        c.waitScreen("GUITransporterRocket", 5_000L);
        Expect.screenStaysOpen(c, "GUITransporterRocket", 5);
        JsonObject field = editBox(c);
        c.click(field.get("center_x").getAsDouble(), field.get("center_y").getAsDouble());
        c.waitTicks(2);
        // the click puts the cursor where it lands; clear the whole name (key(k, n) holds one press n ticks, it
        // does not repeat, so press each key on its own)
        c.key("end");
        for (int i = 0; i < 24; i++) c.key("backspace");
        c.type("Depot");
        c.waitTicks(5);
        c.screenshot("00-pad-gui");
        c.key("escape");
        c.waitScreen("none", 5_000L);
        Expect.that(nbt(c, a).contains("name:\"Depot\""), "the pad was renamed from its screen: " + nbt(c, a));

        // the linker: save both, then link from the first
        Fixtures.giveOnly(c, "ntmspace:transporter_linker", 1);
        c.hotbar(1);
        c.markLog();
        for (int[] pad : new int[][] {a, b}) {
            c.teleport(pad[0] + 0.5, L, pad[2] + 3.5);
            c.lookAt(pad[0] + 0.5, pad[1] + 0.5, pad[2] + 0.5);
            c.waitTicks(3);
            c.keyDown("key.sneak");
            c.waitTicks(2);
            c.use(2);
            c.waitTicks(3);
            c.keyUp("key.sneak");
            c.waitTicks(3);
        }
        Expect.that(c.logSinceMark("Added transporter to linker").size() >= 2, "both pads saved on the linker");

        c.teleport(a[0] + 0.5, L, a[2] + 3.5);
        c.lookAt(a[0] + 0.5, a[1] + 0.5, a[2] + 0.5);
        c.waitTicks(10); // the server sends the linker's list every second tick
        c.use(2);
        JsonObject linker = c.waitScreen("GUITransporterLinker", 5_000L);
        Expect.that(linker != null, "the linker opens its screen on a pad");
        // the linker is no container screen: its 218 x 201 panel is centred in the GUI-scaled window (status gui_*)
        JsonObject s = c.status();
        int width = s.get("gui_width").getAsInt(), height = s.get("gui_height").getAsInt();
        int left = (width - 218) / 2, top = (height - 201) / 2;
        c.mouseMove(left + 19, top + 78);
        c.waitTicks(3);
        c.screenshot("01-linker-gui");
        c.click(left + 19, top + 78);                // the first row's link button
        c.waitScreen("none", 5_000L);
        c.waitTicks(10);
        String na = nbt(c, a), nb = nbt(c, b);
        Expect.that(na.contains("linkedTo") && nb.contains("linkedTo"), "both pads are linked: " + na + " | " + nb);
        Expect.that(na.contains("dimension:\"minecraft:overworld\""), "the link names the level by id: " + na);

        // a shipment
        c.cmd("item replace block " + a[0] + " " + a[1] + " " + a[2] + " container.0 with minecraft:iron_ingot 10");
        c.cmd("data merge block " + a[0] + " " + a[1] + " " + a[2] + " {t8:64000,t9:64000}");
        c.teleport(a[0] + 6.5, L + 2, a[2] + 10.5);
        c.lookAt(a[0] + 0.5, a[1] + 6, a[2] + 0.5);
        boolean arrived = false;
        boolean shot = false;
        for (int i = 0; i < 40 && !arrived; i++) {
            c.waitTicks(5);
            if (!shot && !nbt(c, a).contains("rocket:1b")) {
                c.waitTicks(15);
                c.screenshot("02-rocket-launch");
                shot = true;
            }
            arrived = nbt(c, b).contains("minecraft:iron_ingot");
        }
        Expect.that(arrived, "the ingots reached the second pad: " + nbt(c, b));

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem|transporter)");
    }

    /** Places a pad by hand on block (x, Y, Z), the player looking west; returns its core (the block entity). */
    private static int[] place(Client c, int x) {
        Fixtures.placeOnTop(c, "ntmspace:transporter_rocket", x, Y, Z);
        c.waitTicks(5);
        // facing east (look west): the core is one block west of the clicked-on cell (offset 1)
        int[][] candidates = {{x - 1, L, Z}, {x, L, Z}, {x + 1, L, Z}};
        for (int[] core : candidates) {
            JsonObject be = c.blockEntity(core[0], core[1], core[2]);
            if (be != null && be.has("nbt") && !be.get("nbt").isJsonNull() && be.get("nbt").getAsString().contains("t9")) return core;
        }
        throw new IllegalStateException("no transporter core near " + x + "," + L + "," + Z + ": " + c.block(x, L, Z));
    }

    private static String nbt(Client c, int[] pos) {
        JsonObject be = c.blockEntity(pos[0], pos[1], pos[2]);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }
}
