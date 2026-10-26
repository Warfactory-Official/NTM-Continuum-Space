package com.ntmspace.recipes.tables;

import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.ShredderRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.mojang.datafixers.util.Either;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code ShredderRecipes.registerDefaults}: the rows hbm-space adds (HBM's rows come first, the first match
 * wins). hbm-space's {@code new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE)} for an ore that exists on several
 * bodies is NTM: Space's {@code ntmspace:ores/<ore>} tag (every body's form); a plain block key is metadata 0, the Kerbin
 * form. The rows' ids are the input (the item path, {@code minecraft_<path>} for vanilla) or {@code tag_<path>} for the
 * {@code OreDictManager} key loops. The stone keys keep 1.7's descriptive row ids ({@code stone},
 * {@code cobblestone}, {@code sand}). Replaces the former {@code gen_recipe_changes_metal.py}'s shredder rows and the shredder
 * rows of the machines and ore families (ore_fire, ore_glowstone, ore_mineral, ore_nickel, ore_quartz, laythe_coral_block,
 * vinyl/pvc, bobbleheads).
 */
public final class ShredderTable extends RecipeSource<Row> {

    public ShredderTable() {
        super(ShredderRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        /* Primary recipes */
        setRecipe(item("minecraft:wheat"), item("ntmspace:flour"), 1);
        setRecipe(item("hbm:reinforced_light"), item("minecraft:glowstone_dust"), 4);
        setRecipe("ore_glowstone", ores("ore_glowstone"), item("minecraft:glowstone_dust"), 4);
        setRecipe(item("ntmspace:ore_quartz"), item("hbm:powder_quartz"), 2);
        // new ItemStack(ore_fire, 1, WILDCARD) (its plain ore_fire sibling is the same input: setRecipe keeps the first)
        setRecipe("ore_fire", ores("ore_fire"), item("hbm:powder_fire"), 6);
        // hbm-space has no ore_nether_fire row (1.7's setRecipe(ModBlocks.ore_nether_fire, powder_fire x6))
        remove("hbm:shredder/ore_nether_fire");
        setRecipe(item("ntmspace:ore_gas_empty"), item("minecraft:gravel"), 1);
        setRecipe("ore_rare", ores("ore_rare"), item("hbm:powder_desh_mix"), 1); // every body's form (HBM's hbm:ore_rare row is Kerbin's)
        setRecipe(item("ntmspace:crystal_cleaned"), item("ntmspace:mineral_dust"), 4);
        setRecipe(item("ntmspace:laythe_coral_block"), item("hbm:powder_calcium"), 4);

        setRecipe("ore_mineral", ores("ore_mineral"), item("ntmspace:mineral_dust"), 1); // it was deserved

        setRecipe(item("ntmspace:bean_roast"), item("ntmspace:powder_coffee"), 1);

        // stone_resource LIMESTONE -> powder_calcium x4 is dead in 1.7 (LIMESTONE -> powder_limestone x4 was set earlier)
        setRecipe(item("ntmspace:stone_resource_calcium"), item("hbm:powder_calcium"), 6);
        setRecipe("ore_aluminium", ores("ore_aluminium"), item("hbm:chunk_ore_cryolite"), 2);
        setRecipe("ore_nickel", ores("ore_nickel"), item("ntmspace:chunk_ore_pentlandite"), 2);

        setRecipe(item("ntmspace:vinyl_log"), item("ntmspace:powder_rubber"), 4);
        setRecipe(item("ntmspace:pvc_log"), item("ntmspace:powder_pvc"), 4);
        setRecipe(item("ntmspace:vinyl_planks"), item("ntmspace:powder_rubber"), 1);
        setRecipe(item("ntmspace:pvc_planks"), item("ntmspace:powder_pvc"), 1);

        // the KEY_STONE / KEY_COBBLESTONE / KEY_SAND loops (after the log/plank/sapling loops), keeping 1.7's descriptive row ids
        setRecipe("stone", tag("c:stones"), item("minecraft:gravel"), 1);
        setRecipe("cobblestone", tag("c:cobblestones"), item("minecraft:gravel"), 1);
        setRecipe("sand", tag("c:sands"), item("hbm:dust"), 2);

        /* Shredding bobbleheads: hbm-space's own BobbleTypes, each type.scrap BOARD_BLANK (the others are HBM's rows) */
        for (String type : new String[] {"gwen", "juice", "jamesh_2", "mrkimkimora"}) {
            setRecipe(item("ntmspace:bobblehead_" + type), item("hbm:scrap_plastic_board_blank"), 1);
        }

        /* crystals */
        setRecipe(item("ntmspace:crystal_nickel"), item("ntmspace:powder_nickel"), 3);
        setRecipe(item("ntmspace:crystal_niobium"), item("hbm:powder_niobium"), 3);
        setRecipe(item("ntmspace:crystal_zinc"), item("ntmspace:powder_zinc"), 3);

        // SpaceRawOreRecipes: Fortune multiplies raw drops, so processing keeps 667/1000 of the whole ore result.
        for (var raw : new String[][] {{"nickel_pure", "ntmspace:chunk_ore_pentlandite"},
                {"zinc", "ntmspace:powder_zinc"}, {"australium", "hbm:powder_australium"},
                {"niobium", "hbm:powder_niobium"}, {"lanthanum", "hbm:powder_lanthanium"}}) {
            register("raw_materials_" + raw[0], new Row(Either.left(tag("c:raw_materials/" + raw[0])),
                    new ItemOut(item(raw[1]), 2), 0.667F));
        }
        setRecipe("ores_niobium", tag("c:ores/niobium"), item("hbm:powder_niobium"), 2);
        setRecipe("ores_lanthanum", tag("c:ores/lanthanum"), item("hbm:powder_lanthanium"), 2);

        /* 1.7 registerPost ingot/dust sweep for Space metals (HBM's rows stay NTM's). */
        for (var sweep : new String[][] {{"zinc", "ntmspace:powder_zinc"}, {"gallium", "ntmspace:powder_gallium"}}) {
            register("ingots_" + sweep[0],
                    new Row(Either.right(item("ntmspace:ingot_" + sweep[0])), new ItemOut(item(sweep[1]), 1)));
            register("dusts_" + sweep[0],
                    new Row(Either.right(item(sweep[1])), new ItemOut(item("hbm:dust"), 1)));
        }
    }

    /** hbm-space {@code ShredderRecipes.setRecipe(item, new ItemStack(out, count))}; the id is the input item. */
    private void setRecipe(Item in, Item out, int count) {
        var id = BuiltInRegistries.ITEM.getKey(in);
        register(id.getNamespace().equals("minecraft") ? "minecraft_" + id.getPath() : id.getPath(),
                new Row(Either.right(in), new ItemOut(out, count)));
    }

    /** {@code setRecipe} for a tag input (a wildcard ore or an ore-dictionary key loop). */
    private void setRecipe(String id, TagKey<Item> in, Item out, int count) {
        register(id, new Row(Either.left(in), new ItemOut(out, count)));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    private static TagKey<Item> tag(String id) {
        return TagKey.create(Registries.ITEM, Ids.parse(id));
    }

    /** hbm-space {@code new ItemStack(ModBlocks.<ore>, 1, OreDictionary.WILDCARD_VALUE)}: every body's form. */
    private static TagKey<Item> ores(String ore) {
        return tag("ntmspace:ores/" + ore);
    }
}
