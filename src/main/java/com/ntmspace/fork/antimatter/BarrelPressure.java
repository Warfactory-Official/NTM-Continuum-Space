package com.ntmspace.fork.antimatter;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Space's fluid-specific barrel rupture, using the existing antimatter and suppression-aware Fleija blasts. */
final class BarrelPressure implements TileEntityBarrel.PressureBlast {

    @Override
    public boolean accepts(FluidType fluid, int power) {
        return power > 0 && (fluid == Fluids.AMAT || fluid == Fluids.ASCHRAB);
    }

    @Override
    public void explode(ServerLevel level, BlockPos pos, FluidType fluid, int power) {
        if (fluid == Fluids.AMAT) {
            if (power >= 25) {
                AntimatterBalefire.spawn(level, pos.getX(), pos.getY(), pos.getZ(), power);
                AntimatterBalefire.cloud(level, pos.getX(), pos.getY(), pos.getZ(), power * 1.5F);
            } else {
                new ExplosionVNT(level, pos.getX(), pos.getY(), pos.getZ(), power).makeAmat().explode();
            }
        } else {
            var blast = EntityNukeExplosionMK3.statFacFleija(level, pos.getX() + .5D, pos.getY() + .5D,
                    pos.getZ() + .5D, power);
            if (!blast.isRemoved()) {
                level.addFreshEntity(blast);
                level.addFreshEntity(EntityCloudFleija.statFac(level, power, pos.getX() + .5D,
                        pos.getY() + .5D, pos.getZ() + .5D));
            }
        }
    }
}
