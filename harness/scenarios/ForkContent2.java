import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * hbm-space's non-space fork content, part 2, on a real client (what a human would look at):
 *
 * <ul>
 *   <li>{@code ntmspace:nuke_antimatter}: the bomb's GUI ({@code BlombSchematic}, 00-amat-gui); loaded with two lead
 *       particles, two hafnium ingots and an antimatter pellet the near-black core sphere bobs above it with three dark
 *       blue beams (01-amat-loaded), and the GUI shows the red countdown (02-amat-gui-loaded). It is NOT detonated
 *       (a 150-block crater).</li>
 *   <li>a custom nuke with one antimatter particle (strength 5, hbm-space's {@code particle_amat} entry) fired by
 *       redstone: an antimatter balefire of 5 and the rainbow antimatter cloud (Torex type 2), 03/04-amat-cloud; positive
 *       signal: an {@code hbm:entity_balefire} and an {@code hbm:nuke_torex} appear.</li>
 *   <li>the four new bobbleheads (Gwen, Juicy_Lad, JamesH_2, MrKimkimora's own model) placed from their items
 *       (05-bobbleheads) and Gwen's commemorative panel (06-bobble-gui).</li>
 *   <li>the "Civilian Airliner" designator: "Rerouted Civilian Traffic!" in the log/chat and a 737 crossing
 *       (07-airliner); a B2X summoned by command with its two cyan exhaust plumes (08-b2x).</li>
 *   <li>the flashbang overlay (09-flashbang) and the nitan health-bar overlay (10-nitan-bar) via {@code /effect}.</li>
 * </ul>
 * The wrecked Leviathan's {@code chungus_destroyed} model is in {@code ForkContent}'s 02-chungus-damaged.
 */
public final class ForkContent2 implements Scenario {

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
        c.cmd("effect clear @s");
        c.cmd("time set day");
        c.cmd("gamerule doDaylightCycle false");

        JsonObject pos = c.player();
        int x = pos.get("block_x").getAsInt() + 8, y = pos.get("block_y").getAsInt() + 24, z = pos.get("block_z").getAsInt();
        Fixtures.platform(c, x, y, z, 12);

        // ---------------------------------------------------------------- antimatter bomb
        JsonObject placed = Fixtures.placeOnTop(c, "ntmspace:nuke_antimatter", x, y, z);
        System.out.println("[fork2] bomb placed: " + placed);
        Expect.block(c, x, y + 1, z, "ntmspace:nuke_antimatter");
        Fixtures.interact(c, x, y + 1, z);
        c.waitScreen("GUINukeAntimatter", 5_000L);
        c.waitTicks(5);
        c.screenshot("00-amat-gui");
        c.key("escape");
        c.waitScreen("none", 5_000L);
        String at = x + " " + (y + 1) + " " + z;
        c.cmd("item replace block " + at + " container.0 with hbm:particle_lead");
        c.cmd("item replace block " + at + " container.1 with hbm:particle_lead");
        c.cmd("item replace block " + at + " container.2 with hbm:pellet_antimatter");
        c.cmd("item replace block " + at + " container.3 with ntmspace:ingot_hafnium");
        c.cmd("item replace block " + at + " container.4 with ntmspace:ingot_hafnium");
        c.waitTicks(10);
        c.teleport(x + 4.5, y + 3, z + 0.5);
        c.lookAt(x + 0.5, y + 2.5, z + 0.5);
        c.waitTicks(20);
        c.screenshot("01-amat-loaded");
        Fixtures.interact(c, x, y + 1, z);
        c.waitScreen("GUINukeAntimatter", 5_000L);
        c.waitTicks(5);
        c.screenshot("02-amat-gui-loaded");
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.cmd("setblock " + at + " air");

        // ---------------------------------------------------------------- custom nuke, antimatter stage
        int nx = x, ny = y, nz = z + 30;
        Fixtures.platform(c, nx, ny, nz, 8);
        Fixtures.setBlock(c, nx, ny + 1, nz, "hbm:nuke_custom");
        c.cmd("item replace block " + nx + " " + (ny + 1) + " " + nz + " container.0 with hbm:particle_amat");
        c.waitTicks(5);
        Fixtures.setBlock(c, nx + 40, ny + 11, nz, "minecraft:glass"); // a stand for the camera, or it falls away
        c.teleport(nx + 40.5, ny + 12, nz + 0.5);
        c.lookAt(nx + 0.5, ny + 6, nz + 0.5);
        c.waitTicks(10);
        Fixtures.setBlock(c, nx + 1, ny + 1, nz, "minecraft:redstone_block");
        c.waitTicks(30);
        c.screenshot("03-amat-cloud");
        // HBM's cloud is hbm:entity_effect_torex; read it on the server (the client list holds what it tracks)
        JsonObject cloudQuery = new JsonObject();
        cloudQuery.addProperty("radius", 64);
        cloudQuery.addProperty("type", "torex");
        cloudQuery.addProperty("server", true);
        String ents = c.query("entities", cloudQuery).getAsJsonArray("entities").toString();
        Expect.that(ents.contains("hbm:entity_effect_torex"), "the antimatter stage spawns a cloud; entities " + ents);
        c.waitTicks(80);
        c.screenshot("04-amat-cloud");

        // ---------------------------------------------------------------- bobbleheads
        int bx = x - 6, by = y, bz = z - 6;
        String[] bobbles = {"ntmspace:bobblehead_gwen", "ntmspace:bobblehead_juice", "ntmspace:bobblehead_jamesh_2",
                "ntmspace:bobblehead_mrkimkimora"};
        for (int i = 0; i < bobbles.length; i++) {
            Fixtures.placeOnTop(c, bobbles[i], bx + i * 2, by, bz);
            Expect.block(c, bx + i * 2, by + 1, bz, "ntmspace:bobblehead");
        }
        c.cmd("clear @s");
        c.teleport(bx + 3.5, by + 2, bz + 3.5);
        c.lookAt(bx + 3.5, by + 1.2, bz + 0.5);
        c.waitTicks(20);
        c.screenshot("05-bobbleheads");
        Fixtures.interact(c, bx, by + 1, bz);
        c.waitScreen("GUIScreenBobble", 5_000L);
        c.waitTicks(5);
        c.screenshot("06-bobble-gui");
        c.key("escape");
        c.waitScreen("none", 5_000L);

        // ---------------------------------------------------------------- bombers
        Fixtures.giveOnly(c, "ntmspace:bomb_caller_airliner", 1);
        c.teleport(x + 0.5, y + 2, z + 0.5);
        c.lookAt(x + 0.5, y + 1, z + 4.5);
        c.waitTicks(2);
        c.markLog();
        c.use(2);
        c.waitTicks(40);
        c.look(0, -40);
        c.waitTicks(20);
        c.screenshot("07-airliner");
        String planes = c.entities(250).toString();
        Expect.that(planes.contains("entity_bomber"), "the designator called a bomber; entities " + planes);
        c.cmd("summon hbm:entity_bomber " + (x + 0.5) + " " + (y + 18) + " " + (z + 12.5)
                + " {payload:\"ntmspace:carpet_jet\",skin:\"ntmspace:b2x\",bombStart:100000,bombStop:100001}");
        c.teleport(x + 0.5, y + 14, z - 6.5);
        c.lookAt(x + 0.5, y + 18, z + 12.5);
        c.waitTicks(10);
        c.screenshot("08-b2x");
        c.cmd("kill @e[type=hbm:entity_bomber]");

        // ---------------------------------------------------------------- effect overlays
        c.cmd("gamemode survival");
        c.cmd("effect give @s ntmspace:flashbang 10 0");
        c.waitTicks(10);
        c.screenshot("09-flashbang");
        c.cmd("effect clear @s");
        c.cmd("effect give @s ntmspace:nitan 10 0");
        c.waitTicks(10);
        c.screenshot("10-nitan-bar");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");

        java.util.List<String> errors = c.logSinceMark("Exception|ERROR");
        System.out.println("[fork2] log lines matching Exception/ERROR since the bombers: " + errors.size());
    }
}
