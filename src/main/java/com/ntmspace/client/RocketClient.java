package com.ntmspace.client;

import com.ntmspace.satellite.SatelliteSettings;
import com.ntmspace.client.gui.rocket.GUILaunchPadRocket;
import com.ntmspace.client.gui.rocket.GUIMachineRocketAssembly;
import com.ntmspace.client.render.rocket.RenderNothing;
import com.ntmspace.client.render.rocket.RenderRocketCustom;
import com.ntmspace.client.render.rocket.RocketMachineRenderers;
import com.ntmspace.entity.rocket.EntityRideableRocket;
import com.ntmspace.entity.rocket.RocketEntities;
import com.ntmspace.entity.rocket.RocketInputs;
import com.ntmspace.entity.rocket.RocketStationSync;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.rocket.ItemVOTVdrive;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.PlayerInputs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * The rocket family's client start-up: the VAB and pad screens and renderers, the rocket's renderer (its capsule dummy
 * draws nothing), the transfer station sync, the drive tooltip's level, and the pilot's jump key ("JUMP TO LAUNCH").
 */
public final class RocketClient {

    private RocketClient() {}

    public static void init() {
        ClientRegistrations.screens(screens -> {
            screens.register(RocketBlocks.ROCKET_ASSEMBLY_MENU.get(), GUIMachineRocketAssembly::new);
            screens.register(RocketBlocks.LAUNCH_PAD_ROCKET_MENU.get(), GUILaunchPadRocket::new);
            screens.register(SatelliteSettings.MENU.get(), SatelliteSettingsScreen::new);
        });
        ClientRegistrations.renderers(r -> {
            r.entity(RocketEntities.RIDEABLE_ROCKET.get(), RenderRocketCustom::new);
            r.entity(RocketEntities.RIDEABLE_ROCKET_DUMMY.get(), RenderNothing::new);
            r.blockEntity(RocketBlocks.ROCKET_ASSEMBLY_BE.get(), context -> new RocketMachineRenderers.Assembly());
            r.blockEntity(RocketBlocks.LAUNCH_PAD_ROCKET_BE.get(), context -> new RocketMachineRenderers.Pad());
        });
        RocketStationSync.initClient();
        StationLauncherClient.init();
        DriveProcessorClient.init();
        ItemVOTVdrive.clientLevel = () -> Minecraft.getInstance().level;
        ClientTicks.onEndTick(RocketClient::tick);
        // hbm-space ModEventHandlerRenderer: third person zoomed out to 12 blocks while inside a rocket
        dev.golem.core.client.CameraHooks.DISTANCE.register((player, distance) ->
                player.getVehicle() instanceof EntityRideableRocket ? 12.0F : distance);
        dev.golem.core.client.PlayerRenderHooks.POSE.register(RocketClient::tiltRider);
        FacadeClient.init();
    }

    /**
     * hbm-space {@code onRenderRidingPlayerPre}: a player riding a rocket is drawn rotated by minus the rocket's pitch
     * ({@code rotationPitch}, not interpolated) about Z. 1.7 applied the {@code glRotated} before {@code RenderPlayer}
     * translated to the player, so the pivot is the camera, not the player: odd, kept (the rider swings with the rocket's
     * turn as seen from the camera).
     */
    private static void tiltRider(net.minecraft.world.entity.player.Player player, com.mojang.blaze3d.vertex.PoseStack pose,
                                  float partialTick) {
        if (!(player.getVehicle() instanceof EntityRideableRocket rocket)) return;
        float pitch = rocket.getXRot();
        if (pitch == 0.0F) return;
        net.minecraft.world.phys.Vec3 d = dev.golem.core.client.PlayerRenderHooks.renderOffset(player, partialTick);
        pose.translate(-d.x, -d.y, -d.z);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-pitch));
        pose.translate(d.x, d.y, d.z);
    }

    private static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) return;
        if (!(player.getVehicle() instanceof EntityRideableRocket)) return;
        if (PlayerInputs.read(player).jump()) RocketInputs.sendJump();
    }
}
