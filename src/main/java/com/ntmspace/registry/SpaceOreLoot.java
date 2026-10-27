package com.ntmspace.registry;

import com.hbm.blocks.RawOres;
import dev.golem.core.registry.Loot;

/** The ordinary HBM australium host shares its planetary variants' configured raw acquisition form. */
public final class SpaceOreLoot {

    private SpaceOreLoot() {}

    public static void declare() {
        Loot.replace("hbm", "ore_australium", RawOres.drop("ntmspace:raw_australium", Loot.self()));
    }
}
