package com.ntmspace.satellite;

//? if <26.1 {
/*import net.minecraft.world.InteractionResultHolder;
*///?}
//? if fabric {
/*import net.fabricmc.fabric.api.event.player.UseItemCallback;
*///?}
//? if forge {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
*///?}
//? if neoforge {
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//?}
import com.hbm.items.ISatChip;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteScience;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.rocket.RocketParts;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A satellite's settings on sneak-use and deployment from a station for payloads of at most sixteen tonnes. */
public final class SatelliteItems {

    private static final int HAND_LAUNCH_MASS = 16_000;
    private static boolean installed;

    private SatelliteItems() {}

    public static synchronized void init() {
        if (installed) return;
        installed = true;
        SatelliteSettings.init();
        //? if fabric && >=26.1 {
        /*UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            return use(stack, XSatelliteRegistry.typeFor(stack), level, player, hand);
        });
        *///?} elif fabric {
        /*UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            var result = use(stack, XSatelliteRegistry.typeFor(stack), level, player, hand);
            return result.consumesAction() ? InteractionResultHolder.success(stack)
                    : InteractionResultHolder.pass(stack);
        });
        *///?} elif neoforge {
        NeoForge.EVENT_BUS.addListener(
                (PlayerInteractEvent.RightClickItem event) -> {
                    var player = event.getEntity();
                    var stack = player.getItemInHand(event.getHand());
                    var result = use(stack, XSatelliteRegistry.typeFor(stack), event.getLevel(), player, event.getHand());
                    if (result.consumesAction()) {
                        event.setCanceled(true);
                        event.setCancellationResult(result);
                    }
                });
        //?} elif forge {
        /*MinecraftForge.EVENT_BUS.addListener(
                (PlayerInteractEvent.RightClickItem event) -> {
                    var player = event.getEntity();
                    var stack = player.getItemInHand(event.getHand());
                    var result = use(stack, XSatelliteRegistry.typeFor(stack), event.getLevel(), player, event.getHand());
                    if (result.consumesAction()) {
                        event.setCanceled(true);
                        event.setCancellationResult(result);
                    }
                });
        *///?}
    }

    public static boolean byHand(SatelliteType type) {
        var part = RocketParts.get(XSatelliteRegistry.stackFor(type));
        return part != null && part.mass() <= HAND_LAUNCH_MASS;
    }

    public static InteractionResult use(ItemStack stack, SatelliteType type, Level level, Player player, InteractionHand hand) {
        if (player.isSpectator() || player.getItemInHand(hand) != stack || stack.isEmpty()
                || XSatelliteRegistry.typeFor(stack) != type) return InteractionResult.TRY_WITH_EMPTY_HAND;
        var module = stack.getItem() instanceof ItemSatellite item
                && (item.typeOf(stack) == ItemSatellite.EnumSatType.SCIENCE_SENSOR
                    || item.typeOf(stack) == ItemSatellite.EnumSatType.SCIENCE_ASSEMBLER);
        var space = type != null && SpaceSatellites.isSpace(type);
        if (!space && type != SatelliteType.SCIENCE && !module) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (player.isShiftKeyDown()) {
            if (!space || module) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (player instanceof ServerPlayer serverPlayer) SatelliteSettings.open(serverPlayer, hand);
            return InteractionResult.SUCCESS;
        }

        var part = module ? RocketParts.get(stack) : null;
        if (!(module ? part != null && part.mass() <= HAND_LAUNCH_MASS : byHand(type)) || !Orbit.inOrbit(level)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (module && level.isClientSide()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) {
            var station = OrbitalStation.getStationFromPosition(server.getServer(), player.getBlockX(), player.getBlockZ());
            var target = server.getServer().getLevel(Orbit.levelOf(station.orbiting));
            if (target == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
            var frequency = ISatChip.getFreqS(stack);
            if (module && !(SatelliteSavedData.get(target).getSatFromFreq(frequency) instanceof SatelliteScience)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            Satellite.orbit(target, stack, frequency, player.getX(), player.getY(), player.getZ());
            player.sendSystemMessage(Component.translatable("chat.ntmspace.satellite.launched"));
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
