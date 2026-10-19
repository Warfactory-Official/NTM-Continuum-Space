package com.ntmspace.recipes.tables;

import static com.hbm.inventory.OreDictManager.BI;
import static com.hbm.inventory.OreDictManager.CO;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes.OutgasserRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code OutgasserRecipes.registerDefaults}: the cobalt-60 and polonium rows hbm-space adds after the gold rows;
 * they take the order of the last of those ({@code outgasser.au198_dust}, HBM order 6) and sort by id. Named as HBM names
 * its rows, {@code outgasser.<what it makes>}. Replaces the former {@code gen_recipe_changes_nuclear.py}'s
 * {@code gen_outgasser}.
 */
public final class OutgasserTable extends GenericTableSource<OutgasserRecipe> {

    /** HBM's order of {@code outgasser.au198_dust}, the row these follow. */
    private static final int AFTER_GOLD = 6;

    public OutgasserTable() {
        super(OutgasserRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        // cobalt to cobalt-60
        put("co60_ingot", CO.ingot(), new ItemStack(ModItems.ingot_co60.get()), null);
        put("co60_nugget", CO.nugget(), new ItemStack(ModItems.nugget_co60.get()), null);
        put("co60_dust", CO.dust(), new ItemStack(ModItems.powder_co60.get()), null);

        // bismuth to polonium
        put("polonium_ingot", BI.ingot(), new ItemStack(ModItems.ingot_polonium.get()), null);
        put("polonium_nugget", BI.nugget(), new ItemStack(ModItems.nugget_polonium.get()), null);
        put("polonium_dust", BI.dust(), new ItemStack(ModItems.powder_polonium.get()), null);
    }

    /** 1.7 {@code recipes.put(input, new OutgasserRecipe(solid, fluid))}, under the port's row name. */
    private void put(String name, AStack input, ItemStack solid, FluidStack fluid) {
        OutgasserRecipe recipe = new OutgasserRecipe("outgasser." + name).inputItems(input);
        if (solid != null) recipe.outputItems(solid);
        if (fluid != null) recipe.outputFluids(fluid);
        recipe.setup(OutgasserRecipes.DURATION, 0);
        recipe.order = AFTER_GOLD;
        register(recipe);
    }

    @Override
    protected boolean ordered() {
        return false;
    }
}
