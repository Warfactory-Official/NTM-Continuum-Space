package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.ConstructionRow;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.OutputRow;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.OverlayType;
import com.hbm.inventory.recipes.loader.ItemData;
import com.hbm.inventory.recipes.loader.ItemSpec;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code AnvilRecipes.registerConstruction*}: the construction rows hbm-space adds or changes. Each takes the
 * order of the HBM row it follows, looked up from HBM's loaded table (a changed row, {@code machine_ashpit}, keeps HBM's
 * and replaces it through {@code recipe_removals/anvil_construction.json}). The atmosphere machines are registered by
 * hbm-space's {@code AtmosphereMachines}, after the deuterium tower. Replaces the former
 * {@code gen_recipe_changes_metal.py}'s construction rows.
 */
public final class AnvilConstructionTable extends RecipeSource<ConstructionRow> {

    public AnvilConstructionTable() {
        super(AnvilRecipes.CONSTRUCTION.table);
    }

    @Override
    protected void registerDefaults() {
        // registerConstruction: plates (nickel after titanium, stainless after steel)
        add(3, after("hbm:anvil_construction/plate_titanium"), null, out("ntmspace:plate_nickel", 1), tag("c:ingots/nickel_pure", 1));
        add(3, after("hbm:anvil_construction/plate_steel"), null, out("ntmspace:plate_stainless", 1), tag("c:ingots/stainless", 1));
        // the Mats.orderedList wire loop (tier 4): zinc's WIRE autogen, after zirconium
        add(4, after("hbm:anvil_construction/wire_fine_zirconium"), null, out("ntmspace:wire_fine_zinc", 8), tag("c:ingots/zinc", 1));
        // coke dusts -> coke (after COAL.dust() -> coal)
        add(3, after("hbm:anvil_construction/coal"), null, out("hbm:coke_coal", 1), tag("c:dusts/coal_coke", 1));
        add(3, after("hbm:anvil_construction/coal"), null, out("hbm:coke_lignite", 1), tag("c:dusts/lignite_coke", 1));
        add(3, after("hbm:anvil_construction/coal"), null, out("hbm:coke_petroleum", 1), tag("c:dusts/pet_coke", 1));
        // STAINLESS ingot -> 4 deco_stainless (after deco_tungsten)
        add(1, after("hbm:anvil_construction/deco_tungsten"), OverlayType.CONSTRUCTION, out("ntmspace:deco_stainless", 4), tag("c:ingots/stainless", 1));

        // registerConstructionRecipes: machine_dish_controller, after the assembly machine
        add(3, after("hbm:anvil_construction/machine_assembly_machine"), null, out("ntmspace:dish_control", 1),
                tag("minecraft:planks", 6), tag("c:plates/steel", 8), comp("hbm:circuit_analog", 1), comp("hbm:circuit_vacuum_tube", 8),
                comp("hbm:pole_satellite_receiver", 1));
        // machine_ashpit: KEY_STONE x8 (was Blocks.stone), a changed row: HBM's own order
        remove("hbm:anvil_construction/machine_ashpit");
        add(2, after("hbm:anvil_construction/machine_ashpit"), null, out("hbm:machine_ashpit", 1), tag("c:stones", 8), tag("c:plates/steel", 2), tag("c:ingots/iron", 4));
        // AtmosphereMachines, after the deuterium tower
        add(2, after("hbm:anvil_construction/machine_deuterium_tower"), null, out("ntmspace:machine_atmo_tower", 1),
                mat("steel", MaterialShapes.SHELL, 6), tag("c:plates/stainless", 8), comp("hbm:concrete_smooth", 4), comp("hbm:heater_heatex", 1));
        add(2, after("hbm:anvil_construction/machine_deuterium_tower"), null, out("ntmspace:machine_atmo_vent", 1),
                mat("steel", MaterialShapes.SHELL, 4), tag("c:plates/stainless", 4), comp("hbm:concrete_smooth", 4), comp("hbm:turbine_titanium", 1));
        add(2, after("hbm:anvil_construction/machine_deuterium_tower"), null, out("ntmspace:machine_atmo_emitter", 1),
                mat("steel", MaterialShapes.PIPE, 6), mat("stainless", MaterialShapes.CASTPLATE, 8), tag("c:ingots/any_plastic", 4), comp("hbm:turbine_titanium", 1));
        // flask_infusion NITAN (after plate_armor_dnt)
        add(2, after("hbm:anvil_construction/plate_armor_dnt"), null, out("ntmspace:flask_infusion_nitan", 1),
                comp("hbm:glass_quartz", 3), comp("hbm:pill_herbal", 2), comp("hbm:powder_magic", 2));

        // registerConstructionRecycling: 4 deco_stainless -> stainless ingot (after deco_asbestos)
        add(1, after("hbm:anvil_construction/ingot_asbestos"), OverlayType.RECYCLING, out("ntmspace:ingot_stainless", 1), comp("ntmspace:deco_stainless", 4));
    }

    /** hbm-space {@code constructionRecipes.add(new AnvilConstructionRecipe(inputs, output).setTier(tier))}; the id is the output's path. */
    private void add(int tier, int order, OverlayType overlay, ItemSpec output, AStack... inputs) {
        register(BuiltInRegistries.ITEM.getKey(output.item()).getPath(), new ConstructionRow(List.of(inputs),
                List.of(new OutputRow(output, 1F)), tier, Optional.empty(), Optional.ofNullable(overlay), List.of(), order));
    }

    /** The order of the HBM row this one follows (or, for a changed row, replaces); ties sort by id. */
    private int after(String recipeId) {
        return loaded(recipeId).order();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    private static ItemSpec out(String id, int count) {
        return new ItemSpec(item(id), count, ItemData.NONE);
    }

    private static AStack comp(String id, int count) {
        return new ComparableStack(item(id), count);
    }

    private static AStack tag(String id, int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.parse(id)), count);
    }

    private static AStack mat(String material, MaterialShapes shape, int count) {
        return new MaterialStack(Mats.matByName.get(material), shape, count);
    }
}
