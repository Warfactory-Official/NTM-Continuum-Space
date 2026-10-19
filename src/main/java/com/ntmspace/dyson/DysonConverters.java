package com.ntmspace.dyson;

import com.hbm.tileentity.machine.storage.TileEntityCrateTungsten;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The {@link DysonConverter} of a block entity: its own, or the one hbm-space added to a base-HBM block.
 *
 * <p>hbm-space made HBM's tungsten crate an {@code IDysonConverter}: a beam that reaches it heats it like a laser,
 * {@code addEnergy(energy / 5000, NORTH)}, at any face, and never detonates. The crate is HBM's class, so the addon
 * answers for it here instead of changing it.
 */
public final class DysonConverters {

    private DysonConverters() {}

    public static @Nullable DysonConverter of(@Nullable BlockEntity te) {
        if (te instanceof DysonConverter converter) return converter;
        if (te instanceof TileEntityCrateTungsten crate) {
            return new DysonConverter() {
                @Override
                public boolean provideEnergy(int x, int y, int z, long energy) {
                    if (crate.getLevel() != null) crate.addEnergy(crate.getLevel(), new BlockPos(x, y, z), energy / 5000, Direction.NORTH);
                    return true;
                }

                @Override
                public long maximumEnergy() {
                    return Long.MAX_VALUE;
                }
            };
        }
        return null;
    }
}
