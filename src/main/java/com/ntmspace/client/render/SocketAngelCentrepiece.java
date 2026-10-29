package com.ntmspace.client.render;

import com.hbm.client.render.SocketCentrepieces;
import com.ntmspace.client.render.mob.SpaceMobModels;
import com.ntmspace.registry.SpaceMobs;
import dev.golem.core.client.ClientRegistrations;

/**
 * Space RenderBatterySocket's angel body, turning above the socket with its charged corner arcs.
 */
public final class SocketAngelCentrepiece {
    private SocketAngelCentrepiece() {
    }

    public static void init() {
        ClientRegistrations.renderers(registrations -> SocketCentrepieces.register(SpaceMobs.CORE_ANGEL.get(),
                new SocketCentrepieces.Centrepiece(SpaceMobModels::bfangel, "body", SpaceMobModels.BFANGEL, 2F, 5F)));
    }
}
