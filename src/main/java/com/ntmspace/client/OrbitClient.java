package com.ntmspace.client;

import com.ntmspace.client.gui.GUIOrbitalStationComputer;
import com.ntmspace.client.render.station.StationRenderers;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitEnvironment;
import com.ntmspace.registry.StationBlocks;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.PlayerInputs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * The orbit's client side: the station computer's screen, the station block renderers, the orbit sky
 * ({@link OrbitSky}), and free fall for the local player (1.7 ran {@code ModEventHandler}'s gravity code on the client
 * too; the player's motion is the client's): after each client tick, 1.7's zero-g step with the jump and sneak keys.
 */
public final class OrbitClient {

    private OrbitClient() {}

    static void init() {
        ClientRegistrations.screens(screens -> screens.register(StationBlocks.ORBITAL_STATION_COMPUTER_MENU.get(), GUIOrbitalStationComputer::new));
        ClientRegistrations.renderers(StationRenderers::register);
        OrbitSky.init();
        ClientTicks.onEndTick(OrbitClient::tick);
        com.hbm.client.render.MachineItemNamespaces.add(com.ntmspace.NtmSpace.MOD_ID);
    }

    private static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused() || !Orbit.inOrbit(player.level())) return;
        if (player.isPassenger()) return;
        PlayerInputs.State input = PlayerInputs.read(player);
        OrbitEnvironment.freeFall(player, input.jump(), input.sneak());
    }
}
