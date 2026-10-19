import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * NTM: Space's StarDar ({@code machine_stardar}) and its remote access console ({@code dish_control}):
 * <ul>
 *   <li>the StarDar is placed by hand from the east (offset 2, height 3: the core is the dish platform three blocks up,
 *       two blocks west of the clicked block, which becomes a leg); its GUI opens from that leg and shows the
 *       solar-system map (00-stardar-map);</li>
 *   <li>the planet layers on the map (hbm-space {@code GUIMachineStardar.drawBodyOverlays}, drawn on a Golem
 *       {@code GuiCanvas}; query {@code ntmspace_stardar}: each layer's GUI draw counter must grow while it shows), with
 *       the map focused on Kerbin ("focus current body", button at 129, 123): Kerbin's atmosphere ({@code sky_atmosphere}),
 *       the crescent ({@code stardar_crescent}) and the glow band ({@code glow}) at 05-stardar-kerbin; city lights and
 *       their glow ({@code /ntmspace skytrait lights kerbin 20000}: {@code stardar_nightlights},
 *       {@code stardar_atmosphere_emissive}) at 06-stardar-kerbin-lights; lightning while Kerbin thunders
 *       ({@code sky_lightning}) at 07-stardar-kerbin-lightning; a nuclear blast's flash and ring ({@code sky_nuke}) at
 *       08-stardar-kerbin-nuke; an impact on the Mun (its lava fading in, {@code lava}) at 09-stardar-mun-impact. The
 *       traits and the weather are cleared again;</li>
 *   <li>an empty hard drive is put in the drive slot through real slot clicks, and "Program current body into drive"
 *       (button at 149, 143) turns it into a processed Kerbin drive whose approximate destination is the StarDar's
 *       position (1.7 {@code processDrive}); the GUI switches to the landing view and the height map arrives
 *       (01-stardar-landing);</li>
 *   <li>a click on a flat spot of the platform in the landing view moves the drive's landing coordinates to 1.7's
 *       {@code ax + px - 128}, {@code az + pz - 128} (02-stardar-landing-marker before the click);</li>
 *   <li>a reactor sensor sneak-used on a StarDar leg and then on the console links the console; using the console then
 *       opens the StarDar's GUI from there (03-console-gui), and the dish turns (04-stardar-dish).</li>
 * </ul>
 */
public final class Stardar implements Scenario {

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

        // away from the origin, where other scenarios leave machines and platforms up to y 200: the landing view needs
        // the flat platform around the StarDar
        int x = 1000, z = 1000;
        Fixtures.platform(c, x, Y, z, 14);
        c.cmd("gamerule sendCommandFeedback false");
        Fixtures.placeOnTop(c, "ntmspace:machine_stardar", x, Y, z);
        int[] core = core(c, x, L + 3, z, 2);
        Expect.block(c, core[0], core[1], core[2], "ntmspace:machine_stardar");
        Expect.block(c, x, L, z, "ntmspace:machine_stardar"); // the clicked block is a leg

        // ---------------------------------------------------------------- GUI: the system map
        c.cmd("clear");
        c.cmd("give @s ntmspace:hard_drive 1");
        c.waitTicks(2);
        open(c, x, L, z);
        c.waitTicks(10);
        c.screenshot("00-stardar-map");

        // ---------------------------------------------------------------- the planet layers (before a drive is in the slot)
        planetLayers(c, x, L, z);

        // ---------------------------------------------------------------- program the current body
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        int driveSlot = findPlayerSlot(c, "ntmspace:hard_drive");
        Expect.that(driveSlot >= 0, "the hard drive is in the player inventory on the StarDar screen");
        c.clickSlot(driveSlot);
        c.waitTicks(2);
        c.clickSlot(0);
        c.waitTicks(4);
        Expect.that(itemId(c.slot(0)).equals("ntmspace:hard_drive"), "the hard drive went into the drive slot: " + c.slot(0));

        c.click(left + 149 + 9, top + 143 + 9);
        c.waitTicks(10);
        Expect.that(itemId(c.slot(0)).equals("ntmspace:hard_drive_full_kerbin"), "the drive is now a Kerbin drive: " + c.slot(0));
        String nbt = nbt(c, core);
        Expect.that(has(nbt, "processed", "1b") && has(nbt, "ax", core[0]) && has(nbt, "az", core[2]),
                "processed with the StarDar's position as approximate destination (1.7 processDrive); nbt " + nbt);

        // the landing view: wait for the 16 x 16 chunk height map
        for (int i = 0; i < 90 && !heightmapShown(c); i++) c.waitTicks(10);
        Expect.that(heightmapShown(c), "the StarDar logged its finished height map");
        c.waitTicks(10);
        c.screenshot("01-stardar-landing");

        // ---------------------------------------------------------------- pick a landing spot on the platform
        // map pixel of block (x + 6, z + 6): the map starts at chunk (ax >> 4) - 8; view pixel = left + 9 - 256 + 2 * hx at rest
        int hx = (x + 6) - (((core[0] >> 4) - 8) * 16), hz = (z + 6) - (((core[2] >> 4) - 8) * 16);
        double mx = left + 9 - 256 + 2 * hx + 0.5, my = top + 9 - 256 + 2 * hz + 0.5;
        Expect.that(mx > left + 9 && mx < left + 167 && my > top + 9 && my < top + 117, "the spot is inside the map: " + mx + ", " + my);
        c.mouseMove(mx, my);
        c.waitTicks(4);
        c.screenshot("02-stardar-landing-marker");
        c.click(mx, my);
        c.waitTicks(10);
        nbt = nbt(c, core);
        // 1.7 quirk, kept: relative to ax - 128, not to the map's corner
        int ex = core[0] + hx - 128, ez = core[2] + hz - 128;
        Expect.that(has(nbt, "x", ex) && has(nbt, "z", ez),
                "landing coordinates " + ex + ", " + ez + " (1.7 updateDriveCoords); nbt " + nbt);
        close(c);

        // ---------------------------------------------------------------- remote console and the reactor sensor
        int cx = x + 10;
        Fixtures.placeOnTop(c, "ntmspace:dish_control", cx, Y, z);
        Expect.block(c, cx, L, z, "ntmspace:dish_control");

        Fixtures.giveOnly(c, "hbm:reactor_sensor", 1);
        c.teleport(x + 2.5, L, z + 0.5);
        c.waitTicks(2);
        sneakUse(c, x, L, z);
        c.teleport(cx + 2.5, L, z + 0.5);
        c.waitTicks(2);
        sneakUse(c, cx, L, z);
        String console = nbt(c, new int[] {cx, L, z});
        Expect.that(console.contains("isLinked:1b") && console.contains("linkPosX:" + core[0]) && console.contains("linkPosY:" + core[1]),
                "the console is linked to the StarDar core; nbt " + console);

        Fixtures.interact(c, cx, L, z);
        c.waitScreen("GUIMachineStardar", 5_000L);
        Expect.screenStaysOpen(c, "GUIMachineStardar", 10);
        c.screenshot("03-console-gui");
        close(c);

        c.teleport(x + 10.5, L + 4, z + 10.5);
        c.lookAt(core[0] + 0.5, core[1] + 4, core[2] + 0.5);
        c.waitTicks(60);
        c.screenshot("04-stardar-dish");

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem|stardar)");
    }

    // ------------------------------------------------------------------------------------------------ planet layers

    /** The StarDar's GUI draws of {@code key} so far: a shader id ({@code ntmspace:...}) or {@code glow} / {@code lava}. */
    private static long draws(Client c, String key) throws Exception {
        JsonObject q = c.query("ntmspace_stardar", new JsonObject());
        if (!key.contains(":")) return q.get(key).getAsLong();
        com.google.gson.JsonElement e = q.getAsJsonObject("shaders").get(key);
        return e == null ? 0 : e.getAsLong();
    }

    /** {@code key} is drawn at least once within 10 ticks (every frame while it shows). */
    private static void expectDrawn(Client c, String key, String why) throws Exception {
        long before = draws(c, key);
        c.waitTicks(10);
        long drawn = draws(c, key) - before;
        Expect.that(drawn > 0, why + ": " + key + " drew " + drawn + " times in 10 ticks");
    }

    /** {@code key} is not drawn within 10 ticks. */
    private static void expectNotDrawn(Client c, String key, String why) throws Exception {
        long before = draws(c, key);
        c.waitTicks(10);
        long drawn = draws(c, key) - before;
        Expect.that(drawn == 0, why + ": " + key + " drew " + drawn + " times in 10 ticks");
    }

    /** Opens the StarDar from its leg and focuses the map on the current body (Kerbin) with the 129, 123 button. */
    private static void openFocused(Client c, int x, int y, int z) throws Exception {
        if (c.screenClass() == null || !c.screenClass().contains("GUIMachineStardar")) open(c, x, y, z);
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.click(left + 129 + 9, top + 123 + 9);
        c.mouseMove(left + 60, top + 200); // off the map: no hovered body, no drag
        c.waitTicks(30); // the 420 ms focus animation
        long canvases = c.query("ntmspace_stardar", new JsonObject()).get("canvases").getAsLong();
        c.waitTicks(5);
        Expect.that(c.query("ntmspace_stardar", new JsonObject()).get("canvases").getAsLong() > canvases,
                "the StarDar map is drawn through Golem's GUI canvas");
    }

    private static void skytrait(Client c, String args, String logged) throws Exception {
        c.markLog();
        c.cmd("ntmspace skytrait " + args);
        c.waitTicks(10);
        Expect.that(!c.logSinceMark(logged).isEmpty(), "/ntmspace skytrait " + args + " logged " + logged);
    }

    private static void planetLayers(Client c, int x, int y, int z) throws Exception {
        try {
            openFocused(c, x, y, z);
            // Kerbin: 1 atm of earth air, clouds (atmosphere.frag style 1); the crescent and the glow band on every planet
            expectDrawn(c, "ntmspace:sky_atmosphere", "Kerbin's clouds on the StarDar map");
            expectDrawn(c, "ntmspace:stardar_crescent", "the crescent shadow on the StarDar map");
            expectDrawn(c, "glow", "the atmosphere glow band around Kerbin");
            expectNotDrawn(c, "ntmspace:stardar_nightlights", "no city lights before any are set");
            c.screenshot("05-stardar-kerbin");
            close(c);

            skytrait(c, "lights kerbin 20000", "space_skytrait body=kerbin trait=lights");
            openFocused(c, x, y, z);
            expectDrawn(c, "ntmspace:stardar_nightlights", "Kerbin's city lights on the StarDar map");
            expectDrawn(c, "ntmspace:stardar_atmosphere_emissive", "the glow of Kerbin's city lights in its clouds");
            c.screenshot("06-stardar-kerbin-lights");
            close(c);

            // lightning in Kerbin's clouds while Kerbin thunders (the thunder strength ramps up over some seconds)
            c.cmd("execute in minecraft:overworld run ntmspace weather thunder 600");
            openFocused(c, x, y, z);
            for (int i = 0; i < 30; i++) {
                long before = draws(c, "ntmspace:sky_lightning");
                c.waitTicks(10);
                if (draws(c, "ntmspace:sky_lightning") > before) break;
            }
            expectDrawn(c, "ntmspace:sky_lightning", "lightning in Kerbin's thunderclouds on the StarDar map");
            c.screenshot("07-stardar-kerbin-lightning");
            close(c);
            c.cmd("execute in minecraft:overworld run ntmspace weather clear 600");

            // a nuclear blast on Kerbin: its flash and ring for 220 ticks
            skytrait(c, "nuke kerbin 140", "space_skytrait body=kerbin trait=nuke");
            openFocused(c, x, y, z);
            expectDrawn(c, "ntmspace:sky_nuke", "the nuke flash and ring on Kerbin on the StarDar map");
            c.screenshot("08-stardar-kerbin-nuke");
            close(c);

            // an impact on the Mun (seen in Kerbin's focus): the lava fades in over 10 ticks
            skytrait(c, "impact mun", "space_skytrait body=mun trait=impact");
            openFocused(c, x, y, z);
            expectDrawn(c, "lava", "the Mun's impact lava on the StarDar map");
            c.screenshot("09-stardar-mun-impact");
            close(c);
        } finally {
            close(c);
            c.cmd("ntmspace skytrait clear kerbin lights");
            c.cmd("ntmspace skytrait clear mun impact");
            c.cmd("execute in minecraft:overworld run ntmspace weather clear 600");
        }
        open(c, x, y, z);
        c.waitTicks(10);
    }

    private static void sneakUse(Client c, int x, int y, int z) {
        c.hotbar(1);
        c.lookAt(x, y, z);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(3);
    }

    /** The server logs once per finished scan: "StarDar height map ready: 256 chunks of ... after N ticks". */
    private static boolean heightmapShown(Client c) {
        return !c.logSinceMark("StarDar height map ready").isEmpty();
    }

    /** {@code key:value} as a whole SNBT field ({@code x:20} must not match {@code ax:20}). */
    private static boolean has(String nbt, String key, Object value) {
        return java.util.regex.Pattern.compile("(^|[{,])" + key + ":" + java.util.regex.Pattern.quote(String.valueOf(value)) + "([,}]|$)")
                .matcher(nbt).find();
    }

    private static int findPlayerSlot(Client c, String id) {
        JsonObject screen = c.screen();
        for (var e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && itemId(s).equals(id)) return s.get("index").getAsInt();
        }
        return -1;
    }

    private static String itemId(JsonObject slot) {
        if (slot == null || !slot.has("id") || slot.get("id").isJsonNull()) return "";
        return slot.get("id").getAsString();
    }

    /** The core of a machine placed from the east onto {@code (x, ?, z)}: {@code offset} blocks west, else east. */
    private static int[] core(Client c, int x, int y, int z, int offset) {
        int[] west = {x - offset, y, z};
        JsonObject be = c.blockEntity(west[0], west[1], west[2]);
        if (be.has("nbt") && !be.get("nbt").isJsonNull()) return west;
        System.out.println("[stardar] " + c.node() + " no core at " + west[0] + "," + y + "," + z + "; trying east");
        return new int[] {x + offset, y, z};
    }

    private static String nbt(Client c, int[] pos) {
        JsonObject be = c.blockEntity(pos[0], pos[1], pos[2]);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static void open(Client c, int x, int y, int z) {
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 6");
        c.teleport(x + 2.5, y, z + 0.5);
        c.waitTicks(4);
        Fixtures.interact(c, x, y, z);
        c.waitScreen("GUIMachineStardar", 5_000L);
        Expect.screenStaysOpen(c, "GUIMachineStardar", 5);
    }

    private static void close(Client c) {
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
    }
}
