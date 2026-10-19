package com.ntmspace.recipes.tables;

import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.inventory.recipes.AnnihilatorRecipes.MilestoneRow;
import com.hbm.inventory.recipes.AnnihilatorRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.machine.ItemBlueprints;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code AnnihilatorRecipes.registerDefaults}: the rows hbm-space adds inside {@code if(GeneralConfig.enable528)}
 * (a row's {@code requires}). Keyed by ore name, the id is the tag path with {@code _} for {@code /}; keyed by item, the
 * item's path. Replaces the former {@code gen_recipe_changes_nuclear.py}'s {@code gen_annihilator}.
 */
public final class AnnihilatorTable extends RecipeSource<Row> {

    private static final List<String> requires = List.of("enable_528");

    public AnnihilatorTable() {
        super(AnnihilatorRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        // if(GeneralConfig.enable528) {
        put("c:ingots/aluminium",                       new AnnihilatorRecipe(512, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "aluminium")));
        put("c:ingots/stainless",                       new AnnihilatorRecipe(1024, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "stainless")));
        put(item("ntmspace:circuit_gaschip"),            new AnnihilatorRecipe(512, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "gaschip")));
        // }
    }

    /** hbm-space {@code new AnnihilatorRecipe(new Pair(new BigInteger(amount), payout))}: one milestone. */
    private record AnnihilatorRecipe(long amount, ItemStack payout) {}

    /** hbm-space {@code recipes.put(new ComparableStack(item), recipe)}; the id is the item's name. */
    private void put(Item item, AnnihilatorRecipe recipe) {
        register(BuiltInRegistries.ITEM.getKey(item).getPath(),
                new Row(Optional.of(item), Optional.empty(), Optional.empty(), milestones(recipe), requires));
    }

    /** hbm-space {@code recipes.put(MAT.shape(), recipe)}, keyed by the ore name. */
    private void put(String tag, AnnihilatorRecipe recipe) {
        register(tag.substring(tag.indexOf(':') + 1).replace('/', '_'),
                new Row(Optional.empty(), Optional.of(tag), Optional.empty(), milestones(recipe), requires));
    }

    private static List<MilestoneRow> milestones(AnnihilatorRecipe recipe) {
        return List.of(new MilestoneRow(recipe.amount(), ItemSpec.of(recipe.payout()).getOrThrow()));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }
}
