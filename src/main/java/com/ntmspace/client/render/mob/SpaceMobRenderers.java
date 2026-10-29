package com.ntmspace.client.render.mob;

import com.ntmspace.client.render.SocketAngelCentrepiece;
import com.ntmspace.registry.SpaceMobs;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.living.CompatCreeperRenderer;

/**
 * Renderer bindings for hbm-space's mobs, one line per type (hbm-space {@code ClientProxy.registerEntityRenderer}).
 * Every NTM: Space entity type must have one: a visible type without a renderer takes the render thread down.
 */
public final class SpaceMobRenderers {

    private SpaceMobRenderers() {
    }

    public static void init() {
        MobWeaponRenderers.init();
        SocketAngelCentrepiece.init();
        ClientRegistrations.renderers(r -> {
            r.entity(SpaceMobs.SIEGE_UFO.get(), RenderSiegeUFO::new);
            // 1.7 RenderCreeperUniversal(creeper_flesh.png, vanilla's creeper_armor.png): the vanilla charged overlay
            r.entity(SpaceMobs.CREEPER_FLESH.get(),
                    ctx -> new CompatCreeperRenderer(ctx, SpaceMobModels.CREEPER_FLESH, 1F));
            r.entity(SpaceMobs.BF_ANGEL.get(), RenderBFA::new);
            r.entity(SpaceMobs.MOON_COW.get(), RenderMoonCow::new);
            r.entity(SpaceMobs.SCUTTERFISH.get(), RenderLaytheFauna.Scutterfish::new);
            r.entity(SpaceMobs.TANKBOT.get(), RenderTankbot::new);
            r.entity(SpaceMobs.WAR_BEHEMOTH.get(), RenderBehemoth::new);
            r.entity(SpaceMobs.SCUTTLECRAB.get(), RenderLaytheFauna.Scuttlecrab::new);
            r.entity(SpaceMobs.DEPTH_SQUID.get(), RenderLaytheFauna.DepthSquid::new);
            r.entity(SpaceMobs.SCRAPFISH.get(), RenderLaytheFauna.ScrapFish::new);
            r.entity(SpaceMobs.SIFTER_EEL.get(), RenderLaytheFauna.SifterEel::new);
        });
    }
}
