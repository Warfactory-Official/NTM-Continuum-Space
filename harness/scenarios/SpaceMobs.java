import dev.golem.harness.api.*;

/**
 * hbm-space's mobs on their planets (History §19fb-mobs), on a real client:
 *
 * <ul>
 *   <li>each of the eleven mobs is summoned (NoAI, so the pose is deterministic and nothing flies off) on its body:
 *       the Mun cow, the angel and the mini UFO on the Mun; the scutterfish, scrapfish, sifter eel, depth squid,
 *       scuttlecrab and flesh creeper on Laythe; the tankbot and the war behemoth on Thatmo;</li>
 *   <li>positive signals: every mob reaches the client alive (the client's own entity list, so its renderer was bound
 *       and did not take the render thread down), and the log since the mark has no failed texture or OBJ load for
 *       {@code ntmspace:textures/entity} / {@code ntmspace:models/mobs} and no exception naming ntmspace or golem;</li>
 *   <li>screenshots, front lit (noon, light blocks beside the mob, GUI hidden), close enough to fill the frame:
 *       {@code mob-<entity id>}. A human checks each for the 1.7 look (no magenta-black missing texture, no missing
 *       parts, skins: moon_cow, scutterfish, robtex, wm_*, bfa_body, ufo_siege_*, the multi-skin families).</li>
 * </ul>
 *
 * Independent of other scenarios: it builds its own platform 200 blocks up at 0, 0 on each body and cleans up its mobs.
 */
public final class SpaceMobs implements Scenario {

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
        c.cmd("gamerule doMobSpawning false");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("difficulty normal"); // the angel and the saucers discard themselves in peaceful
        c.markLog();

        StringBuilder missing = new StringBuilder();

        body(c, "mun", missing,
                new Mob("ntmspace:entity_moon_cow", 4),
                new Mob("ntmspace:entity_meme_ufo", 4),
                new Mob("ntmspace:entity_ntm_bfa", 14));
        body(c, "laythe", missing,
                new Mob("ntmspace:entity_scutterfish", 6),
                new Mob("ntmspace:entity_scrapfish", 6),
                new Mob("ntmspace:entity_siftereel", 6),
                new Mob("ntmspace:entity_depthsquid", 4),
                new Mob("ntmspace:entity_scuttlecrab", 4),
                new Mob("ntmspace:entity_mob_flesh_creeper", 4));
        body(c, "thatmo", missing,
                new Mob("ntmspace:entity_tankbot", 8),
                new Mob("ntmspace:entity_war_behemoth", 14));

        c.setOption("hide_gui", "false");
        Expect.that(missing.length() == 0, "every mob must reach the client alive:" + missing);
        Expect.that(c.logSinceMark("(?i)(fail|missing|unable).*ntmspace:(textures/entity|models/mobs)").isEmpty(),
                "no failed mob texture or OBJ load: " + c.logSinceMark("(?i)ntmspace:(textures/entity|models/mobs)"));
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    private record Mob(String id, double distance) {}

    private static void body(Client c, String body, StringBuilder missing, Mob... mobs) {
        c.cmd("ntmspace tp " + body);
        c.waitTicks(40);
        c.cmd("time set 6000");
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(20);
        Fixtures.platform(c, 0, Y, 0, 8, "minecraft:smooth_stone");
        // light on both sides of the spot where the mob stands, so its front is lit whatever the body's sky
        for (int x : new int[] {-3, 3}) {
            c.cmd("setblock " + x + " " + (Y + 2) + " -6 minecraft:light[level=15]");
            c.cmd("setblock " + x + " " + (Y + 6) + " -6 minecraft:light[level=15]");
        }

        for (Mob mob : mobs) {
            c.cmd("kill @e[type=!player,distance=..40]");
            c.cmd("summon " + mob.id() + " 0.5 " + (Y + 1) + " -6.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
            c.waitTicks(10);
            // the camera stands in front of the mob (the mob faces +z, towards it) and looks at its middle
            c.teleport(0.5, Y + 1, -6.5 + mob.distance());
            c.waitTicks(5);
            c.lookAt(0.5, Y + 1.8, -6.5);
            c.waitTicks(10);

            com.google.gson.JsonObject params = new com.google.gson.JsonObject();
            params.addProperty("radius", 40);
            params.addProperty("type", mob.id());
            com.google.gson.JsonArray seen = c.query("entities", params).getAsJsonArray("entities");
            boolean alive = seen != null && seen.size() > 0 && seen.get(0).getAsJsonObject().get("alive").getAsBoolean();
            if (!alive) missing.append(' ').append(mob.id()).append(" on ").append(body);

            c.setOption("hide_gui", "true");
            c.screenshot("mob-" + mob.id().substring(mob.id().indexOf(':') + 1));
            c.setOption("hide_gui", "false");
            System.out.println("[mobs] " + c.node() + " " + body + " " + mob.id() + " client alive=" + alive);
        }
        c.cmd("kill @e[type=!player,distance=..40]");
    }
}
