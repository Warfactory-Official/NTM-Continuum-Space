package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.ntmspace.bedrock.CelestialBedrockOre;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code ElectrolyserMetalRecipes.registerDefaults}: the {@code CelestialBedrockOre.getAllTypes()} loop through
 * {@code makeBedrockOreProduct} (its {@code ItemBedrockOreNew} grades are NTM: Space's
 * {@code bedrock_ore_<grade>_<body>_<suffix>} items; a type's {@code primary} is the port's {@code primary1}). The rows go
 * after HBM's ({@code order} left at its default); HBM's own bedrock rows are removed by
 * {@code recipe_removals/bedrock_ore.json}.
 */
public final class BedrockOreElectrolyserMetalTable extends RecipeSource<Row> {

    public BedrockOreElectrolyserMetalTable() {
        super(ElectrolyserMetalRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        for (BedrockOreType type : CelestialBedrockOre.getAllTypes()) {
            List<Object[]> productsF = new ArrayList<>();
            productsF.add(new Object[] {type.primary1, 8});
            productsF.add(new Object[] {ModBedrockOreItems.make(Grade.CRUMBS, type), 1});
            put(new ComparableStack(ModBedrockOreItems.get(Grade.PRIMARY_FIRST, type)), makeBedrockOreProduct(productsF));

            List<Object[]> productsC = new ArrayList<>();
            productsC.add(new Object[] {type.primary1, 2});

            put(new ComparableStack(ModBedrockOreItems.get(Grade.CRUMBS, type)), makeBedrockOreProduct(productsC));
        }
    }

    /** hbm-space {@code makeBedrockOreProduct}; a {@code Pair<Object, Integer>} is an {@code Object[]{output, amount}}. */
    private static ElectrolysisMetalRecipe makeBedrockOreProduct(List<Object[]> products) {
        List<MaterialStack> moltenProducts = new ArrayList<>();
        List<ItemStack> solidProducts = new ArrayList<>();

        for (Object[] product : products) {
            int amount = (Integer) product[1];
            if (moltenProducts.size() < 2 && product[0] instanceof BedrockOreOutput output) {
                MaterialStack melt = toFluid(output, amount);
                if (melt != null) {
                    moltenProducts.add(melt);
                    continue;
                }
            }

            if (product[0] instanceof BedrockOreOutput output) solidProducts.add(BedrockOreType.fragment(output, amount));
            if (product[0] instanceof ItemStack stack) solidProducts.add(stack.copy());
        }
        if (moltenProducts.isEmpty()) moltenProducts.add(new MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.quantity * 2));

        return new ElectrolysisMetalRecipe(
                moltenProducts.get(0),
                moltenProducts.size() > 1 ? moltenProducts.get(1) : null,
                20,
                solidProducts.toArray(new ItemStack[0]));
    }

    /** hbm-space {@code ItemBedrockOreNew.toFluid}: the molten material of a smeltable output, else nothing. */
    private static @Nullable MaterialStack toFluid(BedrockOreOutput o, double amount) {
        NTMMaterial mat = o.material();
        if (mat != null && mat.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
            return new MaterialStack(mat, (int) Math.ceil(MaterialShapes.FRAGMENT.quantity * o.amount() * amount));
        }
        return null;
    }

    /** hbm-space {@code recipes.put(input, recipe)}; the id is the input item's path. */
    private void put(ComparableStack input, ElectrolysisMetalRecipe recipe) {
        List<MaterialStack> molten = new ArrayList<>();
        molten.add(recipe.output1);
        if (recipe.output2 != null) molten.add(recipe.output2);
        List<ItemOut> byproducts = new ArrayList<>();
        for (ItemStack stack : recipe.byproduct) byproducts.add(ItemOut.of(stack));
        register(BuiltInRegistries.ITEM.getKey(input.item).getPath(),
                new Row(input.item, molten, byproducts, recipe.duration, Integer.MAX_VALUE));
    }

    /** hbm-space {@code ElectrolysisMetalRecipe}'s constructor with a duration. */
    private static final class ElectrolysisMetalRecipe {
        final MaterialStack output1;
        final @Nullable MaterialStack output2;
        final int duration;
        final ItemStack[] byproduct;

        ElectrolysisMetalRecipe(MaterialStack output1, @Nullable MaterialStack output2, int duration, ItemStack... byproduct) {
            this.output1 = output1;
            this.output2 = output2;
            this.duration = duration;
            this.byproduct = byproduct;
        }
    }
}
