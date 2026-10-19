package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * hbm-space {@code ElectrolyserFluidRecipes.registerDefaults}: the rows hbm-space adds. Replaces
 * the former {@code gen_recipe_changes_chem.py}'s electrolyser_fluid rows.
 */
public final class ElectrolyserFluidTable extends RecipeSource<Row> {

    public ElectrolyserFluidTable() {
        super(ElectrolyserFluidRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        put(SpaceFluids.BRINE, 400, new FluidStack(Fluids.HYDROGEN, 200), new FluidStack(Fluids.OXYGEN, 200), 40, out("hbm:powder_sodium", 2));
        put(SpaceFluids.AQUEOUS_NICKEL, 300, new FluidStack(Fluids.NONE, 0), new FluidStack(Fluids.NONE, 0), 40,
                out("ntmspace:powder_nickel", 2), out("hbm:powder_iron", 2), out("hbm:sulfur", 4));
        put(SpaceFluids.COPPERSULFATE, 200, new FluidStack(Fluids.NONE, 0), new FluidStack(Fluids.OXYGEN, 50), 40,
                out("hbm:powder_copper", 2), out("hbm:sulfur", 2));
        put(SpaceFluids.HCL, 1_000, new FluidStack(Fluids.HYDROGEN, 500), new FluidStack(Fluids.CHLORINE, 500), 40);
        put(SpaceFluids.LITHCARBONATE, 1_000, new FluidStack(Fluids.OXYGEN, 12_000), new FluidStack(Fluids.NONE, 0), 10,
                out("hbm:powder_lithium", 1), out("hbm:ingot_graphite", 1));
    }

    /** hbm-space {@code recipes.put(input, new ElectrolysisRecipe(amount, output1, output2, duration, byproduct...))}. */
    private void put(FluidType input, int amount, FluidStack output1, FluidStack output2, int duration, ItemOut... byproduct) {
        register(input.getKey(), new Row(input, amount, output1, output2, duration, List.of(byproduct)));
    }

    private static ItemOut out(String id, int count) {
        return new ItemOut(BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id)), count);
    }
}
