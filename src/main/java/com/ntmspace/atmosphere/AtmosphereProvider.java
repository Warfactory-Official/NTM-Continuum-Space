package com.ntmspace.atmosphere;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * hbm-space's {@code IAtmosphereProvider}: something that keeps a sealed room ({@link AtmosphereBlob}) filled - the air
 * vent, the atmosphere machines. Registered with {@link AtmosphereHandler#registerAtmosphere}.
 */
public interface AtmosphereProvider {

    Level getLevel();

    /** The maximum distance an object can maintain a blob. */
    int getMaxBlobRadius();

    /** The position to use as root; if it can't be pathed to, the bubble is popped. */
    BlockPos getRootPosition();

    // Atmospheric information
    FluidType getFluidType();

    double getFluidPressure();

    void consume(int amount);

    void produce(int amount);

    void onBlobCreated(AtmosphereBlob blob);
}
