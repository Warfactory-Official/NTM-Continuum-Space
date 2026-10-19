package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.ItemOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.ntmspace.bedrock.CelestialBedrockOre;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code CrystallizerRecipes.registerDefaults}: the {@code CelestialBedrockOre.getAllTypes()} grade loop
 * (washing, the three acids, hydrogen and the slop recycling of crumbs; its {@code ItemBedrockOreNew} grades are NTM:
 * Space's {@code bedrock_ore_<grade>_<body>_<suffix>} items). The rows go after HBM's ({@code order} left at its
 * default); HBM's own bedrock rows are removed by {@code recipe_removals/bedrock_ore.json}.
 */
public final class BedrockOreCrystallizerTable extends RecipeSource<Row> {

    public BedrockOreCrystallizerTable() {
        super(CrystallizerRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        int bedrock = 200;
        int washing = 100;
        for (BedrockOreType type : CelestialBedrockOre.getAllTypes()) {
            registerRecipe(in(Grade.BASE, type), new CrystallizerRecipe(make(Grade.BASE_WASHED, type), washing), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.BASE_ROASTED, type), new CrystallizerRecipe(make(Grade.BASE_WASHED, type), washing), new FluidStack(Fluids.WATER, 250));

            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_SULFURIC, type), bedrock), new FluidStack(Fluids.SULFURIC_ACID, 250));
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_SULFURIC, type), bedrock), new FluidStack(Fluids.SULFURIC_ACID, 250));

            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_SOLVENT, type), bedrock), new FluidStack(Fluids.SOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_SOLVENT, type), bedrock), new FluidStack(Fluids.SOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_NOSULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_SOLVENT, type), bedrock), new FluidStack(Fluids.SOLVENT, 250));

            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_RAD, type), bedrock), new FluidStack(Fluids.RADIOSOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_RAD, type), bedrock), new FluidStack(Fluids.RADIOSOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_NOSULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_RAD, type), bedrock), new FluidStack(Fluids.RADIOSOLVENT, 250));
            registerRecipe(in(Grade.PRIMARY_NOSOLVENT, type), new CrystallizerRecipe(make(Grade.PRIMARY_RAD, type), bedrock), new FluidStack(Fluids.RADIOSOLVENT, 250));

            int sulf = 4;
            registerRecipe(in(Grade.SULFURIC_BYPRODUCT, type), new CrystallizerRecipe(make(Grade.SULFURIC_WASHED, type), washing).setReq(sulf), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.SULFURIC_ROASTED, type), new CrystallizerRecipe(make(Grade.SULFURIC_WASHED, type), washing).setReq(sulf), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.SULFURIC_ARC, type), new CrystallizerRecipe(make(Grade.SULFURIC_WASHED, type), washing).setReq(sulf), new FluidStack(Fluids.WATER, 250));

            int solv = 4;
            registerRecipe(in(Grade.SOLVENT_BYPRODUCT, type), new CrystallizerRecipe(make(Grade.SOLVENT_WASHED, type), washing).setReq(solv), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.SOLVENT_ROASTED, type), new CrystallizerRecipe(make(Grade.SOLVENT_WASHED, type), washing).setReq(solv), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.SOLVENT_ARC, type), new CrystallizerRecipe(make(Grade.SOLVENT_WASHED, type), washing).setReq(solv), new FluidStack(Fluids.WATER, 250));

            int rad = 4;
            registerRecipe(in(Grade.RAD_BYPRODUCT, type), new CrystallizerRecipe(make(Grade.RAD_WASHED, type), washing).setReq(rad), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.RAD_ROASTED, type), new CrystallizerRecipe(make(Grade.RAD_WASHED, type), washing).setReq(rad), new FluidStack(Fluids.WATER, 250));
            registerRecipe(in(Grade.RAD_ARC, type), new CrystallizerRecipe(make(Grade.RAD_WASHED, type), washing).setReq(rad), new FluidStack(Fluids.WATER, 250));

            FluidStack primary = new FluidStack(Fluids.HYDROGEN, 250);
            registerRecipe(in(Grade.PRIMARY, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_ROASTED, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_SULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_NOSULFURIC, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_SOLVENT, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_NOSOLVENT, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_RAD, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);
            registerRecipe(in(Grade.PRIMARY_NORAD, type), new CrystallizerRecipe(make(Grade.PRIMARY_FIRST, type), bedrock), primary);

            registerRecipe(in(Grade.CRUMBS, type), new CrystallizerRecipe(make(Grade.BASE, type), bedrock).setReq(64), new FluidStack(Fluids.SLOP, 1000));
        }
    }

    private static ComparableStack in(Grade grade, BedrockOreType type) {
        return new ComparableStack(ModBedrockOreItems.get(grade, type));
    }

    /** hbm-space {@code ItemBedrockOreNew.make(grade, type)}. */
    private static ItemStack make(Grade grade, BedrockOreType type) {
        return ModBedrockOreItems.make(grade, type);
    }

    /**
     * hbm-space {@code registerRecipe(input, recipe, acid)}; {@code setReq} is the input's count. The id is the input
     * item's path and the acid.
     */
    private void registerRecipe(ComparableStack input, CrystallizerRecipe recipe, FluidStack stack) {
        AStack counted = recipe.itemAmount == 1 ? input : input.count(recipe.itemAmount);
        register(BuiltInRegistries.ITEM.getKey(input.item).getPath() + "_" + stack.type.getKey(),
                new Row(counted, new FluidStack(stack.type, stack.fill), new ItemOutput(ItemOut.of(recipe.output)),
                        recipe.duration, 0F, Optional.empty(), Integer.MAX_VALUE));
    }

    /** hbm-space {@code CrystallizerRecipe}: an output, a duration and {@code setReq}. */
    private static final class CrystallizerRecipe {
        final ItemStack output;
        final int duration;
        int itemAmount = 1;

        CrystallizerRecipe(ItemStack output, int duration) {
            this.output = output;
            this.duration = duration;
        }

        CrystallizerRecipe setReq(int amount) {
            this.itemAmount = amount;
            return this;
        }
    }
}
