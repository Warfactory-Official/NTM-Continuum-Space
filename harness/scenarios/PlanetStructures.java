import dev.golem.harness.api.*;

/**
 * hbm-space's planet structures (M3 structures family), on a real client. For each body: a teleport into terrain of
 * the body that no earlier run generated (a time-based offset), then {@code /ntmspace structure goto <spawn>} finds the nearest region whose 1.7 pick is that structure (the same
 * {@code BodyStructures.pick} the structure set generates with; it logs {@code structure_find <spawn> <x> <z>}) and
 * flies there; once the chunks exist, {@code /ntmspace structure here} must log the structure's pieces in that chunk
 * ({@code structure_here ... <spawn>/<piece>}), a positive signal that the world generator built it where 1.7 would.
 *
 * <p>Screenshots (spectator, GUI hidden, looking down at the start chunk): 00-mun-mun_base, 01-minmus-minmus_base,
 * 02-duna-duna_comms, 03-ike-ike_artifact, 04-ike-ancient_tomb, 05-dres-dres_rbmk, 06-moho-moho_base,
 * 07-laythe-laythe_nuke_sub, 08-laythe-laythe_vertibird, 09-thatmo-meteor_dungeon, 10-mun-spaceship. What to look at:
 * the Mun/Minmus bases' domes, panels and tanks on the regolith (Minmus's concrete sand-coloured), Duna's comms dish,
 * Ike's pillar with a divine shard on its pedestal, the concrete pyramid, Dres's tiled RBMK station, Moho's base sunk
 * into the crust with its snorkel, the sunk submarine under Laythe's sea, a vertibird, the meteor dungeon's spike.
 */
public final class PlanetStructures implements Scenario {

    private static final String[][] CASES = {
            {"mun", "mun_base"},
            {"minmus", "minmus_base"},
            {"duna", "duna_comms"},
            {"ike", "ike_artifact"},
            {"ike", "ancient_tomb"},
            {"dres", "dres_rbmk"},
            {"moho", "moho_base"},
            {"laythe", "laythe_nuke_sub"},
            {"laythe", "laythe_vertibird"},
            {"thatmo", "meteor_dungeon"},
            {"mun", "spaceship"},
    };

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
        c.cmd("gamemode spectator");
        StringBuilder summary = new StringBuilder();

        for (int i = 0; i < CASES.length; i++) {
            String body = CASES[i][0];
            String spawn = CASES[i][1];
            // search from terrain no earlier run generated: a chunk generated before (by an earlier scenario, or an
            // older build) keeps the structure starts it was made with, so the 1.7 pick near the origin may hold none
            long fresh = 8000 + (System.currentTimeMillis() / 1000 % 50_000) * 64 + i * 4096L;
            c.cmd("execute in ntmspace:" + body + " run tp @s " + fresh + ".5 200 " + fresh + ".5");
            c.waitTicks(40);
            Expect.equal("ntmspace:" + body, c.status().get("dimension").getAsString(), "dimension after the teleport to " + body);

            c.markLog();
            c.cmd("ntmspace structure goto " + spawn);
            // the search walks regions ring by ring and asks each one's biome: seconds on the layered bodies (Dres)
            java.util.List<String> found = c.logSinceMark("structure_find " + spawn + " ");
            for (int wait = 0; wait < 60 && found.isEmpty(); wait++) {
                c.waitTicks(10);
                found = c.logSinceMark("structure_find " + spawn + " ");
            }
            Expect.that(!found.isEmpty() && c.logSinceMark("structure_find " + spawn + " NONE").isEmpty(),
                    body + ": no " + spawn + " found by 1.7's placement: " + found);
            // let the chunks around the start generate (structures are placed at their FEATURES step)
            c.waitTicks(160);

            c.markLog();
            c.cmd("ntmspace structure here");
            c.waitTicks(20);
            String piece = spawn.equals("spaceship") || spawn.equals("ancient_tomb") ? spawn : spawn + "/";
            java.util.List<String> here = c.logSinceMark("structure_here .*" + java.util.regex.Pattern.quote(piece));
            summary.append(body).append(':').append(spawn).append('=').append(!here.isEmpty()).append(' ');

            c.setOption("hide_gui", "true");
            c.look(0, 70);
            c.waitTicks(10);
            c.screenshot(String.format("%02d-%s-%s", i, body, spawn));
            c.setOption("hide_gui", "false");
            Expect.that(!here.isEmpty(), body + ": the world has no " + spawn + " where 1.7 put it: " + c.logSinceMark("structure_here"));
        }

        c.cmd("gamemode creative");
        System.out.println("[structures] " + c.node() + " " + summary);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem|NBTStructure|structures)");
    }
}
