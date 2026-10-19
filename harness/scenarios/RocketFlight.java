import dev.golem.harness.api.*;

/**
 * NTM: Space's rockets (M3, rocket family), on a real client, Kerbin to the Mun:
 *
 * <ul>
 *   <li>a VAB ({@code /ntmrocket place vab}, test-only) takes a landing capsule, a 12 m fuselage, landing legs and a size
 *       20 kerosene thruster through its screen; the rocket is valid, and CONSTRUCT puts an {@code ntmspace:rocket_custom}
 *       in the result slot (screenshot 01-vab-gui, 02-vab-built after construction is taken back apart and rebuilt);</li>
 *   <li>a rocket launch pad ({@code /ntmrocket place pad}) takes the rocket and a processed Mun drive
 *       ({@code /ntmrocket drive mun 0 0}); {@code /ntmrocket fuel} fills its kerosene/peroxide tanks and power; the
 *       screen's lamp shows it can launch (03-pad-gui), COMMIT TO LAUNCH spawns the rocket;</li>
 *   <li>the player boards it (use), holds jump: the server logs {@code Rocket <id> -> LAUNCHING}, then TRANSFER in the
 *       orbit level, then LANDING and LANDED on the Mun (04-launch, 05-transfer, 06-mun-landed third person,
 *       07-mun-landed first person; 04b-launch-rider-tilted: the 12-block camera and the rider turned with the pitch);</li>
 *   <li>no exception mentioning ntmspace or golem in the log.</li>
 * </ul>
 * Kerbin -> Mun for this rocket needs 26 600 mB of its 64 000 (hbm-space's formula, GameTest rocket_fuel_matches_17).
 */
public final class RocketFlight implements Scenario {

    private static final int Y = 100;
    private static final int VAB_X = 0, VAB_Y = Y + 3, VAB_Z = 12;
    private static final int PAD_X = 0, PAD_Y = Y + 1, PAD_Z = -14;

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
        c.cmd("execute in minecraft:overworld run tp @s 0 " + (Y + 1) + " 0");
        c.waitTicks(20);
        Fixtures.platform(c, 0, Y, 0, 26);
        c.cmd("kill @e[type=ntmspace:entity_rideable_rocket]");
        c.cmd("effect give @s minecraft:resistance 1000 255 true");

        // ---- the VAB
        c.cmd("ntmrocket place vab " + VAB_X + " " + VAB_Y + " " + VAB_Z);
        c.waitTicks(5);
        Expect.block(c, VAB_X, VAB_Y, VAB_Z, "ntmspace:machine_rocket_assembly");

        c.cmd("clear");
        c.cmd("give @s ntmspace:rp_c_20 1");
        c.cmd("give @s ntmspace:rp_f_20_12 1");
        c.cmd("give @s ntmspace:rp_l_20 1");
        c.cmd("give @s hbm:mp_thruster_20_kerosene 1");
        c.waitTicks(5);

        // stand on the deck and open the VAB
        c.teleport(VAB_X + 2.5, VAB_Y + 1, VAB_Z + 3.5);
        c.waitTicks(5);
        Fixtures.interact(c, VAB_X + 2, VAB_Y, VAB_Z + 1);
        c.waitScreen("RocketAssembly", 5000);
        Expect.screenStaysOpen(c, "RocketAssembly", 10);

        // hotbar 1..4 are menu slots 54..57 (27 machine slots, 27 inventory slots before the hotbar)
        int hotbar = 27 + 27;
        int[] targets = {0, 1, 2, 3};
        for (int i = 0; i < targets.length; i++) {
            c.slotClick(hotbar + i, 0, "PICKUP");
            c.waitTicks(2);
            c.slotClick(targets[i], 0, "PICKUP");
            c.waitTicks(2);
        }
        c.waitTicks(20);
        Expect.slotHolds(c, 0, "ntmspace:rp_c_20", 1);
        Expect.slotHolds(c, 1, "ntmspace:rp_f_20_12", 1);
        Expect.slotHolds(c, 3, "hbm:mp_thruster_20_kerosene", 1);
        c.screenshot("01-vab-gui");

