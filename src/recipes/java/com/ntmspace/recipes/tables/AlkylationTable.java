package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.recipe.AlkylationRecipes;
import java.util.Locale;

/**
 * hbm-space {@code AlkylationRecipes.registerDefaults}: one row per {@code recipes.put(input, Triplet(X, Y, Z))}, named
 * {@code alkylation.<input>}, in registration order. A row is 100 mB of the input (the amount 1.7's
 * {@code TileEntityMachineAlkylation} takes per step) plus {@code X} (left out where 1.7 has {@code NONE, 0}), making
 * {@code Y} and {@code Z}, for 4,000 HE every second tick ({@code duration} 2), as {@link AlkylationRecipes} reads it.
 * Replaces the authored rows of {@code data/ntmspace/recipe/alkylation/}.
 */
public final class AlkylationTable extends GenericTableSource<GenericRecipe> {

    /** 1.7 {@code TileEntityMachineAlkylation}: mB of the input per step. */
    private static final int INPUT = 100;
    /** 1.7 {@code TileEntityMachineAlkylation}: HE per step. */
    private static final long POWER = 4_000;
    /** 1.7 ran a step every second tick. */
    private static final int DURATION = 2;

    public AlkylationTable() {
        super(AlkylationRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        // Useful halogenation recipes
        put(SpaceFluids.CHLOROMETHANE, // Alkylation via halogenated methane
            new FluidStack(Fluids.NONE, 0),
            new FluidStack(Fluids.UNSATURATEDS, 75),
            new FluidStack(Fluids.CHLORINE, 25));
        put(Fluids.UNSATURATEDS, // iron oxide catalyst in the unit + heat produces cyclic chains
            new FluidStack(Fluids.NONE, 0),
            new FluidStack(Fluids.AROMATICS, 70),
            new FluidStack(Fluids.PETROLEUM, 30));
        put(Fluids.AROMATICS, // Dichloromethane triggers a further alkylation process, creating high-octane aromatics!
            new FluidStack(Fluids.RADIOSOLVENT, 40),
            new FluidStack(Fluids.XYLENE, 100),
            new FluidStack(Fluids.CHLORINE, 10)); // Chlorine is preserved in all, but you'll need to re-irradiate here

        // Real world alkylation unit recipes
        put(Fluids.PETROLEUM, // Alkys are generally used to turn isobutane into gasoline
            new FluidStack(Fluids.SULFURIC_ACID, 50), // "significant volumes" of sulfuric acid are required
            new FluidStack(Fluids.LIGHTOIL, 50), // but the output is highly useful!
            new FluidStack(Fluids.PEROXIDE, 20));

        put(Fluids.XYLENE,
            new FluidStack(SpaceFluids.CCL, 60),
            new FluidStack(Fluids.REFORMGAS, 100),
            new FluidStack(SpaceFluids.CHLOROMETHANE, 50));

        put(SpaceFluids.CHLOROETHANE,
            new FluidStack(Fluids.UNSATURATEDS, 60),
            new FluidStack(SpaceFluids.CBENZ, 70),
            new FluidStack(SpaceFluids.CHLOROMETHANE, 10));
    }

    /** 1.7 {@code recipes.put(input, new Triplet<>(secondary, out1, out2))}. */
    private void put(FluidType input, FluidStack secondary, FluidStack out1, FluidStack out2) {
        GenericRecipe recipe = new GenericRecipe("alkylation." + input.getName().toLowerCase(Locale.ROOT)).setup(DURATION, POWER);
        if (secondary.type == Fluids.NONE) recipe.inputFluids(new FluidStack(input, INPUT));
        else recipe.inputFluids(new FluidStack(input, INPUT), secondary);
        recipe.outputFluids(out1, out2);
        register(recipe);
    }
}
