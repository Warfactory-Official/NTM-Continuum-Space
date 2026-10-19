import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * NTM: Space's orbit and orbital stations (M3), on a real client:
 *
 * <ul>
 *   <li>{@code /ntmstations launch 3 4 kerbin} marks grid square 3, 4 as a station orbiting Kerbin, and
 *       {@code /ntmstations tp 3 4} moves the player into {@code ntmspace:orbit} at the square's centre (3584, 130, 4608)
 *       and places the station core ({@code ntmspace:orbital_station}) at y 127 under it (1.7 {@code OrbitalStation.spawn});</li>
 *   <li>the orbit is void: the column beside the station is air down to y 0;</li>
 *   <li>free fall: standing still in the air for two seconds the player drifts at most a few blocks (1.7's zero-g motion,
 *       not vanilla's fall);</li>
 *   <li>a station computer placed on the port opens its screen ({@code GUIOrbitalStationComputer}) with an empty hand;
 *       typing a name renames the station ({@code /ntmstations list} logs it);</li>
 *   <li>screenshots: 00-station-core (the docking port from above), 01-computer-gui, the orbit sky 02-sky-down (the
 *       planet below, Kerbin filling the view) and 03-sky-horizons (stars, the sun and its flare).</li>
 * </ul>
 */
public final class StationOrbit implements Scenario {

    private static final int GX = 3, GZ = 4;
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
        String dim = c.status().get("dimension").getAsString();
        Expect.equal("ntmspace:orbit", dim, "dimension after /ntmstations tp");

        Expect.block(c, X, CORE_Y, Z, "ntmspace:orbital_station");
        for (int y = 0; y < 120; y += 7) Expect.block(c, X + 40, y, Z + 40, "minecraft:air");

        // free fall: 1.7's zero-g, a held position drifts only slowly
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 1000 255 true");
        c.teleport(X + 0.5, 140, Z + 0.5);
        c.waitTicks(5);
        double y0 = c.player().get("y").getAsDouble();
        c.waitTicks(40);
        double y1 = c.player().get("y").getAsDouble();
        System.out.println("[orbit] " + c.node() + " free fall over 40 ticks: " + (y0 - y1));
        Expect.that(y0 - y1 < 4.0, "in orbit a player must float (1.7 zero-g), fell " + (y0 - y1) + " in 2 s");
        c.cmd("gamemode creative");

        // look at the port
        c.teleport(X + 0.5, CORE_Y + 8, Z + 6.5);
        c.lookAt(X + 0.5, CORE_Y + 1, Z + 0.5);
        c.waitTicks(10);
        c.screenshot("00-station-core");

        // the computer, on the north rim
        int cx = X, cy = CORE_Y + 2, cz = Z - 2;
        c.cmd("setblock " + cx + " " + cy + " " + cz + " ntmspace:orbital_station_computer[facing=south]");
        c.waitTicks(5);
        Expect.block(c, cx, cy, cz, "ntmspace:orbital_station_computer");
        c.cmd("clear @s");
        c.teleport(cx + 0.5, cy, cz + 2.5);
        c.lookAt(cx + 0.5, cy + 0.5, cz + 0.5);
        c.waitTicks(5);
        c.use(1);
        JsonObject screen = c.waitScreen("GUIOrbitalStationComputer", 10_000L);
        Expect.that(screen != null, "the station computer opens its screen");
        // the station (saved data) outlives the run: clear the name an earlier run gave it before typing
        c.key("end");
        for (int i = 0; i < 40; i++) c.key("backspace");
        c.type("Harness Station");
        c.waitTicks(10);
        c.screenshot("01-computer-gui");
        c.key("escape");
        c.waitScreen("none", 5_000L);

        c.markLog();
        c.cmd("ntmstations list");
        c.waitTicks(10);
        Expect.that(!c.logSinceMark("ntmstations list: .* - Harness Station grid 3,4 orbiting kerbin").isEmpty(),
                "the station is listed under its new name: " + c.logSinceMark("ntmstations list"));

        // the sky
        c.teleport(X + 0.5, CORE_Y + 4, Z + 0.5);
        c.setOption("hide_gui", "true");
        c.look(0, 90);
        c.waitTicks(10);
        c.screenshot("02-sky-down");
        String[] names = {"south", "west", "north", "east"};
        for (int i = 0; i < 4; i++) {
            c.look(i * 90, -10);
            c.waitTicks(5);
            c.screenshot("03-sky-" + names[i]);
        }
        c.look(0, -90);
        c.waitTicks(5);
        c.screenshot("03-sky-zenith");
        c.setOption("hide_gui", "false");

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }
}
