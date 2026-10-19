import dev.golem.harness.api.*;

/**
 * NTM: Space's Mun slice (M1), on a real client:
 *
 * <ul>
 *   <li>{@code /ntmspace tp mun} puts the player in {@code ntmspace:mun};</li>
 *   <li>{@code /ntmspace check mun} compares the generator's columns with the 1.7 terrain port ("mun_check OK");</li>
 *   <li>gravity: a standing jump on the Mun rises several times higher than on Kerbin (hbm-space's g/1.6 scale
 *       through the vanilla gravity attribute, Golem Core's backport on 1.20.1);</li>
 *   <li>breathing: a vanilla furnace with coal lights on Kerbin and stays unlit on the Mun (no air), while lava
 *       still lights it there (HBM {@code Environment.BREATHING} through Golem {@code Furnaces.IGNITION});</li>
 *   <li>a coal furnace lights on the Mun where the probe addon seals the spot: its hook phase is ordered before NTM:
 *       Space's ({@code /ntmprobe seal});</li>
 *   <li>a powered HBM fan blows an item away on Kerbin and moves nothing on the Mun (hbm-space: no air, no fan);</li>
 *   <li>screenshots: 00-mun-surface, 01-mun-furnaces, 02-kerbin-furnace, the Mun's sky 03-zenith and 04-horizons
 *       (stars, sun and flare, Kerbin with its phase, the other bodies as points).</li>
 * </ul>
 */
public final class MunSlice implements Scenario {

    private static final int Y = 200;

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
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 1000 255 true");
        c.cmd("effect give @s minecraft:saturation 1000 255 true");

        // Kerbin (overworld) reference
        c.cmd("execute in minecraft:overworld run tp @s 0 " + (Y + 1) + " 0");
        c.waitTicks(20);
        Fixtures.platform(c, 0, Y, 0, 6);
        double kerbinJump = jump(c, 0.5, Y + 1, 0.5);
        furnace(c, 3, Y + 1, 0, "minecraft:coal");
        c.waitTicks(40);
        boolean kerbinLit = lit(c, 3, Y + 1, 0);
        c.teleport(0.5, Y + 1, -3.5);
        c.lookAt(3.5, Y + 1.5, 0.5);
        c.waitTicks(5);
        c.screenshot("02-kerbin-furnace");
        double kerbinFan = fanPush(c);

        // the Mun
        c.cmd("ntmspace tp mun");
        c.waitTicks(40);
        String dim = c.status().get("dimension").getAsString();
        Expect.equal("ntmspace:mun", dim, "dimension after /ntmspace tp mun");
        c.waitTicks(20);
        c.screenshot("00-mun-surface");

        // compare terrain generated in this game session: hbm-space's noise caves are seeded per session (a ported 1.7
        // quirk, see MapGenGreg), and the harness world outlives a session, so pick chunks no earlier run generated
        long fresh = 4000 + (System.currentTimeMillis() / 1000 % 100_000) * 64;
        c.cmd("tp @s " + fresh + ".5 250 " + fresh + ".5");
        c.waitTicks(60);
        c.markLog();
        c.cmd("ntmspace check mun");
        c.waitTicks(20);
        Expect.that(!c.logSinceMark("mun_check (OK|MISMATCH)").isEmpty() && c.logSinceMark("mun_check MISMATCH").isEmpty(),
                "/ntmspace check mun must log mun_check OK: " + c.logSinceMark("mun_check"));

        c.cmd("tp @s 0 " + (Y + 1) + " 0");
        c.waitTicks(20);
        Fixtures.platform(c, 0, Y, 0, 6);
        double munJump = jump(c, 0.5, Y + 1, 0.5);
        furnace(c, 3, Y + 1, 0, "minecraft:coal");
        furnace(c, 3, Y + 1, 2, "minecraft:lava_bucket");
        // the probe addon's sealed spot: its hook phase runs before NTM: Space's, so a fire breathes there
        c.cmd("ntmprobe seal 3 " + (Y + 1) + " 4");
        furnace(c, 3, Y + 1, 4, "minecraft:coal");
        c.waitTicks(40);
        boolean munSealedLit = lit(c, 3, Y + 1, 4);
        boolean munCoalLit = lit(c, 3, Y + 1, 0);
        boolean munLavaLit = lit(c, 3, Y + 1, 2);
        c.teleport(0.5, Y + 1, -3.5);
        c.lookAt(3.5, Y + 1.5, 1.5);
        c.waitTicks(5);
        c.screenshot("01-mun-furnaces");
        double munFan = fanPush(c);
        sky(c);

        c.cmd("gamemode creative");
        System.out.println("[mun] " + c.node() + " jump kerbin=" + kerbinJump + " mun=" + munJump
                + " furnace kerbin=" + kerbinLit + " mun coal=" + munCoalLit + " mun lava=" + munLavaLit
                + " sealed=" + munSealedLit + " fan kerbin=" + kerbinFan + " mun=" + munFan);

