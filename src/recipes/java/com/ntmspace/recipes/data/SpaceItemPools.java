package com.ntmspace.recipes.data;

import com.google.gson.JsonElement;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPool.PoolFile;
import com.hbm.itempool.ItemPool.Row;
import dev.golem.core.world.DataSource;
import java.util.List;
import net.minecraft.core.HolderLookup;

/**
 * hbm-space {@code ItemPoolsComponent.init()}: the pools NTM: Space adds and its additions to HBM's, written as
 * {@code data/ntmspace/item_pool/<pool>.json} for HBM's {@link ItemPool} (a pool file adds its entries to the pool of that
 * name in lower-priority packs). A 1.7 (item, metadata) pair is the port's split item id.
 */
public final class SpaceItemPools extends DataSource<PoolFile> {

    public SpaceItemPools() {
        super(ItemPool.FOLDER);
    }

    @Override
    public JsonElement encode(PoolFile row, HolderLookup.Provider registries) {
        return row.write();
    }

    @Override
    public PoolFile decode(JsonElement json, HolderLookup.Provider registries) {
        return PoolFile.read(json.getAsJsonObject());
    }

    @Override
    protected void registerDefaults() {
        // POOL_OFFICE_TRASH: the two lines hbm-space adds to 1.7's pool (after ModItems.coffee)
        pool("pool_office_trash",
                weighted("ntmspace:bean_raw", 1, 4, 4),
                weighted("ntmspace:teaseeds", 1, 4, 3));

        // POOL_SUBMARINE: submarines found sunk on Laythe
        pool("pool_submarine",
                weighted("hbm:nugget_u235", 3, 12, 5),
                weighted("hbm:nugget_pu238", 3, 12, 5),
                weighted("hbm:nugget_ra226", 3, 6, 5),
                weighted("ntmspace:oxy_pinwheel", 1, 1, 1),
                weighted("hbm:gas_mask_m65", 1, 1, 2),
                weighted("hbm:gas_mask_mono", 1, 1, 2),
                weighted("hbm:gas_mask_filter", 1, 1, 2),
                weighted("hbm:powder_iodine", 1, 1, 1),
                weighted("hbm:powder_bromine", 1, 2, 1),
                weighted("hbm:hazmat_kit", 1, 1, 2),
                weighted("hbm:gun_carbine", 1, 1, 2),
                weighted("hbm:ammo_standard_r762_fmj", 8, 32, 4), // ammo_standard, EnumAmmo.R762_FMJ
                weighted("hbm:yellow_barrel", 1, 3, 3),
                weighted("hbm:powder_neptunium", 1, 1, 1),
                weighted("hbm:pwr_fuel_meu", 1, 3, 3), // pwr_fuel, EnumPWRFuel.MEU
                weighted("hbm:pwr_fuel_mep", 1, 3, 3), // pwr_fuel, EnumPWRFuel.MEP
                weighted("hbm:pwr_fuel_heu235", 1, 3, 4), // pwr_fuel, EnumPWRFuel.HEU235
                weighted("minecraft:paper", 1, 6, 5),
                weighted("minecraft:clock", 1, 1, 3),
                weighted("minecraft:compass", 1, 1, 3),
                weighted("minecraft:book", 1, 5, 4));

        // POOL_MACHINE_PARTSTHAT
        pool("pool_machine_partsthat",
                weighted("hbm:nugget_beryllium", 3, 12, 5),
                weighted("ntmspace:circuit_aero", 3, 12, 5), // circuit, EnumCircuitType.AERO
                weighted("hbm:circuit_pcb", 3, 6, 5), // circuit, EnumCircuitType.PCB
                weighted("hbm:plate_steel", 1, 1, 1),
                weighted("hbm:powder_steel", 1, 1, 2));
    }

    /** 1.7 {@code new ItemPool(name) {{ this.pool = ... }}}: the file is the pool name lower-cased. */
    private void pool(String name, Row... rows) {
        register(name, new PoolFile(false, List.of(rows)));
    }

    /** 1.7 {@code weighted(item, 0, min, max, weight)}. */
    private static Row weighted(String item, int min, int max, int weight) {
        return new Row(item, min, max, weight, null, null);
    }
}
