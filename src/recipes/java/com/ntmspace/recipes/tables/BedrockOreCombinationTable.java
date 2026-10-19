package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CombinationRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.ntmspace.bedrock.CelestialBedrockOre;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code CombinationRecipes.registerDefaults}: the {@code CelestialBedrockOre.getAllTypes()} roasting loop
 * (its {@code ItemBedrockOreNew} grades are NTM: Space's {@code bedrock_ore_<grade>_<body>_<suffix>} items). The rows go
 * after HBM's ({@code order} left at its default); HBM's own bedrock rows are removed by
 * {@code recipe_removals/bedrock_ore.json}.
 */
public final class BedrockOreCombinationTable extends RecipeSource<Row> {

    public BedrockOreCombinationTable() {
        super(CombinationRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        for (BedrockOreType type : CelestialBedrockOre.getAllTypes()) {
            put(in(Grade.BASE, type), new Pair(ModBedrockOreItems.make(Grade.BASE_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50)));
            put(in(Grade.PRIMARY, type), new Pair(ModBedrockOreItems.make(Grade.PRIMARY_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50)));
            put(in(Grade.SULFURIC_BYPRODUCT, type), new Pair(ModBedrockOreItems.make(Grade.SULFURIC_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50)));
            put(in(Grade.SOLVENT_BYPRODUCT, type), new Pair(ModBedrockOreItems.make(Grade.SOLVENT_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50)));
            put(in(Grade.RAD_BYPRODUCT, type), new Pair(ModBedrockOreItems.make(Grade.RAD_ROASTED, type), new FluidStack(Fluids.VITRIOL, 50)));
        }
    }

    private static ComparableStack in(Grade grade, BedrockOreType type) {
        return new ComparableStack(ModBedrockOreItems.get(grade, type));
    }

    private record Pair(ItemStack item, FluidStack fluid) {}

    /** hbm-space {@code recipes.put(input, output)}; the id is the input item's path. */
    private void put(ComparableStack input, Pair output) {
        register(BuiltInRegistries.ITEM.getKey(input.item).getPath(), new Row(input,
                Optional.ofNullable(output.item()).map(stack -> ItemSpec.of(stack).getOrThrow()),
                Optional.ofNullable(output.fluid()), Integer.MAX_VALUE));
    }
}
