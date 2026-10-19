package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.PressRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemData;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.machine.ItemStamp.StampType;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code PressRecipes.registerDefaults}: the rows hbm-space adds, each with the order of the HBM row it follows
 * (the stable sort places it right after). The GaAs circuit is NTM: Space's {@code circuit_gaas}; zinc's wire comes from
 * the {@code Mats.orderedList} loop (hbm-space's {@code MAT_ZINC} has the WIRE autogen). The id is the output's path.
 * Replaces the former {@code gen_recipe_changes_metal.py}'s press rows.
 */
public final class PressTable extends RecipeSource<Row> {

    public PressTable() {
        super(PressRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        makeRecipe(StampType.CIRCUIT, tag("c:billets/gaas"), item("ntmspace:circuit_gaas"), 1, after("hbm:press/circuit_silicon"));
        makeRecipe(StampType.PLATE, tag("c:ingots/nickel"), item("ntmspace:plate_nickel"), 1, after("hbm:press/plate_titanium"));
        makeRecipe(StampType.PLATE, tag("c:ingots/stainless"), item("ntmspace:plate_stainless"), 1, after("hbm:press/plate_steel"));
        makeRecipe(StampType.WIRE, tag("c:ingots/zinc"), item("ntmspace:wire_fine_zinc"), 8, after("hbm:press/wire_fine_zirconium")); // before carbon
    }

    /** hbm-space {@code makeRecipe(type, in, out)}. */
    private void makeRecipe(StampType type, AStack in, Item out, int count, int order) {
        register(BuiltInRegistries.ITEM.getKey(out).getPath(), new Row(type, in, new ItemSpec(out, count, ItemData.NONE), order));
    }

    /** The order of the HBM row this one follows: the stable sort places it right after (ties sort by id). */
    private int after(String recipeId) {
        return loaded(recipeId).order();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    private static AStack tag(String id) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.parse(id)));
    }
}
