package com.ntmspace.gametest;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.ReformingRecipes;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The recipe-change family's chemistry tables (generated from {@code src/recipes}): every row hbm-space ("NTM: Space")
 * added, changed or removed in ChemicalPlant, Mixer, Crystallizer, Centrifuge, ElectrolyserFluid, Liquefaction,
 * Solidification, Fraction, Cracking, Reforming, Hydrotreating, VacuumRefinery, Coker, GasCentrifuge, PyroOven and
 * MachineRecipes, read back through each table's own lookup. The expected values are written here from hbm-space's
 * source classes (not from the JSON files), as one line per row in a fixed text form.
 */
public final class RecipeChangeChemGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function CHEMICAL_PLANT = TESTS.function("recipe_change_chem_chemical_plant", 40,
            () -> helper -> run(helper, RecipeChangeChemGameTests::chemicalPlant));
    public static final GameTests.Function MIXER = TESTS.function("recipe_change_chem_mixer", 40,
            () -> helper -> run(helper, RecipeChangeChemGameTests::mixer));
    public static final GameTests.Function CRYSTALLIZER = TESTS.function("recipe_change_chem_crystallizer", 40,
            () -> helper -> run(helper, RecipeChangeChemGameTests::crystallizer));
    public static final GameTests.Function CENTRIFUGE = TESTS.function("recipe_change_chem_centrifuge", 40,
            () -> helper -> run(helper, RecipeChangeChemGameTests::centrifuge));
    public static final GameTests.Function FLUID_TABLES = TESTS.function("recipe_change_chem_fluid_tables", 40,
            () -> helper -> run(helper, RecipeChangeChemGameTests::fluidTables));
    public static final GameTests.Function ITEM_FLUID_TABLES = TESTS.function("recipe_change_chem_item_fluid_tables", 40,
            () -> helper -> run(helper, RecipeChangeChemGameTests::itemFluidTables));

    private RecipeChangeChemGameTests() {}

    public static void init() {}

    // ------------------------------------------------------------------------------------------------ chemical plant

    private static void chemicalPlant(List<String> errors) {
        ChemicalPlantRecipes table = ChemicalPlantRecipes.INSTANCE;
        table.registerRecipes();
        // name -> items | fluids in | items out | fluids out | duration | power  (hbm-space ChemicalPlantRecipes)
        String[][] rows = {
                {"chem.biogas", "hbm:biomassx16", "", "", "hbm:biogasx2000", "60", "100"},
                {"chem.rubber", "#c:dusts/sulfurx1,#c:dusts/zincx1", "hbm:unsaturatedsx500", "hbm:ingot_rubberx2", "", "100", "200"},
                {"chem.vinylrubber", "", "ntmspace:vinylx500,hbm:unsaturatedsx400", "hbm:ingot_rubberx2", "", "100", "400"},
                {"chem.coppersulf", "", "ntmspace:aqueous_copperx500", "ntmspace:powder_nickelx2,hbm:powder_copperx2", "ntmspace:coppersulfatex200", "50", "100"},
                {"chem.uraniumbromide", "#c:billets/u235x1,hbm:powder_brominex1,#c:ingots/asbestosx1", "hbm:hydrogenx4000", "", "ntmspace:uranium_bromidex4000", "200", "1000"},
                {"chem.thoriumbromide", "#c:billets/thoriumx1,hbm:powder_brominex1,#c:ingots/asbestosx1", "hbm:hydrogenx4000", "", "ntmspace:thorium_bromidex4000", "200", "1000"},
                {"chem.hydrazine", "", "hbm:nitric_acidx2000,ntmspace:ammoniax1000", "", "ntmspace:hydrazinex800", "250", "1000"},
                {"chem.ammonia", "", "ntmspace:nitrogenx600,minecraft:waterx1000", "", "ntmspace:ammoniax800", "50", "100"},
                {"chem.bloodfuel", "", "ntmspace:ammoniax350,hbm:bloodx800", "", "ntmspace:bloodgasx1000", "250", "1000"},
                {"chem.ammoniumnitrate", "", "ntmspace:ammoniax500,ntmspace:nitrogenx1000", "ntmspace:ammonium_nitratex4", "", "250", "1000"},
                {"chem.nmass", "hbm:pellet_chargedx1,hbm:ingot_euphemiumx1", "hbm:schrabidicx650,hbm:iongelx800", "", "ntmspace:nmassx1000,hbm:wastegasx2000", "250", "10000"},
                // ChemicalPlantTable quarters 1.7's four-cake batch: cake stacks to 1
                {"chem.masscake", "minecraft:sugarx" + 8 / 4 + ",minecraft:eggx" + 4 / 4,
                        "ntmspace:cmilkx" + 4000 / 4 + ",ntmspace:creamx" + 1000 / 4,
                        "minecraft:cakex" + 4 / 4, "", String.valueOf(200 / 4), "100"},
                {"chem.butter", "", "ntmspace:emilkx1000", "ntmspace:butterx1", "", "100", "100"},
                {"chem.strawberryicecream", "#c:foods/butterx2,minecraft:packed_icex1,#c:crops/strawberryx4", "ntmspace:creamx1000", "ntmspace:s_creamx4", "", "150", "100"},
                {"chem.minmusicecream", "#c:foods/butterx4,ntmspace:minmus_smoothx2,ntmspace:minmus_stonex2", "ntmspace:creamx1000", "ntmspace:min_creamx4", "", "150", "100"},
                {"chem.minmussmoothstone", "minecraft:icex4,ntmspace:mint_leavesx2,ntmspace:minmus_smoothx2", "ntmspace:milkx350,ntmspace:emilkx250", "ntmspace:minmus_smoothx4", "", "200", "1000"},
                {"chem.menthol", "ntmspace:mint_leavesx4", "hbm:steamx350", "ntmspace:ingot_mentholx2", "", "50", "100"},
                {"chem.mentholfromminmus", "ntmspace:minmus_smoothx40", "hbm:steamx350,hbm:sulfuric_acidx500", "ntmspace:ingot_mentholx15", "", "50", "300"},
                {"chem.mintslice", "ntmspace:billet_mentholx3,ntmspace:minmus_stonex1,minecraft:sugarx6", "ntmspace:milkx350", "ntmspace:chocolate_mint_billetx3", "", "50", "100"},
                {"chem.minmusstone", "ntmspace:minmus_smoothx2", "hbm:sulfuric_acidx500", "ntmspace:minmus_stonex2", "", "300", "500"},
                {"chem.soil", "ntmspace:ammonium_nitratex1,minecraft:gravelx8", "minecraft:waterx4000", "minecraft:dirtx8", "", "100", "1000"},
                {"chem.chloromethane", "", "hbm:gasx750,hbm:chlorinex250", "", "ntmspace:chloromethanex1000", "50", "1000"},
                {"chem.nitricacidalt", "", "minecraft:waterx500,ntmspace:ammoniax1000", "", "hbm:nitric_acidx1000", "50", "1000"},
                {"chem.hydrapiss", "hbm:rag_pissx1", "hbm:nitric_acidx2000", "", "ntmspace:hydrazinex800", "250", "1000"},
                {"chem.synleather", "#c:ingots/any_plasticx1,minecraft:stringx4", "hbm:peroxidex250", "minecraft:leatherx1", "", "200", "500"},
        };
        for (String[] row : rows) {
            GenericRecipe recipe = table.getRecipe(row[0]);
            if (recipe == null) {
                errors.add(row[0] + " missing");
                continue;
            }
            String got = String.join(" | ", stacks(recipe.inputItem), fluids(recipe.inputFluid), items(recipe.outputItem),
                    fluids(recipe.outputFluid), String.valueOf(recipe.duration), String.valueOf(recipe.power));
            String want = String.join(" | ", row[1], row[2], row[3], row[4], row[5], row[6]);
            if (!got.equals(want)) errors.add(row[0] + ": " + got + " != " + want);
        }
        // the cryo-distillery provides these in hbm-space (removed by another family's file; checked here as a whole)
        for (String gone : new String[] {"chem.oxygen", "chem.xenon", "chem.xenonoxy", "chem.birkeland"}) {
            if (table.getRecipe(gone) != null) errors.add(gone + " still present");
        }
    }

    // ------------------------------------------------------------------------------------------------ mixer

    private static void mixer(List<String> errors) {
        // output -> its rows in selector order: item in | fluids in | output mB | duration
        mixRows(errors, "hbm:nitric_acid",
                " | ntmspace:ammoniax1000,minecraft:waterx500 | 1000 | 50",
                "#c:dusts/niterx1 | hbm:sulfuric_acidx500 | 1000 | 50");
        mixRows(errors, "hbm:radiosolvent",
                " | hbm:reformgasx750,hbm:chlorinex250 | 1000 | 50",
                "ntmspace:geysir_chloricx1 | hbm:reformgasx500 | 5000 | 20");
        mixRows(errors, "hbm:oxyhydrogen", " | hbm:hydrogenx500,hbm:oxygenx500 | 1000 | 50");
        mixRows(errors, "ntmspace:smilk", "#c:crops/strawberryx4 | ntmspace:milkx500 | 500 | 50");
        mixRows(errors, "ntmspace:coffee", "ntmspace:powder_coffeex4 | minecraft:waterx500 | 100 | 50");
        mixRows(errors, "ntmspace:tea", "ntmspace:tea_leafx2 | minecraft:waterx500 | 200 | 50");
        mixRows(errors, "ntmspace:elbowgrease", " | hbm:reformgasx500,hbm:syngasx500 | 1000 | 50");
        mixRows(errors, "ntmspace:htco4", "hbm:nugget_technetiumx1 | hbm:nitric_acidx500 | 250 | 50");
        mixRows(errors, "hbm:redmud", "ntmspace:duna_sandsx4 | ntmspace:hclx1400 | 50 | 100");
        mixRows(errors, "ntmspace:dicyanoacetylene", " | ntmspace:ammoniax300,hbm:unsaturatedsx500 | 750 | 50");
        mixRows(errors, "hbm:ethanol", " | ntmspace:chloroethanex250,hbm:redmudx300 | 450 | 30");
        mixRows(errors, "ntmspace:scutterblood", "ntmspace:scuttertailx1 | ntmspace:hclx300 | 550 | 50");
        mixRows(errors, "ntmspace:conglomera", "ntmspace:stone_resource_conglomeratex1 | ntmspace:aqueous_nickelx250 | 1000 | 70");
        mixRows(errors, "ntmspace:aqueous_nickel", "#c:dusts/nickelx1 | ntmspace:brinex250 | 1000 | 80");
        mixRows(errors, "ntmspace:aqueous_copper", "hbm:chunk_ore_malachitex1 | ntmspace:brinex250 | 1000 | 80");
        mixRows(errors, "ntmspace:vinyl",
                "ntmspace:pvc_logx1 | hbm:radiosolventx250 | 1000 | 20",
                "ntmspace:powder_pvcx1 | hbm:radiosolventx125 | 500 | 20",
                "hbm:powder_cadmiumx1 | ntmspace:hgasx500,hbm:unsaturatedsx250 | 500 | 20");
        mixRows(errors, "ntmspace:cbenz", "ntmspace:leaf_petx32 | hbm:radiosolventx500 | 250 | 20");
        mixRows(errors, "ntmspace:lithydro", "hbm:powder_lithiumx3 | ntmspace:brinex125 | 1000 | 100");
    }

    private static void mixRows(List<String> errors, String output, String... want) {
        MixerRecipes.MixerRecipe[] rows = MixerRecipes.getOutput(fluid(output));
        List<String> got = new ArrayList<>();
        if (rows != null) {
            for (MixerRecipes.MixerRecipe row : rows) {
                List<FluidStack> in = new ArrayList<>();
                if (row.input1 != null) in.add(row.input1);
                if (row.input2 != null) in.add(row.input2);
                got.add((row.solidInput == null ? "" : GenericRecipeCodec.describe(row.solidInput)) + " | "
                        + fluids(in.toArray(FluidStack[]::new)) + " | " + row.output + " | " + row.processTime);
            }
        }
        if (!got.equals(List.of(want))) errors.add("mixer " + output + ": " + got + " != " + List.of(want));
    }

    // ------------------------------------------------------------------------------------------------ crystallizer

    private static void crystallizer(List<String> errors) {
        // input item, acid -> output, acid mB, duration, input count  (hbm-space CrystallizerRecipes)
        crys(errors, "ntmspace:ore_fire", "hbm:peroxide", "hbm:crystal_phosphorusx1", 500, 600, 1);
        crys(errors, "ntmspace:crystal_mineral", "hbm:peroxide", "hbm:crystal_diamondx1", 500, 600, 1);
        crys(errors, "minecraft:cobblestone", "hbm:peroxide", "hbm:reinforced_stonex1", 500, 100, 1);
        crys(errors, "ntmspace:saltleaf", "ntmspace:scutterblood", "hbm:gem_sodalitex1", 1000, 600, 5);
        crys(errors, "hbm:chunk_ore_malachite", "ntmspace:coppersulfate", "hbm:crystal_copperx1", 350, 600, 1);
        crys(errors, "ntmspace:nickel_salts", "hbm:nitric_acid", "ntmspace:crystal_nickelx1", 500, 600, 1);
        crys(errors, "ntmspace:leaf_rubber", "ntmspace:hcl", "hbm:ingot_rubberx1", 500, 600, 64);
        String[][] fragments = {{"hbm:peroxide", "peroxide", "500"}, {"hbm:nitric_acid", "nitric", "500"},
                {"hbm:sulfuric_acid", "sulfuric", "500"}, {"hbm:solvent", "solvent", "500"},
                {"ntmspace:hcl", "hydrochloric", "500"}, {"hbm:schrabidic", "schrabidic", "1000"}};
        for (String[] f : fragments) {
            crys(errors, "ntmspace:mineral_dust", f[0], "ntmspace:mineral_fragment_" + f[1] + "x1", Integer.parseInt(f[2]), 300, 1);
        }
        for (String oil : new String[] {"hbm:woodoil", "hbm:fishoil", "hbm:lightoil"}) {
            crys(errors, "hbm:powder_coal", oil, "hbm:chemical_dye_blackx4", 100, 20, 1);
            crys(errors, "ntmspace:powder_coke_coal", oil, "hbm:chemical_dye_blackx4", 100, 20, 1);
        }
        // the cobblestone row reads the tag now (1.7 KEY_COBBLESTONE)
        CrystallizerRecipes.CrystallizerRecipe cobble = CrystallizerRecipes.getOutput(stack("minecraft:cobblestone"), fluid("hbm:peroxide"));
        if (cobble != null && !GenericRecipeCodec.describe(cobble.input).equals("#c:cobblestonesx1")) {
            errors.add("crystallizer cobblestone input " + GenericRecipeCodec.describe(cobble.input));
        }
    }

    private static void crys(List<String> errors, String input, String acid, String output, int amount, int duration, int count) {
        CrystallizerRecipes.CrystallizerRecipe recipe = CrystallizerRecipes.getOutput(stack(input, 64), fluid(acid));
        String want = output + " " + amount + " " + duration + " " + count;
        String got = recipe == null ? "none" : item(recipe.output) + " " + recipe.acidAmount + " " + recipe.duration + " "
                + recipe.input.stacksize;
        if (!got.equals(want)) errors.add("crystallizer " + input + "+" + acid + ": " + got + " != " + want);
    }

    // ------------------------------------------------------------------------------------------------ centrifuge

    private static void centrifuge(List<String> errors) {
        cent(errors, "ntmspace:ore_fire", "minecraft:blaze_powderx2,hbm:powder_firex2,hbm:ingot_phosphorusx1,minecraft:gravelx1");
        cent(errors, "ntmspace:mineral_fragment_peroxide", "hbm:powder_niobiumx4,hbm:powder_cobaltx2,hbm:powder_zirconiumx1,hbm:powder_berylliumx1");
        cent(errors, "ntmspace:mineral_fragment_nitric", "ntmspace:powder_galliumx4,hbm:powder_berylliumx2,hbm:powder_niobiumx1,hbm:fragment_lanthaniumx1");
        cent(errors, "ntmspace:mineral_fragment_sulfuric", "hbm:powder_berylliumx4,hbm:powder_niobiumx2,ntmspace:powder_galliumx1,hbm:fragment_lanthaniumx1");
        cent(errors, "ntmspace:mineral_fragment_solvent", "hbm:powder_cobaltx4,ntmspace:powder_galliumx2,hbm:powder_niobiumx1,hbm:powder_coltanx1");
        cent(errors, "ntmspace:mineral_fragment_hydrochloric", "hbm:powder_zirconiumx4,hbm:powder_neodymiumx2,hbm:powder_niobiumx1,hbm:powder_cobaltx1");
        cent(errors, "ntmspace:mineral_fragment_schrabidic", "hbm:powder_co60x1,hbm:nugget_bismuthx1,hbm:powder_asbestosx6,hbm:nugget_technetiumx1");
        cent(errors, "ntmspace:ferric_clay", "minecraft:clay_ballx1,minecraft:clay_ballx1,hbm:powder_ironx1,hbm:powder_ironx1");
        cent(errors, "ntmspace:ingot_magma", "ntmspace:bedrock_ore_fragment_galliumx4,ntmspace:bedrock_ore_fragment_arsenicx2,hbm:powder_meteorite_tinyx2,hbm:dust_tinyx3");
        cent(errors, "ntmspace:crystal_mineral", "ntmspace:mineral_dustx2,hbm:powder_ironx2,hbm:powder_aluminiumx2,hbm:powder_lithium_tinyx1");
        cent(errors, "ntmspace:crystal_nickel", "ntmspace:powder_nickelx2,ntmspace:powder_nickelx2,hbm:powder_ironx2,hbm:powder_titaniumx1");
        cent(errors, "ntmspace:crystal_niobium", "hbm:powder_niobiumx2,hbm:powder_niobiumx2,hbm:powder_ironx2,ntmspace:nugget_hafniumx1");
        cent(errors, "ntmspace:crystal_zinc", "ntmspace:powder_zincx2,ntmspace:powder_zincx2,hbm:sulfurx2,hbm:powder_aluminiumx1");
        cent(errors, "ntmspace:nickel_salts", "hbm:powder_ironx2,ntmspace:powder_nickelx2,hbm:powder_sodiumx1,hbm:sulfurx1");
    }

    private static void cent(List<String> errors, String input, String want) {
        ItemStack[] out = CentrifugeRecipes.getOutput(stack(input));
        String got = out == null ? "none" : items(out);
        if (!got.equals(want)) errors.add("centrifuge " + input + ": " + got + " != " + want);
    }

    // ------------------------------------------------------------------------------------------------ fluid-keyed tables

    private static void fluidTables(List<String> errors) {
        // ElectrolyserFluidRecipes: input mB, out1, out2, duration, byproducts
        electro(errors, "ntmspace:brine", "400 hbm:hydrogenx200 hbm:oxygenx200 40 hbm:powder_sodiumx2");
        electro(errors, "ntmspace:aqueous_nickel", "300 - - 40 ntmspace:powder_nickelx2,hbm:powder_ironx2,hbm:sulfurx4");
        electro(errors, "ntmspace:coppersulfate", "200 - hbm:oxygenx50 40 hbm:powder_copperx2,hbm:sulfurx2");
        electro(errors, "ntmspace:hcl", "1000 hbm:hydrogenx500 hbm:chlorinex500 40 ");
        electro(errors, "ntmspace:lithcarbonate", "1000 hbm:oxygenx12000 - 10 hbm:powder_lithiumx1,hbm:ingot_graphitex1");

        frac(errors, "ntmspace:conglomera", FractionRecipes.getFractions(fluid("ntmspace:conglomera")), "ntmspace:brinex25 ntmspace:aqueous_nickelx75");
        frac(errors, "ntmspace:hgas", FractionRecipes.getFractions(fluid("ntmspace:hgas")), "ntmspace:chloromethanex25 hbm:chlorinex85");
        frac(errors, "ntmspace:halolight", FractionRecipes.getFractions(fluid("ntmspace:halolight")), "hbm:phosgenex35 ntmspace:chloroethanex75");

        crack(errors, "hbm:oil_ds", "hbm:crackoil_dsx80 hbm:petroleumx20");
        crack(errors, "hbm:diesel_reform", "hbm:kerosene_reformx40 hbm:petroleumx30");
        crack(errors, "hbm:diesel_crack_reform", "hbm:kerosene_reformx40 hbm:petroleumx30");
        crack(errors, "ntmspace:chloroethane", "ntmspace:polythylenex50 hbm:chlorinex30");
        crack(errors, "ntmspace:elbowgrease", "hbm:mercuryx70 hbm:sourgasx50");
        crack(errors, "ntmspace:cbenz", "hbm:aromaticsx50 hbm:chlorinex30");
        if (CrackingRecipes.getCracking(fluid("hbm:gas")) != null) errors.add("cracking hbm:gas still present (commented out in hbm-space)");

        reform(errors, "ntmspace:halolight", "hbm:unsaturatedsx50,hbm:reformgasx35,ntmspace:hclx10");
        reform(errors, "ntmspace:hgas", "hbm:aromaticsx60,ntmspace:chloromethanex25,ntmspace:hclx10");

        hydro(errors, "ntmspace:ccl", "hbm:hydrogenx10@1 ntmspace:chloromethanex80,ntmspace:hclx20");
        hydro(errors, "ntmspace:cbenz", "hbm:hydrogenx10@1 hbm:xylenex50,hbm:chlorinex40");
        hydro(errors, "ntmspace:chloroethane", "hbm:hydrogenx10@1 ntmspace:vinylx50,hbm:chlorinex20");

        RefineryRecipes.VacuumRecipe vacuum = RefineryRecipes.getVacuum(fluid("ntmspace:tcrude"));
        String vac = vacuum == null ? "none" : fluids(new FluidStack[] {vacuum.frac1(), vacuum.frac2(), vacuum.frac3(), vacuum.frac4()});
        if (!vac.equals("ntmspace:halolightx40,ntmspace:chloromethanex25,ntmspace:hclx20,ntmspace:hgasx15")) {
            errors.add("vacuum ntmspace:tcrude: " + vac);
        }

        coker(errors, "ntmspace:bromine", "1000 hbm:powder_brominex1 hbm:gasx500");
        coker(errors, "ntmspace:scutterblood", "16000 hbm:coke_petroleumx1 hbm:gas_cokerx1600");

        solid(errors, "hbm:blood", "1280 ntmspace:flesh_waferx8");
        solid(errors, "hbm:carbondioxide", "1000 ntmspace:dry_icex1");
        solid(errors, "ntmspace:aqueous_nickel", "500 ntmspace:nickel_saltsx1");
        solid(errors, "ntmspace:vinyl", "1000 hbm:ingot_pvcx1");

        // GasCentrifugeRecipes: MINSOL (500 consumed, 500 produced, -> MINSOLSEP; iron) and MINSOLSEP (500, 0; cleaned crystal)
        GasCentrifugeRecipes.PseudoFluidType minsol = GasCentrifugeRecipes.FLUID_CONVERSIONS.get(fluid("ntmspace:minsol"));
        if (minsol == null || !minsol.getName().equals("MINSOL") || minsol.getFluidConsumed() != 500 || minsol.getFluidProduced() != 500
                || !items(minsol.getOutput()).equals("hbm:powder_ironx1")) {
            errors.add("pseudo fluid MINSOL wrong: " + (minsol == null ? "none" : minsol.getName()));
        } else {
            GasCentrifugeRecipes.PseudoFluidType sep = minsol.getOutputType();
            if (!sep.getName().equals("MINSOLSEP") || sep.getFluidConsumed() != 500 || sep.getFluidProduced() != 0
                    || !items(sep.getOutput()).equals("ntmspace:crystal_cleanedx1")
                    || sep.getOutputType() != GasCentrifugeRecipes.PseudoFluidType.NONE) {
                errors.add("pseudo fluid MINSOLSEP wrong: " + sep.getName());
            }
        }
        boolean shown = false;
        for (GasCentrifugeRecipes.GasCentrifugeRecipe row : GasCentrifugeRecipes.RECIPES) {
            if (row.input.type == fluid("ntmspace:minsol") && row.input.fill == 1000 && !row.highSpeed && row.centrifuges == 2
                    && items(row.output).equals("hbm:powder_ironx1,ntmspace:crystal_cleanedx1")) shown = true;
        }
        if (!shown) errors.add("gas centrifuge display row for ntmspace:minsol missing");

        // PyroOvenRecipes registerSFAuto(POLYTHYLENE): 1,440,000 TU * 1000 * 0.5 / 50,000 TU/B = 14,400 -> 14,000 mB
        boolean pyro = false;
        for (PyroOvenRecipes.PyroOvenRecipe row : PyroOvenRecipes.getAll()) {
            if (row.inputFluid != null && row.inputFluid.type == fluid("ntmspace:polythylene")) {
                pyro = row.inputFluid.fill == 14000 && row.inputItem == null && row.outputFluid == null
                        && item(row.outputItem).equals("hbm:solid_fuelx1") && row.duration == 60;
                if (!pyro) errors.add("pyrolysis polythylene: " + row.inputFluid.fill + " mB -> " + item(row.outputItem));
            }
        }
        if (!pyro) errors.add("pyrolysis polythylene missing or wrong");
    }

    private static void electro(List<String> errors, String input, String want) {
        ElectrolyserFluidRecipes.ElectrolysisRecipe r = ElectrolyserFluidRecipes.getRecipe(fluid(input));
        String got = r == null ? "none" : r.amount + " " + one(r.output1) + " " + one(r.output2) + " " + r.duration + " " + items(r.byproduct);
        if (!got.equals(want)) errors.add("electrolyser " + input + ": " + got + " != " + want);
    }

    private static void frac(List<String> errors, String input, FractionRecipes.FractionPair pair, String want) {
        String got = pair == null ? "none" : one(pair.left()) + " " + one(pair.right());
        if (!got.equals(want)) errors.add("fraction " + input + ": " + got + " != " + want);
    }

    private static void crack(List<String> errors, String input, String want) {
        CrackingRecipes.CrackingRecipe r = CrackingRecipes.getCracking(fluid(input));
        String got = r == null ? "none" : one(r.left()) + " " + one(r.right());
        if (!got.equals(want)) errors.add("cracking " + input + ": " + got + " != " + want);
    }

    private static void reform(List<String> errors, String input, String want) {
        ReformingRecipes.ReformingRecipe r = ReformingRecipes.getOutput(fluid(input));
        String got = r == null ? "none" : fluids(new FluidStack[] {r.out1(), r.out2(), r.out3()});
        if (!got.equals(want)) errors.add("reforming " + input + ": " + got + " != " + want);
    }

    private static void hydro(List<String> errors, String input, String want) {
        HydrotreatingRecipes.HydrotreatingRecipe r = HydrotreatingRecipes.getOutput(fluid(input));
        String got = r == null ? "none" : one(r.hydrogen()) + "@" + r.hydrogen().pressure + " " + fluids(new FluidStack[] {r.out1(), r.out2()});
        if (!got.equals(want)) errors.add("hydrotreating " + input + ": " + got + " != " + want);
    }

    private static void coker(List<String> errors, String input, String want) {
        CokerRecipes.CokerRecipe r = CokerRecipes.getOutput(fluid(input));
        String got = r == null ? "none" : r.amount() + " " + item(r.output()) + " " + one(r.byproduct());
        if (!got.equals(want)) errors.add("coker " + input + ": " + got + " != " + want);
    }

    private static void solid(List<String> errors, String input, String want) {
        SolidificationRecipes.SolidRecipe r = SolidificationRecipes.getOutput(fluid(input));
        String got = r == null ? "none" : r.amount() + " " + item(r.output());
        if (!got.equals(want)) errors.add("solidification " + input + ": " + got + " != " + want);
    }

    // ------------------------------------------------------------------------------------------------ item-keyed tables

    private static void itemFluidTables(List<String> errors) {
        // LiquefactionRecipes (hbm-space): KEY_COBBLESTONE / KEY_STONE 250 lava, packed ice 2000 water, dry ice 1000 CO2,
        // flesh 100 blood, osmiridium ingot 24000 ethanol
        liquefy(errors, "minecraft:cobblestone", "minecraft:lavax250");
        liquefy(errors, "minecraft:stone", "minecraft:lavax250");
        liquefy(errors, "minecraft:packed_ice", "minecraft:waterx2000");
        liquefy(errors, "ntmspace:dry_ice", "hbm:carbondioxidex1000");
        liquefy(errors, "ntmspace:flesh", "hbm:bloodx100");
        liquefy(errors, "hbm:ingot_osmiridium", "hbm:ethanolx24000");
        for (String id : new String[] {"minecraft:cobblestone", "minecraft:stone"}) {
            Item item = BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow();
            if (LiquefactionRecipes.getItemRecipes().containsKey(item)) errors.add("liquefaction " + id + " is still an item row (1.7: ore-dict key)");
        }

        // MachineRecipes.getAlloyFuels gains powder_coke (DiFurnace fuel 400, as coke)
        for (String id : new String[] {"ntmspace:powder_coke_coal", "ntmspace:powder_coke_lignite", "ntmspace:powder_coke_petroleum"}) {
            int power = DiFurnaceRecipes.getItemPower(stack(id));
            if (power != 400) errors.add("difurnace fuel " + id + " = " + power);
            boolean listed = false;
            for (ItemStack fuel : DiFurnaceRecipes.getAlloyFuels()) listed |= item(fuel).equals(id + "x1");
            if (!listed) errors.add("alloy fuels lack " + id);
        }
    }

    private static void liquefy(List<String> errors, String input, String want) {
        FluidStack out = LiquefactionRecipes.getOutput(stack(input));
        String got = out == null ? "none" : one(out);
        if (!got.equals(want)) errors.add("liquefaction " + input + ": " + got + " != " + want);
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static void run(GameTestHelper helper, Consumer<List<String>> check) {
        List<String> errors = new ArrayList<>();
        try {
            check.accept(errors);
        } catch (RuntimeException e) {
            errors.add(e.toString());
        }
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static FluidType fluid(String id) {
        for (FluidType type : Fluids.getAll()) if (type.getRegistryName().equals(id)) return type;
        throw new IllegalStateException("no fluid " + id);
    }

    private static ItemStack stack(String id) {
        return stack(id, 1);
    }

    private static ItemStack stack(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no item " + id)), count);
    }

    private static String item(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount();
    }

    private static String items(ItemStack[] stacks) {
        List<String> out = new ArrayList<>();
        if (stacks != null) for (ItemStack stack : stacks) out.add(item(stack));
        return String.join(",", out);
    }

    private static String stacks(AStack[] stacks) {
        List<String> out = new ArrayList<>();
        if (stacks != null) for (AStack stack : stacks) out.add(GenericRecipeCodec.describe(stack));
        return String.join(",", out);
    }

    private static String one(FluidStack stack) {
        return stack == null || stack.type == Fluids.NONE || stack.fill == 0 ? "-" : stack.type.getRegistryName() + "x" + stack.fill;
    }

    private static String fluids(FluidStack[] stacks) {
        List<String> out = new ArrayList<>();
        if (stacks != null) for (FluidStack stack : stacks) if (stack != null && stack.type != Fluids.NONE) out.add(one(stack));
        return String.join(",", out);
    }
}
