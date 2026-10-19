package com.ntmspace.client;

import com.ntmspace.client.gui.machine.GUIHydroponic;
import com.ntmspace.client.gui.machine.GUIVacuumCircuit;
import com.ntmspace.client.render.machine.MachineRenderersB;
import com.ntmspace.registry.SpaceMachinesB;
import dev.golem.core.client.ClientRegistrations;

/** The progression machines' client half, part B (screens and renderers of the machines in {@code SpaceMachinesB}). */
public final class MachinesClientB {

    private MachinesClientB() {}

    public static void init() {
        ClientRegistrations.screens(screens -> {
            screens.register(SpaceMachinesB.HYDROBAY_MENU.get(), GUIHydroponic::new);
            screens.register(SpaceMachinesB.VACUUM_CIRCUIT_MENU.get(), GUIVacuumCircuit::new);
        });
        ClientRegistrations.renderers(MachineRenderersB::register);
    }
}
