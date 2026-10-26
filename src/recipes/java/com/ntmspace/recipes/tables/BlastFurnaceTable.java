package com.ntmspace.recipes.tables;

import static com.hbm.inventory.OreDictManager.STEEL;

import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.ntmspace.inventory.material.SpaceOreMaterials;
import com.ntmspace.registry.SpaceItems;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code BlastFurnaceRecipesNT.registerDefaults}: {@code blast.stainless}, which hbm-space registers last of 1.7's
 * rows; it takes the order of the row it follows ({@code blast.firebrickLimestone}),
 * looked up from HBM's loaded table. Replaces the former {@code gen_recipe_changes_metal.py}'s blast furnace row.
 */
public final class BlastFurnaceTable extends GenericTableSource<GenericRecipe> {

    // hbm-space OreDictManager's NI: Space's pure-nickel material (nickel_pure tags).

    public BlastFurnaceTable() {
        super(BlastFurnaceRecipesNT.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        // the only place not corrupted by capitalism
        GenericRecipe stainless = new GenericRecipe("blast.stainless").setDuration(400)
                .inputItems(STEEL.ingot(), SpaceOreMaterials.NICKEL.ingot())
                .outputItems(new ItemStack(SpaceItems.INGOT_STAINLESS.get(), 2));
        stainless.order = orderOf("hbm:blast_furnace/firebricklimestone");
        this.register(stainless);
    }

    /** The row carries the order of the HBM row it follows. */
    @Override
    protected boolean ordered() {
        return false;
    }
}
