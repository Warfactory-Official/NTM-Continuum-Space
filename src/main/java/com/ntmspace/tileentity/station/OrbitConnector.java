package com.ntmspace.tileentity.station;

import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.orbit.Orbit;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Orbital engine inputs refuse contact wherever their server tick cannot subscribe to a network. */
public interface OrbitConnector extends IEnergyConnectorMK2, IFluidConnectorMK2 {
    @Override
    default boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && Orbit.inOrbit(((BlockEntity) this).getLevel());
    }

    @Override
    default boolean canConnect(FluidType type, ForgeDirection dir) {
        return canConnect(dir);
    }
}
