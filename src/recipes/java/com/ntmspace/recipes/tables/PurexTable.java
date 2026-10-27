package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ModItems;
import com.ntmspace.registry.SpaceItems;
import com.ntmspace.registry.SpaceSpecialItems;
import dev.golem.core.world.ItemStacks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code PUREXRecipes.registerDefaults}: the Watz rows for the pellet types hbm-space adds to
 * {@code EnumWatzType} (NTM: Space's {@code watz_pellet_depleted_<type>} items, {@link SpaceSpecialItems#WATZ_DEPLETED}),
 * in 1.7's file order after 1.7's Watz rows. Replaces the former {@code gen_recipe_changes_nuclear.py}'s
 * {@code gen_purex}.
 */
public final class PurexTable extends GenericTableSource<GenericRecipe> {

    public PurexTable() {
        super(PUREXRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        long watzPower = 10_000;
        String autoWatz = "autoswitch.watz";

        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzpu241").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("pu241"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_am242.get(), 12),
                        new ItemStack(ModItems.nugget_am241.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzamf").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("amf"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(SpaceItems.NUGGET_CM_MIX.get(), 6),
                        new ItemStack(SpaceItems.NUGGET_BK247.get(), 3),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzamrg").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("amrg"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(SpaceItems.NUGGET_CM_MIX.get(), 12),
                        new ItemStack(SpaceItems.NUGGET_BK247.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzcmf").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("cmf"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(SpaceItems.NUGGET_CM_MIX.get(), 12),
                        new ItemStack(SpaceItems.NUGGET_ES253.get(), 3),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzcmrg").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("cmrg"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(SpaceItems.NUGGET_CM_MIX.get(), 12),
                        new ItemStack(SpaceItems.NUGGET_CF252.get(), 3),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzbk247").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("bk247"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(ModItems.nugget_am_mix.get(), 12),
                        new ItemStack(ModItems.nugget_pu239.get(), 3),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzcf251").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("cf251"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(SpaceItems.NUGGET_CM_MIX.get(), 3),
                        new ItemStack(SpaceItems.NUGGET_CF251.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzcf252").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("cf252"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(SpaceItems.NUGGET_CF252.get(), 3),
                        new ItemStack(SpaceItems.NUGGET_CM_MIX.get(), 6),
                        new ItemStack(ModItems.nuclear_waste.get(), 2))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
        this.register(setIconToFirstIngredient(new GenericRecipe("purex.watzes253").setup(60, watzPower).setNameWrapper("purex.recycle").setGroup(autoWatz, this)
                .inputItems(new ComparableStack(watzPelletDepleted("es253"), 1))
                .inputFluids(new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250))
                .outputItems(new ItemStack(SpaceItems.NUGGET_ES253.get(), 3),
                        new ItemStack(SpaceItems.NUGGET_CF252.get(), 3),
                        new ItemStack(ModItems.nuclear_waste.get(), 24))
                .outputFluids(new FluidStack(Fluids.WATZ, 1_000))));
    }

    /** Rows run in call order, which is 1.7's file order; the base positions them after its own. */
    private int nextWatzOrder = -1;

    /** Every row follows 1.7's Watz rows, in call order right after HBM's last one. */
    @Override
    public void register(String id, GenericRecipe recipe) {
        if (nextWatzOrder < 0) nextWatzOrder = orderOf("hbm:purex/schrabmen") + 1;
        recipe.order = nextWatzOrder++;
        add(id, recipe);
    }

    /** 1.7 {@code new ComparableStack(ModItems.watz_pellet_depleted, 1, EnumWatzType.X)}: hbm-space's type as its own item. */
    private static Item watzPelletDepleted(String type) {
        return SpaceSpecialItems.WATZ_DEPLETED.get(type).get();
    }

    /** 1.7 {@code setIconToFirstIngredient()}: the row's icon is its first item ingredient. */
    private static GenericRecipe setIconToFirstIngredient(GenericRecipe recipe) {
        return recipe.setIcon(ItemStacks.copyWithCount(recipe.inputItem[0].getDisplayStack(), 1));
    }
}
