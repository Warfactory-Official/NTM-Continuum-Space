package com.ntmspace.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.ntmspace.client.gui.rocket.GUIOrbitalStationLauncher;
import com.ntmspace.client.render.station.StationLauncherRenderer;
import com.ntmspace.registry.StationLauncherBlocks;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The orbital station launcher's client start-up: its screen and renderer (1.7 {@code ClientProxy}). */
public final class StationLauncherClient {

    private StationLauncherClient() {}

    public static void init() {
        ClientRegistrations.screens(screens ->
                screens.register(StationLauncherBlocks.ORBITAL_STATION_LAUNCHER_MENU.get(), GUIOrbitalStationLauncher::new));
        ClientRegistrations.renderers(renderers -> {
            HbmBlockEntityRenderers.Sink sink = new HbmBlockEntityRenderers.Sink() {
                @Override
                public <T extends BlockEntity, S extends CompatRenderState> void register(BlockEntityType<? extends T> type,
                                                                                             CompatBERProvider<T, S> provider) {
                    renderers.blockEntity(type, provider);
                }
            };
            HbmBlockEntityRenderers.regGem(sink, StationLauncherBlocks.ORBITAL_STATION_LAUNCHER_BE.get(), StationLauncherRenderer::new);
        });
    }
}
