package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.SmithingRow;
import com.hbm.inventory.recipes.loader.ItemData;
import com.hbm.inventory.recipes.loader.ItemSpec;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

/**
 * hbm-space {@code AnvilRecipes.registerSmithing}: the one row hbm-space changes, gunmetal from copper and zinc (was
 * aluminium). It keeps HBM's row's 1.7 index and replaces it through {@code recipe_removals/anvil_smithing.json}.
 * Replaces the former {@code gen_recipe_changes_metal.py}'s smithing row.
 */
public final class AnvilSmithingTable extends RecipeSource<SmithingRow> {

    public AnvilSmithingTable() {
        super(AnvilRecipes.SMITHING.table);
    }

    @Override
    protected void registerDefaults() {
        // smithingRecipes.add(new AnvilSmithingRecipe(1, new ItemStack(ModItems.ingot_gunmetal, 1), CU.ingot(), ZI.ingot()));
        remove("hbm:anvil_smithing/ingot_gunmetal");
        register("ingot_gunmetal", new SmithingRow(1,
                new ItemSpec(BuiltInRegistries.ITEM.getValue(Ids.parse("hbm:ingot_gunmetal")), 1, ItemData.NONE),
                tag("c:ingots/copper"), tag("c:ingots/zinc"), false, false, 38));
    }

    private static TagStack tag(String id) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.parse(id)));
    }
}
