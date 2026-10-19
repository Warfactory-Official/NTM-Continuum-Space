package com.ntmspace.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.ntmspace.client.render.RenderGeysirElectric;
import com.ntmspace.client.render.RenderRubberCactus;
import com.ntmspace.registry.FloraBlocks;
import dev.golem.core.client.ClientRegistrations;

/**
 * The flora family's client half: the tall rubber plant's OBJ ({@link RenderRubberCactus}) and the discharge geyser's
 * flash ({@link RenderGeysirElectric}), bound through HBM's gem registration (the vanilla renderer and Golem's
 * instancing engine get the same renderer) at the loader's renderer moment.
 */
public final class FloraClient {

    private FloraClient() {}

    public static void init() {
        ClientRegistrations.renderers(renderers -> {
            HbmBlockEntityRenderers.regGem(renderers::blockEntity, FloraBlocks.RUBBER_TALL_BE.get(), RenderRubberCactus::new);
            HbmBlockEntityRenderers.regGem(renderers::blockEntity, FloraBlocks.GEYSIR_ELECTRIC_BE.get(), RenderGeysirElectric::new);
        });
    }
}
