package com.ntmspace.client;

import com.ntmspace.client.render.machine.MachineRenderersC;
import dev.golem.core.client.ClientRegistrations;

/**
 * The progression machines' client half, part C: the renderers of the machines in {@code SpaceMachinesC} (none of them
 * has a screen in 1.7).
 */
public final class MachinesClientC {

    private MachinesClientC() {}

    public static void init() {
        ClientRegistrations.renderers(MachineRenderersC::register);
    }
}
