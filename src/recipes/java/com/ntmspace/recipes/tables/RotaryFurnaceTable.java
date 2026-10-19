package com.ntmspace.recipes.tables;

import static com.hbm.inventory.material.MaterialShapes.INGOT;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes.Row;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

/**
 * hbm-space {@code RotaryFurnaceRecipes.registerDefaults}: the one row hbm-space changes, gunmetal from copper and zinc
 * (was aluminium). It keeps HBM's row's id and order and replaces it through
 * {@code recipe_removals/rotary_furnace.json}. Replaces the former {@code gen_recipe_changes_metal.py}'s rotary_furnace row.
 */
public final class RotaryFurnaceTable extends RecipeSource<Row> {

    public RotaryFurnaceTable() {
        super(RotaryFurnaceRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        // recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_GUNMETAL, INGOT.q(4)), 200, 100, CU.ingot(3), ZI.ingot(1)));
        remove("hbm:rotary_furnace/gunmetal");
        register("gunmetal", new Row(List.of(tag("c:ingots/copper", 3), tag("c:ingots/zinc", 1)), Optional.empty(),
                new MaterialStack(Mats.matByName.get("gunmetal"), INGOT.quantity * 4), 200, 100, 6));
    }

    private static AStack tag(String id, int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.parse(id)), count);
    }
}
