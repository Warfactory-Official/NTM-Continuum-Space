package com.ntmspace.client;

import com.ntmspace.client.gui.GUIOrbitalStationComputer;
import com.ntmspace.client.render.station.StationRenderers;
import com.ntmspace.registry.StationBlocks;
import dev.golem.core.client.ClientRegistrations;

/**
 * The orbit's client side: the station computer's screen, the station block renderers, the orbit sky
 * ({@link OrbitSky}). Living free fall is applied by the common vanilla-travel hooks on the client and server.
 */
public final class OrbitClient {

    private OrbitClient() {}

    static void init() {
        ClientRegistrations.screens(screens -> screens.register(StationBlocks.ORBITAL_STATION_COMPUTER_MENU.get(), GUIOrbitalStationComputer::new));
        ClientRegistrations.renderers(StationRenderers::register);
        OrbitSky.init();
        com.hbm.client.render.MachineItemNamespaces.add(com.ntmspace.NtmSpace.MOD_ID);
    }

}
