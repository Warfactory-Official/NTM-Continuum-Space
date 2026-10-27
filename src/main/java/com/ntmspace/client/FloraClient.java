package com.ntmspace.client;

import com.ntmspace.block.terrain.BlockLaytheSilt;
import com.ntmspace.registry.LaytheTektoBlocks;
import dev.golem.core.client.BlockTint;
import dev.golem.core.client.Tints;
import dev.golem.core.render.ConnectedTextures;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
        Tints.registerBlockTint(new BlockTint() {
            @Override
            public int color(BlockState state) {
                return BlockLaytheSilt.color(state);
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return Set.of(BlockLaytheSilt.SHADE);
            }
        }, LaytheTektoBlocks.LAYTHE_SILT);
        String[] rootSheets = {"ntmspace:blocks/laythe_root", "ntmspace:blocks/laythe_root_ct"};
        ConnectedTextures.register(LaytheTektoBlocks.LAYTHE_ROOT::get,
                state -> rootSheets, (level, pos, self, neighbour) -> neighbour.getBlock() == self.getBlock());
        ClientRegistrations.renderers(renderers -> {
            HbmBlockEntityRenderers.regGem(renderers::blockEntity, FloraBlocks.RUBBER_TALL_BE.get(), RenderRubberCactus::new);
            HbmBlockEntityRenderers.regGem(renderers::blockEntity, FloraBlocks.GEYSIR_ELECTRIC_BE.get(), RenderGeysirElectric::new);
        });
    }
}
