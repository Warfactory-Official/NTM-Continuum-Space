package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes.Row;
import com.hbm.inventory.recipes.CrystallizerRecipes.ItemOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.Output;
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
import net.minecraft.world.item.Items;

/**
 * hbm-space {@code CentrifugeRecipes.registerDefaults}: the {@code CelestialBedrockOre.getAllTypes()} grade loop (its
 * {@code ItemBedrockOreNew} grades are NTM: Space's {@code bedrock_ore_<grade>_<body>_<suffix>} items; a type's
 * {@code primary}, {@code byproductAcid}, {@code byproductSolvent} and {@code byproductRad} are the port's
 * {@code primary1}, {@code byproductAcid1}, {@code byproductSolvent1} and {@code byproductRad1}). The rows go after HBM's
 * ({@code order} left at its default); HBM's own bedrock rows are removed by {@code recipe_removals/bedrock_ore.json}.
 */
public final class BedrockOreCentrifugeTable extends RecipeSource<Row> {

    public BedrockOreCentrifugeTable() {
        super(CentrifugeRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        for (BedrockOreType type : CelestialBedrockOre.getAllTypes()) {

            put(in(Grade.BASE, type), new ItemStack[] {make(Grade.PRIMARY, type), new ItemStack(Items.GRAVEL)});
            put(in(Grade.BASE_ROASTED, type), new ItemStack[] {make(Grade.PRIMARY, type), new ItemStack(Items.GRAVEL)});
            put(in(Grade.BASE_WASHED, type), new ItemStack[] {make(Grade.PRIMARY, type), make(Grade.PRIMARY, type), new ItemStack(Items.GRAVEL)});

            put(in(Grade.PRIMARY_SULFURIC, type), new ItemStack[] {make(Grade.PRIMARY_NOSULFURIC, type, 2), make(Grade.SULFURIC_BYPRODUCT, type, 2)});
            put(in(Grade.PRIMARY_SOLVENT, type), new ItemStack[] {make(Grade.PRIMARY_NOSOLVENT, type, 2), make(Grade.SULFURIC_BYPRODUCT, type, 2), make(Grade.SOLVENT_BYPRODUCT, type, 2)});
            put(in(Grade.PRIMARY_RAD, type), new ItemStack[] {make(Grade.PRIMARY_NORAD, type, 2), make(Grade.SULFURIC_BYPRODUCT, type, 2), make(Grade.SOLVENT_BYPRODUCT, type, 2), make(Grade.RAD_BYPRODUCT, type, 2)});

            put(in(Grade.PRIMARY, type), new ItemStack[] {extract(type.primary1, 1)});
            put(in(Grade.PRIMARY_ROASTED, type), new ItemStack[] {extract(type.primary1, 1)});
            put(in(Grade.PRIMARY_NOSULFURIC, type), new ItemStack[] {extract(type.primary1, 1), make(Grade.CRUMBS, type)});
            put(in(Grade.PRIMARY_NOSOLVENT, type), new ItemStack[] {extract(type.primary1, 1), make(Grade.CRUMBS, type)});
            put(in(Grade.PRIMARY_NORAD, type), new ItemStack[] {extract(type.primary1, 1), make(Grade.CRUMBS, type)});
            put(in(Grade.PRIMARY_FIRST, type), new ItemStack[] {extract(type.primary1, 1), extract(type.primary1, 1), make(Grade.CRUMBS, type, 1)});

            put(in(Grade.SULFURIC_WASHED, type), new ItemStack[] {extract(type.byproductAcid1, 1), make(Grade.CRUMBS, type)});
            put(in(Grade.SOLVENT_WASHED, type), new ItemStack[] {extract(type.byproductSolvent1, 1), make(Grade.CRUMBS, type)});
            put(in(Grade.RAD_WASHED, type), new ItemStack[] {extract(type.byproductRad1, 1), make(Grade.CRUMBS, type)});
        }
    }

    private static ComparableStack in(Grade grade, BedrockOreType type) {
        return new ComparableStack(ModBedrockOreItems.get(grade, type));
    }

    /** hbm-space {@code ItemBedrockOreNew.make(grade, type)}. */
    private static ItemStack make(Grade grade, BedrockOreType type) {
        return ModBedrockOreItems.make(grade, type);
    }

    /** hbm-space {@code ItemBedrockOreNew.make(grade, type, amount)}. */
    private static ItemStack make(Grade grade, BedrockOreType type, int amount) {
        return ModBedrockOreItems.make(grade, type, amount);
    }

    /** hbm-space {@code ItemBedrockOreNew.extract}: the output's fragments, ceil(amount * multiplier), at most 64. */
    private static ItemStack extract(BedrockOreOutput output, double amount) {
        return BedrockOreType.fragment(output, amount);
    }

    /** hbm-space {@code recipes.put(input, outputs)}; the id is the input item's path. */
    private void put(ComparableStack input, ItemStack[] outputs) {
        List<Output> out = new ArrayList<>();
        for (ItemStack stack : outputs) out.add(new ItemOutput(ItemOut.of(stack)));
        register(BuiltInRegistries.ITEM.getKey(input.item).getPath(), new Row(input, out, List.of(), orderAfterBase(Row::order)));
    }
}
