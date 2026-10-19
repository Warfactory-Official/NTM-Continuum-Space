package com.ntmspace.client;

import com.ntmspace.registry.StructureBlocks;
import dev.golem.core.render.ConnectedTextures;

/**
 * Client side of the planet structures' blocks: {@code deco_stainless} is a 1.7 {@code BlockDecoCT}, its faces drawn in
 * quarters from {@code deco_stainless_ct} by Golem Core's connected textures, joining only itself (the
 * {@code IBlockCT} default, as HBM's deco metals in {@code ConnectedTextureRegistrations}).
 */
public final class StructureClient {

    private StructureClient() {}

    public static void init() {
        String[] sheets = {"ntmspace:blocks/deco_stainless", "ntmspace:blocks/deco_stainless_ct"};
        ConnectedTextures.register(StructureBlocks.DECO_STAINLESS::get, state -> sheets,
                (level, pos, self, neighbour) -> neighbour.getBlock() == self.getBlock());
    }
}
