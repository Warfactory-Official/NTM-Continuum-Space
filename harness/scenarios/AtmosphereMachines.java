import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.List;

/**
 * NTM: Space's atmosphere machines on Duna (air: 0.1 atm Duna air). None of them has a GUI in hbm-space; what a player
 * looks at is each machine's look overlay, so the screenshots aim at the machine with the crosshair.
 *
 * <ul>
 *   <li>atmospheric compressor ({@code machine_atmo_vent}): powered, it collects Duna air (its tank becomes
 *       {@code ntmspace:dunaair} and fills 200 mB a tick); 40-compressor;</li>
 *   <li>atmospheric liquefaction tower ({@code machine_atmo_tower}): 50,000 mB Earth air and 30,000 HE become 35,000 mB
 *       and 300 mB nitrogen (hbm-space: 5,000 mB -> 100 mB per 10,000 HE); 41-tower;</li>
 *   <li>atmospheric emitter ({@code machine_atmo_emitter}): 1,000 mB xenon and 10,000 HE vent into the air: the tank is
 *       empty and {@code /ntmspace atmosphere} now lists xenon; 42-emitter (the plume);</li>
 *   <li>CO2 scrubber ({@code air_scrubber}): placed and powered; 43-scrubber;</li>
 *   <li>atmosphere editor: on, 10^11 mB (0.1 atm) of Earth air every 5 ticks; after 100 ticks the readout shows Earth
 *       air above 1 atm and the sky has changed (44-sky-before, 45-sky-after, 46-editor); then capture mode takes it all
 *       back and the readout no longer lists Earth air (47-sky-restored). The xenon is captured back the same way.</li>
 * </ul>
 */
public final class AtmosphereMachines implements Scenario {

    private static final int Y = 120, L = 121;

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
        c.cmd("gamemode creative");
        c.cmd("ntmspace tp duna");
        c.waitTicks(40);
        Expect.equal("ntmspace:duna", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp duna");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");
        int z = 300;
        c.teleport(8.5, L + 2, z + 8.5);
        c.waitTicks(20);
        c.markLog();
        String start = atmosphere(c);
        Expect.that(start.contains("ntmspace:dunaair=") && !start.contains("ntmspace:earthair"),
                "Duna's air at the start: Duna air only; " + start);

        // ---------------------------------------------------------------- compressor (4 high, offset 0)
        int x = 0;
        Fixtures.platform(c, x, Y, z, 6);
        Fixtures.placeOnTop(c, "ntmspace:machine_atmo_vent", x, Y, z);
        int[] vent = {x, L, z};
        Expect.block(c, vent[0], vent[1], vent[2], "ntmspace:machine_atmo_vent");
        c.cmd("data merge block " + vent[0] + " " + vent[1] + " " + vent[2] + " {power:1000000L}");
        c.waitTicks(10);
        String nbt = nbt(c, vent);
        Expect.that(nbt.contains("water_type:\"ntmspace:dunaair\"") && !nbt.contains("water:0,"),
                "compressor collects Duna air (200 mB a tick); nbt " + nbt);
        shoot(c, vent, 4, 2, "40-compressor");

        // ---------------------------------------------------------------- tower (7 high)
        x = 20;
        Fixtures.platform(c, x, Y, z, 6);
        Fixtures.placeOnTop(c, "ntmspace:machine_atmo_tower", x, Y, z);
        int[] tower = {x, L, z};
        Expect.block(c, tower[0], tower[1], tower[2], "ntmspace:machine_atmo_tower");
        c.cmd("data merge block " + tower[0] + " " + tower[1] + " " + tower[2] + " {water:50000,power:30000L}");
        c.waitTicks(10);
        nbt = nbt(c, tower);
        Expect.that(nbt.contains("water:35000") && nbt.contains("heavyWater:300") && nbt.contains("power:0L"),
                "tower: 50,000 mB Earth air + 30,000 HE -> 35,000 mB + 300 mB nitrogen; nbt " + nbt);
        shoot(c, tower, 6, 4, "41-tower");

        // ---------------------------------------------------------------- emitter (3 high)
        x = 40;
        Fixtures.platform(c, x, Y, z, 6);
        Fixtures.placeOnTop(c, "ntmspace:machine_atmo_emitter", x, Y, z);
        int[] emitter = {x, L, z};
        Expect.block(c, emitter[0], emitter[1], emitter[2], "ntmspace:machine_atmo_emitter");
        c.cmd("data merge block " + emitter[0] + " " + emitter[1] + " " + emitter[2] + " {t:1000,t_type:\"hbm:xenon\",power:10000L}");
        c.teleport(emitter[0] + 6.5, L + 3, emitter[2] + 6.5);
        c.lookAt(emitter[0] + 0.5, L + 2.5, emitter[2] + 0.5);
        c.waitTicks(3);
        c.screenshot("42-emitter"); // running: fan and plume
        c.waitTicks(30);
        nbt = nbt(c, emitter);
        Expect.that(nbt.contains("t:0,") || nbt.contains("t:0}"), "emitter vented its xenon; nbt " + nbt);
        String afterEmitter = atmosphere(c);
        Expect.that(afterEmitter.contains("hbm:xenon="), "the emitter's xenon is in Duna's air; " + afterEmitter);

