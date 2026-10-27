package com.ntmspace.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** hbm-space {@code EntityScutterfish}: Laythe's common fish (swim speed 1.5, watches players 6 blocks away). */
public class EntityScutterfish extends EntityFish {

    public EntityScutterfish(EntityType<? extends EntityScutterfish> type, Level level) {
        super(type, level, 1.5, 6.0F);
    }

}
