package com.ntmspace.fork.antimatter;

import com.hbm.entity.logic.EntityBalefire;
import com.hbm.explosion.ExplosionBalefire;
import com.ntmspace.fork.ForkContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The antimatter balefire's entity identity, sharing the balefire driver and Space's crater variant. */
public class EntityAntimatterBlast extends EntityBalefire {

    public EntityAntimatterBlast(EntityType<? extends EntityAntimatterBlast> type, Level level) {
        super(type, level);
        variant = AntimatterBalefire.ID;
    }

    public static EntityAntimatterBlast spawn(Level level, double x, double y, double z, int range) {
        var blast = new EntityAntimatterBlast(ForkContent.ANTIMATTER_BLAST.get(), level);
        blast.setPos(x, y, z);
        blast.destructionRange = range;
        level.addFreshEntity(blast);
        return blast;
    }

    /** The first nine entity ticks carry the shielded radiation flash. */
    public void flash() {
        if (level() instanceof ServerLevel server && tickCount < 10) {
            AntimatterBalefire.radiate(server, this, 40000F * destructionRange, destructionRange * 2);
        }
    }

    @Override
    protected ExplosionBalefire createExplosion() {
        return new ExplosionAntimatter((int) getX(), (int) getY(), (int) getZ(), level(), destructionRange);
    }
}
