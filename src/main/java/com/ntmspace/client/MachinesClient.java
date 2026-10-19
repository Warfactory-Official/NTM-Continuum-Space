package com.ntmspace.client;

import com.ntmspace.client.gui.machine.GUIMachineCryoDistill;
import com.ntmspace.client.gui.machine.GUIMilkReformer;
import com.ntmspace.client.render.machine.MachineRenderers;
import com.ntmspace.registry.SpaceMachines;
import dev.golem.core.client.ClientRegistrations;

/** The progression machines' client half: their screens and renderers (part one here, B and C in their own classes). */
public final class MachinesClient {

    private MachinesClient() {}

    public static void init() {
        ClientRegistrations.screens(screens -> {
            screens.register(SpaceMachines.CRYO_DISTILL_MENU.get(), GUIMachineCryoDistill::new);
            screens.register(SpaceMachines.MILK_REFORMER_MENU.get(), GUIMilkReformer::new);
        });
        ClientRegistrations.renderers(MachineRenderers::register);
        MachinesClientB.init();
        MachinesClientC.init();
    }
}