        // ---------------------------------------------------------------- CO2 scrubber (2 high)
        x = 60;
        Fixtures.platform(c, x, Y, z, 4);
        Fixtures.placeOnTop(c, "ntmspace:air_scrubber", x, Y, z);
        int[] scrubber = {x, L, z};
        Expect.block(c, scrubber[0], scrubber[1], scrubber[2], "ntmspace:air_scrubber");
        c.cmd("data merge block " + scrubber[0] + " " + scrubber[1] + " " + scrubber[2] + " {power:10000L}");
        c.waitTicks(10);
        shoot(c, scrubber, 3, 1, "43-scrubber");

        // ---------------------------------------------------------------- atmosphere editor: terraform and restore
        x = 80;
        Fixtures.platform(c, x, Y, z, 4);
        c.cmd("setblock " + x + " " + L + " " + z + " ntmspace:atmosphere_editor");
        c.teleport(x + 3.5, L + 1, z + 3.5);
        c.lookAt(x + 0.5, L + 30.0, z - 40.5); // the sky
        c.waitTicks(10);
        c.screenshot("44-sky-before");
        c.cmd("data merge block " + x + " " + L + " " + z + " {isOn:1b,throughput:11,fluid:\"ntmspace:earthair\",emit:1b}");
        c.waitTicks(100);
        c.cmd("data merge block " + x + " " + L + " " + z + " {isOn:0b}");
        String terraformed = atmosphere(c);
        Expect.that(pressureOf(terraformed, "ntmspace:earthair") > 1.0,
                "the editor put more than 1 atm of Earth air into Duna's air in 100 ticks; " + terraformed);
        c.waitTicks(40); // the sky follows the synced traits
        c.screenshot("45-sky-after");
        c.lookAt(x + 0.5, L + 0.5, z + 0.5);
        c.waitTicks(5);
        c.screenshot("46-editor");

        // restore: capture the Earth air and the xenon again (1.7 consumeGas drops a gas at 0)
        c.cmd("data merge block " + x + " " + L + " " + z + " {isOn:1b,emit:0b}");
        c.waitTicks(140);
        c.cmd("data merge block " + x + " " + L + " " + z + " {fluid:\"hbm:xenon\",throughput:6}");
        c.waitTicks(10);
        c.cmd("data merge block " + x + " " + L + " " + z + " {isOn:0b}");
        String restored = atmosphere(c);
        Expect.that(!restored.contains("ntmspace:earthair") && !restored.contains("hbm:xenon") && restored.contains("ntmspace:dunaair="),
                "capture mode gave Duna its air back; " + restored);
        c.lookAt(x + 0.5, L + 30.0, z - 40.5);
        c.waitTicks(40);
        c.screenshot("47-sky-restored");

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem|atmo|scrubber|emitter)");
    }

    /** {@code /ntmspace atmosphere}: the {@code atmosphere_check} log line for the air at the player's eyes. */
    private static String atmosphere(Client c) {
        c.markLog();
        c.cmd("ntmspace atmosphere");
        c.waitTicks(5);
        List<String> lines = c.logSinceMark("atmosphere_check");
        Expect.that(!lines.isEmpty(), "no atmosphere_check line");
        String line = lines.get(lines.size() - 1);
        System.out.println("[atmosphereMachines] " + c.node() + " " + line);
        return line;
    }

    private static double pressureOf(String line, String fluid) {
        int at = line.indexOf(fluid + "=");
        if (at < 0) return 0;
        int from = at + fluid.length() + 1;
        int to = from;
        while (to < line.length() && ",]".indexOf(line.charAt(to)) < 0) to++;
        return Double.parseDouble(line.substring(from, to));
    }

    private static void shoot(Client c, int[] at, double dist, double up, String name) {
        c.teleport(at[0] + dist + 0.5, at[1] + up, at[2] + dist + 0.5);
        c.lookAt(at[0] + 0.5, at[1] + 0.5, at[2] + 0.5);
        c.waitTicks(10);
        c.screenshot(name);
    }

    private static String nbt(Client c, int[] pos) {
        JsonObject be = c.blockEntity(pos[0], pos[1], pos[2]);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }
}
