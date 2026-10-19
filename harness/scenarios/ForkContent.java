import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * hbm-space's non-space fork content on a real client (package {@code com.ntmspace.fork}):
 *
 * <ul>
 *   <li>the NITAN flask is drunk by holding use: the player gets Ambrosia ({@code effect.ntmspace.nitan}) and
 *       Resistance; the inventory screen shows the effect icons (00-nitan-inventory: the 1.7 potion-sheet bottle icon);</li>
 *   <li>{@code ntmspace:det_salt} (cobalt top) and {@code ntmspace:salted_fallout} (HBM's ash layer) stand on a platform
 *       next to HBM's {@code det_nuke} (01-det-salt);</li>
 *   <li>a Leviathan turbine placed by hand is wrecked with {@code data merge ... {damaged:1b}} (what an NBT structure
 *       does); looking at it shows hbm-space's repair overlay ("Repair with:", "Tool: Blowtorch") (02-chungus-damaged);</li>
 *   <li>a derrick-less check of the per-body oil ore: {@code ntmspace:ore_oil[body=duna]} is placed for the screenshot
 *       (03-duna-oil), the drilling itself is covered by the GameTest {@code fork_fluid_deposits}.</li>
 * </ul>
 */
public final class ForkContent implements Scenario {

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
        // away from the origin, where other scenarios leave taint, fallout and machines (a survival player there was
        // "consumed by taint" while drinking)
        Fixtures.platform(c, 2000, 100, 0, 4);
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");

        // the NITAN count is saved on the player and survives death: the third flask kills (1.7), so earlier runs'
        // flasks are cleared first the 1.7 way, with ipecac (food: the player is made hungry so it can eat)
        c.cmd("effect give @s minecraft:hunger 3 100 true");
        c.waitTicks(70);
        c.cmd("effect clear @s");
        Fixtures.giveOnly(c, "hbm:med_ipecac", 1);
        c.use(45);
        c.waitTicks(5);
        c.cmd("effect clear @s");

        // NITAN flask: 32 ticks of drinking
        Fixtures.giveOnly(c, "ntmspace:flask_infusion_nitan", 1);
        c.use(45);
        c.waitTicks(5);
        String effects = c.player().get("effects").toString();
        Expect.that(effects.contains("nitan"), "drinking the NITAN flask gives Ambrosia; effects " + effects);
        Expect.that(effects.toLowerCase().contains("resistance"), "drinking the NITAN flask gives Resistance; effects " + effects);
        c.key("e");
        c.waitScreen("InventoryScreen", 5_000L);
        c.waitTicks(5);
        c.screenshot("00-nitan-inventory");
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");

        // det_salt and salted fallout
        JsonObject pos = c.player();
        int x = pos.get("block_x").getAsInt() + 6, y = pos.get("block_y").getAsInt() + 20, z = pos.get("block_z").getAsInt();
        Fixtures.platform(c, x, y, z, 8);
        Fixtures.setBlock(c, x - 1, y + 1, z, "hbm:det_nuke");
        Fixtures.setBlock(c, x, y + 1, z, "ntmspace:det_salt");
        Fixtures.setBlock(c, x + 1, y + 1, z, "ntmspace:salted_fallout");
        Expect.block(c, x, y + 1, z, "ntmspace:det_salt");
        Expect.block(c, x + 1, y + 1, z, "ntmspace:salted_fallout");
        c.teleport(x + 0.5, y + 2.5, z + 3.5);
        c.lookAt(x + 0.5, y + 1.3, z + 0.5);
        c.waitTicks(20);
        c.screenshot("01-det-salt");

        // a wrecked Leviathan
        int tx = x, ty = y, tz = z - 6;
        JsonObject placed = Fixtures.placeOnTop(c, "hbm:machine_chungus", tx, ty, tz);
        System.out.println("[fork] chungus placed: " + placed);
        int[] core = findCore(c, tx, ty + 1, tz);
        Expect.that(core != null, "the Leviathan placed a core near " + tx + "," + (ty + 1) + "," + tz);
        String at = core[0] + " " + core[1] + " " + core[2];
        c.cmd("data merge block " + at + " {damaged:1b}");
        c.waitTicks(10);
        String nbt = c.blockEntity(core[0], core[1], core[2]).toString().replaceAll("\\s+", "");
        Expect.that(nbt.contains("damaged:1b"), "the Leviathan is wrecked; nbt " + nbt);
        c.cmd("clear @s");
        c.teleport(core[0] + 5.5, core[1] + 1, core[2] + 0.5);
        c.lookAt(core[0] + 0.5, core[1] + 1, core[2] + 0.5);
        c.waitTicks(20);
        c.screenshot("02-chungus-damaged");

        // a Duna oil deposit block
        Fixtures.setBlock(c, x + 3, y + 1, z, "ntmspace:ore_oil[body=duna]");
        Expect.block(c, x + 3, y + 1, z, "ntmspace:ore_oil");
        c.teleport(x + 3.5, y + 2.5, z + 2.5);
        c.lookAt(x + 3.5, y + 1.5, z + 0.5);
        c.waitTicks(10);
        c.screenshot("03-duna-oil");

        java.util.List<String> errors = c.logSinceMark("Exception|ERROR");
        System.out.println("[fork] log lines matching Exception/ERROR since the start: " + errors.size());
    }

    /** The turbine core: the placed block's 1.7 offset is three cells away along the facing; search a small box. */
    private static int[] findCore(Client c, int x, int y, int z) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                JsonObject be = c.blockEntity(x + dx, y, z + dz);
                if (be != null && be.has("nbt") && !be.get("nbt").isJsonNull() && be.toString().contains("machine_chungus")) {
                    return new int[] {x + dx, y, z + dz};
                }
            }
        }
        return null;
    }
}