        com.google.gson.JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt();
        int top = screen.get("top").getAsInt();
        c.click(left + 41 + 9, top + 62 + 9); // CONSTRUCT
        c.waitTicks(10);
        Expect.slotHolds(c, 16, "ntmspace:rocket_custom", 1);
        Expect.slotEmpty(c, 0);

        // take it apart and build it again: the VAB shows the rocket standing in it while the parts are in
        c.click(left + 41 + 9, top + 62 + 9); // DECONSTRUCT
        c.waitTicks(10);
        Expect.slotHolds(c, 0, "ntmspace:rp_c_20", 1);
        c.key("escape");
        c.waitTicks(10);
        c.teleport(VAB_X + 12.5, VAB_Y + 1, VAB_Z + 0.5);
        c.lookAt(VAB_X + 0.5, VAB_Y + 8, VAB_Z + 0.5);
        c.waitTicks(10);
        c.screenshot("02-vab-built");
        c.teleport(VAB_X + 2.5, VAB_Y + 1, VAB_Z + 3.5);
        c.waitTicks(5);
        Fixtures.interact(c, VAB_X + 2, VAB_Y, VAB_Z + 1);
        c.waitScreen("RocketAssembly", 5000);
        screen = c.screen();
        left = screen.get("left").getAsInt();
        top = screen.get("top").getAsInt();
        c.click(left + 41 + 9, top + 62 + 9); // CONSTRUCT
        c.waitTicks(10);
        Expect.slotHolds(c, 16, "ntmspace:rocket_custom", 1);
        c.slotClick(16, 0, "QUICK_MOVE");
        c.waitTicks(5);
        c.key("escape");
        c.waitTicks(10);
        Expect.inventoryHolds(c, "ntmspace:rocket_custom", 1);

        // ---- the pad
        c.cmd("ntmrocket place pad " + PAD_X + " " + PAD_Y + " " + PAD_Z);
        c.cmd("ntmrocket drive mun 0 0");
        c.waitTicks(5);
        Expect.block(c, PAD_X, PAD_Y, PAD_Z, "ntmspace:launch_pad_rocket");
        c.teleport(PAD_X + 2.5, PAD_Y + 3, PAD_Z + 3.5);
        c.waitTicks(5);
        Fixtures.interact(c, PAD_X + 2, PAD_Y + 2, PAD_Z + 2);
        c.waitScreen("LaunchPadRocket", 5000);
        Expect.screenStaysOpen(c, "LaunchPadRocket", 10);

        int padHotbar = 5 + 27; // 5 pad slots, 27 inventory slots before the hotbar
        int rocketSlot = -1, driveSlot = -1;
        for (int i = 0; i < 9; i++) {
            com.google.gson.JsonObject s = c.slot(padHotbar + i);
            if (s == null || s.get("empty").getAsBoolean()) continue;
            String id = s.get("id").getAsString();
            if (id.equals("ntmspace:rocket_custom")) rocketSlot = padHotbar + i;
            if (id.equals("ntmspace:hard_drive_full_mun")) driveSlot = padHotbar + i;
        }
        Expect.that(rocketSlot >= 0 && driveSlot >= 0, "the rocket and the Mun drive are in the hotbar");
        c.slotClick(rocketSlot, 0, "PICKUP");
        c.waitTicks(2);
        c.slotClick(0, 0, "PICKUP");
        c.waitTicks(2);
        c.slotClick(driveSlot, 0, "PICKUP");
        c.waitTicks(2);
        c.slotClick(1, 0, "PICKUP");
        c.waitTicks(10);
        c.cmd("ntmrocket fuel " + PAD_X + " " + PAD_Y + " " + PAD_Z);
        c.waitTicks(20);
        c.screenshot("03-pad-gui");

