package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.PseudoRow;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModItems;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.registry.SpaceItems;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code GasCentrifugeRecipes.PseudoFluidType}'s two added constants and its {@code fluidConversions} entry for
 * them: one row per pseudo-fluid, its id the name in lower case, as HBM's table. Replaces
 * the former {@code gen_recipe_changes_chem.py}'s pseudo-fluid rows.
 */
public final class PseudoFluidTable extends RecipeSource<PseudoRow> {

    /** The pseudo-fluids as declared, before {@code fluidConversions} adds where each comes from. */
    private final Map<String, PseudoRow> types = new LinkedHashMap<>();

    public PseudoFluidTable() {
        super(GasCentrifugeRecipes.PSEUDO_FLUIDS.table);
    }

    @Override
    protected void registerDefaults() {
        // 1.7 PseudoFluidType constants (NONE ends a chain)
        type("MINSOLSEP", 500, 0, null, false, new ItemStack(SpaceItems.CRYSTAL_CLEANED.get(), 1));
        type("MINSOL", 500, 500, "MINSOLSEP", false, new ItemStack(ModItems.powder_iron.get(), 1));

        // 1.7 registerDefaults
        fluidConversions(SpaceFluids.MINSOL, "MINSOL");

        types.forEach((name, row) -> register(name.toLowerCase(Locale.ROOT), row));
    }

    /** 1.7 {@code new PseudoFluidType(name, fluidConsumed, fluidProduced, outputFluid, isHighSpeed, output...)}. */
    private void type(String name, int consumed, int produced, String next, boolean highSpeed, ItemStack... output) {
        List<ItemOut> outputs = new ArrayList<>();
        for (ItemStack stack : output) outputs.add(ItemOut.of(stack));
        types.put(name, new PseudoRow(name, Optional.empty(), consumed, produced, Optional.ofNullable(next), highSpeed, outputs));
    }

    /** 1.7 {@code fluidConversions.put(fluid, type)}. */
    private void fluidConversions(FluidType fluid, String name) {
        PseudoRow row = types.get(name);
        types.put(name, new PseudoRow(row.name(), Optional.of(fluid), row.consumed(), row.produced(), row.next(), row.highSpeed(), row.outputs()));
    }
}
