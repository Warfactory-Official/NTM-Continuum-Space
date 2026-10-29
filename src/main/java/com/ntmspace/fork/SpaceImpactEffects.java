package com.ntmspace.fork;

import com.hbm.saveddata.TomSaveData;
import dev.golem.core.world.Spawns;

/** Space's extinction rule applies to every impacted level. */
public final class SpaceImpactEffects {

    private SpaceImpactEffects() {
    }

    public static void init() {
        Spawns.onSpawnCheck((mob, level, reason) -> {
            if (!TomSaveData.impact(level)) return false;
            var water = switch (mob.getType().getCategory()) {
                case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> true;
                default -> false;
            };
            return mob.getBbHeight() >= 0.85F || mob.getBbWidth() >= 0.85F && !water && !mob.isBaby();
        });
    }
}
