package com.ntmspace.recipes.tables;

import static com.hbm.inventory.OreDictManager.STEEL;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import com.ntmspace.inventory.material.SpaceOreMaterials;
import com.ntmspace.registry.SpaceItems;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code BlastFurnaceRecipes.registerDefaults} (the legacy alloy furnace): its stainless steel row, named as HBM
 * names the table's rows ({@code difurnace.<product>}) with the furnace's fixed processing time as its duration. It is
 * registered after the steel-with-flux row (HBM order 4) and takes that order (ids sort it after). Replaces
 * the former {@code gen_recipe_changes_metal.py}'s difurnace row.
 */
public final class DiFurnaceTable extends GenericTableSource<GenericRecipe> {

    // hbm-space OreDictManager's NI: Space's pure-nickel material (nickel_pure tags).
    private static final int PROCESSING_SPEED = 400;

    public DiFurnaceTable() {
        super(DiFurnaceRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        // after steelWithFlux
        addRecipe("stainless", 4, SpaceOreMaterials.NICKEL, STEEL, new ItemStack(SpaceItems.INGOT_STAINLESS.get(), 2));
    }

    /** The row carries the order of the HBM row it follows. */
    @Override
    protected boolean ordered() {
        return false;
    }

    /** 1.7 {@code addRecipe(in1, in2, out)}: a material stands for its ingot, plate, gem and dust. */
    private void addRecipe(String name, int order, NTMMaterial in1, NTMMaterial in2, ItemStack out) {
        GenericRecipe recipe = new GenericRecipe("difurnace." + name).setup(PROCESSING_SPEED, 0)
                .inputItems(stack(in1), stack(in2)).outputItems(out);
        recipe.order = order;
        register(recipe);
    }

    /** 1.7 {@code getRecipeStacks} of a material. */
    private static AStack stack(NTMMaterial frame) {
        return new RecipeIngredients.AnyOf(List.of(frame.ingot(), frame.plate(), frame.gem(), frame.dust()), 1);
    }
}
