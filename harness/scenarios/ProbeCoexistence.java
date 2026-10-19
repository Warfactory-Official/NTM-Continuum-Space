import dev.golem.harness.api.*;
import com.google.gson.JsonObject;

/**
 * Two addons on every client extension point NTM: Space uses (scratchpad m2/probe-hook-families.md), on a real client,
 * through the dev probe addon's query {@code ntmprobe_coexist}:
 *
 * <ul>
 *   <li>listener order: the probe's client listeners (sky modifiers, skyboxes, light map, fog, camera distance, player
 *       pose, capes, look overlay) run after NTM: Space's, in the probe's phase ordered after the default one; the
 *       player pose, humanoid model and lowered-camera lists keep HBM's listener (phase {@code hbm:player}) before the
 *       probe's (these were single slots a second mod replaced);</li>
 *   <li>combined values: the probe's sky colour modifier receives NTM: Space's Mun colour and halves it; the fog is the
 *       nearest of both; the camera distance is chained; NTM: Space's named player keeps NTM: Space's cape while the
 *       local player gets the probe's;</li>
 *   <li>second claims refused: the probe's weather and clouds for the Mun and its item renderer under NTM: Space's name
 *       are start-up errors (Golem {@code Precipitation} / {@code Clouds}, HBM {@code CustomItemRenderers}), and NTM:
 *       Space's Mun weather and clouds stay; the probe's own dimension {@code ntmprobe:probe} has the probe's;</li>
 *   <li>the probe's payload beside NTM: Space's ({@code /ntmprobe ping}), the probe's entity with NTM: Space's renderer,
 *       a placed and broken block reaching the probe's Golem {@code BlockPlaces} / {@code BlockBreaks} listeners;</li>
 *   <li>screenshots: 01-probe-cow, 02-mun-sky, 03-probe-dimension.</li>
 * </ul>
 */
public final class ProbeCoexistence implements Scenario {

    private static final int Y = 200;

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
        c.cmd("execute in minecraft:overworld run tp @s 0.5 " + (Y + 1) + " 0.5");
        c.waitTicks(20);
        Fixtures.platform(c, 0, Y, 0, 6);
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(10);
        c.markLog();

        // ---- order, refusals, registries (Kerbin)
        JsonObject q = query(c, new JsonObject());
        JsonObject order = q.getAsJsonObject("order");
        for (String hook : order.keySet()) {
            Expect.that(order.get(hook).getAsBoolean(), "client hook " + hook + ": the probe's listener must run after NTM: Space's");
        }
        JsonObject refused = q.getAsJsonObject("refused");
        for (String claim : refused.keySet()) {
            Expect.that(refused.get(claim).getAsBoolean(), "a second registration must be refused: " + claim);
        }
        Expect.that(q.get("item_renderers").getAsBoolean(), "both addons' hbm:custom_item renderers exist");
        Expect.that(q.get("machine_namespaces").getAsBoolean(), "both addons' OBJ machine item namespaces are bound");
        Expect.that(q.get("fog_nearest").getAsBoolean(), "fog: the nearest far distance of both: " + q.get("fog_off") + " / " + q.get("fog_on"));
        Expect.that(q.get("camera_chained").getAsBoolean(), "camera distance chained: " + q.get("camera_off") + " -> " + q.get("camera_on"));
        Expect.that(!q.get("lower_camera_off").getAsBoolean(), "HBM's lowered camera is off without a glider");
        Expect.that(q.get("lower_camera_on").getAsBoolean(), "the probe's lowered-camera listener answers beside HBM's");
        Expect.equal(q.get("cape_space").getAsString(), q.get("cape_space_with_probe").getAsString(), "NTM: Space's named player's cape with the probe on");
        if (q.get("cape_own_off").getAsString().equals("null")) {
            Expect.that(q.get("cape_own_on").getAsString().contains("creeper"), "the probe's cape where nobody earlier names one: " + q.get("cape_own_on"));
        }

        // ---- the probe's payload
        c.cmd("ntmprobe ping 42");
        c.waitTicks(10);
        Expect.equal(42, query(c, new JsonObject()).get("ping").getAsInt(), "the probe's payload reached the client");

        // ---- a placed and broken block reaches the probe's listeners (Golem BlockPlaces / BlockBreaks)
        c.cmd("item replace entity @s weapon.mainhand with minecraft:lapis_block");
        c.waitTicks(5);
        c.lookAt(2.5, Y + 1.0, 0.5);
        c.waitTicks(5);
        c.use();
        c.waitTicks(10);
        boolean placed = c.block(2, Y + 1, 0).get("block").getAsString().equals("minecraft:lapis_block");
        c.lookAt(2.5, Y + 1.5, 0.5);
        c.waitTicks(5);
        c.attack();
        c.waitTicks(10);
        Expect.that(placed, "the lapis block was placed by the player");
        Expect.that(!c.logSinceMark("probe_place").isEmpty(), "BlockPlaces reached the probe");
        Expect.that(!c.logSinceMark("probe_break").isEmpty(), "BlockBreaks reached the probe");

