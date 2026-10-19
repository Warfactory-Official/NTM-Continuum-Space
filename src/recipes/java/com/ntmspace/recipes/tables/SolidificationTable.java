package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.inventory.recipes.SolidificationRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * hbm-space {@code SolidificationRecipes.registerDefaults}: the rows hbm-space adds. Replaces
 * the former {@code gen_recipe_changes_chem.py}'s solidification rows.
 */
public final class SolidificationTable extends RecipeSource<Row> {

    public SolidificationTable() {
        super(SolidificationRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        registerRecipe(Fluids.BLOOD, 1_280, out("ntmspace:flesh_wafer", 8));
        registerRecipe(Fluids.CARBONDIOXIDE, 1_000, out("ntmspace:dry_ice", 1));
        registerRecipe(SpaceFluids.AQUEOUS_NICKEL, 500, out("ntmspace:nickel_salts", 1));
        registerRecipe(SpaceFluids.VINYL, 1_000, out("hbm:ingot_pvc", 1));
    }

    /** hbm-space {@code registerRecipe(type, quantity, output)}; the id is the fluid. */
    private void registerRecipe(FluidType type, int quantity, ItemOut output) {
        register(type.getKey(), new Row(type, Optional.of(quantity), Optional.empty(), output));
    }

    private static ItemOut out(String id, int count) {
        return new ItemOut(BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id)), count);
    }
}
