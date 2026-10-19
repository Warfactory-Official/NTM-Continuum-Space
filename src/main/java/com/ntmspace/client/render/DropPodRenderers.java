package com.ntmspace.client.render;

import com.ntmspace.client.render.rocket.RenderCombatDropPod;
import com.ntmspace.registry.DropPodContent;
import dev.golem.core.client.ClientRegistrations;

/** The combat drop pod's renderers (hbm-space {@code ClientProxy}: the entity and the landed block). */
public final class DropPodRenderers {

    private DropPodRenderers() {}

    public static void init() {
        ClientRegistrations.renderers(r -> {
            r.entity(DropPodContent.COMBAT_POD.get(), RenderCombatDropPod::new);
            RenderCombatPod.register(r);
        });
    }
}