        // ---- the probe's entity, drawn by NTM: Space's renderer class (a type without a renderer kills the render thread)
        c.cmd("kill @e[type=ntmprobe:probe_cow]");
        c.cmd("summon ntmprobe:probe_cow 3.5 " + (Y + 1) + " 3.5 {NoAI:1b}");
        c.teleport(0.5, Y + 1, -2.5);
        c.lookAt(3.5, Y + 1.5, 3.5);
        c.waitTicks(20);
        JsonObject params = new JsonObject();
        params.addProperty("radius", 30);
        params.addProperty("type", "ntmprobe:probe_cow");
        Expect.that(c.query("entities", params).getAsJsonArray("entities").size() > 0, "the probe cow exists on the client");
        c.screenshot("01-probe-cow");
        Expect.that(c.inWorld(), "the client survived drawing the probe cow");
        c.cmd("kill @e[type=ntmprobe:probe_cow]");

        // ---- player pose in third person, light map modifier frames, the skybox's after-sky layer
        JsonObject third = new JsonObject();
        third.addProperty("third_person", true);
        third.addProperty("lightmap", true);
        JsonObject before = query(c, third);
        long poses = before.get("pose_calls").getAsLong();
        long models = before.get("model_calls").getAsLong();
        c.waitTicks(20);
        JsonObject first = new JsonObject();
        first.addProperty("third_person", false);
        first.addProperty("lightmap", false);
        JsonObject after = query(c, first);
        Expect.that(after.get("pose_calls").getAsLong() > poses, "PlayerRenderHooks.POSE reached the probe in third person");
        Expect.that(after.get("model_calls").getAsLong() > models, "PlayerRenderHooks.MODEL reached the probe in third person");
        Expect.that(after.get("lightmap_adds").getAsLong() > 0, "Lightmaps.MODIFIERS reached the probe");
        Expect.that(after.get("skybox_after_calls").getAsLong() > 0, "Skyboxes.SKYBOXES reached the probe");

        // ---- the Mun: NTM: Space's sky colour, chained into the probe's; its weather and clouds kept
        c.cmd("ntmspace tp mun");
        JsonObject mun = waitForDimension(c, "ntmspace:mun");
        Expect.equal("ntmspace:mun", mun.get("dimension").getAsString(), "dimension after /ntmspace tp mun");
        Expect.that(mun.get("sky_chained").getAsBoolean(), "Skies: the probe received NTM: Space's Mun colour " + mun.get("sky_off")
                + " and halved it: " + mun.get("sky_on"));
        Expect.that(!mun.get("sky_off").getAsString().equals("1.0,1.0,1.0"), "NTM: Space's sky modifier changed the Mun's colour");
        Expect.that(mun.get("clouds_custom").getAsBoolean() && mun.getAsJsonArray("cloud_layers").isEmpty(),
                "the Mun keeps NTM: Space's clouds (none, airless): " + mun.get("cloud_layers"));
        Expect.that(mun.get("precipitation_custom").getAsBoolean(), "the Mun keeps NTM: Space's weather renderer");
        c.setOption("hide_gui", "true");
        c.look(0, -30);
        for (int i = 0; i < 60 && c.screenClass() != null; i++) c.waitTicks(5);
        c.waitTicks(5);
        c.screenshot("02-mun-sky");
        c.setOption("hide_gui", "false");

        // ---- the probe's own dimension: its clouds and weather renderer, its light map formula
        c.cmd("execute in ntmprobe:probe run forceload add 0 0");
        c.cmd("execute in ntmprobe:probe run setblock 0 139 0 minecraft:glass");
        c.cmd("execute in ntmprobe:probe run tp @s 0.5 140 0.5");
        // the first trip into a fresh dimension can outlast a fixed wait (1.21.1 Fabric generates its spawn chunks first)
        JsonObject own = waitForDimension(c, "ntmprobe:probe");
        Expect.equal("ntmprobe:probe", own.get("dimension").getAsString(), "dimension after tp to ntmprobe:probe");
        Expect.that(own.get("clouds_custom").getAsBoolean() && own.getAsJsonArray("cloud_layers").toString().contains("160.0/1.0"),
                "the probe's clouds in its own dimension: " + own.get("cloud_layers"));
        Expect.that(own.get("precipitation_custom").getAsBoolean(), "the probe's weather renderer in its own dimension");
        Expect.that(own.get("lightmap_classic").getAsBoolean(), "the probe's classic light map in its own dimension");
        c.setOption("hide_gui", "true");
        String cloudsBefore = c.setOption("clouds", "fancy");
        c.look(0, -35); // up at the probe's green clouds at y 160
        c.waitTicks(100);
        c.screenshot("03-probe-dimension");
        Expect.that(query(c, new JsonObject()).get("cloud_layers_drawn").getAsLong() > own.get("cloud_layers_drawn").getAsLong(),
                "Golem drew the probe's cloud layer in its dimension");
        c.setOption("clouds", cloudsBefore);
        c.setOption("hide_gui", "false");

        c.cmd("execute in minecraft:overworld run tp @s 0.5 " + (Y + 1) + " 0.5");
        c.waitTicks(20);
        System.out.println("[probe] " + c.node() + " kerbin=" + q + " mun=" + mun + " own=" + own);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(ntmspace|ntmprobe|golem)");
    }

    /** Polls until the client is in {@code dimension} with no screen open (at most 30 s), then lets the sky settle. */
    private static JsonObject waitForDimension(Client c, String dimension) {
        for (int i = 0; i < 120; i++) {
            c.waitTicks(5);
            if (c.screenClass() == null && dimension.equals(query(c, new JsonObject()).get("dimension").getAsString())) break;
        }
        c.waitTicks(40);
        return query(c, new JsonObject());
    }

    private static JsonObject query(Client c, JsonObject params) {
        return c.query("ntmprobe_coexist", params);
    }
}
