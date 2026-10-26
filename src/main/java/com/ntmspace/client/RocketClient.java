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
        dev.golem.core.client.Tooltips.onItemTooltip(RocketClient::partStats);
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
        float pitch = rocket.getPitch(1.0F);
        if (pitch == 0.0F) return;
        net.minecraft.world.phys.Vec3 d = dev.golem.core.client.PlayerRenderHooks.renderOffset(player, partialTick);
        pose.translate(-d.x, -d.y, -d.z);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-pitch));
        pose.translate(d.x, d.y, d.z);
    }

    /**
     * hbm-space's rocket stats in a missile part's tooltip (its {@code ItemCustomMissilePart.addInformation}: mass for
     * warheads and fuselages, thrust, ISP and mass for thrusters). HBM's own lines stay as 1.7 wrote them (tonnes,
     * litres), these follow; hbm-space replaced HBM's weight line instead.
     */
    private static void partStats(net.minecraft.world.item.ItemStack stack, java.util.List<net.minecraft.network.chat.Component> lines) {
        if (!(stack.getItem() instanceof com.hbm.items.weapon.ItemCustomMissilePart)) return;
        com.ntmspace.rocket.RocketPart part = com.ntmspace.rocket.RocketParts.get(stack);
        if (part == null) return;
        switch (part.type()) {
            case THRUSTER -> {
                lines.add(stat("thrust", part.getThrust() + "N"));
                lines.add(stat("isp", part.getISP() + "s"));
                lines.add(stat("mass", part.mass() + "kg"));
            }
            case WARHEAD, FUSELAGE -> lines.add(stat("mass", part.mass() + "kg"));
            default -> {}
        }
    }

    private static net.minecraft.network.chat.Component stat(String key, String value) {
        return net.minecraft.network.chat.Component.translatable("item.missile.part." + key).append(": ")
                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.BOLD)
                .append(net.minecraft.network.chat.Component.literal(value).withStyle(style -> style.withBold(false)
                        .withColor(net.minecraft.ChatFormatting.GRAY)));
    }

    private static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) return;
        if (!(player.getVehicle() instanceof EntityRideableRocket)) return;
        if (PlayerInputs.read(player).jump()) RocketInputs.sendJump();
    }
}
