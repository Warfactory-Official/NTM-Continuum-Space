package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.inventory.recipes.CokerRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * hbm-space {@code CokerRecipes.registerDefaults}: the rows hbm-space adds ({@code coke(PETROLEUM)} is HBM's
 * {@code coke_petroleum}). Replaces the former {@code gen_recipe_changes_chem.py}'s coker rows.
 */
public final class CokerTable extends RecipeSource<Row> {

    public CokerTable() {
        super(CokerRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        registerRecipe(SpaceFluids.BROMINE, 1_000, out("hbm:powder_bromine"), new FluidStack(Fluids.GAS, 500));
        registerRecipe(SpaceFluids.SCUTTERBLOOD, 16_000, out("hbm:coke_petroleum"), new FluidStack(Fluids.GAS_COKER, 1_600));
    }

    private void registerRecipe(FluidType type, int quantity, ItemOut output, FluidStack byproduct) {
        register(type.getKey(), new Row(type, Optional.of(quantity), Optional.empty(), output, Optional.ofNullable(byproduct), Optional.empty()));
    }

    private static ItemOut out(String id) {
        return new ItemOut(BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id)), 1);
    }
}
