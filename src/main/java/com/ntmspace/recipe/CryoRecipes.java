package com.ntmspace.recipe;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.ntmspace.NtmSpace;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code CryoRecipes} as the datapack table {@code ntmspace:cryo_distill}: one file per atmosphere in
 * {@code data/ntmspace/recipe/cryo_distill/}, converted once from the 1.7 table (GameTest {@code cryo_table_matches_17}).
 * A row is one input fluid of 100 mB (the amount the distiller takes per step), four output fractions in tank order,
 * {@code power} 20,000 HE per step and {@code duration} 1 (one step per tick). 1.7 looked recipes up by the input tank's
 * type; so does {@link #getOutput}.
 */
public final class CryoRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final CryoRecipes INSTANCE = new CryoRecipes();

    /** One step: {@code input} mB of the atmosphere become the four {@code fractions}, for {@code power} HE. */
    public record Output(int input, long power, FluidStack[] fractions) {}

    private volatile Map<FluidType, Output> byInput = Map.of();

    private CryoRecipes() {
        super(NtmSpace.MOD_ID, "cryo_distill");
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    @Override
    protected void rebuilt() {
        Map<FluidType, Output> map = new HashMap<>();
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe.inputFluid == null || recipe.inputFluid.length != 1 || recipe.outputFluid == null
                    || recipe.outputFluid.length != 4) {
                NtmSpace.LOG.warn("Cryo distillation recipe {} needs one input and four output fluids; ignored", recipe.getInternalName());
                continue;
            }
            map.putIfAbsent(recipe.inputFluid[0].type, new Output(recipe.inputFluid[0].fill, recipe.power, recipe.outputFluid));
        }
        byInput = map;
    }

    /** 1.7 {@code CryoRecipes.getOutput(type)}. */
    public @Nullable Output getOutput(FluidType type) {
        registerRecipes();
        return byInput.get(type);
    }
}
