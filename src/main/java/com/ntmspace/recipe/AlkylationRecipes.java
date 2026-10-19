package com.ntmspace.recipe;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.ntmspace.NtmSpace;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code AlkylationRecipes} as the datapack table {@code ntmspace:alkylation}
 * ({@code data/ntmspace/recipe/alkylation/}, converted once from the 1.7 table; GameTest {@code alkylation_table_matches_17}).
 * A row: the primary input (100 mB per step), an optional secondary input (1.7's {@code X}; absent where 1.7 had
 * {@code NONE, 0}), two outputs ({@code Y}, {@code Z}), {@code power} 4,000 HE and {@code duration} 2 (1.7 ran a step every
 * second tick). Looked up by the primary input's type.
 */
public final class AlkylationRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final AlkylationRecipes INSTANCE = new AlkylationRecipes();

    /** One step: {@code input} mB of the primary and {@code secondary} make {@code outputs}, for {@code power} HE. */
    public record Output(int input, FluidStack secondary, FluidStack out1, FluidStack out2, long power) {}

    private volatile Map<FluidType, Output> byInput = Map.of();

    private AlkylationRecipes() {
        super(NtmSpace.MOD_ID, "alkylation");
    }

    public static void declare() {}

    @Override
    protected void rebuilt() {
        Map<FluidType, Output> map = new HashMap<>();
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe.inputFluid == null || recipe.inputFluid.length < 1 || recipe.inputFluid.length > 2
                    || recipe.outputFluid == null || recipe.outputFluid.length != 2) {
                NtmSpace.LOG.warn("Alkylation recipe {} needs one or two inputs and two output fluids; ignored", recipe.getInternalName());
                continue;
            }
            FluidStack secondary = recipe.inputFluid.length > 1 ? recipe.inputFluid[1] : new FluidStack(Fluids.NONE, 0);
            map.putIfAbsent(recipe.inputFluid[0].type, new Output(recipe.inputFluid[0].fill, secondary,
                    recipe.outputFluid[0], recipe.outputFluid[1], recipe.power));
        }
        byInput = map;
    }

    /** 1.7 {@code AlkylationRecipes.getOutput(type)}. */
    public @Nullable Output getOutput(FluidType type) {
        registerRecipes();
        return byInput.get(type);
    }
}
