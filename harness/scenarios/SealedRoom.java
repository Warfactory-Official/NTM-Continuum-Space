import dev.golem.harness.api.*;

/**
 * NTM: Space's sealed rooms and breathing (hbm-space {@code handler/atmosphere}, {@code TileEntityAirPump},
 * {@code EntityEffectHandler.handleOxy}), on a real client on the Mun:
 *
 * <ul>
 *   <li>a hollow stone room with an air vent ({@code ntmspace:air_vent}) filled with oxygen becomes a sealed room: the
 *       vent's room holds 0.2 atm of oxygen ({@code /ntmspace atmosphere} inside logs {@code breathable=true room=true});</li>
 *   <li>a coal furnace inside the room lights, one outside on the airless Mun does not (HBM {@code Environment.BREATHING}
 *       answered by the room);</li>
 *   <li>a survival player outside without a suit loses oxygen (100 to about 40 in 60 ticks, the HUD shows the bubble row)
 *       and gets it back at once inside the room;</li>
 *   <li>opening the room (breaking a wall block as a player would) voids it: the inside reads {@code room=false};</li>
 *   <li>screenshots: 00-room-outside (both furnaces), 01-vent-overlay (the vent's look overlay: oxygen 0.2 atm),
 *       02-oxygen-hud (bubbles while suffocating), 03-room-inside.</li>
 * </ul>
 */
public final class SealedRoom implements Scenario {

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
        c.cmd("gamemode creative");
        c.cmd("ntmspace tp mun");
        c.waitTicks(40);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp mun");
        c.cmd("tp @s 46 " + (Y + 1) + " 4");
        c.waitTicks(20);
        Fixtures.platform(c, 40, Y, 0, 8);

        // the room: a hollow 7 x 5 x 7 stone box, inside 5 x 3 x 5, an air vent on its floor
        c.cmd("fill 37 " + (Y + 1) + " -3 43 " + (Y + 5) + " 3 minecraft:stone hollow");
        c.cmd("setblock 40 " + (Y + 2) + " 0 ntmspace:air_vent");
        furnace(c, 42, Y + 2, 2, "minecraft:coal");   // inside
        furnace(c, 45, Y + 1, 0, "minecraft:coal");   // outside, on the platform
        c.waitTicks(5);
        c.markLog();
        c.cmd("ntmspace vent fill 40 " + (Y + 2) + " 0");
        c.waitTicks(60); // the vent waits 10 ticks before it registers its room, then fills 20 blocks a tick
        Expect.that(!c.logSinceMark("vent_fill").isEmpty(), "the vent was filled: " + c.logSinceMark("vent_fill"));

        boolean insideLit = lit(c, 42, Y + 2, 2);
        boolean outsideLit = lit(c, 45, Y + 1, 0);

        // a survival player outside the room, on the airless Mun, without a suit
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:saturation 1000 255 true");
        c.teleport(46.5, Y + 1, 4.5);
        c.lookAt(41.5, Y + 2.5, 0.5);
        c.waitTicks(60);
        c.screenshot("02-oxygen-hud");
        String outside = check(c);
        c.screenshot("00-room-outside");

        // into the room
        c.teleport(39.5, Y + 2, -1.5);
        c.lookAt(40.5, Y + 2.5, 0.5);
        c.waitTicks(10);
        String inside = check(c);
        c.screenshot("01-vent-overlay");
        c.lookAt(42.5, Y + 2.5, 2.5);
        c.waitTicks(5);
        c.screenshot("03-room-inside");

        // break a wall block: the room joins the vacuum and is voided
        c.cmd("gamemode creative");
        c.teleport(46.5, Y + 1, 0.5);
        c.waitTicks(5);
        c.lookAt(43.5, Y + 2.5, 0.5);
        c.waitTicks(5);
        c.attack(); // creative: one hit breaks the block, through the break event as a player's
        c.waitTicks(10);
        boolean wallGone = c.block(43, Y + 2, 0).get("block").getAsString().equals("minecraft:air");
        c.teleport(39.5, Y + 2, -1.5);
        c.waitTicks(5);
        String opened = check(c);
        c.cmd("gamemode creative");

        System.out.println("[sealed-room] " + c.node() + " furnace inside=" + insideLit + " outside=" + outsideLit
                + " | outside: " + outside + " | inside: " + inside + " | wall gone=" + wallGone + " opened: " + opened);

        Expect.that(insideLit, "a coal furnace in the sealed room lights (0.2 atm of oxygen)");
        Expect.that(!outsideLit, "a coal furnace outside on the Mun stays unlit (no air)");
        Expect.that(outside.contains("breathable=false") && outside.contains("room=false"), "outside: no air: " + outside);
        int oxygen = Integer.parseInt(outside.replaceAll(".*oxygen=(-?\\d+).*", "$1"));
        Expect.that(oxygen < 60 && oxygen > 0, "60 ticks outside without a suit leave about 40 oxygen: " + oxygen);
        Expect.that(inside.contains("breathable=true") && inside.contains("room=true") && inside.contains("oxygen=100")
                && inside.contains("hbm:oxygen=0.2"), "inside: the room's 0.2 atm of oxygen, breath restored: " + inside);
        if (wallGone) Expect.that(opened.contains("room=false"), "an opened room is voided: " + opened);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    /** {@code /ntmspace atmosphere}'s line for the player's position. */
    private static String check(Client c) {
        c.markLog();
        c.cmd("ntmspace atmosphere");
        c.waitTicks(5);
        java.util.List<String> lines = c.logSinceMark("atmosphere_check");
        Expect.that(!lines.isEmpty(), "/ntmspace atmosphere logged nothing");
        return lines.get(lines.size() - 1);
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
