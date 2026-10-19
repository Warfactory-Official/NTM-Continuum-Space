package com.ntmspace.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.inventory.recipes.LiquefactionRecipes.Row;
import com.mojang.datafixers.util.Either;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code LiquefactionRecipes.registerDefaults}: the rows hbm-space adds or changes. {@code Blocks.cobblestone}
 * and {@code Blocks.stone} became {@code KEY_COBBLESTONE} and {@code KEY_STONE}, and packed ice yields 2,000 mB instead
 * of 1,000 (HBM's rows removed by {@code recipe_removals/liquefaction.json}). The id is the input as HBM spells it.
 * Replaces the former {@code gen_recipe_changes_chem.py}'s liquefaction rows.
 */
public final class LiquefactionTable extends RecipeSource<Row> {

    public LiquefactionTable() {
        super(LiquefactionRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        remove("hbm:liquefaction/cobblestone");
        put(tag("c:cobblestones"), new FluidStack(250, Fluids.LAVA));
        remove("hbm:liquefaction/stone");
        put(tag("c:stones"), new FluidStack(250, Fluids.LAVA));
        remove("hbm:liquefaction/packed_ice");
        put(item("minecraft:packed_ice"), new FluidStack(2000, Fluids.WATER));
        put(item("ntmspace:dry_ice"), new FluidStack(1000, Fluids.CARBONDIOXIDE));
        put(item("ntmspace:flesh"), new FluidStack(100, Fluids.BLOOD));
        put(item("hbm:ingot_osmiridium"), new FluidStack(24_000, Fluids.ETHANOL)); // sic: 24,000 mB ethanol, as hbm-space
    }

    /** hbm-space {@code recipes.put(tag, output)}. */
    private void put(TagKey<Item> input, FluidStack output) {
        Identifier location = input.location();
        register("tag_" + location.getNamespace() + "_" + location.getPath().replace('/', '_'), new Row(Either.left(input), output));
    }

    /** hbm-space {@code recipes.put(new ComparableStack(item), output)}. */
    private void put(Item input, FluidStack output) {
        Identifier location = BuiltInRegistries.ITEM.getKey(input);
        register(location.getNamespace().equals("minecraft") ? location.getPath() : location.getNamespace() + "_" + location.getPath(),
                new Row(Either.right(input), output));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    private static TagKey<Item> tag(String id) {
        return TagKey.create(Registries.ITEM, Ids.parse(id));
    }
}
