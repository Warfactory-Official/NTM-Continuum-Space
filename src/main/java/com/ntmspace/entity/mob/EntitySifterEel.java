package com.ntmspace.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * hbm-space {@code EntitySifterEel} (swim speed 1.8, watches players 8 blocks away), with one of six skins. 1.7 rolled
 * the skin in the constructor from the world's random and neither saved nor synced it, so every client (and every
 * reload) rolls its own: a quirk ported as found, see {@link EntityDepthSquid}. It drops nothing (1.7 had no drop item).
 */
public class EntitySifterEel extends EntityFish implements IEntityEnumMulti {

    public enum SifterEel {
        PLAIN,
        FAST,
        EXOTIC,
        PHASED,
        ELEMENTAL,
        PERFECT,
    }

    public final SifterEel type;

    public EntitySifterEel(EntityType<? extends EntitySifterEel> type, Level level) {
        super(type, level, 1.8, 8.0F);
        this.type = SifterEel.values()[level.getRandom().nextInt(SifterEel.values().length)];
    }

    @Override
    public Enum<?> getEnum() {
        return this.type;
    }
}
