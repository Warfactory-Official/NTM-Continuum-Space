package com.ntmspace.client;

import com.hbm.client.render.BarrelRenderer;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.item.CustomItemRenderers;
import com.ntmspace.block.generic.BrownBarrel;
import com.ntmspace.client.render.RenderAlgaeFilm;
import com.ntmspace.client.render.RenderOrrery;
import com.ntmspace.client.render.RenderSpacePlushie;
import com.ntmspace.registry.LeftoverContent;
import dev.golem.core.client.ClientRegistrations;

/**
 * The leftover blocks' client half: the orrery, the algae film and the plushies through their own renderers, the oil
 * barrel through HBM's {@link BarrelRenderer} (its skin named by {@link BrownBarrel#barrelSkin}, HBM {@code BarrelSkin}), and the plushie items'
 * {@code ntmspace:plushie} item renderer.
 */
public final class LeftoverClient {

    private LeftoverClient() {}

    public static void init() {
        CustomItemRenderers.register("ntmspace:plushie", RenderSpacePlushie.Item::new);
        ClientRegistrations.renderers(renderers -> {
            HbmBlockEntityRenderers.regGem(renderers::blockEntity, LeftoverContent.ORRERY_BE.get(), RenderOrrery::new);
            HbmBlockEntityRenderers.regGem(renderers::blockEntity, LeftoverContent.ALGAE_FILM_BE.get(), RenderAlgaeFilm::new);
            HbmBlockEntityRenderers.regGem(renderers::blockEntity, LeftoverContent.PLUSHIE_BE.get(), RenderSpacePlushie::new);
            HbmBlockEntityRenderers.regGem(renderers::blockEntity, LeftoverContent.OIL_BARREL_BE.get(),
                    BarrelRenderer<BrownBarrel.Barrel>::new);
        });
    }
}
