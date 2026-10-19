package com.ntmspace.recipe;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.ntmspace.NtmSpace;
import java.util.ArrayList;
import java.util.List;

/**
 * hbm-space's {@code AtmosphereRecipes} ("Atmospheric Chemistry", NEI shown on the emitter) as the datapack table
 * {@code ntmspace:atmosphere}, one file per reaction in {@code data/ntmspace/recipe/atmosphere/}. A row is two input gases
 * and one product gas: once a second {@code CelestialTraits.updateChemistry} reacts it 64 times at once when the body's
 * air holds 64 times each input (1.7 {@code CelestialBody.reactAtmosphere}). The 1.7 table has one row: 16,000 mB
 * lithium hydroxide and 256,000 mB Duna air make 32,000 mB lithium carbonate. 1.7's {@code hbmAtmosphere.json} could not
 * be edited (its reader was empty); the datapack table can.
 */
public final class AtmosphereRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final AtmosphereRecipes INSTANCE = new AtmosphereRecipes();

    /** 1.7 {@code AtmosphereRecipe}: two inputs, one output (the map's key). */
    public record Reaction(FluidStack[] inputs, FluidStack output) {}

    private volatile List<Reaction> reactions = List.of();

    private AtmosphereRecipes() {
        super(NtmSpace.MOD_ID, "atmosphere");
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    @Override
    protected void rebuilt() {
        List<Reaction> list = new ArrayList<>();
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe.inputFluid == null || recipe.inputFluid.length != 2 || recipe.outputFluid == null
                    || recipe.outputFluid.length != 1) {
                NtmSpace.LOG.warn("Atmosphere recipe {} needs two input fluids and one output fluid; ignored", recipe.getInternalName());
                continue;
            }
            list.add(new Reaction(recipe.inputFluid, recipe.outputFluid[0]));
        }
        reactions = List.copyOf(list);
    }

    /** 1.7 {@code getRecipesMap()}, in table order. */
    public List<Reaction> reactions() {
        registerRecipes();
        return reactions;
    }
}
