package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.ItemOutput;
import com.hbm.inventory.recipes.CrystallizerRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code CrystallizerRecipes.registerDefaults}: the rows hbm-space adds or changes. A changed row (cobblestone
 * and the three black-dye oils) replaces HBM's under a new id ({@code recipe_removals/crystallizer.json}) and keeps HBM's
 * {@code order}; an added row takes the order of the HBM row it follows, both looked up from
 * HBM's loaded table. hbm-space's acids:
 * {@code sulfur} 500 mB sulfuric acid, {@code nitric} 500 mB nitric acid, {@code organic} 500 mB solvent, {@code chloric}
 * 500 mB HCl, {@code schrabidic} 1,000 mB schrabidic acid, the default 500 mB peroxide. {@code oreAll}/wildcard ores are
 * NTM: Space's {@code ntmspace:ores/<ore>} tags. Replaces the former {@code gen_recipe_changes_chem.py}'s crystallizer rows and
 * the per-body ore rows ({@code ore_zinc}, {@code ore_niobium}, {@code ore_mineral}).
 */
public final class CrystallizerTable extends RecipeSource<Row> {

    private static final int baseTime = 600;
    private static final int utilityTime = 100;
    private static final int mixingTime = 20;
    private static final int mineralTime = 300;

    private static final FluidStack sulfur = new FluidStack(Fluids.SULFURIC_ACID, 500);
    private static final FluidStack nitric = new FluidStack(Fluids.NITRIC_ACID, 500);
    private static final FluidStack organic = new FluidStack(Fluids.SOLVENT, 500);
    private static final FluidStack chloric = new FluidStack(SpaceFluids.HCL, 500);
    private static final FluidStack schrabidic = new FluidStack(Fluids.SCHRABIDIC, 1000);

