package com.ntmspace.recipes.tables;

import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ArcFurnaceRecipes.Row;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.ntmspace.bedrock.CelestialBedrockOre;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code ArcFurnaceRecipes.registerDefaults}: the {@code CelestialBedrockOre.getAllTypes()} grade loop (its
 * {@code ItemBedrockOreNew} grades are NTM: Space's {@code bedrock_ore_<grade>_<body>_<suffix>} items; a type's
 * {@code primary}, {@code byproductAcid}, {@code byproductSolvent} and {@code byproductRad} are the port's
 * {@code primary1}, {@code byproductAcid1}, {@code byproductSolvent1} and {@code byproductRad1}). The rows go after HBM's
 * ({@code order} left at its default); HBM's own bedrock rows are removed by {@code recipe_removals/bedrock_ore.json}.
 */
public final class BedrockOreArcFurnaceTable extends RecipeSource<Row> {

    public BedrockOreArcFurnaceTable() {
        super(ArcFurnaceRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        for (BedrockOreType type : CelestialBedrockOre.getAllTypes()) {
            register(in(Grade.SULFURIC_BYPRODUCT, type), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.SULFURIC_ARC, type, 2)));
            register(in(Grade.SULFURIC_ROASTED, type), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.SULFURIC_ARC, type, 4)));
            register(in(Grade.SOLVENT_BYPRODUCT, type), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.SOLVENT_ARC, type, 2)));
            register(in(Grade.SOLVENT_ROASTED, type), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.SOLVENT_ARC, type, 4)));
            register(in(Grade.RAD_BYPRODUCT, type), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.RAD_ARC, type, 2)));
            register(in(Grade.RAD_ROASTED, type), new ArcFurnaceRecipe().solid(ModBedrockOreItems.make(Grade.RAD_ARC, type, 4)));

            register(in(Grade.PRIMARY_FIRST, type), new ArcFurnaceRecipe().fluidNull(toFluid(type.primary1, 5)));
            register(in(Grade.CRUMBS, type), new ArcFurnaceRecipe().fluidNull(toFluid(type.primary1, 1)));

            register(in(Grade.SULFURIC_WASHED, type), new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductAcid1, 3)));
            register(in(Grade.SOLVENT_WASHED, type), new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductSolvent1, 3)));
            register(in(Grade.RAD_WASHED, type), new ArcFurnaceRecipe().fluidNull(toFluid(type.byproductRad1, 3)));
        }
    }

    private static ComparableStack in(Grade grade, BedrockOreType type) {
        return new ComparableStack(ModBedrockOreItems.get(grade, type));
    }

    /**
     * hbm-space {@code register(input, recipe)}. A recipe with neither output (a grade whose output cannot melt, from
     * {@code fluidNull(null)}) is not a row: hbm-space registered it, and it never matched anything (as in HBM's table).
     * The id is the input item's path.
     */
    private void register(ComparableStack input, ArcFurnaceRecipe recipe) {
        if (recipe.solidOutput == null && recipe.fluidOutput.isEmpty()) return;
        register(BuiltInRegistries.ITEM.getKey(input.item).getPath(), new Row(input,
                Optional.ofNullable(recipe.solidOutput).map(ItemOut::of), recipe.fluidOutput, Integer.MAX_VALUE));
    }

    /** hbm-space {@code ItemBedrockOreNew.toFluid}: the molten material of a smeltable output, else nothing. */
    private static @Nullable MaterialStack toFluid(@Nullable BedrockOreOutput o, double amount) {
        if (o == null) return null;
        NTMMaterial mat = o.material();
        if (mat != null && mat.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
            return new MaterialStack(mat, (int) Math.ceil(MaterialShapes.FRAGMENT.quantity * o.amount() * amount));
        }
        return null;
    }

    /** hbm-space {@code ArcFurnaceRecipe}'s builder. */
    private static final class ArcFurnaceRecipe {
        @Nullable ItemStack solidOutput;
        List<MaterialStack> fluidOutput = List.of();

        ArcFurnaceRecipe fluidNull(@Nullable MaterialStack... outputs) {
            List<MaterialStack> mat = new ArrayList<>();
            for (MaterialStack stack : outputs) if (stack != null) mat.add(stack);
            if (!mat.isEmpty()) this.fluidOutput = mat;
            return this;
        }

        ArcFurnaceRecipe solid(ItemStack output) {
            this.solidOutput = output;
            return this;
        }
    }
}
