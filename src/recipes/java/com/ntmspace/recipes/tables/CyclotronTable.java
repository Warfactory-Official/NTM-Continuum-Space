package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code CyclotronRecipes.register}: the row hbm-space adds, euphemium-irradiated balefire logs (no order: after
 * HBM's rows). The id is the output's path.
 */
public final class CyclotronTable extends RecipeSource<Row> {

    public CyclotronTable() {
        super(CyclotronRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        makeRecipe(new ComparableStack(item("hbm:block_euphemium")), new ComparableStack(item("ntmspace:bf_log")), item("ntmspace:eu_log"), 0);
    }

    /** hbm-space {@code makeRecipe(part, in, out, amat)}. */
    private void makeRecipe(ComparableStack part, ComparableStack in, Item out, int amat) {
        register(BuiltInRegistries.ITEM.getKey(out).getPath(), new Row(part.item, in, new ItemOut(out, 1), amat, Integer.MAX_VALUE));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }
}