        Expect.that(kerbinJump > 0.9 && kerbinJump < 1.5, "Kerbin standing jump is vanilla's ~1.25 (polled, so the peak may be missed by a tick): " + kerbinJump);
        Expect.that(munJump > 4 * kerbinJump, "Mun jump must be several times Kerbin's: " + munJump + " vs " + kerbinJump);
        Expect.that(kerbinLit, "a coal furnace lights on Kerbin");
        Expect.that(!munCoalLit, "a coal furnace must not light on the Mun (no air)");
        Expect.that(munLavaLit, "a lava furnace still lights on the Mun");
        Expect.that(munSealedLit, "a coal furnace in the probe's sealed spot lights on the Mun (hook phase order)");
        Expect.that(kerbinFan > 1.0, "a powered fan blows an item away on Kerbin: " + kerbinFan);
        Expect.that(Math.abs(munFan) < 0.3, "a fan moves nothing on the Mun (no air): " + munFan);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    /** A powered HBM fan facing east at x -4, z -3 (on the platform), an item one block in front: how far east it went in 40 ticks. */
    private static double fanPush(Client c) {
        int z = -3;
        c.cmd("setblock -5 " + (Y + 1) + " " + z + " minecraft:redstone_block");
        c.cmd("setblock -4 " + (Y + 1) + " " + z + " hbm:fan[facing=east]");
        c.cmd("kill @e[type=item,distance=..30]");
        boolean legacy = c.node().startsWith("1.20.1");
        c.cmd("summon item -2.5 " + (Y + 1) + " " + (z + 0.5) + " {PickupDelay:32767,Item:{id:\"minecraft:stone\","
                + (legacy ? "Count:1b" : "count:1") + "}}");
        c.waitTicks(5);
        double start = itemX(c);
        c.waitTicks(40);
        double moved = itemX(c) - start;
        c.cmd("kill @e[type=item,distance=..30]");
        c.cmd("setblock -5 " + (Y + 1) + " " + z + " minecraft:air");
        c.cmd("setblock -4 " + (Y + 1) + " " + z + " minecraft:air");
        return moved;
    }

    private static double itemX(Client c) {
        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("radius", 30);
        params.addProperty("type", "item");
        params.addProperty("server", true);
        com.google.gson.JsonArray list = c.query("entities", params).getAsJsonArray("entities");
        Expect.that(list != null && list.size() > 0, "the fan test item exists");
        return list.get(0).getAsJsonObject().get("x").getAsDouble();
    }

    /** The Mun's sky: the zenith and four horizons, GUI hidden. */
    private static void sky(Client c) {
        c.setOption("hide_gui", "true");
        c.look(0, -90);
        c.waitTicks(5);
        c.screenshot("03-mun-sky-zenith");
        String[] names = {"south", "west", "north", "east"};
        for (int i = 0; i < 4; i++) {
            c.look(i * 90, -25);
            c.waitTicks(5);
            c.screenshot("04-mun-sky-" + names[i]);
        }
        c.setOption("hide_gui", "false");
    }

    /** Peak height above {@code y} of one standing jump from (x, y, z). */
    private static double jump(Client c, double x, double y, double z) {
        c.teleport(x, y, z);
        c.waitTicks(20);
        double base = c.player().get("y").getAsDouble();
        c.keyDown("key.jump");
        c.waitTicks(2);
        c.keyUp("key.jump");
        double peak = base;
        for (int i = 0; i < 80; i++) {
            c.waitTicks(1);
            JsonObjectHolder p = new JsonObjectHolder(c.player());
            peak = Math.max(peak, p.y);
            if (i > 5 && p.onGround) break;
        }
        return peak - base;
    }

    private static final class JsonObjectHolder {
        final double y;
        final boolean onGround;

        JsonObjectHolder(com.google.gson.JsonObject p) {
            y = p.get("y").getAsDouble();
            onGround = p.has("on_ground") && p.get("on_ground").getAsBoolean();
        }
    }

    private static void furnace(Client c, int x, int y, int z, String fuel) {
        c.cmd("setblock " + x + " " + y + " " + z + " minecraft:furnace[facing=north]");
        boolean legacy = c.node().startsWith("1.20.1");
        String count = legacy ? "Count:1b" : "count:1";
        String ore = legacy ? "Count:16b" : "count:16";
        c.cmd("data merge block " + x + " " + y + " " + z + " {Items:[{Slot:0b,id:\"minecraft:raw_iron\"," + ore
                + "},{Slot:1b,id:\"" + fuel + "\"," + count + "}]}");
    }

    private static boolean lit(Client c, int x, int y, int z) {
        com.google.gson.JsonObject b = c.block(x, y, z);
        return b.getAsJsonObject("properties").get("lit").getAsString().equals("true");
    }
}
