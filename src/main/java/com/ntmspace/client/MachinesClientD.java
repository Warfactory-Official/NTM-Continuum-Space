package com.ntmspace.client;

import com.ntmspace.client.gui.machine.GUIMachineStardar;
import com.ntmspace.client.render.machine.MachineRenderersD;
import com.ntmspace.registry.SpaceMachinesD;
import dev.golem.core.client.ClientRegistrations;

/** The progression machines' client half, part D: the StarDar's screen and the StarDar and console renderers. */
public final class MachinesClientD {

    private MachinesClientD() {}

    public static void init() {
        ClientRegistrations.screens(screens -> screens.register(SpaceMachinesD.STARDAR_MENU.get(), GUIMachineStardar::new));
        ClientRegistrations.renderers(MachineRenderersD::register);
    }
}
