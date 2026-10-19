import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * NTM: Space's orbital station launcher (hbm-space {@code TileEntityOrbitalStationLauncher}) on a real client, in orbit:
 *
 * <ul>
 *   <li>a station is launched over Kerbin ({@code /ntmstations launch 5 7 kerbin}) and the player moved to it;</li>
 *   <li>{@code ntmspace:orbital_station_launcher} is placed by hand on a temporary platform beside the station, which is
 *       then removed so the launcher hangs in the void like a station port;</li>
 *   <li>R1's parts (rp_c_20, rp_f_20_12, rp_l_20, mp_thruster_20_kerosene) go into slots 3..6: the launcher's tanks become
 *       kerosene and peroxide (block-entity NBT {@code t0_type}/{@code t1_type}) and the rocket hangs under the port
 *       (00-launcher-rocket);</li>
 *   <li>the screen opens with an empty hand ({@code GUIOrbitalStationLauncher}); a processed Mun drive
 *       ({@code /ntmrocket drive mun 0 0}) goes into the drive slot, the tanks are filled with {@code data merge}
 *       (01-launcher-gui: the rocket preview, the issue list, full green tank lines); COMMIT TO LAUNCH (41, 12) spawns one
 *       rideable rocket and the screen shows "Ready for launch!" (02-ready), the arms fold in (03-docked).</li>
 * </ul>
 */
public final class StationLauncher implements Scenario {

    private static final int GX = 5, GZ = 7;
    private static final int X = GX * 1024 + 512, Z = GZ * 1024 + 512, CORE_Y = 127;

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

        c.cmd("ntmstations launch " + GX + " " + GZ + " kerbin");
        c.cmd("ntmstations tp " + GX + " " + GZ);
        c.waitTicks(60);
        Expect.equal("ntmspace:orbit", c.status().get("dimension").getAsString(), "dimension after /ntmstations tp");

        // the launcher, placed by hand beside the station
        int px = X + 16, py = CORE_Y, pz = Z;
        Fixtures.platform(c, px, py, pz, 6);
        Fixtures.placeOnTop(c, "ntmspace:orbital_station_launcher", px, py, pz);
        int[] core = core(c, px, py + 1, pz);
        Expect.block(c, core[0], core[1], core[2], "ntmspace:orbital_station_launcher");
        c.cmd("fill " + (px - 6) + " " + py + " " + (pz - 6) + " " + (px + 6) + " " + py + " " + (pz + 6) + " minecraft:air");
        c.waitTicks(2);

        String at = core[0] + " " + core[1] + " " + core[2];
        c.cmd("item replace block " + at + " container.3 with ntmspace:rp_c_20");
        c.cmd("item replace block " + at + " container.4 with ntmspace:rp_f_20_12");
        c.cmd("item replace block " + at + " container.5 with ntmspace:rp_l_20");
        c.cmd("item replace block " + at + " container.6 with hbm:mp_thruster_20_kerosene");
        c.waitTicks(10);
        String nbt = nbt(c, core);
        Expect.that(nbt.contains("hbm:kerosene") && nbt.contains("hbm:peroxide"), "R1 gives kerosene and peroxide tanks; nbt " + nbt);

        c.teleport(core[0] + 14.5, core[1] - 8, core[2] + 0.5);
        c.lookAt(core[0] + 0.5, core[1] - 8, core[2] + 0.5);
        c.waitTicks(20);
        c.screenshot("00-launcher-rocket");

        // the screen
        c.cmd("clear @s");
        c.cmd("ntmrocket drive mun 0 0");
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 6");
        c.teleport(core[0] + 0.5, core[1] + 2, core[2] + 1.5);
        c.waitTicks(4);
        Fixtures.interact(c, core[0], core[1] + 1, core[2]); // the inner top of the port
        JsonObject screen = c.waitScreen("GUIOrbitalStationLauncher", 10_000L);
        Expect.that(screen != null, "the launcher opens its screen");
        Expect.screenStaysOpen(c, "GUIOrbitalStationLauncher", 5);

        int hotbar = 29 + 27; // 29 launcher slots, 27 inventory slots before the hotbar
        int driveSlot = -1;
        for (int i = 0; i < 9; i++) {
            JsonObject s = c.slot(hotbar + i);
            if (s == null || s.get("empty").getAsBoolean()) continue;
            if (s.get("id").getAsString().equals("ntmspace:hard_drive_full_mun")) driveSlot = hotbar + i;
        }
        Expect.that(driveSlot >= 0, "the Mun drive is in the hotbar");
        c.slotClick(driveSlot, 0, "PICKUP");
        c.waitTicks(2);
        c.slotClick(0, 0, "PICKUP");
        c.waitTicks(5);
        Expect.slotHolds(c, 0, "ntmspace:hard_drive_full_mun", 1);

        c.cmd("data merge block " + at + " {t0:64000,t1:64000}");
        c.waitTicks(20);
        c.screenshot("01-launcher-gui");

        screen = c.screen();
        int left = screen.get("left").getAsInt();
        int top = screen.get("top").getAsInt();
        c.click(left + 41 + 9, top + 12 + 8); // COMMIT TO LAUNCH
        c.waitTicks(20);

        JsonObject params = new JsonObject();
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
        c.screenshot("02-ready");
        c.key("escape");
        c.waitScreen("none", 5_000L);

        c.teleport(core[0] + 10.5, core[1] + 4, core[2] + 0.5);
        c.lookAt(core[0] + 0.5, core[1] - 4, core[2] + 0.5);
        c.waitTicks(40);
        c.screenshot("03-docked");

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    /** The core of the launcher placed from the east onto (x, ?, z): two blocks west, or east. */
    private static int[] core(Client c, int x, int y, int z) {
        int[] west = {x - 2, y, z};
        JsonObject be = c.blockEntity(west[0], west[1], west[2]);
        if (be.has("nbt") && !be.get("nbt").isJsonNull()) return west;
        System.out.println("[launcher] " + c.node() + " no core at " + west[0] + "," + y + "," + z + "; trying east");
        return new int[] {x + 2, y, z};
    }

    private static String nbt(Client c, int[] pos) {
        JsonObject be = c.blockEntity(pos[0], pos[1], pos[2]);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }
}
