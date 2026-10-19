package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ReformingRecipes;
import com.hbm.inventory.recipes.ReformingRecipes.Row;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.world.RecipeSource;
import java.util.List;

/**
 * hbm-space {@code ReformingRecipes.registerDefaults}: the rows hbm-space adds. Replaces
 * the former {@code gen_recipe_changes_chem.py}'s reforming rows.
 */
public final class ReformingTable extends RecipeSource<Row> {

    public ReformingTable() {
        super(ReformingRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        put(SpaceFluids.HALOLIGHT,
                new FluidStack(Fluids.UNSATURATEDS, 50),
                new FluidStack(Fluids.REFORMGAS, 35),
                new FluidStack(SpaceFluids.HCL, 10)
        );
        put(SpaceFluids.HGAS,
                new FluidStack(Fluids.AROMATICS, 60),
                new FluidStack(SpaceFluids.CHLOROMETHANE, 25),
                new FluidStack(SpaceFluids.HCL, 10)
        );
    }

    /** hbm-space {@code recipes.put(input, new Triplet(out1, out2, out3))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack out1, FluidStack out2, FluidStack out3) {
        register(input.getKey(), new Row(input, List.of(out1, out2, out3)));
    }
}
