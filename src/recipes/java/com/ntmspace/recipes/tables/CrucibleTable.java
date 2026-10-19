package com.ntmspace.recipes.tables;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes.CrucibleRecipe;
import com.hbm.items.ModItems;
import com.ntmspace.inventory.material.SpaceAlloyMaterials;
import com.ntmspace.inventory.material.SpaceOreMaterials;
import com.ntmspace.registry.SpaceItems;
import dev.golem.core.world.RecipeSource;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code CrucibleRecipes.registerDefaults}: the alloys hbm-space adds, each taking the order of the HBM row it
 * follows (equal orders sort by id): {@code crucible.hsss} after {@code crucible.hss} (HBM order 4),
 * {@code crucible.arse} and {@code crucible.stainless} after {@code crucible.bscco} (12). hbm-space's materials are
 * NTM: Space's ({@link SpaceAlloyMaterials}, {@link SpaceOreMaterials}). Replaces
 * the former {@code gen_recipe_changes_metal.py}'s crucible rows.
 */
public final class CrucibleTable extends RecipeSource<CrucibleRecipe> {

    public CrucibleTable() {
        super(CrucibleRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        int n = MaterialShapes.NUGGET.quantity;
        int i = MaterialShapes.INGOT.quantity;

        // after crucible.hss
        this.register(4, new CrucibleRecipe("crucible.hsss").crucibleSetup(12, new ItemStack(ModItems.ingot_dura_steel.get()))
                .inputs(new MaterialStack(SpaceAlloyMaterials.STAINLESS, n * 5), new MaterialStack(Mats.MAT_TUNGSTEN, n * 3), new MaterialStack(Mats.MAT_COBALT, n * 1))
                .outputs(new MaterialStack(Mats.MAT_DURA, i * 2)));

        // after crucible.bscco
        this.register(12, new CrucibleRecipe("crucible.arse").crucibleSetup(9, new ItemStack(SpaceItems.INGOT_GAAS.get()))
                .inputs(new MaterialStack(SpaceOreMaterials.GALLIUM, n * 6), new MaterialStack(Mats.MAT_ARSENIC, n * 3))
                .outputs(new MaterialStack(SpaceAlloyMaterials.GAAS, i)));

        this.register(12, new CrucibleRecipe("crucible.stainless").crucibleSetup(2, new ItemStack(SpaceItems.INGOT_STAINLESS.get()))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n), new MaterialStack(SpaceOreMaterials.NICKEL, n))
                .outputs(new MaterialStack(SpaceAlloyMaterials.STAINLESS, n * 2)));
    }

    /** A row at the {@code order} of the HBM row it follows, under its name without the prefix. */
    private void register(int order, CrucibleRecipe recipe) {
        recipe.order = order;
        String name = recipe.getInternalName();
        register(name.substring(name.indexOf('.') + 1).toLowerCase(Locale.ROOT), recipe);
    }
}
