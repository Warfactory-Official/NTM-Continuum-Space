package com.ntmspace.client;

import dev.golem.core.client.Translations;
import java.util.List;

/** hbm-space's names for HBM blocks and items, read from this mod's lang files as {@code ntmspace.rename.<key>}. */
public final class SpaceNames {

    private static final List<String> KEYS = List.of("block.hbm.fissure_bomb", "block.hbm.moon_turf",
            "block.hbm.refueler", "block.hbm.machine_lpw2", "item.hbm.upgrade_speed_1");

    private SpaceNames() {}

    static void init() {
        for (String key : KEYS) Translations.redirect(key, "ntmspace.rename." + key);
    }
}
