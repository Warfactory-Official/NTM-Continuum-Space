package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.recipe.AtmosphereRecipes;
import java.util.Locale;

/**
 * hbm-space {@code AtmosphereRecipes.registerDefaults}: one row per {@code recipes.put(product, AtmosphereRecipe(a, b))},
 * named {@code atmo.<product>}: the two input gases and the product, as {@link AtmosphereRecipes} reads it. The rows carry
 * no {@code order} (the reactions are looked up, not listed). Replaces the authored row of
 * {@code data/ntmspace/recipe/atmosphere/}.
 */
public final class AtmosphereTable extends GenericTableSource<GenericRecipe> {

    public AtmosphereTable() {
        super(AtmosphereRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        put(new FluidStack(SpaceFluids.LITHCARBONATE, 32_000),
            new FluidStack(SpaceFluids.LITHYDRO, 16_000),
            new FluidStack(SpaceFluids.DUNAAIR, 256_000));
    }

    @Override
    protected boolean ordered() {
        return false;
    }

    /** 1.7 {@code recipes.put(output, new AtmosphereRecipe(a, b))}. */
    private void put(FluidStack output, FluidStack a, FluidStack b) {
        register(new GenericRecipe("atmo." + output.type.getName().toLowerCase(Locale.ROOT))
                .inputFluids(a, b)
                .outputFluids(output));
    }
}
