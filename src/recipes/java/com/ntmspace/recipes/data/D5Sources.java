package com.ntmspace.recipes.data;

import dev.golem.core.world.DataSource;
import java.util.List;

/** NTM: Space's fluid property files and its patches of HBM's fluids (hbm-space {@code Fluids.init}). */
public final class D5Sources {

    private D5Sources() {}

    public static List<DataSource<?>> sources() {
        return List.of(new SpaceFluidPropertySource(), new SpaceFluidPatchSource());
    }
}