        screen = c.screen();
        left = screen.get("left").getAsInt();
        top = screen.get("top").getAsInt();
        c.markLog();
        c.click(left + 56 + 9, top + 20 + 8); // COMMIT TO LAUNCH
        c.waitTicks(10);
        c.key("escape");
        c.waitTicks(10);

        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("radius", 30);
        params.addProperty("type", "ntmspace:entity_rideable_rocket");
        params.addProperty("server", true);
        com.google.gson.JsonArray found = c.query("entities", params).getAsJsonArray("entities");
        // the type filter matches by prefix, so it also lists the rocket's cap hitbox (hbm-space's
        // EntityRideableRocketDummy, spawned by the rocket itself): one of each is the 1.7 result
        int rocketCount = 0, dummyCount = 0;
        if (found != null) {
            for (com.google.gson.JsonElement e : found) {
                String type = e.getAsJsonObject().get("type").getAsString();
                if (type.equals("ntmspace:entity_rideable_rocket")) rocketCount++;
                else if (type.equals("ntmspace:entity_rideable_rocket_dummy")) dummyCount++;
            }
        }
        Expect.that(rocketCount == 1 && dummyCount <= 1, "COMMIT TO LAUNCH spawns one rocket (and at most its one cap hitbox): " + found);

        // ---- board and launch
        c.teleport(PAD_X + 2.5, PAD_Y + 3, PAD_Z + 0.5);
        c.waitTicks(5);
        c.hotbar(9);
        c.lookAt(PAD_X + 0.5, PAD_Y + 4.5, PAD_Z + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(10);
        c.keyDown("key.jump");
        c.waitTicks(10);
        c.keyUp("key.jump");
        Expect.that(!c.logSinceMark("Rocket .* -> LAUNCHING").isEmpty(), "jump launches the boarded rocket: "
                + c.logSinceMark("Rocket"));
        c.key("key.togglePerspective");
        c.waitTicks(60);
        c.screenshot("04-launch");
        // hbm-space's rocket view (Golem CameraHooks.DISTANCE, PlayerRenderHooks.POSE; query from FacadeClient): third
        // person 12 blocks out, the rider turned with the rocket's pitch once it tips over in its launch
        com.google.gson.JsonObject view = c.query("ntmspace_view", new com.google.gson.JsonObject());
        Expect.that(view.get("riding_rocket").getAsBoolean() && view.get("camera_distance").getAsDouble() > 10,
                "the launching rocket's third-person camera stands about 12 blocks off: " + view);
        float pitch = 0;
        for (int i = 0; i < 12 && pitch < 5 && c.logSinceMark("Rocket .* -> TRANSFER").isEmpty(); i++) {
            c.waitTicks(5);
            pitch = c.query("ntmspace_view", new com.google.gson.JsonObject()).get("rocket_pitch").getAsFloat();
        }
        c.screenshot("04b-launch-rider-tilted");
        Expect.that(pitch > 0, "the rocket pitches over during its launch (the rider is drawn turned with it): " + pitch);

        boolean transfer = false, landed = false;
        for (int i = 0; i < 240 && !landed; i++) {
            c.waitTicks(20);
            if (!transfer && !c.logSinceMark("Rocket .* -> TRANSFER").isEmpty()) {
                transfer = true;
                c.waitTicks(40);
                c.screenshot("05-transfer");
            }
            landed = !c.logSinceMark("Rocket .* -> LANDED in .*ntmspace:mun").isEmpty();
        }
        Expect.that(transfer, "the rocket went into transfer: " + c.logSinceMark("Rocket"));
        Expect.that(landed, "the rocket landed on the Mun: " + c.logSinceMark("Rocket"));
        c.waitTicks(40);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "the pilot is on the Mun");
        c.screenshot("06-mun-landed");
        c.key("key.togglePerspective");
        c.key("key.togglePerspective");
        c.waitTicks(10);
        c.screenshot("07-mun-landed-first-person");

        System.out.println("[rocket] " + c.node() + " " + String.join(" | ", c.logSinceMark("Rocket .* -> ")));
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }
}
