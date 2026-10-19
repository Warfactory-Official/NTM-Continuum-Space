package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.inventory.recipes.FractionRecipes.Row;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.world.RecipeSource;

/**
 * hbm-space {@code FractionRecipes.registerDefaults}: the rows hbm-space adds. Replaces
 * the former {@code gen_recipe_changes_chem.py}'s fraction rows.
 */
public final class FractionTable extends RecipeSource<Row> {

    public FractionTable() {
        super(FractionRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        put(SpaceFluids.CONGLOMERA,     new FluidStack(SpaceFluids.BRINE,           25),    new FluidStack(SpaceFluids.AQUEOUS_NICKEL,  75));
        put(SpaceFluids.HGAS,           new FluidStack(SpaceFluids.CHLOROMETHANE,   25),    new FluidStack(Fluids.CHLORINE,             85));
        put(SpaceFluids.HALOLIGHT,      new FluidStack(Fluids.PHOSGENE,             35),    new FluidStack(SpaceFluids.CHLOROETHANE,    75));
    }

    /** hbm-space {@code fractions.put(input, new Pair(left, right))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack left, FluidStack right) {
        register(input.getKey(), new Row(input, left, right));
    }
}
