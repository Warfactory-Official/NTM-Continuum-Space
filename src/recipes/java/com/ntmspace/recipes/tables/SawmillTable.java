package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.SawmillRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemData;
import com.hbm.inventory.recipes.loader.ItemSpec;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code TileEntitySawmill.getOutput}: the branches hbm-space puts before the generic ones, for its vinyl and
 * PVC wood, one row per branch, with {@code sawdustChance(result)} as the row's sawdust chance. The rows go before HBM's
 * ({@code order} below 0). The id is the input item's path. (hbm-space's NEI list, {@code getRecipes}, gives the PVC
 * planks PVC sticks; the machine, which this follows, gives vinyl sticks.)
 */
public final class SawmillTable extends RecipeSource<Row> {

    /** hbm-space {@code sawdustChance}'s zero cases among these results: the vinyl/PVC sticks, planks and logs. */
    private static final Set<String> NO_SAWDUST = Set.of("ntmspace:stick_pvc", "ntmspace:stick_vinyl", "ntmspace:vinyl_planks",
            "ntmspace:pvc_planks", "ntmspace:vinyl_log", "ntmspace:pvc_log");

    public SawmillTable() {
        super(SawmillRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        // getOutput's branches in hbm-space's order, -6 (checked first) to -1
        put(-6, "ntmspace:stick_vinyl", "hbm:dust", 1);
        put(-5, "ntmspace:stick_pvc", "hbm:dust", 1);
        put(-4, "ntmspace:vinyl_planks", "ntmspace:stick_vinyl", 6);
        put(-3, "ntmspace:pvc_planks", "ntmspace:stick_vinyl", 6);
        put(-2, "ntmspace:vinyl_log", "ntmspace:vinyl_planks", 4 * 6 / 4);
        put(-1, "ntmspace:pvc_log", "ntmspace:pvc_planks", 4 * 6 / 4);
    }

    /** One {@code getOutput} branch. */
    private void put(int order, String input, String output, int count) {
        Item in = item(input);
        register(BuiltInRegistries.ITEM.getKey(in).getPath(), new Row(new ComparableStack(in),
                new ItemSpec(item(output), count, ItemData.NONE), Optional.empty(), sawdustChance(output), false, order));
    }

    /** hbm-space {@code sawdustChance(result)} for these results (none is sawdust or a vanilla stick). */
    private static float sawdustChance(String result) {
        return NO_SAWDUST.contains(result) ? 0F : 0.5F;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }
}
