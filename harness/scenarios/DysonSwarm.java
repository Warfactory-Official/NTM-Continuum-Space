import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * NTM: Space's Dyson swarm on a real client (Kerbin at noon, then the Mun):
 * <ul>
 *   <li>the spinlauncher, kept charged and stocked by {@code /ntmdyson charge} every tick as a network would, spins up
 *       for 132 ticks and adds four members to its swarm ({@code dyson_swarm id=4243 members=4}); 50-launcher-spinning
 *       mid spin-up, 51-launcher-launch as the payload flies;</li>
 *   <li>three receivers at noon, each beaming into one converter's intake 11 blocks behind it - two on swarm 4242
 *       (4,096 members, 2 consumers): the power converter holds power, the heat converter heat; one on swarm 4244 (512):
 *       the terraformer converts (oxygen); 52-receiver-he (the beam and the converter's coils), 53-receiver-overlay (look
 *       overlay: ID, swarm, consumers, power), 54-receiver-tu, 55-anatmogenesis-overlay;</li>
 *   <li>the swarm in the sky (30,000 members): 56-sky-noon-zenith (the sun with the swarm's black specks across it),
 *       57-sky-sunset-west; on the Mun (no air, black sky) 58-mun-sky-zenith and the four horizons 59-mun-sky-*.</li>
 * </ul>
 * All machines are assembled by {@code /ntmdyson place}; the scenario restores the swarms to 0 at the end.
 */
public final class DysonSwarm implements Scenario {

    private static final int Y = 100, L = 101, Z = 300;

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
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        c.cmd("weather clear");
        c.cmd("gamerule doDaylightCycle false");

        // ---------------------------------------------------------------- spinlauncher (offset 0, facing north)
        int lx = -80;
        Fixtures.platform(c, lx, Y, Z, 14);
        c.cmd("fill " + (lx - 12) + " " + L + " " + (Z - 14) + " " + (lx + 12) + " " + (L + 18) + " " + (Z + 14) + " minecraft:air");
        c.cmd("ntmdyson place launcher " + lx + " " + L + " " + Z + " north");
        // hbm-space CBT_Dyson.attenuate: every swarm loses a member with chance (all members of all swarms) / 102,400 a
        // tick, so the 4,608 members a previous run left in swarms 4242/4244 would take one of the four new members
        // within the window about every other launch; empty them so the launch is counted exactly
        c.cmd("ntmdyson swarm 4242 0");
        c.cmd("ntmdyson swarm 4244 0");
        c.cmd("ntmdyson swarm 4243 0");
        c.cmd("ntmdyson chip " + lx + " " + L + " " + Z + " 4243");
        c.teleport(lx + 22.5, L + 10, Z + 14.5);
        c.lookAt(lx + 0.5, L + 8, Z + 0.5);
        for (int i = 0; i < 100; i++) {
            c.cmd("ntmdyson charge " + lx + " " + L + " " + Z);
            c.waitTicks(1);
        }
        c.screenshot("50-launcher-spinning");
        boolean launched = false;
        int previous = 0; // swarm 4243 was set to 0 above
        // up to two full launch cycles (133 spin-up + 69 spin-down ticks each); a launch is a step of +4 between two
        // consecutive counts (a slow client may see two launches, +8, in one step). The step, not the absolute count, is
        // judged: CBT_Dyson.attenuate may take a member at any tick, faithfully to hbm-space.
        for (int i = 0; i < 200 && !launched; i++) {
            c.cmd("ntmdyson charge " + lx + " " + L + " " + Z);
            c.waitTicks(1);
            c.markLog();
            c.cmd("ntmdyson count 4243");
            c.waitTicks(1);
            java.util.List<String> counts = c.logSinceMark("dyson_swarm id=4243 members=");
            if (!counts.isEmpty()) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("members=(\\d+)").matcher(counts.get(counts.size() - 1));
                if (m.find()) {
                    int members = Integer.parseInt(m.group(1));
                    int step = members - previous;
                    if (step > 0) System.out.println(c.node() + " swarm 4243: " + previous + " -> " + members);
                    launched = step >= 4 && step % 4 == 0;
                    previous = members;
                }
            }
        }
        c.waitTicks(2);
        c.screenshot("51-launcher-launch");
        Expect.that(launched, "the spinlauncher adds four members to swarm 4243 after its 132-tick spin-up");

        // ---------------------------------------------------------------- receivers and converters (beam to the south)
        // the terraformer gets its own small swarm (512 members: 88 mB of oxygen a tick), so the harness world's air
        // is not terraformed by the scenario
        c.cmd("ntmdyson swarm 4242 4096");
        c.cmd("ntmdyson swarm 4244 512");
        int[] xs = {0, 40, 80};
        int[] swarms = {4242, 4242, 4244};
        String[] converters = {"he", "tu", "anatmogenesis"};
        int[] offsets = {15, 17, 16};
        for (int i = 0; i < 3; i++) {
            int x = xs[i];
            Fixtures.platform(c, x, Y, Z + 8, 14);
            c.cmd("fill " + (x - 8) + " " + L + " " + (Z - 8) + " " + (x + 8) + " " + (L + 20) + " " + (Z + 24) + " minecraft:air");
            c.cmd("ntmdyson place receiver " + x + " " + L + " " + Z + " north");
            c.cmd("ntmdyson place " + converters[i] + " " + x + " " + L + " " + (Z + offsets[i]) + " north");
            c.cmd("ntmdyson chip " + x + " " + L + " " + Z + " " + swarms[i]);
        }
        c.waitTicks(40);

        // one receiver or converter at a time, each read right after its own command. Members decay from the first
        // tick on (CBT_Dyson attenuate: 4,096 -> ~4,092 and 512 -> ~509 in these 40 ticks), so the counts are ranges.
        String[] receiverPattern = {
                "dyson_state receiver id=4242 swarm=(40\\d\\d|39\\d\\d) consumers=2 receiving=true beam=11",
                "dyson_state receiver id=4242 swarm=(40\\d\\d|39\\d\\d) consumers=2 receiving=true beam=11",
                "dyson_state receiver id=4244 swarm=(51[0-2]|50\\d|49\\d) consumers=1 receiving=true beam=11"};
        String[] converterPattern = {"dyson_state he power=[1-9]", "dyson_state tu heat=[1-9]",
                "dyson_state anatmogenesis fluid=OXYGEN emitting=true converting=true"};
        String[] converterName = {"the power converter holds power", "the heat converter holds heat",
                "the terraformer converts the beam into oxygen"};
        for (int i = 0; i < 3; i++) {
            c.markLog();
            c.cmd("ntmdyson state " + xs[i] + " " + L + " " + Z);
            java.util.List<String> state = c.logSinceMark("CHAT\\] dyson_state receiver");
            java.util.regex.Pattern wanted = java.util.regex.Pattern.compile(receiverPattern[i]);
            // a busy client logs the answer some ticks later; poll for the answer itself
            for (int w = 0; w < 20 && (state.isEmpty() || !wanted.matcher(state.get(state.size() - 1)).find()); w++) {
                c.waitTicks(3);
                state = c.logSinceMark("CHAT\\] dyson_state receiver");
            }
            Expect.that(!state.isEmpty() && java.util.regex.Pattern.compile(receiverPattern[i]).matcher(state.get(state.size() - 1)).find(),
                    "receiver " + i + " at noon (swarm 4242: 2 consumers, 4244: 1) with an 11-block beam: " + state);
            c.markLog();
            c.cmd("ntmdyson state " + xs[i] + " " + L + " " + (Z + offsets[i]));
            for (int w = 0; w < 20 && c.logSinceMark(converterPattern[i]).isEmpty(); w++) c.waitTicks(3);
            Expect.that(!c.logSinceMark(converterPattern[i]).isEmpty(), converterName[i] + ": " + c.logSinceMark("dyson_state"));
        }

        shoot(c, 12.5, L + 4, Z + 7.5, 0.5, L + 1.5, Z + 12.5, "52-receiver-he");
        shoot(c, 4.5, L + 3, Z - 6.5, 0.5, L + 2, Z + 0.5, "53-receiver-overlay");
        shoot(c, 52.5, L + 4, Z + 7.5, 40.5, L + 1.5, Z + 13.5, "54-receiver-tu");
        shoot(c, 84.5, L + 2, Z + 22.5, 80.5, L + 1, Z + 16.5, "55-anatmogenesis-overlay");

        // ---------------------------------------------------------------- the swarm in the sky
        c.cmd("ntmdyson swarm 4242 30000");
        c.waitTicks(10);
        c.teleport(0.5, L + 30, Z - 30.5);
        c.setOption("hide_gui", "true");
        c.look(0, -90);
        c.waitTicks(10);
        c.screenshot("56-sky-noon-zenith");
        c.cmd("time set 12600");
        c.look(90, -5);
        c.waitTicks(10);
        c.screenshot("57-sky-sunset-west");

        c.cmd("ntmspace tp mun");
        c.waitTicks(60);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp mun");
        c.look(0, -90);
        c.waitTicks(10);
        c.screenshot("58-mun-sky-zenith");
        String[] names = {"south", "west", "north", "east"};
        for (int i = 0; i < 4; i++) {
            c.look(i * 90, -25);
            c.waitTicks(5);
            c.screenshot("59-mun-sky-" + names[i]);
        }
        c.setOption("hide_gui", "false");

        c.cmd("ntmdyson swarm 4242 0");
        c.cmd("ntmdyson swarm 4243 0");
        c.cmd("ntmdyson swarm 4244 0");
        c.cmd("gamerule doDaylightCycle true");
        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem|dyson)");
    }

    private static void shoot(Client c, double x, double y, double z, double tx, double ty, double tz, String name) {
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(10);
        c.screenshot(name);
    }
}
