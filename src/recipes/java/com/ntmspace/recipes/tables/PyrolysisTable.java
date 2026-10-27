package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code PyroOvenRecipes.registerDefaults}: the row hbm-space adds, {@code registerSFAuto(POLYTHYLENE)} (the mB
 * follow from polythylene's flammable energy, computed by HBM's {@code PyroOvenRecipes}). Replaces
 * the former {@code gen_recipe_changes_chem.py}'s pyrolysis row.
 */
public final class PyrolysisTable extends RecipeSource<Row> {

    public PyrolysisTable() {
        super(PyroOvenRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        registerSFAuto(SpaceFluids.POLYTHYLENE);
    }

    private void registerSFAuto(FluidType fluid) {
        Item solidFuel = BuiltInRegistries.ITEM.getValue(Ids.parse("hbm:solid_fuel"));
        registerSFAuto(fluid, 1_440_000L, solidFuel); //3200 burntime * 1.5 burntime bonus * 300 TU/t
    }

    /** hbm-space {@code registerSFAuto}, a 60-tick recipe. */
    private void registerSFAuto(FluidType fluid, long tuPerSF, Item fuel) {
        register("solid_fuel_" + fluid.getKey(), new Row(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(new ItemOut(fuel, 1)), 60, Optional.of(fluid), Optional.of(tuPerSF),
                loaded("hbm:pyrolysis/solid_fuel_balefire").order()));
    }
}
