package com.ntmspace.fork.antimatter;

import com.hbm.entity.effect.EntityNukeTorex;
import net.minecraft.world.level.Level;

/** The rainbow antimatter cloud, using the shared Torex's existing type-2 palette. */
public final class TorexAntimatter {

    public static final int TYPE = 2;

    private TorexAntimatter() {}

    public static EntityNukeTorex startFacAnti(Level level, double x, double y, double z, float scale) {
        return AntimatterBalefire.cloud(level, x, y, z, scale);
    }
}
