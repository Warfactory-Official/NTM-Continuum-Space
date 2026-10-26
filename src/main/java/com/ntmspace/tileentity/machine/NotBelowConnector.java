package com.ntmspace.tileentity.machine;

import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;

/** The alkylator, cryo distiller and milk reformer share their 1.7 power/fluid refusal below the machine. */
public interface NotBelowConnector extends IEnergyConnectorMK2, IFluidConnectorMK2 {
    @Override
    default boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && dir != ForgeDirection.DOWN;
    }

    @Override
    default boolean canConnect(FluidType type, ForgeDirection dir) {
        return canConnect(dir);
    }
}
