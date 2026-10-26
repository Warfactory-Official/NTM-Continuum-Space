package com.ntmspace.fork.antimatter;

import com.hbm.explosion.ExplosionBalefire;
import net.minecraft.world.level.Level;

/** An antimatter crater, reusing the shared spiral and Space's registered block behavior. */
public class ExplosionAntimatter extends ExplosionBalefire {

    public ExplosionAntimatter(int x, int y, int z, Level level, int range) {
        super(x, y, z, level, range);
        variant = AntimatterBalefire.CRATER;
    }
}
