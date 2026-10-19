package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.inventory.recipes.CrackingRecipes.Row;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.world.RecipeSource;

/**
 * hbm-space {@code CrackingRecipes.registerDefaults}: the rows hbm-space adds (the GAS row it commented out is a removal
 * in {@code recipe_removals/cracking.json}). Replaces the former {@code gen_recipe_changes_chem.py}'s cracking rows.
 */
public final class CrackingTable extends RecipeSource<Row> {

    public CrackingTable() {
        super(CrackingRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        // cracking.put(Fluids.GAS, ...) is commented out in hbm-space
        remove("hbm:cracking/gas");

        put(Fluids.OIL_DS,              new FluidStack(Fluids.CRACKOIL_DS,      80),    new FluidStack(Fluids.PETROLEUM,    20));
        put(Fluids.DIESEL_REFORM,       new FluidStack(Fluids.KEROSENE_REFORM,  40),    new FluidStack(Fluids.PETROLEUM,    30));
        put(Fluids.DIESEL_CRACK_REFORM, new FluidStack(Fluids.KEROSENE_REFORM,  40),    new FluidStack(Fluids.PETROLEUM,    30));
        put(SpaceFluids.CHLOROETHANE,   new FluidStack(SpaceFluids.POLYTHYLENE, 50),    new FluidStack(Fluids.CHLORINE,     30));
        put(SpaceFluids.ELBOWGREASE,    new FluidStack(Fluids.MERCURY,          70),    new FluidStack(Fluids.SOURGAS,      50));
        put(SpaceFluids.CBENZ,          new FluidStack(Fluids.AROMATICS,        50),    new FluidStack(Fluids.CHLORINE,     30));
    }

    /** hbm-space {@code cracking.put(input, new Pair(left, right))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack left, FluidStack right) {
        register(input.getKey(), new Row(input, left, right));
    }
}