    public CrystallizerTable() {
        super(CrystallizerRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        // hbm-space's LI.ore() line is 1.7's, unchanged: HBM's row (tag_ores_lithium_sulfuric_acid), not repeated here.
        // ZI, NB and ore_mineral follow CO.ore() (the per-body ore families' ids; equal orders sort by id)
        registerRecipe("ore_zinc", tag("c:ores/zinc"), new CrystallizerRecipe(item("ntmspace:crystal_zinc"), baseTime).prod(0.05F), nitric, after("hbm:crystallizer/tag_ores_cobalt_sulfuric_acid"));
        registerRecipe("ore_niobium", tag("c:ores/niobium"), new CrystallizerRecipe(item("ntmspace:crystal_niobium"), baseTime).prod(0.05F), sulfur, after("hbm:crystallizer/tag_ores_cobalt_sulfuric_acid"));
        registerRecipe("ore_mineral", tag("ntmspace:ores/ore_mineral"), new CrystallizerRecipe(item("ntmspace:crystal_mineral"), baseTime).prod(0.05F), after("hbm:crystallizer/tag_ores_cobalt_sulfuric_acid")); //temp

        // P_RED.ore() is oreAll(ore_fire): every body's ore_fire, after HBM's phosphorus row
        registerRecipe("ore_fire_peroxide", tag("ntmspace:ores/ore_fire"), new CrystallizerRecipe(item("hbm:crystal_phosphorus"), baseTime).prod(0.05F), after("hbm:crystallizer/tag_ores_lead_peroxide"));
        registerRecipe("crystal_mineral_peroxide", comp("ntmspace:crystal_mineral"), new CrystallizerRecipe(item("hbm:crystal_diamond"), baseTime).prod(0.05F), after("hbm:crystallizer/gravel_diamond_peroxide"));
        // Blocks.cobblestone -> KEY_COBBLESTONE (changed row)
        remove("hbm:crystallizer/minecraft_cobblestone_peroxide");
        registerRecipe("tag_cobblestones_peroxide", tag("c:cobblestones"), new CrystallizerRecipe(item("hbm:reinforced_stone"), utilityTime), after("hbm:crystallizer/minecraft_cobblestone_peroxide"));

        // mineral_dust -> mineral_fragment metas 0..5, one acid each (after 1.7's legacy bedrock-ore loop)
        registerRecipe("mineral_dust_peroxide", comp("ntmspace:mineral_dust"), new CrystallizerRecipe(item("ntmspace:mineral_fragment_peroxide"), mineralTime), after("hbm:crystallizer/scrap_plastic_card_processor_peroxide"));
        registerRecipe("mineral_dust_nitric_acid", comp("ntmspace:mineral_dust"), new CrystallizerRecipe(item("ntmspace:mineral_fragment_nitric"), mineralTime), nitric, after("hbm:crystallizer/scrap_plastic_card_processor_peroxide"));
        registerRecipe("mineral_dust_sulfuric_acid", comp("ntmspace:mineral_dust"), new CrystallizerRecipe(item("ntmspace:mineral_fragment_sulfuric"), mineralTime), sulfur, after("hbm:crystallizer/scrap_plastic_card_processor_peroxide"));
        registerRecipe("mineral_dust_solvent", comp("ntmspace:mineral_dust"), new CrystallizerRecipe(item("ntmspace:mineral_fragment_solvent"), mineralTime), organic, after("hbm:crystallizer/scrap_plastic_card_processor_peroxide"));
        registerRecipe("mineral_dust_hcl", comp("ntmspace:mineral_dust"), new CrystallizerRecipe(item("ntmspace:mineral_fragment_hydrochloric"), mineralTime), chloric, after("hbm:crystallizer/scrap_plastic_card_processor_peroxide"));
        registerRecipe("mineral_dust_schrabidic", comp("ntmspace:mineral_dust"), new CrystallizerRecipe(item("ntmspace:mineral_fragment_schrabidic"), mineralTime), schrabidic, after("hbm:crystallizer/scrap_plastic_card_processor_peroxide"));

        // after powder_impure_osmiridium (schrabidic acid)
        registerRecipe("saltleaf_scutterblood", comp("ntmspace:saltleaf"), new CrystallizerRecipe(item("hbm:gem_sodalite"), baseTime).setReq(5), new FluidStack(SpaceFluids.SCUTTERBLOOD, 1000), after("hbm:crystallizer/powder_impure_osmiridium_schrabidic"));
        registerRecipe("tag_ingots_malachite_coppersulfate", tag("c:ingots/malachite"), new CrystallizerRecipe(item("hbm:crystal_copper"), baseTime).prod(0.1F), new FluidStack(SpaceFluids.COPPERSULFATE, 350), after("hbm:crystallizer/powder_impure_osmiridium_schrabidic"));
        registerRecipe("nickel_salts_nitric_acid", comp("ntmspace:nickel_salts"), new CrystallizerRecipe(item("ntmspace:crystal_nickel"), baseTime), nitric, after("hbm:crystallizer/powder_impure_osmiridium_schrabidic"));
        registerRecipe("leaf_rubber_hcl", comp("ntmspace:leaf_rubber"), new CrystallizerRecipe(item("hbm:ingot_rubber"), baseTime).setReq(64), chloric, after("hbm:crystallizer/powder_impure_osmiridium_schrabidic"));

        // black dye: COAL.dust() -> ANY_COAL_COKE.dust() (coke dusts and coal dust), for each of the three dye oils (changed rows)
        remove("hbm:crystallizer/tag_dusts_coal_woodoil");
        registerRecipe("tag_dusts_any_coal_coke_woodoil", tag("c:dusts/any_coal_coke"), new CrystallizerRecipe(item("hbm:chemical_dye_black"), 4, mixingTime).prod(0.15F), new FluidStack(Fluids.WOODOIL, 100), after("hbm:crystallizer/tag_dusts_coal_woodoil"));
        remove("hbm:crystallizer/tag_dusts_coal_fishoil");
        registerRecipe("tag_dusts_any_coal_coke_fishoil", tag("c:dusts/any_coal_coke"), new CrystallizerRecipe(item("hbm:chemical_dye_black"), 4, mixingTime).prod(0.15F), new FluidStack(Fluids.FISHOIL, 100), after("hbm:crystallizer/tag_dusts_coal_fishoil"));
        remove("hbm:crystallizer/tag_dusts_coal_lightoil");
        registerRecipe("tag_dusts_any_coal_coke_lightoil", tag("c:dusts/any_coal_coke"), new CrystallizerRecipe(item("hbm:chemical_dye_black"), 4, mixingTime).prod(0.15F), new FluidStack(Fluids.LIGHTOIL, 100), after("hbm:crystallizer/tag_dusts_coal_lightoil"));
    }

    /** hbm-space {@code registerRecipe(input, recipe)}: 500 mB of hydrogen peroxide. */
    private void registerRecipe(String id, AStack input, CrystallizerRecipe recipe, int order) {
        registerRecipe(id, input, recipe, new FluidStack(Fluids.PEROXIDE, 500), order);
    }

    /** hbm-space {@code registerRecipe(input, recipe, acid)}; {@code setReq} is the input's count. */
    private void registerRecipe(String id, AStack input, CrystallizerRecipe recipe, FluidStack acid, int order) {
        AStack counted = recipe.itemAmount == 1 ? input : input.count(recipe.itemAmount);
        register(id, new Row(counted, new FluidStack(acid.type, acid.fill), new ItemOutput(recipe.output), recipe.duration,
                recipe.productivity, Optional.empty(), order));
    }

    /** The order of the HBM row this one follows (or, for a changed row, replaces): ties sort by id. */
    private int after(String recipeId) {
        return loaded(recipeId).order();
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

    /** hbm-space {@code CrystallizerRecipe}: an output, a duration, {@code prod} and {@code setReq}. */
    private static final class CrystallizerRecipe {
        final ItemOut output;
        final int duration;
        int itemAmount = 1;
        float productivity = 0F;

        CrystallizerRecipe(Item output, int duration) { this(output, 1, duration); }

        CrystallizerRecipe(Item output, int count, int duration) {
            this.output = new ItemOut(output, count);
            this.duration = duration;
        }

        CrystallizerRecipe setReq(int amount) {
            this.itemAmount = amount;
            return this;
        }

        CrystallizerRecipe prod(float productivity) {
            this.productivity = productivity;
            return this;
        }
    }
}
