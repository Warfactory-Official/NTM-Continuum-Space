package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.DisplayRow;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * hbm-space {@code GasCentrifugeRecipes.register}: the {@code gasCent} display row hbm-space adds, mineral solution
 * (its pseudo-fluid chain MINSOL to MINSOLSEP is the {@code pseudo_fluid} table). Replaces
 * the former {@code gen_recipe_changes_chem.py}'s gas_centrifuge row.
 */
public final class GasCentrifugeTable extends RecipeSource<DisplayRow> {

    public GasCentrifugeTable() {
        super(GasCentrifugeRecipes.DISPLAY.table);
    }

    @Override
    protected void registerDefaults() {
        put("minsol", new FluidStack(1000, SpaceFluids.MINSOL), new ItemOut[] {out("hbm:powder_iron"), out("ntmspace:crystal_cleaned")}, false, 2);
    }

    /** hbm-space {@code gasCent.put(input, new Object[] {outputs, isHighSpeed, centrifuges})}, under an id per row. */
    private void put(String id, FluidStack input, ItemOut[] outputs, boolean highSpeed, int centrifuges) {
        register(id, new DisplayRow(input, List.of(outputs), highSpeed, centrifuges));
    }

    private static ItemOut out(String id) {
        return new ItemOut(BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id)), 1);
    }
}
