package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes.Row;
import com.hbm.inventory.recipes.CrystallizerRecipes.ItemOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.Output;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code CentrifugeRecipes.register}: the rows hbm-space adds (the bedrock-ore loops are
 * {@code BedrockOre*} tables). Each row takes the order of the HBM row it follows in hbm-space's list, looked up from
 * HBM's loaded table. {@code oreAll(ore_fire)} is NTM:
 * Space's {@code ntmspace:ores/ore_fire} tag; {@code Mats.MAT_GALLIUM/MAT_ARSENIC.make(bedrock_ore_fragment, n)} are NTM: Space's fragment items. The id is the
 * input item's path, or {@code ore_<name>} for an ore tag. Replaces the former {@code gen_recipe_changes_chem.py}'s centrifuge
 * rows and the ore family's {@code ore_nickel}.
 */
public final class CentrifugeTable extends RecipeSource<Row> {

    public CentrifugeTable() {
        super(CentrifugeRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        // hbm-space puts NI.ore() right after IRON.ore()
        put("ore_nickel", tag("c:ores/nickel"), after("hbm:centrifuge/tag_ores_iron"),
                out("ntmspace:chunk_ore_pentlandite", 2), out("hbm:sulfur", 1), out("hbm:powder_iron", 1), out("minecraft:gravel", 1));

        put("ore_fire", tag("ntmspace:ores/ore_fire"), after("hbm:centrifuge/ore_nether_fire"),
                out("minecraft:blaze_powder", 2), out("hbm:powder_fire", 2), out("hbm:ingot_phosphorus", 1), out("minecraft:gravel", 1));

        put(comp("ntmspace:mineral_fragment_peroxide"), after("hbm:centrifuge/powder_tektite"),
                out("hbm:powder_niobium", 4), out("hbm:powder_cobalt", 2), out("hbm:powder_zirconium", 1), out("hbm:powder_beryllium", 1));
        put(comp("ntmspace:mineral_fragment_nitric"), after("hbm:centrifuge/powder_tektite"),
                out("ntmspace:powder_gallium", 4), out("hbm:powder_beryllium", 2), out("hbm:powder_niobium", 1), out("hbm:fragment_lanthanium", 1));
        put(comp("ntmspace:mineral_fragment_sulfuric"), after("hbm:centrifuge/powder_tektite"),
                out("hbm:powder_beryllium", 4), out("hbm:powder_niobium", 2), out("ntmspace:powder_gallium", 1), out("hbm:fragment_lanthanium", 1));
        put(comp("ntmspace:mineral_fragment_solvent"), after("hbm:centrifuge/powder_tektite"),
                out("hbm:powder_cobalt", 4), out("ntmspace:powder_gallium", 2), out("hbm:powder_niobium", 1), out("hbm:powder_coltan", 1));
        put(comp("ntmspace:mineral_fragment_hydrochloric"), after("hbm:centrifuge/powder_tektite"),
                out("hbm:powder_zirconium", 4), out("hbm:powder_neodymium", 2), out("hbm:powder_niobium", 1), out("hbm:powder_cobalt", 1));
        put(comp("ntmspace:mineral_fragment_schrabidic"), after("hbm:centrifuge/powder_tektite"),
                out("hbm:powder_co60", 1), out("hbm:nugget_bismuth", 1), out("hbm:powder_asbestos", 6), out("hbm:nugget_technetium", 1));

        put(comp("ntmspace:ferric_clay"), after("hbm:centrifuge/powder_ash_coal"),
                out("minecraft:clay_ball", 1), out("minecraft:clay_ball", 1), out("hbm:powder_iron", 1), out("hbm:powder_iron", 1));
        put(comp("ntmspace:ingot_magma"), after("hbm:centrifuge/powder_ash_coal"),
                out("ntmspace:bedrock_ore_fragment_gallium", 4), out("ntmspace:bedrock_ore_fragment_arsenic", 2),
                out("hbm:powder_meteorite_tiny", 2), out("hbm:dust_tiny", 3));

        put(comp("ntmspace:crystal_mineral"), after("hbm:centrifuge/crystal_cobalt"),
                out("ntmspace:mineral_dust", 2), out("hbm:powder_iron", 2), out("hbm:powder_aluminium", 2), out("hbm:powder_lithium_tiny", 1));
        put(comp("ntmspace:crystal_nickel"), after("hbm:centrifuge/crystal_cobalt"),
                out("ntmspace:powder_nickel", 2), out("ntmspace:powder_nickel", 2), out("hbm:powder_iron", 2), out("hbm:powder_titanium", 1));
        put(comp("ntmspace:crystal_niobium"), after("hbm:centrifuge/crystal_cobalt"),
                out("hbm:powder_niobium", 2), out("hbm:powder_niobium", 2), out("hbm:powder_iron", 2), out("ntmspace:nugget_hafnium", 1));
        put(comp("ntmspace:crystal_zinc"), after("hbm:centrifuge/crystal_cobalt"),
                out("ntmspace:powder_zinc", 2), out("ntmspace:powder_zinc", 2), out("hbm:sulfur", 2), out("hbm:powder_aluminium", 1));
        put(comp("ntmspace:nickel_salts"), after("hbm:centrifuge/crystal_cobalt"),
                out("hbm:powder_iron", 2), out("ntmspace:powder_nickel", 2), out("hbm:powder_sodium", 1), out("hbm:sulfur", 1));
    }

    /** hbm-space {@code recipes.put(new ComparableStack(item), outputs)}; the id is the item's path. */
    private void put(ComparableStack input, int order, ItemOut... outputs) {
        put(BuiltInRegistries.ITEM.getKey(input.item).getPath(), input, order, outputs);
    }

    private void put(String id, AStack input, int order, ItemOut... outputs) {
        List<Output> out = new java.util.ArrayList<>();
        for (ItemOut item : outputs) out.add(new ItemOutput(item));
        register(id, new Row(input, out, List.of(), order));
    }

    /** The order of the HBM row this one follows: the stable sort places it right after (ties sort by id). */
    private int after(String recipeId) {
        return loaded(recipeId).order();
    }

    private static ItemOut out(String id, int count) {
        return new ItemOut(item(id), count);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    private static ComparableStack comp(String id) {
        return new ComparableStack(item(id));
    }

    private static TagStack tag(String id) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.parse(id)));
    }
}
