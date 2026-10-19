package com.ntmspace.recipes.tables;

import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.ShredderRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.mojang.datafixers.util.Either;
import com.ntmspace.registry.BedrockOreItems;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * hbm-space {@code ShredderRecipes.registerDefaults}: its one bedrock ore line,
 * {@code setRecipe(new ItemStack(ModItems.bedrock_ore, 1, OreDictionary.WILDCARD_VALUE), new ItemStack(Blocks.gravel))}.
 * Every metadata of 1.7's {@code bedrock_ore} is one of NTM: Space's flattened items, so the wildcard is their tag
 * ({@code ntmspace:bedrock_ores}).
 */
public final class BedrockOreShredderTable extends RecipeSource<Row> {

    public BedrockOreShredderTable() {
        super(ShredderRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        TagKey<net.minecraft.world.item.Item> bedrockOres = TagKey.create(Registries.ITEM,
                Ids.of(BedrockOreItems.BEDROCK_ORES.namespace(), BedrockOreItems.BEDROCK_ORES.path()));
        register("bedrock_ore", new Row(Either.left(bedrockOres), ItemOut.of(new ItemStack(Items.GRAVEL))));
    }
}
