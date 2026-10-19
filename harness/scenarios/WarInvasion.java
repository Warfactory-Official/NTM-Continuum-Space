import dev.golem.harness.api.*;

/**
 * hbm-space's war between bodies and its invasions, on a real client on the Mun:
 *
 * <ul>
 *   <li>Invasion ({@code /ntmwar invasion start mun 40}; in play the angel's death starts it): the countdown's warning
 *       ("Incoming Invasion!" with the alarm), "Wave 1 is starting!", the client's synced invasion (wave 1, 20 kills
 *       needed), HBM's boss bar "Wave 1", meteors in the sky, and within ~13 s something the invasion sent (a drop pod or
 *       glyphids from one, or a siege saucer); 20 kills move it to wave 2. Screenshots {@code 00-invasion-wave1} (looking
 *       up: boss bar, meteors, saucers) and {@code 01-invasion-ground}.</li>
 *   <li>War ({@code /ntmwar projectile mun huge 40}): the projectile reaches the client, arrives after 40 ticks, its flash
 *       rises (synced, 1.7 perma-sync) and the Mun loses 50 health. Screenshots {@code 02-war-impact-*} towards the
 *       shockwave (x + 70, y 55, z + 50 in the sky: yaw -45, pitch -29) while it grows.</li>
 *   <li>Railgun: a war controller ({@code /ntmwar controller}) with a Mun drive and a chip on 4242, a railgun launched on
 *       4242 ({@code /ntmwar railgun 4242 orbit}); the terminal opened by hand: {@code getsat} answers
 *       "Satellite: SatelliteRailgun", {@code health} "health: 0", {@code fire} "Firing!" (screenshot
 *       {@code 03-terminal}). Then {@code /ntmwar railgun 4242 fire} twice (1.7 {@code onClick}, which nothing in 1.7
 *       called): the client's railgun flash runs (interp > 0) and a projectile is on its way to the Mun. Screenshots
 *       {@code 04-railgun-sky-*} of the sky (the railgun model, its flash).</li>
 * </ul>
 *
 * A human checks: the boss bar text, the meteor streaks, the saucers and pods, the shockwave and cens rings, the railgun
 * model in the sky. Independent: it clears the Mun's war and invasion and its railgun before and after, and the drop pods
 * that landed: a landed pod is a block ({@code ntmspace:combat_drop}, hbm-space's {@code TileEntityCombatDropPod}, 3 x 7
 * blocks of golden OBJ), which {@code /kill} does not remove and which 1.7 keeps for good ({@code combatPodDespawn}
 * defaults to off); left behind they stood in the Mun shots of later scenarios.
 */
public final class WarInvasion implements Scenario {

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
        c.cmd("difficulty normal"); // saucers discard themselves in peaceful
        c.cmd("effect give @s minecraft:resistance 1000 255 true");
        c.cmd("ntmspace tp mun");
        c.waitTicks(40);
        Expect.equal("ntmspace:mun", c.status().get("dimension").getAsString(), "dimension after /ntmspace tp mun");
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(20);
        Fixtures.platform(c, 0, Y, 0, 8, "minecraft:smooth_stone");
        c.cmd("ntmwar clear mun");
        c.cmd("ntmwar railgun 4242 remove");
        c.cmd("kill @e[type=!player,distance=..300]");
        clearLandedPods(c);
        c.waitTicks(10);
        c.markLog();

        // ---- the invasion
        c.cmd("ntmwar invasion start mun 40");
        c.waitTicks(60);
        Expect.that(!c.logSinceMark("Incoming Invasion!").isEmpty(), "the countdown's warning reached the chat");
        Expect.that(!c.logSinceMark("Wave 1 is starting!").isEmpty(), "wave 1 was announced");
        com.google.gson.JsonObject war = c.query("ntmspace_war", new com.google.gson.JsonObject());
        Expect.equal("mun", war.get("body").getAsString(), "client body");
        Expect.that(war.has("invasion_wave") && war.get("invasion_wave").getAsInt() == 1
                && war.get("invasion_invading").getAsBoolean() && war.get("invasion_killreq").getAsInt() == 20,
                "the client sees wave 1 invading, 20 kills needed: " + war);
        com.google.gson.JsonObject boss = new com.google.gson.JsonObject();
        boss.addProperty("mode", "boss");
        com.google.gson.JsonObject bar = c.query("hbm_small", boss);
        Expect.that(bar.get("showing").getAsBoolean() && "Wave 1".equals(bar.get("name").getAsString()), "boss bar 'Wave 1': " + bar);

        boolean sent = false;
        int meteors = 0;
        for (int i = 0; i < 26 && !sent; i++) {
            c.waitTicks(10);
            meteors = Math.max(meteors, c.query("ntmspace_war", new com.google.gson.JsonObject()).get("meteors").getAsInt());
            for (String type : new String[] {"ntmspace:entity_combat_pod", "ntmspace:entity_meme_ufo", "hbm:entity_glyphid"}) {
                com.google.gson.JsonObject params = new com.google.gson.JsonObject();
                params.addProperty("radius", 300);
                params.addProperty("type", type);
                params.addProperty("server", true);
                com.google.gson.JsonArray list = c.query("entities", params).getAsJsonArray("entities");
                if (list != null && list.size() > 0) sent = true;
            }
        }
        Expect.that(meteors > 0, "meteors fall while the invasion runs");
        Expect.that(sent, "the invasion sent a drop pod, glyphids or a saucer within ~13 s");
        c.look(0, -60);
        c.waitTicks(5);
        c.screenshot("00-invasion-wave1");
        c.look(0, 20);
        c.waitTicks(5);
        c.screenshot("01-invasion-ground");

