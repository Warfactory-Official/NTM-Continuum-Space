import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * hbm-space behaviour that runs through HBM and Golem Core hooks:
 *
 * <ul>
 *   <li>a survival player who places an assembly machine around themselves walks out of it without suffocating
 *       (hbm-space {@code BlockDummyable.internalPlayers}), and collides with it again once outside;</li>
 *   <li>drinking coffee leaves exactly one {@code ntmspace:cmug_empty};</li>
 *   <li>HBM keys renamed by Space read Space's names ({@code block.hbm.moon_turf} is "Mun Turf");</li>
 *   <li>hbm-space's config keys are in {@code config/ntmspace.cfg}.</li>
 * </ul>
 */
public final class ReviewFixes implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        if (c.screenClass() != null && c.screenClass().contains("DeathScreen")) {
            c.waitTicks(30);
            c.clickWidget("Respawn");
            c.waitTicks(40);
        }
        for (int i = 0; i < 3 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode creative");
        c.cmd("execute in minecraft:overworld run tp @s 0.5 101 0.5");
        c.waitTicks(20);
        c.markLog();

        walkOut(c);
        coffee(c);
        names(c);
        config(c);

        List<String> errors = c.logSinceMark("Exception|/ERROR\\]");
        Expect.that(errors.isEmpty(), "no exceptions or errors since the start: " + errors);
    }

    private static void walkOut(Client c) throws Exception {
        int x = 3000, y = 100, z = 0;
        Fixtures.platform(c, x, y, z, 12);
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:saturation 30 10 true");
        c.teleport(x + 0.5, y + 1, z + 0.5);
        c.look(0, 30);
        c.waitTicks(10);
        float health = c.player().get("health").getAsFloat();

        // Space places a multiblock clicked on a wall against that wall, so a 3-deep machine clicked on a wall two
        // blocks south of the player reaches back over the player.
        c.cmd("fill " + (x - 3) + " " + (y + 1) + " " + (z + 2) + " " + (x + 3) + " " + (y + 4) + " " + (z + 2) + " minecraft:stone");
        Fixtures.giveOnly(c, "hbm:machine_assembly_machine", 1);
        c.lookAt(x + 0.5, y + 1.5, z + 1.98);
        c.waitTicks(2);
        c.use();
        c.waitTicks(10);
        String feet = c.block(x, y + 1, z).toString();
        Expect.that(feet.contains("hbm:"), "the machine was placed around the player; block at feet " + feet);
        c.screenshot("00-inside-machine");

        c.look(180, 0);
        c.keyDown("key.forward");
        c.waitTicks(20);
        c.keyUp("key.forward");
        c.waitTicks(10);
        JsonObject after = c.player();
        double dz = after.get("z").getAsDouble() - (z + 0.5);
        Expect.that(dz < -2.0, "the player walked out north; z moved by " + dz);
        float healthAfter = after.get("health").getAsFloat();
        Expect.that(healthAfter >= health, "no in-wall damage while walking out: health " + health + " -> " + healthAfter);
        c.screenshot("01-walked-out");

        // Outside, the machine is solid again: walking back south stops at its edge.
        c.look(0, 0);
        c.keyDown("key.forward");
        c.waitTicks(20);
        c.keyUp("key.forward");
        c.waitTicks(10);
        double back = c.player().get("z").getAsDouble();
        Expect.that(back < z - 1.0, "the machine blocks a player who has left it; z " + back);
        c.cmd("gamemode creative");
    }

    private static void coffee(Client c) throws Exception {
        Fixtures.platform(c, 3100, 100, 0, 3);
        c.teleport(3100.5, 101, 0.5);
        c.look(0, -90);
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:hunger 3 100 true");
        c.waitTicks(70);
        c.cmd("effect clear @s");
        Fixtures.giveOnly(c, "hbm:coffee", 1);
        c.use(45);
        c.waitTicks(10);
        String inv = c.inventory().toString();
        Expect.that(count(inv, "ntmspace:cmug_empty") == 1, "drinking coffee leaves one empty mug; inventory " + inv);
        Expect.that(!inv.contains("\"hbm:coffee\""), "the coffee was drunk; inventory " + inv);
        c.cmd("gamemode creative");
        c.cmd("clear @s");
    }

    private static void names(Client c) throws Exception {
        c.markLog();
        c.cmd("tellraw @s {\"translate\":\"block.hbm.moon_turf\"}");
        c.cmd("tellraw @s {\"translate\":\"block.hbm.machine_lpw2\"}");
        c.waitTicks(10);
        List<String> chat = c.logSinceMark("\\[CHAT\\]");
        String all = String.join("\n", chat);
        Expect.that(all.contains("Mun Turf"), "moon_turf reads Space's name; chat " + all);
        Expect.that(all.contains("LPW-2 Station Thruster"), "machine_lpw2 reads Space's name; chat " + all);
        // a resource reload injects a fresh language; the redirect must survive it
        c.markLog();
        c.cmd("reload");
        c.keyDown("f3");
        c.key("t");
        c.keyUp("f3");
        c.waitTicks(200);
        c.cmd("tellraw @s {\"translate\":\"block.hbm.moon_turf\"}");
        c.waitTicks(10);
        String reloaded = String.join("\n", c.logSinceMark("\\[CHAT\\]"));
        Expect.that(reloaded.contains("Mun Turf"), "moon_turf keeps Space's name after a resource reload; chat " + reloaded);
    }

    private static void config(Client c) throws Exception {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("new-hbm-space"))) dir = dir.getParent();
        Expect.that(dir != null, "found the workspace root");
        Path found = dir.resolve("new-hbm-space/versions/" + c.node() + "/runs/harness/config/ntmspace.cfg");
        Expect.that(Files.isRegularFile(found), "ntmspace.cfg was written at " + found);
        String cfg = Files.readString(found);
        Expect.that(cfg.contains("1.90_maxProbeDistance"), "ntmspace.cfg has hbm-space's 1.90_maxProbeDistance");
        System.out.println("[review] config: " + found);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) n++;
        return n;
    }
}
