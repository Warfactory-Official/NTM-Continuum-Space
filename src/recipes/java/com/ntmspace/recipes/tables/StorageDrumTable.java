package com.ntmspace.recipes.tables;

import com.hbm.inventory.recipes.StorageDrumRecipes;
import com.hbm.inventory.recipes.StorageDrumRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code TileEntityStorageDrum.updateEntity}, the short-waste decay rules hbm-space changes. Short waste is
 * looked up with {@code ItemWasteLong.rectify(meta)} (meta % 5, the long family's size), as in 1.7. hbm-space inserted
 * {@code AMERICIUM241} at meta 7 of {@code ItemWasteShort.WasteClass} and appended {@code BERKELIUM247}, {@code CURIUM244}
 * and {@code CURIUM245}, so metas 0..6 keep HBM's rows and metas 7..11 are rows here: the four new classes (NTM: Space's
 * items) and schrabidium, whose meta moved from 7 to 8 and so now releases Pu-239's liquid and gas instead of Np-237's
 * (its HBM rows are removed by {@code recipe_removals/storage_drum.json}). The id is the input item.
 */
public final class StorageDrumTable extends RecipeSource<Row> {

    /** hbm-space {@code ItemWasteShort.WasteClass}, in declaration order. */
    private static final String[] SHORT = {"uranium235", "uranium233", "neptunium", "plutonium239", "plutonium240",
            "plutonium241", "americium242", "americium241", "schrabidium", "berkelium247", "curium244", "curium245"};
    /** Their {@code liquid} and {@code gas}, as far as {@code rectify} reaches (meta % 5). */
    private static final int[] LIQUID = {0, 50, 150, 250, 350};
    private static final int[] GAS = {100, 100, 500, 1000, 1000};

    public StorageDrumTable() {
        super(StorageDrumRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        // schrabidium moved from meta 7 to 8: HBM's rows are replaced by the meta-8 rows below
        remove("hbm:storage_drum/nuclear_waste_short_schrabidium");
        remove("hbm:storage_drum/nuclear_waste_short_tiny_schrabidium");

        for (int meta = 7; meta < SHORT.length; meta++) {
            int wasteClass = rectify(meta);
            put(waste("nuclear_waste_short", meta), waste("nuclear_waste_short_depleted", meta),
                    "short", 1, LIQUID[wasteClass], GAS[wasteClass]);
        }

        for (int meta = 7; meta < SHORT.length; meta++) {
            int wasteClass = rectify(meta);
            put(waste("nuclear_waste_short_tiny", meta), waste("nuclear_waste_short_depleted_tiny", meta),
                    "short", 10, LIQUID[wasteClass] / 10, GAS[wasteClass] / 10);
        }
    }

    /** hbm-space {@code ItemWasteLong.rectify}: {@code abs(meta) % } the long waste class count (5). */
    private static int rectify(int meta) {
        return Math.abs(meta) % 5;
    }

    /** The item of a waste family at an hbm-space meta: HBM's for its classes, NTM: Space's for the four it added. */
    private static Item waste(String family, int meta) {
        String wasteClass = SHORT[meta];
        String ns = wasteClass.equals("schrabidium") ? "hbm" : "ntmspace";
        String id = ns + ":" + family + "_" + wasteClass;
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    /** One decay rule; the id is the input item. */
    private void put(Item input, Item output, String decay, int divisor, int liquid, int gas) {
        register(BuiltInRegistries.ITEM.getKey(input).getPath(),
                new Row(new ItemOut(input, 1), new ItemOut(output, 1), decay, divisor, liquid, gas));
    }
}
