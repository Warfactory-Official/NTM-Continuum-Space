package com.ntmspace.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.ntmspace.client.gui.GUITransporterLinker;
import com.ntmspace.client.gui.GUITransporterRocket;
import com.ntmspace.client.render.transporter.RenderTransporterRocket;
import com.ntmspace.item.ItemTransporterLinker;
import com.ntmspace.registry.TransporterBlocks;
import dev.golem.core.client.ClientRegistrations;
import dev.golem.core.render.CompatBERProvider;
import dev.golem.core.render.CompatRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The cargo transporter's client side: the pad's screen and renderer, the linker's screen (opened by the item's
 * client-side use) and the linker's transporter list from the server (1.7 {@code TransporterLinkerPacket}).
 */
public final class TransporterClient {

    private TransporterClient() {}

    public static void init() {
        ClientRegistrations.screens(screens -> screens.register(TransporterBlocks.TRANSPORTER_ROCKET_MENU.get(), GUITransporterRocket::new));
        ClientRegistrations.renderers(renderers -> {
            HbmBlockEntityRenderers.Sink sink = new HbmBlockEntityRenderers.Sink() {
                @Override
                public <T extends BlockEntity, S extends CompatRenderState> void register(BlockEntityType<? extends T> type,
                                                                                             CompatBERProvider<T, S> provider) {
                    renderers.blockEntity(type, provider);
                }
            };
            HbmBlockEntityRenderers.regGem(sink, TransporterBlocks.TRANSPORTER_ROCKET_BE.get(), RenderTransporterRocket::new);
        });
        ItemTransporterLinker.registerClientPayload();
        ItemTransporterLinker.setScreenOpener(GUITransporterLinker::open);
    }
}
