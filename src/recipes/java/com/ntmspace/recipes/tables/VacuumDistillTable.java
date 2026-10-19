package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.RefineryRecipes.VacuumRow;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.world.RecipeSource;
import java.util.List;

/**
 * hbm-space {@code VacuumRefineryRecipes.registerDefaults}: the row hbm-space adds, T-crude (vac_frac_heavy 40,
 * vac_frac_reform 25, vac_frac_light 20, vac_frac_sour 15). Replaces the former {@code gen_recipe_changes_chem.py}'s
 * vacuum_distill row.
 */
public final class VacuumDistillTable extends RecipeSource<VacuumRow> {

    public VacuumDistillTable() {
        super(RefineryRecipes.VACUUM.table.table);
    }

    @Override
    protected void registerDefaults() {
        put(SpaceFluids.TCRUDE,
                new FluidStack(SpaceFluids.HALOLIGHT, 40),
                new FluidStack(SpaceFluids.CHLOROMETHANE, 25),
                new FluidStack(SpaceFluids.HCL, 20),
                new FluidStack(SpaceFluids.HGAS, 15)
        );
    }

    /** hbm-space {@code recipes.put(input, new Quartet(frac1, frac2, frac3, frac4))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack frac1, FluidStack frac2, FluidStack frac3, FluidStack frac4) {
        register(input.getKey(), new VacuumRow(input, List.of(frac1, frac2, frac3, frac4)));
    }
}
