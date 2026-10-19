package com.ntmspace.recipes.tables;

import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.inventory.recipes.MagicRecipes.Row;
import com.mojang.datafixers.util.Either;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code MagicRecipes.register}: the row hbm-space adds, Gwen's bobblehead (1.7 bobblehead meta 21, BobbleType
 * GWEN; NTM: Space's {@code bobblehead_gwen}), after the diamond gavel. The id is the result's path. Replaces
 * the former {@code gen_recipe_changes_nuclear.py}'s {@code gen_magic}.
 */
public final class MagicTable extends RecipeSource<Row> {

    public MagicTable() {
        super(MagicRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        add(item("ntmspace:bobblehead_gwen"), 7, comp(item("ntmspace:ingot_gwenium")), comp(item("hbm:ingot_dineutronium")), comp(item("hbm:ingot_dineutronium")));
    }

    /** hbm-space {@code recipes.add(new MagicRecipe(out, in...))}. */
    @SafeVarargs
    private void add(Item out, int order, Either<TagKey<Item>, Item>... in) {
        register(BuiltInRegistries.ITEM.getKey(out).getPath(), new Row(List.of(in), out, order));
    }

    /** hbm-space {@code new ComparableStack(item)}. */
    private static Either<TagKey<Item>, Item> comp(Item item) {
        return Either.right(item);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }
}
