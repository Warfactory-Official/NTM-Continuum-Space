package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.SawmillRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemData;
import com.hbm.inventory.recipes.loader.ItemSpec;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Space's six polymer sawing overrides, ordered before HBM's generic wood tags. PVC planks produce PVC sticks,
 * and these inputs never produce sawdust, including polymer sticks sawn into dust.
 */
public final class SawmillTable extends RecipeSource<Row> {

    public SawmillTable() {
        super(SawmillRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        // getOutput's branches in hbm-space's order, -6 (checked first) to -1
        put(-6, "ntmspace:stick_vinyl", "hbm:dust", 1);
        put(-5, "ntmspace:stick_pvc", "hbm:dust", 1);
        put(-4, "ntmspace:vinyl_planks", "ntmspace:stick_vinyl", 6);
        put(-3, "ntmspace:pvc_planks", "ntmspace:stick_pvc", 6);
        put(-2, "ntmspace:vinyl_log", "ntmspace:vinyl_planks", 4 * 6 / 4);
        put(-1, "ntmspace:pvc_log", "ntmspace:pvc_planks", 4 * 6 / 4);
    }

    /** One {@code getOutput} branch. */
    private void put(int order, String input, String output, int count) {
        Item in = item(input);
        register(BuiltInRegistries.ITEM.getKey(in).getPath(), new Row(new ComparableStack(in),
                new ItemSpec(item(output), count, ItemData.NONE), Optional.empty(), 0F, false, order));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }
}
