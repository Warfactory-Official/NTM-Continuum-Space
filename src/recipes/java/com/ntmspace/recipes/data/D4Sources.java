package com.ntmspace.recipes.data;

import dev.golem.core.world.DataSource;
import java.util.List;

/** Data agent D4's sources: NTM: Space's removal lists, its patches of HBM's data maps and its item pools. */
public final class D4Sources {

    private D4Sources() {
    }

    public static List<DataSource<?>> sources() {
        return List.of(
                new SpaceRemovals(),
                new SpaceDeposits.Blocks(),
                new SpaceDeposits.States(),
                new SpaceFluidContainers.Colors(),
                new SpaceFluidContainers.Items(),
                new SpaceMachineValues.CustomNuke(),
                new SpaceMachineValues.DiFurnaceFuel(),
                new SpaceMachineValues.RadGenFuels(),
                new SpaceMachineValues.RTGPellets(),
                new SpaceMachineValues.Satellites(),
                new SpaceHazards(),
                new SpaceWatzPellets(),
                new SpaceItemPools());
    }
}
