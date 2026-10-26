package com.ntmspace.satellite;

import com.hbm.registration.Reg;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import dev.golem.core.gui.Menus;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;

/** The held satellite's settings menu and the one server implementation of its edits. */
public final class SatelliteSettings {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NtmSpace.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);
    public static final RegistryObject<MenuType<SatelliteSettingsMenu>> MENU =
            REG.menu("satellite_settings", () -> Menus.typeWithHand(SatelliteSettingsMenu::new));

    private SatelliteSettings() {}

    public record Edit(int containerId, String owner, float altitude, float inclination, float phaseOffset,
                       boolean blinking, float blinkPeriod, int red, int green, int blue) implements GolemPayload {
        @Override public Identifier id() { return CODEC.id(); }
    }

    public static final GolemCodec<Edit> CODEC = GolemCodec.of(Edit.class, Ids.of(NtmSpace.MOD_ID, "satellite_settings"),
            (edit, buffer) -> {
                buffer.writeVarInt(edit.containerId());
                buffer.writeUtf(edit.owner(), 64);
                buffer.writeFloat(edit.altitude());
                buffer.writeFloat(edit.inclination());
                buffer.writeFloat(edit.phaseOffset());
                buffer.writeBoolean(edit.blinking());
                buffer.writeFloat(edit.blinkPeriod());
                buffer.writeInt(edit.red());
                buffer.writeInt(edit.green());
                buffer.writeInt(edit.blue());
            }, buffer -> new Edit(buffer.readVarInt(), buffer.readUtf(64), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readBoolean(), buffer.readFloat(), buffer.readInt(), buffer.readInt(), buffer.readInt()));

    static void init() {
        GolemNetwork.registerPlayToServer(CODEC, (edit, player) -> apply(player, edit.containerId(), edit.owner(),
                edit.altitude(), edit.inclination(), edit.phaseOffset(), edit.blinking(), edit.blinkPeriod(),
                edit.red(), edit.green(), edit.blue()));
    }

    public static int open(ServerPlayer player, InteractionHand hand) {
        Menus.openWithHand(player, new SimpleMenuProvider((id, inventory, opener) ->
                new SatelliteSettingsMenu(id, inventory, hand), Component.translatable("gui.ntmspace.satellite_settings.title")), hand);
        return player.containerMenu.containerId;
    }

    public static void apply(ServerPlayer player, int containerId, String owner, float altitude, float inclination,
                             float phaseOffset, boolean blinking, float blinkPeriod, int red, int green, int blue) {
        if (!Float.isFinite(altitude) || !Float.isFinite(inclination) || !Float.isFinite(phaseOffset)
                || !Float.isFinite(blinkPeriod)) throw new DecoderException("Nonfinite satellite setting");
        if (!(player.containerMenu instanceof SatelliteSettingsMenu menu) || menu.containerId != containerId
                || menu.getType() != MENU.get() || !menu.stillValid(player)) return;
        var stack = menu.stack();
        var type = XSatelliteRegistry.typeFor(stack);
        if (type == null || !SpaceSatellites.isSpace(type)) return;

        var previous = SpaceSatellites.orbit(stack);
        var resolvedOwner = owner.equals(previous.owner()) ? previous.owner() : player.getName().getString();
        SpaceSatellites.ORBIT.set(stack, new SatelliteOrbit(
                Mth.clamp(inclination, SatelliteOrbit.MIN_INCLINATION, SatelliteOrbit.MAX_INCLINATION),
                Mth.clamp(altitude, SatelliteOrbit.MIN_ALTITUDE_KM, SatelliteOrbit.MAX_ALTITUDE_KM),
                phaseOffset, blinking, blinkPeriod, resolvedOwner,
                Mth.clamp(red, 0, 255) / 255F, Mth.clamp(green, 0, 255) / 255F, Mth.clamp(blue, 0, 255) / 255F));
        player.inventoryMenu.broadcastChanges();
    }
}
