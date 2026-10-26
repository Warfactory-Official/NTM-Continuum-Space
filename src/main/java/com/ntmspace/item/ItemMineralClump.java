package com.ntmspace.item;

import com.hbm.items.special.ItemCustomLore;

/** An acid-washed mineral fragment, with its acid kind attached to the registered item. */
public final class ItemMineralClump extends ItemCustomLore {

    public final Clump clump;

    public ItemMineralClump(Properties properties, Clump clump) {
        super(properties);
        this.clump = clump;
    }

    /** 1.7 ItemMineralOre metadata order, retained by the mineral fragment registrations. */
    public enum Clump {
        PEROXIDE, NITRIC, SULFURIC, SOLVENT, HYDROCHLORIC, SCHRABIDIC
    }
}
