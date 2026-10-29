package com.ntmspace.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemRBMKPellet;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The "nuclear" group of the recipe-change family: hbm-space's changes to base HBM's SILEX, outgasser, plasma forge,
 * arc welder, soldering and annihilator tables (rows generated from {@code src/recipes}), checked
 * row by row through each table's own lookup against values taken from hbm-space's recipe classes. SILEX expectations
 * are hbm-space's loop formulas in Java int arithmetic ({@code 2 / 50 + i * 2}); a weight-0 output (never rolled by
 * 1.7's {@code WeightedRandom}) is not a row entry, so stage 0/5 rows stay HBM's.
 */
public final class RecipeChangeNuclearGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function SILEX = TESTS.function("recipe_change_nuclear_silex", 40,
            () -> RecipeChangeNuclearGameTests::silex);
    public static final GameTests.Function OUTGASSER = TESTS.function("recipe_change_nuclear_outgasser", 40,
            () -> RecipeChangeNuclearGameTests::outgasser);
    public static final GameTests.Function PLASMA_FORGE = TESTS.function("recipe_change_nuclear_plasma_forge", 40,
            () -> RecipeChangeNuclearGameTests::plasmaForge);
    public static final GameTests.Function ARC_WELDER = TESTS.function("recipe_change_nuclear_arc_welder", 40,
            () -> RecipeChangeNuclearGameTests::arcWelder);
    public static final GameTests.Function SOLDERING = TESTS.function("recipe_change_nuclear_soldering", 40,
            () -> RecipeChangeNuclearGameTests::soldering);
    public static final GameTests.Function ANNIHILATOR = TESTS.function("recipe_change_nuclear_annihilator", 40,
            () -> RecipeChangeNuclearGameTests::annihilator);

    public static final GameTests.Function STORAGE_DRUM = TESTS.function("recipe_change_nuclear_storage_drum", 40,
            () -> RecipeChangeNuclearGameTests::storageDrum);

    private RecipeChangeNuclearGameTests() {
    }

    // ------------------------------------------------------------------------------------------------ storage drum

    /**
     * hbm-space's {@code TileEntityStorageDrum} picks a short waste's liquid/gas by
     * {@code ShortWasteClass.values()[meta % 5]}, and its short class list inserts AMERICIUM241 before SCHRABIDIUM and
     * appends BERKELIUM247, CURIUM244, CURIUM245. So with NTM: Space installed Sa-326 (meta 8 -> 3, Pu-239) gives 250/1000
     * where plain HBM (1.7: meta 7 -> 2, Np-237) gives 150/500; tiny waste a tenth. The addon's own classes:
     * Am-241 7 -> Np-237 150/500, Bk-247 9 -> Pu-240 350/1000, Cm-244 10 -> U-235 0/100, Cm-245 11 -> U-233 50/100.
     */
    private static void storageDrum(net.minecraft.gametest.framework.GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        Object[][] rows = {
                {"hbm:nuclear_waste_short_schrabidium", 250, 1000},
                {"hbm:nuclear_waste_short_tiny_schrabidium", 25, 100},
                {"ntmspace:nuclear_waste_short_americium241", 150, 500},
                {"ntmspace:nuclear_waste_short_berkelium247", 350, 1000},
                {"ntmspace:nuclear_waste_short_curium244", 0, 100},
                {"ntmspace:nuclear_waste_short_curium245", 50, 100}};
        for (Object[] row : rows) {
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getOptional(Ids.parse((String) row[0])).orElse(null);
            if (item == null) {
                errors.add(row[0] + " is not registered");
                continue;
            }
            int[] waste = com.hbm.inventory.recipes.StorageDrumRecipes.getWaste(
                    new net.minecraft.world.item.ItemStack(item));
            if (waste == null) errors.add(row[0] + " has no storage drum row");
            else if (waste[1] != (int) row[1] || waste[2] != (int) row[2])
                errors.add(
                        row[0] + " gives " + waste[1] + "/" + waste[2] + ", hbm-space gives " + row[1] + "/" + row[2]);
        }
        long saRows = com.hbm.inventory.recipes.StorageDrumRecipes.getAll().stream()
                                                                  .filter(r -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                                                                          r.input().getItem()).toString()
                                                                                                                                   .equals("hbm:nuclear_waste_short_schrabidium"))
                                                                  .count();
        if (saRows != 1)
            errors.add("Sa-326 short waste has " + saRows + " drum rows, expected exactly 1 (HBM's removed)");
        helper.assertTrue(errors.isEmpty(), String.join("; ", errors));
        helper.succeed();
    }

    public static void init() {
    }

    // ------------------------------------------------------------------------------------------------ SILEX

    private static final String XE = "hbm:powder_xe135_tiny";
    private static final String WT = "hbm:nuclear_waste_short_tiny_";
    private static final String CM = "ntmspace:nugget_cm_mix";

    private static void silex(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        checkSilex(errors, item("ntmspace:ingot_cm_mix"), 900, 100, "visible", "ntmspace:nugget_cm244", 3,
                "ntmspace:nugget_cm245", 6);
        // hbm-space's tier ints are EnumWavelengths.values()[n] with XRAY inserted: 4 = X-ray, 5 = gamma (1.7's 4)
        checkSilex(errors, ItemFluidIcon.make(Fluids.DEATH), 1000, 1000, "gamma", "hbm:powder_impure_osmiridium", 1);
        for (int i = 0; i < 5; i++) {
            String pu = i < 1 ? "hbm:nugget_pu239" : "hbm:nugget_pu_mix";
            checkSilex(errors, pellet("hbm:rbmk_pellet_lep", i), 600, 100, "ir", "hbm:nugget_plutonium_fuel",
                    84 - i * 14,
                    pu, 6 + i * 2, CM, 2 / 50 + i * 2, WT + "plutonium239", 7 + 8 * i, WT + "plutonium240", 3 + 4 * i);
            checkSilex(errors, pellet("hbm:rbmk_pellet_lep", i + 5), 600, 100, "ir", XE, 1, "hbm:nugget_plutonium_fuel",
                    83 - i * 14,
                    pu, 6 + i * 2, CM, 2 / 50 + i * 2, WT + "plutonium239", 7 + 8 * i, WT + "plutonium240", 3 + 4 * i);
            checkSilex(errors, pellet("hbm:rbmk_pellet_mep", i), 600, 100, "ir", "hbm:nugget_pu_mix", 85 - i * 20,
                    CM, 2 / 50 + i * 3, WT + "plutonium239", 10 + 10 * i, WT + "plutonium240", 5 + 5 * i);
            checkSilex(errors, pellet("hbm:rbmk_pellet_mep", i + 5), 600, 100, "ir", XE, 1, "hbm:nugget_pu_mix",
                    84 - i * 20,
                    CM, 2 / 50 + i * 3, WT + "plutonium239", 10 + 10 * i, WT + "plutonium240", 5 + 5 * i);
            checkSilex(errors, pellet("hbm:rbmk_pellet_hep", i), 600, 100, "ir", "hbm:nugget_pu239", 85 - i * 20,
                    CM, 2 / 50 + i * 4, WT + "plutonium239", 15 + 20 * i);
            checkSilex(errors, pellet("hbm:rbmk_pellet_hep", i + 5), 600, 100, "ir", XE, 1, "hbm:nugget_pu239",
                    84 - i * 20,
                    CM, 2 / 50 + i * 4, WT + "plutonium239", 15 + 20 * i);
            checkSilex(errors, pellet("hbm:rbmk_pellet_hep241", i), 600, 100, "visible", "hbm:nugget_pu241",
                    85 - i * 20,
                    CM, 2 / 50 + i * 4 + 1 / 2, WT + "plutonium241", 15 + 20 * i);
            checkSilex(errors, pellet("hbm:rbmk_pellet_hep241", i + 5), 600, 100, "visible", XE, 1, "hbm:nugget_pu241",
                    84 - i * 20,
                    CM, 2 / 50 + i * 4 + 1 / 2, WT + "plutonium241", 15 + 20 * i);
            for (int stage : new int[]{i, i + 5}) {
                Object[] six = {"hbm:undefined", 1, "hbm:undefined", 1, "hbm:undefined", 1, "hbm:undefined", 1, "hbm:undefined", 1,
                        "hbm:undefined", 1};
                checkSilex(errors, pellet("hbm:rbmk_pellet_drx", stage), 600, 100, "gamma", six);
            }
            checkSilex(errors, pellet("hbm:rbmk_pellet_hea242", i), 600, 100, X, "hbm:nugget_am242", 100 - i * 20,
                    "ntmspace:nugget_bk247", 1 + 10 * i / 50, WT + "americium242", 4 + 6 * i);
            checkSilex(errors, pellet("hbm:rbmk_pellet_hea242", i + 5), 600, 100, X, "hbm:nugget_am242", 100 - i * 20,
                    "ntmspace:nugget_bk247", 1 + 10 * i / 50, XE, 3, WT + "americium242", 4 + 6 * i);
            // the families hbm-space adds in the same loop (SILEXRecipe(600, 100, 4): X-ray), each with its xenon twin
            String a242 = WT + "americium242", a241 = WS + "americium241", b247 = WS + "berkelium247";
            String c244 = WS + "curium244", c245 = WS + "curium245";
            family(errors, "hbm:rbmk_pellet_lea", i, "hbm:nugget_americium_fuel", 100 - i * 20, "ntmspace:nugget_bk247",
                    1 + 10 * i / 90,
                    a242, 1 + 3 * (i / 2), a241, 1 + (2 + 1 / 2) * (i / 2));
            family(errors, "hbm:rbmk_pellet_mea", i, "hbm:nugget_am_mix", 100 - i * 20, "ntmspace:nugget_bk247",
                    1 + 10 * i / 70,
                    a242, 2 + 3 * i, a241, 2 + 2 * i);
            family(errors, "hbm:rbmk_pellet_hea241", i, "hbm:nugget_am241", 100 - i * 20, "ntmspace:nugget_bk247",
                    1 + 10 * i / 50,
                    a241, 4 + 6 * i);
            family(errors, "ntmspace:rbmk_pellet_bk247", i, "ntmspace:nugget_bk247", 100 - i * 20,
                    "ntmspace:nugget_cf251", 1 + 10 * i / 40,
                    b247, 4 + 6 * i);
            family(errors, "ntmspace:rbmk_pellet_lecm", i, "ntmspace:nugget_cm_fuel", 100 - i * 20, "hbm:nugget_pu_mix",
                    50 - i * 10,
                    "ntmspace:nugget_cm247", 1 + 2 * i / 50, c244, 2 + 1 * i, c245, 2 + 1 * i);
            family(errors, "ntmspace:rbmk_pellet_mecm", i, "ntmspace:nugget_cm_mix", 100 - i * 20,
                    "ntmspace:nugget_cm247", 2 + 4 * i / 30,
                    c244, 4 + 4 * i, c245, 5 + 4 * i);
            family(errors, "ntmspace:rbmk_pellet_hecm", i, "ntmspace:nugget_cm245", 100 - i * 20,
                    "ntmspace:nugget_cm247", 3 + 5 * i / 30,
                    c244, 6 + 7 * i, c245, 7 + 8 * i);
        }
        checkSilex(errors, item("hbm:nuclear_waste_short_americium242"), 900, 100, "visible", CM, 70,
                "hbm:nugget_pu239", 10,
                "hbm:powder_cs137_tiny", 5, "hbm:powder_i131_tiny", 5, "hbm:nuclear_waste_tiny", 10);
        checkSilex(errors, item("hbm:nuclear_waste_short_depleted_americium242"), 900, 100, "visible", CM, 50,
                "hbm:nugget_pu239", 20, "hbm:nuclear_waste_tiny", 10, "ntmspace:nugget_cm242", 20);
        // hbm-space's own waste classes (SILEXRecipe(900, 100, 2): visible)
        String[][] waste = {
                {"nuclear_waste_short_americium241", "ntmspace:nugget_cm_mix", "40", "hbm:nugget_pu239", "10", "hbm:powder_cs137_tiny", "5",
                        "hbm:powder_i131_tiny", "5", "hbm:nuclear_waste_tiny", "10", "hbm:nugget_am242", "30"},
                {"nuclear_waste_short_depleted_americium241", "ntmspace:nugget_cm_mix", "50", "hbm:nugget_pu239", "20",
                        "hbm:nuclear_waste_tiny", "20", "ntmspace:nugget_cm242", "10"},
                {"nuclear_waste_short_berkelium247", "ntmspace:nugget_cf251", "40", "hbm:nugget_am_mix", "10", "hbm:powder_cs137_tiny", "5",
                        "hbm:powder_sr90_tiny", "5", "hbm:nuclear_waste_tiny", "10"},
                {"nuclear_waste_short_depleted_berkelium247", "ntmspace:nugget_cm_mix", "50", "ntmspace:nugget_cf251", "20",
                        "hbm:nuclear_waste_tiny", "10", "hbm:nugget_am_mix", "20"},
                {"nuclear_waste_short_curium244", "ntmspace:nugget_cm245", "30", "ntmspace:nugget_cm246", "15", "ntmspace:nugget_cm247", "10",
                        "ntmspace:nugget_cf251", "25", "ntmspace:nugget_cf252", "20", "ntmspace:nugget_es253", "10"},
                {"nuclear_waste_short_depleted_curium244", "ntmspace:nugget_cm_mix", "40", "ntmspace:nugget_cm246", "5",
                        "ntmspace:nugget_cm247", "15", "hbm:nuclear_waste_tiny", "10", "ntmspace:nugget_cf251", "20", "ntmspace:nugget_cf252", "10"},
                {"nuclear_waste_short_curium245", "ntmspace:nugget_cm246", "15", "ntmspace:nugget_cm247", "10", "ntmspace:nugget_cf251", "35",
                        "ntmspace:nugget_cf252", "30", "ntmspace:nugget_es253", "10"},
                {"nuclear_waste_short_depleted_curium245", "ntmspace:nugget_cm_mix", "10", "ntmspace:nugget_cm246", "5",
                        "ntmspace:nugget_cm247", "15", "hbm:nuclear_waste_tiny", "10", "ntmspace:nugget_cf252", "15", "ntmspace:nugget_cf251", "25"}};
        for (String[] row : waste) {
            Object[] outputs = new Object[row.length - 1];
            for (int k = 1; k < row.length; k += 2) {
                outputs[k - 1] = row[k];
                outputs[k] = Integer.parseInt(row[k + 1]);
            }
            checkSilex(errors, item("ntmspace:" + row[0]), 900, 100, "visible", outputs);
            // 1.7 tinyWasteTranslation: the tiny form runs the full form's row with (900 / 900) * 100 mB of solution
            String tiny = row[0].replace("nuclear_waste_short_", "nuclear_waste_short_tiny_")
                                .replace("nuclear_waste_short_tiny_depleted_", "nuclear_waste_short_depleted_tiny_");
            checkSilex(errors, item("ntmspace:" + tiny), 100, 100, "visible", outputs);
        }
        finish(helper, errors);
    }

    private static final String X = "XRAY";
    private static final String WS = "ntmspace:nuclear_waste_short_tiny_";

    /**
     * An X-ray family's stage {@code i} and its xenon twin {@code i + 5} (the xenon output after the head outputs).
     */
    private static void family(List<String> errors, String pellet, int i, Object... outputs) {
        checkSilex(errors, pellet(pellet, i), 600, 100, X, outputs);
        int head = pellet.endsWith("lecm") ? 6 : 4;
        Object[] xenon = new Object[outputs.length + 2];
        System.arraycopy(outputs, 0, xenon, 0, head);
        xenon[head] = XE;
        xenon[head + 1] = 3;
        System.arraycopy(outputs, head, xenon, head + 2, outputs.length - head);
        checkSilex(errors, pellet(pellet, i + 5), 600, 100, X, xenon);
    }

    /**
     * {@code outputs}: id, weight pairs in hbm-space's order; weight-0 pairs are skipped (not rows).
     */
    private static void checkSilex(List<String> errors, ItemStack input, int produced, int consumed, String laser,
                                   Object... outputs) {
        String in = RecipeText.describe(input);
        SILEXRecipes.SILEXRecipe recipe = SILEXRecipes.getOutput(input);
        if (recipe == null) {
            errors.add("silex " + in + " missing");
            return;
        }
        List<String> expected = new ArrayList<>();
        for (int k = 0; k < outputs.length; k += 2) {
            if ((Integer) outputs[k + 1] > 0) expected.add(outputs[k] + "@" + outputs[k + 1]);
        }
        List<String> actual = new ArrayList<>();
        for (SILEXRecipes.WeightedOutput out : recipe.outputs)
            actual.add(id(out.stack().getItem()) + "@" + out.weight());
        String wave = recipe.laserStrength.name().toLowerCase(Locale.ROOT);
        var expectedWavelength = SILEXRecipes.WAVELENGTH.parse(JsonOps.INSTANCE, new JsonPrimitive(laser)).result()
                                                        .orElseThrow(() -> new IllegalStateException(
                                                                "No native SILEX wavelength " + laser));
        if (recipe.fluidProduced != produced || recipe.fluidConsumed != consumed || recipe.laserStrength != expectedWavelength || !actual.equals(
                expected)) {
            errors.add(
                    "silex " + in + ": " + recipe.fluidProduced + "/" + recipe.fluidConsumed + " " + wave + " " + actual
                            + " expected " + produced + "/" + consumed + " " + laser + " " + expected);
        }
    }

    private static ItemStack pellet(String id, int stage) {
        Item item = BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow();
        return ((ItemRBMKPellet) item).of(stage);
    }

    // ------------------------------------------------------------------------------------------------ outgasser

    private static void outgasser(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        String[][] rows = {
                {"hbm:ingot_cobalt", "hbm:ingot_co60"}, {"hbm:nugget_cobalt", "hbm:nugget_co60"}, {"hbm:powder_cobalt", "hbm:powder_co60"},
                {"hbm:ingot_bismuth", "hbm:ingot_polonium"}, {"hbm:nugget_bismuth", "hbm:nugget_polonium"},
                {"hbm:powder_bismuth", "hbm:powder_polonium"}};
        for (String[] row : rows) {
            GenericRecipe recipe = OutgasserRecipes.INSTANCE.getRecipe(item(row[0]));
            String out = recipe == null || recipe.outputItem == null || recipe.outputItem.length != 1 ? "none"
                    : id(recipe.outputItem[0].getItem()) + " x" + recipe.outputItem[0].getCount();
            boolean fluid = recipe != null && recipe.outputFluid != null && recipe.outputFluid.length > 0;
            if (!out.equals(row[1] + " x1") || fluid) errors.add("outgasser " + row[0] + " -> " + out);
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ plasma forge

    private static void plasmaForge(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        checkForge(errors, "plsm.weldstainless", "ntmspace:plate_welded_stainless", 125, 20_000L, 500_000L, null, 0,
                "#c:plates_cast/stainless_steel x2");
        String osm = "mat:osmiridium/WELDEDPLATE x16", cmb = "mat:combine_steel/WELDEDPLATE x16";
        checkForge(errors, "plsm.dysonlauncher", "ntmspace:dyson_launcher", 1_200, 10_000_000L, 50_000_000L,
                "hbm:stellar_flux", 8_000,
                osm, cmb, "mat:stainless/WELDEDPLATE x16", "hbm:circuit_controller_advanced x8");
        checkForge(errors, "plsm.dysonreceiver", "ntmspace:dyson_receiver", 1_200, 10_000_000L, 50_000_000L,
                "hbm:stellar_flux", 8_000,
                osm, cmb, "hbm:crystal_xen x1", "mat:bscco/DENSEWIRE x64", "mat:bscco/DENSEWIRE x64",
                "hbm:circuit_controller_quantum x16");
        checkForge(errors, "plsm.dysonconverterhe", "ntmspace:dyson_converter_he", 1_200, 10_000_000L, 50_000_000L,
                "hbm:stellar_flux",
                8_000, osm, cmb, "mat:starmetal/CASTPLATE x16", "mat:bscco/DENSEWIRE x64",
                "hbm:circuit_controller_quantum x4");
        checkForge(errors, "plsm.dysonconvertertu", "ntmspace:dyson_converter_tu", 1_200, 10_000_000L, 50_000_000L,
                "hbm:stellar_flux",
                8_000, osm, cmb, "mat:starmetal/CASTPLATE x16", "hbm:circuit_controller_advanced x4");
        checkForge(errors, "plsm.dysonconverteranatmo", "ntmspace:dyson_converter_anatmogenesis", 1_200, 10_000_000L,
                50_000_000L,
                "hbm:stellar_flux", 16_000, osm, cmb, "mat:dineutronium/DENSEWIRE x16", "hbm:singularity_spark x1",
                "hbm:circuit_controller_quantum x16");
        finish(helper, errors);
    }

    private static void checkForge(List<String> errors, String name, String output, int duration, long power,
                                   long ignition,
                                   String fluid, int amount, String... inputs) {
        PlasmaForgeRecipes.INSTANCE.registerRecipes();
        GenericRecipe found = PlasmaForgeRecipes.INSTANCE.getRecipe(name);
        if (!(found instanceof PlasmaForgeRecipe recipe)) {
            errors.add(name + " missing");
            return;
        }
        String out = recipe.outputItem == null || recipe.outputItem.length != 1 ? "none"
                : id(recipe.outputItem[0].getItem()) + " x" + recipe.outputItem[0].getCount();
        String fluids = describe(recipe.inputFluid);
        String expectedFluids = fluid == null ? "[]" : "[" + fluid + " " + amount + "]";
        if (!out.equals(
                output + " x1") || recipe.duration != duration || recipe.power != power || recipe.ignitionTemp != ignition
                || !describe(recipe.inputItem).equals(List.of(inputs)) || !fluids.equals(expectedFluids)) {
            errors.add(
                    name + ": " + out + " " + recipe.duration + "t " + recipe.power + " HE " + recipe.ignitionTemp + " "
                            + describe(recipe.inputItem) + " " + fluids);
        }
    }

    // ------------------------------------------------------------------------------------------------ arc welder

    private static void arcWelder(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        checkWelder(errors, "ntmspace:plate_welded_stainless", 250, 20_000L, null, 0, "mat:stainless/CASTPLATE x2");
        checkWelder(errors, "ntmspace:machine_xenon_thruster", 200, 50_000L, "ntmspace:argon", 1_000,
                "mat:tungsten/WELDEDPLATE x2", "#c:plates/stainless x6", "hbm:arc_electrode x1");
        checkWelder(errors, "ntmspace:insert_cmb", 600, 50_000L, "ntmspace:neon", 2_000, "#c:plates/combine_steel x2",
                "#c:ingots/u238 x1");
        checkWelder(errors, "ntmspace:circuit_avionics", 250, 25_000L, null, 0, "mat:aluminium/CASTPLATE x2",
                "ntmspace:circuit_aero x2");
        finish(helper, errors);
    }

    private static void checkWelder(List<String> errors, String output, int duration, long consumption, String fluid,
                                    int amount,
                                    String... inputs) {
        ArcWelderRecipes.ArcWelderRecipe recipe = null;
        for (ArcWelderRecipes.ArcWelderRecipe r : ArcWelderRecipes.recipes) {
            if (id(r.output.getItem()).equals(output)) recipe = r;
        }
        if (recipe == null) {
            errors.add("arc welder " + output + " missing");
            return;
        }
        String fluids = recipe.fluid == null ? "[]" : describe(new FluidStack[]{recipe.fluid});
        String expectedFluids = fluid == null ? "[]" : "[" + fluid + " " + amount + "]";
        if (recipe.output.getCount() != 1 || recipe.duration != duration || recipe.consumption != consumption
                || !describe(recipe.ingredients).equals(List.of(inputs)) || !fluids.equals(expectedFluids)) {
            errors.add("arc welder " + output + ": " + recipe.duration + "t " + recipe.consumption + " HE "
                    + describe(recipe.ingredients) + " " + fluids);
        }
    }

    // ------------------------------------------------------------------------------------------------ soldering

    private static void soldering(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // the changed row: the atomic clock is gone, lanthanium capacitors take its place (lbsm off)
        checkSolder(errors, "hbm:circuit_controller_advanced", 600, 25_000, "hbm:perfluoromethyl 4000",
                List.of("hbm:circuit_chip_bismoid x16", "hbm:circuit_capacitor_tantalium x48",
                        "ntmspace:circuit_capacitor_lanthanium x32"),
                List.of("hbm:circuit_controller_chassis x1", "#c:ingots/any_hardplastic x4"),
                List.of("mat:lead/WIRE x24"));
        for (SolderingRecipes.SolderingRecipe r : SolderingRecipes.recipes) {
            if (id(r.output.getItem()).equals("hbm:circuit_controller_advanced")
                    && describe(r.toppings).stream().anyMatch(s -> s.startsWith("hbm:circuit_atomic_clock"))) {
                errors.add("HBM's atomic-clock controller_advanced row is still present");
            }
        }
        checkSolder(errors, "ntmspace:circuit_aero", 300, 1_000, "[]", List.of("hbm:circuit_chip x3"),
                List.of("hbm:circuit_advanced x1", "#c:ingots/rubber x4"), List.of("mat:lead/WIRE x4"));
        checkSolder(errors, "hbm:circuit_capacitor_board", 200, 300, "hbm:peroxide 500",
                List.of("ntmspace:circuit_capacitor_lanthanium x3"),
                List.of("hbm:circuit_pcb x1"), List.of("mat:lead/WIRE x3"));
        checkSolder(errors, "hbm:circuit_bismoid", 400, 10_000, "ntmspace:polythylene 1000",
                List.of("hbm:circuit_chip_bismoid x4", "ntmspace:circuit_gaschip x4", "hbm:circuit_capacitor x8"),
                List.of("hbm:circuit_pcb x6"), List.of("mat:lead/WIRE x12"));
        checkSolder(errors, "hbm:circuit_advanced", 300, 1_000, "ntmspace:polythylene 250",
                List.of("ntmspace:circuit_gaschip x2", "hbm:circuit_capacitor x2"), List.of("hbm:circuit_pcb x4"),
                List.of("mat:lead/WIRE x8"));
        finish(helper, errors);
    }

    /**
     * Finds the row with this output and these toppings (several rows may make one circuit) and checks the rest.
     */
    private static void checkSolder(List<String> errors, String output, int duration, long consumption, String fluid,
                                    List<String> toppings, List<String> pcb, List<String> solder) {
        for (SolderingRecipes.SolderingRecipe r : SolderingRecipes.recipes) {
            if (!id(r.output.getItem()).equals(output) || !describe(r.toppings).equals(toppings)) continue;
            String f = r.fluid == null ? "[]" : id(r.fluid.type) + " " + r.fluid.fill;
            if (r.output.getCount() != 1 || r.duration != duration || r.consumption != consumption || !f.equals(fluid)
                    || !describe(r.pcb).equals(pcb) || !describe(r.solder).equals(solder)) {
                errors.add(
                        "soldering " + output + ": " + r.duration + "t " + r.consumption + " HE " + f + " " + describe(
                                r.pcb)
                                + " " + describe(r.solder));
            }
            return;
        }
        errors.add("soldering " + output + " " + toppings + " missing");
    }

    // ------------------------------------------------------------------------------------------------ annihilator

    private static void annihilator(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        // the rows are 528-mode only ("requires": enable_528), so they are read as rows, not from the 528-off lookup
        checkAnnihilator(errors, "c:ingots/aluminium", 512, "528.aluminium");
        checkAnnihilator(errors, "c:ingots/stainless_steel", 1024, "528.stainless");
        checkAnnihilator(errors, "ntmspace:circuit_gaschip", 512, "528.gaschip");
        finish(helper, errors);
    }

    private static void checkAnnihilator(List<String> errors, String key, long amount, String pool) {
        for (AnnihilatorRecipes.Row row : AnnihilatorRecipes.TABLE.rows()) {
            String k = row.item().map(RecipeChangeNuclearGameTests::id).orElse(row.tag().orElse(""));
            if (!k.equals(key)) continue;
            if (row.milestones().size() != 1) {
                errors.add("annihilator " + key + " milestones " + row.milestones().size());
                return;
            }
            AnnihilatorRecipes.MilestoneRow m = row.milestones().get(0);
            ItemSpec payout = m.payout();
            String json = ItemSpec.CODEC.encodeStart(JsonOps.INSTANCE, payout).result().map(JsonElement::toString)
                                        .orElse("?");
            if (m.amount() != amount || !id(payout.item()).equals("hbm:blueprints") || !json.contains(
                    "\"" + pool + "\"")
                    || !row.requires().contains("enable_528")) {
                errors.add("annihilator " + key + ": " + m.amount() + " " + json + " " + row.requires());
            }
            return;
        }
        errors.add("annihilator " + key + " missing");
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow());
    }

    private static String id(Item item) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(item));
    }

    private static String id(com.hbm.inventory.fluid.FluidType fluid) {
        return String.valueOf(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
    }

    private static List<String> describe(AStack[] stacks) {
        List<String> out = new ArrayList<>();
        if (stacks != null) for (AStack s : stacks) out.add(RecipeText.describe(s));
        return out;
    }

    private static String describe(FluidStack[] stacks) {
        List<String> out = new ArrayList<>();
        if (stacks != null) for (FluidStack s : stacks) out.add(id(s.type) + " " + s.fill);
        return out.toString();
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        RecipeChangeGameTests.finish(helper, errors);
    }
}
