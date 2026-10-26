package com.ntmspace.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.ModUpgradeItems;
import com.ntmspace.inventory.material.SpaceAlloyMaterials;
import com.ntmspace.registry.DysonContent;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code PlasmaForgeRecipes.registerDefaults}: the rows hbm-space adds to HBM's table, each taking the order of
 * the HBM row it follows (equal orders sort by id): {@code plsm.weldstainless} after {@code plsm.weldosmiridium}, the
 * Dyson swarm machines after {@code plsm.dfcstabilizer}. Replaces the former {@code gen_recipe_changes_nuclear.py}'s
 * {@code gen_plasma_forge}.
 */
public final class PlasmaForgeTable extends GenericTableSource<PlasmaForgeRecipe> {

    public PlasmaForgeTable() {
        super(PlasmaForgeRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        NTMMaterial STAINLESS = SpaceAlloyMaterials.STAINLESS;
        String autoPlate = "autoswitch.weldPlates";
        int dyson = orderOf("hbm:plasma_forge/dfcstabilizer");

        // after plsm.weldosmiridium
        this.register(at(orderOf("hbm:plasma_forge/weldosmiridium"), (PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.weldstainless").setInputEnergy(500_000).setup(125, 20_000L)
                .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, STAINLESS), 1))
                .inputItems(new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "plates_cast/stainless_steel")), 2)).setGroup(autoPlate, this)));

        // after plsm.dfcstabilizer
        this.register(at(dyson, (PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dysonlauncher").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(DysonContent.DYSON_LAUNCHER.get()))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 8_000))
                .inputItems(
                        OSMIRIDIUM.plateWelded().count(16),
                        CMB.plateWelded().count(16),
                        STAINLESS.plateWelded().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 8))));

        this.register(at(dyson, (PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dysonreceiver").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(DysonContent.DYSON_RECEIVER.get()))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 8_000))
                .inputItems(
                        OSMIRIDIUM.plateWelded().count(16),
                        CMB.plateWelded().count(16),
                        new ComparableStack(ModUpgradeItems.crystal_xen.get()),
                        BSCCO.wireDense().count(64),
                        BSCCO.wireDense().count(64),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 16))));

        this.register(at(dyson, (PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dysonconverterhe").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(DysonContent.DYSON_CONVERTER_HE.get()))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 8_000))
                .inputItems(
                        OSMIRIDIUM.plateWelded().count(16),
                        CMB.plateWelded().count(16),
                        STAR.plateCast().count(16),
                        BSCCO.wireDense().count(64),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 4))));

        this.register(at(dyson, (PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dysonconvertertu").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(DysonContent.DYSON_CONVERTER_TU.get()))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 8_000))
                .inputItems(
                        OSMIRIDIUM.plateWelded().count(16),
                        CMB.plateWelded().count(16),
                        STAR.plateCast().count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 4))));

        this.register(at(dyson, (PlasmaForgeRecipe) new PlasmaForgeRecipe("plsm.dysonconverteranatmo").setInputEnergy(50_000_000).setup(1_200, 10_000_000).outputItems(new ItemStack(DysonContent.DYSON_CONVERTER_ANATMOGENESIS.get()))
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 16_000))
                .inputItems(
                        OSMIRIDIUM.plateWelded().count(16),
                        CMB.plateWelded().count(16),
                        DNT.wireDense().count(16),
                        new ComparableStack(ModSingularityItems.singularity_spark.get()),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM), 16))));
    }

    /** Rows carry the {@code order} {@link #at} gives them: the order of the HBM row they follow. */
    @Override
    protected boolean ordered() {
        return false;
    }

    private static PlasmaForgeRecipe at(int order, PlasmaForgeRecipe recipe) {
        recipe.order = order;
        return recipe;
    }
}
