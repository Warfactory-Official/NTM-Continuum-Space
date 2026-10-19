package com.ntmspace.probe.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ntmspace.probe.ProbeEntry;
import com.ntmspace.probe.ProbeMarks;
import com.ntmspace.probe.ProbeNet;
import com.ntmspace.probe.ProbeRecords;
import com.ntmspace.probe.ProbeWorld;
import dev.golem.core.hook.Hook;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The probe on every client extension point NTM: Space uses (scratchpad m2/probe-hook-families.md). Each listener is
 * inert until the harness query {@code ntmprobe_coexist} switches it on, and runs in the probe's phase
 * {@link #AFTER}, ordered after the default phase NTM: Space registers in: Fabric runs the two mods' {@code client}
 * entrypoints in no defined order, so only a phase orders two mods' client listeners.
 *
 * <p>The query ({@link #query}) answers, per family, the listener order and the combined value with the probe off and
 * on; the scenario {@code harness/scenarios/ProbeCoexistence.java} asserts them.
 */
public final class ProbeClient {

    /** The probe's client phase, after the default phase. */
    public static final String AFTER = "ntmprobe:after";

    private static final ResourceKey<Level> LEVEL = dev.golem.core.world.Dimensions.level(ProbeEntry.MOD_ID, ProbeWorld.DIMENSION);

    private static long skyboxAfter;
    private static long poses;
    private static long models;
    private static long lightmapAdds;
    private static long lookOverlays;
    private static long precipitationFrames;
    private static JsonObject refusals;

    private ProbeClient() {}

    private static <T> void after(Hook<T> hook, T listener) {
        hook.addPhaseOrdering(Hook.DEFAULT_PHASE, AFTER);
        hook.register(AFTER, listener);
    }

    public static void init() {
        ProbeNet.initClient();
        // NTM: Space's skyboxes are in its own phase before the default one
        dev.golem.core.render.Skyboxes.SKYBOXES.register(new dev.golem.core.render.Skyboxes.Skybox() {
            @Override
            public void afterSky(dev.golem.core.render.Sky sky) {
                skyboxAfter++;
            }
        });
        after(dev.golem.core.client.Lightmaps.MODIFIERS, new dev.golem.core.client.Lightmaps.Modifier() {
            @Override
            public boolean active(net.minecraft.client.multiplayer.ClientLevel level) {
                return ProbeMarks.flag("lightmap") != null;
            }

            @Override
            public void add(net.minecraft.client.multiplayer.ClientLevel level, int block, int sky, int[] rgb) {
                lightmapAdds++;
            }
        });
        dev.golem.core.client.Lightmaps.useClassicFormula(level -> level.dimension().equals(LEVEL));
        after(dev.golem.core.client.Fogs.MODIFIERS, new dev.golem.core.client.Fogs.Modifier() {
            @Override
            public boolean active(net.minecraft.world.entity.Entity camera) {
                return ProbeMarks.flag("fog") != null;
            }

            @Override
            public float far(net.minecraft.world.entity.Entity camera, float renderDistanceBlocks) {
                return ProbeMarks.flag("fog") instanceof Number n ? n.floatValue() : Float.NaN;
            }
        });
        after(dev.golem.core.client.CameraHooks.DISTANCE, (player, distance) ->
                ProbeMarks.flag("camera") instanceof Number n ? distance + n.floatValue() : distance);
        after(dev.golem.core.client.PlayerRenderHooks.POSE, (player, pose, partialTick) -> poses++);
        // HBM's model and lower-camera listeners (glider, gun arm pose) are in these lists too: a second mod's joins them
        after(dev.golem.core.client.PlayerRenderHooks.MODEL, parts -> models++);
        after(dev.golem.core.client.CameraHooks.LOWER_THIRD_PERSON, player -> ProbeMarks.flag("lower_camera") != null);
        var cape = dev.golem.core.Ids.of("minecraft", "textures/entity/creeper/creeper.png");
        after(dev.golem.core.client.Capes.PROVIDERS, (id, name) -> ProbeMarks.flag("cape") != null ? cape : null);
        after(com.hbm.interfaces.ILookOverlay.PROVIDERS, (level, pos, player) -> {
            lookOverlays++;
            return null;
        });
        // the probe's own dimension: its weather and clouds (NTM: Space's bodies keep theirs; see the refusals)
        dev.golem.core.client.Precipitation.register(LEVEL, frame -> precipitationFrames++);
        dev.golem.core.client.Clouds.register(LEVEL, (level, partialTick) ->
                List.of(new dev.golem.core.client.Clouds.Layer(160F, 0.25F, 1.0F, 0.25F, 0.8F, 0, 0)));
        // HBM's item renderers (by name) and OBJ machine item namespaces
        com.hbm.client.render.item.CustomItemRenderers.register(ProbeEntry.MOD_ID + ":plushie",
                com.ntmspace.client.render.RenderSpacePlushie.Item::new);
        com.hbm.client.render.MachineItemNamespaces.add(ProbeEntry.MOD_ID);
        // an entity renderer for the probe's entity type (NTM: Space's renderer class)
        dev.golem.core.client.ClientRegistrations.renderers(r ->
                r.entity(ProbeRecords.PROBE_COW.get(), com.ntmspace.client.render.mob.RenderMoonCow::new));
        dev.golem.core.harness.HarnessQueryRegistry.register("ntmprobe_coexist", ProbeClient::query);
    }

    /**
     * Second registrations of what NTM: Space already owns, tried once at the first query (Fabric may run the probe's
     * client entrypoint before NTM: Space's, so not at start-up): each must be refused, and NTM: Space's must stay.
     */
    private static synchronized JsonObject refusals() {
        if (refusals != null) return refusals;
        refusals = new JsonObject();
        refusals.addProperty("precipitation_mun", refused(() -> dev.golem.core.client.Precipitation.register(
                com.ntmspace.world.SpaceDimensions.MUN, frame -> {})));
        refusals.addProperty("clouds_mun", refused(() -> dev.golem.core.client.Clouds.register(
                com.ntmspace.world.SpaceDimensions.MUN, (level, partialTick) -> List.of())));
        refusals.addProperty("item_renderer_plushie", refused(() -> com.hbm.client.render.item.CustomItemRenderers.register(
                "ntmspace:plushie", com.ntmspace.client.render.RenderSpacePlushie.Item::new)));
        return refusals;
    }

    private static boolean refused(Runnable registration) {
        try {
            registration.run();
            return false;
        } catch (IllegalStateException expected) {
            ProbeEntry.LOG.info("probe_refused {}", expected.getMessage());
            return true;
        }
    }

    // ---------------------------------------------------------------------------------------------- the query

    /**
     * Harness query {@code ntmprobe_coexist}. Params (all optional): {@code third_person} (switches the camera),
     * {@code lightmap} (switches the probe's light-map modifier on or off). Answers each family's listener order
     * ({@code order.<hook>}: probe after NTM: Space) and its combined value with the probe off and on.
     */
    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject out = new JsonObject();
        out.add("refused", refusals());
        if (params.has("third_person")) {
            mc.options.setCameraType(params.get("third_person").getAsBoolean()
                    ? net.minecraft.client.CameraType.THIRD_PERSON_BACK : net.minecraft.client.CameraType.FIRST_PERSON);
        }
        if (params.has("lightmap")) {
            if (params.get("lightmap").getAsBoolean()) ProbeMarks.flag("lightmap", true);
            else ProbeMarks.clear("lightmap");
        }

        JsonObject order = new JsonObject();
        order.addProperty("skies", after(dev.golem.core.world.Skies.MODIFIERS));
        order.addProperty("skyboxes", after(dev.golem.core.render.Skyboxes.SKYBOXES));
        order.addProperty("lightmaps", after(dev.golem.core.client.Lightmaps.MODIFIERS));
        order.addProperty("fogs", after(dev.golem.core.client.Fogs.MODIFIERS));
        order.addProperty("camera", after(dev.golem.core.client.CameraHooks.DISTANCE));
        order.addProperty("pose", after(dev.golem.core.client.PlayerRenderHooks.POSE));
        order.addProperty("pose_hbm", afterHbm(dev.golem.core.client.PlayerRenderHooks.POSE));
        order.addProperty("model_hbm", afterHbm(dev.golem.core.client.PlayerRenderHooks.MODEL));
        order.addProperty("lower_camera_hbm", afterHbm(dev.golem.core.client.CameraHooks.LOWER_THIRD_PERSON));
        order.addProperty("capes", after(dev.golem.core.client.Capes.PROVIDERS));
        order.addProperty("look_overlay", after(com.hbm.interfaces.ILookOverlay.PROVIDERS));
        out.add("order", order);

        out.addProperty("skybox_after_calls", skyboxAfter);
        out.addProperty("pose_calls", poses);
        out.addProperty("model_calls", models);
        out.addProperty("lightmap_adds", lightmapAdds);
        out.addProperty("look_overlay_calls", lookOverlays);
        out.addProperty("precipitation_frames", precipitationFrames);
        out.addProperty("item_renderers", com.hbm.client.render.item.CustomItemRenderers.exists("ntmspace:plushie")
                && com.hbm.client.render.item.CustomItemRenderers.exists(ProbeEntry.MOD_ID + ":plushie"));
        out.addProperty("machine_namespaces", com.hbm.client.render.MachineItemNamespaces.contains("ntmspace")
                && com.hbm.client.render.MachineItemNamespaces.contains(ProbeEntry.MOD_ID));
        out.addProperty("ping", ProbeNet.lastPing);
        if (mc.level == null || mc.player == null) return out;

        var level = mc.level;
        out.addProperty("dimension", dev.golem.core.Ids.keyId(level.dimension()).toString());
        out.addProperty("precipitation_custom", dev.golem.core.client.Precipitation.has(level));
        out.addProperty("clouds_custom", dev.golem.core.client.Clouds.has(level));
        JsonArray layers = new JsonArray();
        for (var layer : dev.golem.core.client.Clouds.layers(level, 1.0F)) layers.add(layer.height() + "/" + layer.green());
        out.add("cloud_layers", layers);
        out.addProperty("lightmap_classic", dev.golem.core.client.Lightmaps.classic(level));
        out.addProperty("cloud_layers_drawn", dev.golem.core.render.CloudRendering.layersDrawn());

        // sky colour: NTM: Space's celestial phase first, then the probe halves what it received
        Vec3 white = new Vec3(1, 1, 1);
        Vec3 skyOff = dev.golem.core.world.Skies.skyColor(level, white);
        ProbeMarks.level("skies", level.dimension(), 0.5D);
        Vec3 skyOn;
        try {
            skyOn = dev.golem.core.world.Skies.skyColor(level, white);
        } finally {
            ProbeMarks.clear("skies");
        }
        Object input = ProbeMarks.flag("skies_input_color");
        out.addProperty("sky_off", skyOff.x + "," + skyOff.y + "," + skyOff.z);
        out.addProperty("sky_on", skyOn.x + "," + skyOn.y + "," + skyOn.z);
        out.addProperty("sky_chained", input instanceof Vec3 v && v.distanceTo(skyOff) < 1e-6 && skyOn.distanceTo(skyOff.scale(0.5)) < 1e-6);
        ProbeMarks.clear("skies_input_color");

        // fog: the nearest far distance of the active modifiers
        var camera = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        float rd = dev.golem.core.client.Fogs.renderDistanceBlocks();
        float fogOff = dev.golem.core.client.Fogs.far(camera, rd);
        ProbeMarks.flag("fog", 24.0F);
        float fogOn;
        try {
            fogOn = dev.golem.core.client.Fogs.far(camera, rd);
        } finally {
            ProbeMarks.clear("fog");
        }
        out.addProperty("fog_off", fogOff);
        out.addProperty("fog_on", fogOn);
        out.addProperty("fog_nearest", fogOn == (Float.isNaN(fogOff) ? 24.0F : Math.min(fogOff, 24.0F)));

        // camera distance: each listener receives the previous one's answer
        float camOff = dev.golem.core.client.CameraHooks.thirdPersonDistance(mc.player, 4.0F);
        ProbeMarks.flag("camera", 1.5F);
        float camOn;
        try {
            camOn = dev.golem.core.client.CameraHooks.thirdPersonDistance(mc.player, 4.0F);
        } finally {
            ProbeMarks.clear("camera");
        }
        out.addProperty("camera_off", camOff);
        out.addProperty("camera_on", camOn);
        out.addProperty("camera_chained", Math.abs(camOn - (camOff + 1.5F)) < 1e-4);

        // lowered third-person camera: any listener asks (HBM's glider rule, then the probe's mark)
        boolean lowerOff = dev.golem.core.client.CameraHooks.lowerThirdPersonCamera(mc.player);
        ProbeMarks.flag("lower_camera", true);
        boolean lowerOn;
        try {
            lowerOn = dev.golem.core.client.CameraHooks.lowerThirdPersonCamera(mc.player);
        } finally {
            ProbeMarks.clear("lower_camera");
        }
        out.addProperty("lower_camera_off", lowerOff);
        out.addProperty("lower_camera_on", lowerOn);

        // capes: the first provider that names one; NTM: Space's named players keep NTM: Space's cape
        java.util.UUID gwen = java.util.UUID.fromString(com.ntmspace.fork.client.ForkClient.DUODEC_);
        var spaceCape = dev.golem.core.client.Capes.capeFor(gwen, "probe");
        var ownOff = dev.golem.core.client.Capes.capeFor(mc.player.getUUID(), mc.player.getName().getString());
        ProbeMarks.flag("cape", true);
        Object ownOn;
        Object spaceOn;
        try {
            ownOn = dev.golem.core.client.Capes.capeFor(mc.player.getUUID(), mc.player.getName().getString());
            spaceOn = dev.golem.core.client.Capes.capeFor(gwen, "probe");
        } finally {
            ProbeMarks.clear("cape");
        }
        out.addProperty("cape_space", String.valueOf(spaceCape));
        out.addProperty("cape_space_with_probe", String.valueOf(spaceOn));
        out.addProperty("cape_own_off", String.valueOf(ownOff));
        out.addProperty("cape_own_on", String.valueOf(ownOn));
        return out;
    }

    /**
     * True when every probe listener of {@code hook} runs after every HBM listener (and both exist): HBM's single-slot
     * player hooks became lists, so a second mod no longer replaces HBM's.
     */
    private static boolean afterHbm(Hook<?> hook) {
        int lastHbm = -1;
        int firstProbe = Integer.MAX_VALUE;
        List<?> listeners = hook.listeners();
        for (int i = 0; i < listeners.size(); i++) {
            String owner = listeners.get(i).getClass().getName();
            if (owner.startsWith("com.ntmspace.probe.")) firstProbe = Math.min(firstProbe, i);
            else if (owner.startsWith("com.hbm.")) lastHbm = i;
        }
        return lastHbm >= 0 && firstProbe != Integer.MAX_VALUE && lastHbm < firstProbe;
    }

    /** True when every probe listener of {@code hook} runs after every NTM: Space listener (and both exist). */
    private static boolean after(Hook<?> hook) {
        int lastSpace = -1;
        int firstProbe = Integer.MAX_VALUE;
        List<?> listeners = hook.listeners();
        for (int i = 0; i < listeners.size(); i++) {
            String owner = listeners.get(i).getClass().getName();
            if (owner.startsWith("com.ntmspace.probe.")) firstProbe = Math.min(firstProbe, i);
            else if (owner.startsWith("com.ntmspace.")) lastSpace = i;
        }
        return lastSpace >= 0 && firstProbe != Integer.MAX_VALUE && lastSpace < firstProbe;
    }
}
