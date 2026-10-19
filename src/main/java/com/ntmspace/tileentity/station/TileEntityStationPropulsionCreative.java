package com.ntmspace.tileentity.station;

import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.Propulsion;
import com.ntmspace.registry.StationBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code TileEntityStationPropulsionCreative}: always able to burn, 10 MN of thrust, one second to start and
 * to stop. Registers on its first server tick in orbit, unregisters when removed or unloaded.
 */
public class TileEntityStationPropulsionCreative extends BlockEntity implements Propulsion {

    private boolean hasRegistered = false;

    public TileEntityStationPropulsionCreative(BlockPos pos, BlockState state) {
        super(StationBlocks.PROPULSION_CREATIVE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityStationPropulsionCreative te) {
        if (!Orbit.inOrbit(level)) return;
        if (!te.hasRegistered) {
            te.registerPropulsion();
            te.hasRegistered = true;
        }
    }

    /** 1.7 {@code invalidate} and {@code onChunkUnload} both unregister; a modern block entity is removed for both. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (hasRegistered) {
            unregisterPropulsion();
            hasRegistered = false;
        }
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public boolean canPerformBurn(int shipMass, double deltaV) {
        return true;
    }

    @Override
    public void addErrors(List<Component> errors) {}

    @Override
    public float getThrust() {
        return 10_000_000;
    }

    @Override
    public int startBurn() {
        return 20;
    }

    @Override
    public int endBurn() {
        return 20;
    }
}
