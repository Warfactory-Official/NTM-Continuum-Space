package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.inventory.recipes.HydrotreatingRecipes.Row;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.world.RecipeSource;
import java.util.List;

/**
 * hbm-space {@code HydrotreatingRecipes.registerDefaults}: the rows hbm-space adds. Replaces
 * the former {@code gen_recipe_changes_chem.py}'s hydrotreating rows.
 */
public final class HydrotreatingTable extends RecipeSource<Row> {

    public HydrotreatingTable() {
        super(HydrotreatingRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        put(SpaceFluids.CCL, new FluidStack(Fluids.HYDROGEN, 10, 1),
                new FluidStack(SpaceFluids.CHLOROMETHANE, 80),
                new FluidStack(SpaceFluids.HCL, 20)
        );
        put(SpaceFluids.CBENZ, new FluidStack(Fluids.HYDROGEN, 10, 1),
                new FluidStack(Fluids.XYLENE, 50),
                new FluidStack(Fluids.CHLORINE, 40)
        );
        put(SpaceFluids.CHLOROETHANE, new FluidStack(Fluids.HYDROGEN, 10, 1),
                new FluidStack(SpaceFluids.VINYL, 50),
                new FluidStack(Fluids.CHLORINE, 20)
        );
    }

    /** hbm-space {@code recipes.put(input, new Triplet(hydrogen, out1, out2))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack hydrogen, FluidStack out1, FluidStack out2) {
        register(input.getKey(), new Row(input, hydrogen, List.of(out1, out2)));
    }
}
