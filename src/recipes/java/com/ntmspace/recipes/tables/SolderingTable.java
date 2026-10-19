package com.ntmspace.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.SolderingRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemData;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * hbm-space {@code SolderingRecipes.registerDefaults}: the rows hbm-space adds or changes. 1.7's
 * {@code lbsm ? a : b} counts ({@code GeneralConfig.enableLBSM && enableLBSMSimpleCrafting}) are one row per branch: the
 * default ({@code b}) keeps its id with {@code !lbsm_simple_crafting}, the other is {@code <id>.lbsm_simple_crafting}. The high-tier
 * controller swaps its atomic clock for lanthanium capacitors (a changed row: HBM's {@code order}, HBM's row removed by
 * {@code recipe_removals/soldering.json}); the space rows are listed after {@code sat_chip} and go after HBM's rows (no
 * order). 1.7's {@code ModItems.circuit} types are the port's {@code circuit_<type>} items, {@code X.wireFine()} the
 * material's wire. Ids are the output's path, or the output and what tells the row apart. Replaces
 * the former {@code gen_recipe_changes_nuclear.py}'s {@code gen_soldering} and the machines family's {@code hard_drive}.
 */
public final class SolderingTable extends RecipeSource<Row> {

    private static final int LAST = Integer.MAX_VALUE;

    /** 1.7 {@code GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting}. */
    private static final String LBSM = "lbsm_simple_crafting";

    public SolderingTable() {
        super(SolderingRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        // if(!GeneralConfig.enable528): the atomic clock is replaced by lanthanium capacitors (16 / 48 / 32)
        int controllerAdvanced = replacing("hbm:soldering/circuit_controller_advanced");
        remove("hbm:soldering/circuit_controller_advanced");
        remove("hbm:soldering/circuit_controller_advanced.lbsm_simple_crafting");
        lbsm("circuit_controller_advanced", List.of("!enable_528"), controllerAdvanced, lbsm -> new SolderingRecipe(item("hbm:circuit_controller_advanced"), 600, 25_000,
                new FluidStack(Fluids.PERFLUOROMETHYL, 4_000),
                new AStack[] {
                        comp("hbm:circuit_chip_bismoid", lbsm ? 8 : 16),
                        comp("hbm:circuit_capacitor_tantalium", lbsm ? 16 : 48),
                        comp("ntmspace:circuit_capacitor_lanthanium", lbsm ? 8 : 32)},
                new AStack[] {
                        comp("hbm:circuit_controller_chassis", 1),
                        tag("c:ingots/any_hardplastic", 4)},
                new AStack[] {
                        wire("lead", 24)}
        ));

        // SPACE
        add("circuit_aero", List.of(), LAST, new SolderingRecipe(item("ntmspace:circuit_aero"), 300, 1_000,
                new AStack[] {
                        comp("hbm:circuit_chip", 3)},
                new AStack[] {
                        comp("hbm:circuit_advanced", 1),
                        tag("c:ingots/rubber", 4)},
                new AStack[] {
                        wire("lead", 4)}
        ));

        add("hard_drive", List.of(), LAST, new SolderingRecipe(item("ntmspace:hard_drive"), 200, 250,
                new AStack[] {
                        comp("hbm:circuit_chip", 2)},
                new AStack[] {
                        comp("hbm:circuit_pcb", 16)},
                new AStack[] {
                        wire("red_copper", 4)}
        ));
        // for Martian Mode
        add("circuit_capacitor_board_lanthanium", List.of(), LAST, new SolderingRecipe(item("hbm:circuit_capacitor_board"), 200, 300,
                new FluidStack(Fluids.PEROXIDE, 500),
                new AStack[] {
                        comp("ntmspace:circuit_capacitor_lanthanium", 3)},
                new AStack[] {
                        comp("hbm:circuit_pcb", 1)},
                new AStack[] {
                        wire("lead", 3)}
        ));

        lbsm("circuit_bismoid_gaschip", List.of(), LAST, lbsm -> new SolderingRecipe(item("hbm:circuit_bismoid"), 400, 10_000,
                new FluidStack(SpaceFluids.POLYTHYLENE, 1_000),
                new AStack[] {
                        comp("hbm:circuit_chip_bismoid", 4),
                        comp("ntmspace:circuit_gaschip", lbsm ? 1 : 4),
                        comp("hbm:circuit_capacitor", lbsm ? 2 : 8)},
                new AStack[] {
                        comp("hbm:circuit_pcb", 6)},
                new AStack[] {
                        wire("lead", 12)}
        ));

        lbsm("circuit_advanced_gaschip", List.of(), LAST, lbsm -> new SolderingRecipe(item("hbm:circuit_advanced"), 300, 1_000,
                new FluidStack(SpaceFluids.POLYTHYLENE, 250),
                new AStack[] {
                        comp("ntmspace:circuit_gaschip", lbsm ? 1 : 2),
                        comp("hbm:circuit_capacitor", 2)},
                new AStack[] {
                        comp("hbm:circuit_pcb", 4)},
                new AStack[] {
                        wire("lead", 8)}
        ));
    }

    /** hbm-space {@code SolderingRecipe}. */
    private record SolderingRecipe(Item output, int duration, long consumption, FluidStack fluid, AStack[] toppings,
                                   AStack[] pcb, AStack[] solder) {

        SolderingRecipe(Item output, int duration, long consumption, AStack[] toppings, AStack[] pcb, AStack[] solder) {
            this(output, duration, consumption, null, toppings, pcb, solder);
        }
    }

    /** hbm-space {@code recipes.add(recipe)}. */
    private void add(String id, List<String> requires, int order, SolderingRecipe recipe) {
        register(id, new Row(new ItemSpec(recipe.output(), 1, ItemData.NONE), recipe.duration(), recipe.consumption(),
                Optional.ofNullable(recipe.fluid()), List.of(recipe.toppings()), List.of(recipe.pcb()), List.of(recipe.solder()),
                requires, order));
    }

    /**
     * A row with 1.7's {@code lbsm ? a : b} counts: {@code make(false)} under {@code id} with {@code !lbsm_simple_crafting},
     * {@code make(true)} under {@code <id>.lbsm_simple_crafting} with {@code lbsm_simple_crafting}, both also with
     * {@code requires}.
     */
    private void lbsm(String id, List<String> requires, int order, LbsmRow make) {
        add(id, with(requires, "!" + LBSM), order, make.row(false));
        add(id + "." + LBSM, with(requires, LBSM), order, make.row(true));
    }

    private static List<String> with(List<String> requires, String flag) {
        List<String> all = new java.util.ArrayList<>(requires);
        all.add(flag);
        return List.copyOf(all);
    }

    private interface LbsmRow {
        SolderingRecipe row(boolean lbsm);
    }

    /** A changed row: the order of HBM's row it replaces, looked up from HBM's loaded table. */
    private int replacing(String recipeId) {
        return loaded(recipeId).order();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id));
    }

    private static AStack comp(String id, int count) {
        return new ComparableStack(item(id), count);
    }

    private static AStack tag(String id, int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.parse(id)), count);
    }

    /** hbm-space {@code new OreDictStack(MAT.wireFine(), count)}. */
    private static AStack wire(String material, int count) {
        return new MaterialStack(Mats.matByName.get(material), MaterialShapes.WIRE, count);
    }
}