        c.cmd("ntmwar invasion kills mun 20");
        c.waitTicks(5);
        Expect.that(!c.logSinceMark("Wave 2 is starting!").isEmpty(), "20 kills start wave 2");
        c.cmd("ntmwar invasion stop mun");
        c.cmd("kill @e[type=!player,distance=..300]");
        c.waitTicks(10);

        // ---- the war: one projectile at the Mun
        c.cmd("ntmwar projectile mun huge 40");
        c.waitTicks(5);
        war = c.query("ntmspace_war", new com.google.gson.JsonObject());
        Expect.that(war.has("projectiles") && war.getAsJsonArray("projectiles").size() == 1, "the client sees the projectile: " + war);
        c.waitTicks(45);
        c.setOption("hide_gui", "true");
        c.look(-45, -29);
        float flash = 0;
        for (int i = 0; i < 3; i++) {
            c.waitTicks(10);
            c.screenshot("02-war-impact-" + i);
            war = c.query("ntmspace_war", new com.google.gson.JsonObject());
            if (war.has("projectiles") && war.getAsJsonArray("projectiles").size() > 0) {
                flash = Math.max(flash, war.getAsJsonArray("projectiles").get(0).getAsJsonObject().get("flash").getAsFloat());
            }
        }
        c.setOption("hide_gui", "false");
        Expect.that(flash > 0, "the impact flash rose on the client (synced): " + war);
        c.waitTicks(80);
        c.markLog();
        c.cmd("ntmwar status mun");
        c.waitTicks(5);
        Expect.that(!c.logSinceMark("status mun: war health 50 ").isEmpty(), "the Mun lost 50 health: " + c.logSinceMark("status mun"));

        // ---- the railgun and the war controller
        c.cmd("ntmwar clear mun");
        c.cmd("ntmwar railgun 4242 orbit");
        c.cmd("ntmwar controller 3 " + (Y + 1) + " 3 4242 mun");
        c.waitTicks(10);
        c.teleport(3.5, Y + 1, 0.5);
        c.waitTicks(5);
        Fixtures.interact(c, 3, Y + 1, 3);
        c.waitScreen("", 5000);
        c.waitTicks(10);
        for (String command : new String[] {"getsat", "health", "fire"}) {
            c.type(command);
            c.key("enter");
            c.waitTicks(3);
        }
        war = c.query("ntmspace_war", new com.google.gson.JsonObject());
        String terminal = war.has("terminal") ? war.getAsJsonArray("terminal").toString() : "";
        c.screenshot("03-terminal");
        Expect.that(terminal.contains("Satellite: SatelliteRailgun"), "getsat names the railgun: " + terminal);
        Expect.that(terminal.contains("health: 0"), "health answers 0 (1.7 never set it): " + terminal);
        Expect.that(terminal.contains("Firing!"), "fire answers Firing!: " + terminal);
        c.key("escape");
        c.waitTicks(5);

        c.cmd("ntmwar railgun 4242 fire"); // starts the war on the Mun
        c.waitTicks(5);
        c.cmd("ntmwar railgun 4242 fire"); // fires one projectile
        c.setOption("hide_gui", "true");
        float interp = 0;
        String[] views = {"zenith", "east", "west"};
        double[][] looks = {{0, -90}, {-90, -40}, {90, -40}};
        for (int i = 0; i < views.length; i++) {
            c.look(looks[i][0], looks[i][1]);
            c.waitTicks(6);
            c.screenshot("04-railgun-sky-" + views[i]);
            war = c.query("ntmspace_war", new com.google.gson.JsonObject());
            for (com.google.gson.JsonElement r : war.getAsJsonArray("railguns")) {
                interp = Math.max(interp, r.getAsJsonObject().get("interp").getAsFloat());
            }
        }
        c.setOption("hide_gui", "false");
        Expect.that(war.getAsJsonArray("railguns").size() == 1, "the client draws the one railgun: " + war);
        Expect.that(interp > 0, "the railgun's flash ran on the client");
        Expect.that(war.has("projectiles") && war.getAsJsonArray("projectiles").size() == 1, "the railgun's shot is on its way: " + war);

        c.cmd("ntmwar clear mun");
        c.cmd("ntmwar railgun 4242 remove");
        c.cmd("setblock 3 " + (Y + 1) + " 3 minecraft:air");
        c.cmd("kill @e[type=!player,distance=..300]");
        clearLandedPods(c);
        System.out.println("[war] " + c.node() + " meteors=" + meteors + " flash=" + flash + " interp=" + interp);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|golem)");
    }

    /**
     * Removes every landed drop pod within 64 blocks of the Mun's origin, the whole height (pods land at 1.7's
     * {@code player + nextGaussian() * 15}, so 64 is over four sigma), 16 x 128 x 16 per fill (vanilla's 32,768-block fill
     * limit). Reports how many were removed on stdout.
     */
    private static void clearLandedPods(Client c) {
        c.markLog();
        for (int x = -64; x < 64; x += 16) {
            for (int z = -64; z < 64; z += 16) {
                for (int y = 0; y < 256; y += 128) {
                    c.cmd("fill " + x + " " + y + " " + z + " " + (x + 15) + " " + (y + 127) + " " + (z + 15)
                            + " minecraft:air replace ntmspace:combat_drop");
                }
            }
        }
        System.out.println("[war] " + c.node() + " pods removed: " + c.logSinceMark("Successfully filled"));
    }
}
