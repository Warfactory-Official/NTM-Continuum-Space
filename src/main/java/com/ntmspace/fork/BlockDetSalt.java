package com.ntmspace.fork;

import com.hbm.blocks.bomb.ExplosiveCharge;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code ExplosiveCharge(det_salt)}: det_nuke's blast, its fallout salted. */
public class BlockDetSalt extends ExplosiveCharge {

    public BlockDetSalt(Properties properties) {
        super(properties, Kind.NUKE);
    }

    @Override
    public IBomb.BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return IBomb.BombReturnCode.DETONATED;
        level.removeBlock(pos, false);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        // 1.7 statFacSalted sets a flag its fallout rain never receives; fixed to salt the rain.
        EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.statFac(level, BombConfig.missileRadius, x, y, z)
                .falloutLayer(ForkContent.SALTED_FALLOUT.get());
        if (detonator != null) mk5.detonator = detonator.getUUID();
        if (level.addFreshEntity(mk5)) mk5.startChunkLoading();
        EntityNukeTorex torex = EntityNukeTorex.statFac(level, x, y, z, BombConfig.missileRadius);
        if (level.addFreshEntity(torex)) torex.startChunkLoading();
        return IBomb.BombReturnCode.DETONATED;
    }
}
