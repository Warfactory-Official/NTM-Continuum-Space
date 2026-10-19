package com.ntmspace.gametest;

import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.BedrockOreChainRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.BedrockOreType.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.ntmspace.bedrock.CelestialBedrockOre;
import com.ntmspace.bedrock.CelestialBedrockOre.Spec;
import com.ntmspace.bedrock.SpaceBedrockOres;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.StationData;
import com.ntmspace.registry.BedrockOreItems;
import com.ntmspace.satellite.SatelliteOrbit;
import com.ntmspace.satellite.SatelliteRailgun;
import com.ntmspace.satellite.SpaceSatellites;
import com.ntmspace.world.SolarSystem;
import com.ntmspace.world.SpaceDimensions;
import com.ntmspace.world.populate.Populate17;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Per-body bedrock ores (hbm-space {@code CelestialBedrockOre}) and satellites across the bodies.
 *
 * <p>The recipe checks compute every expected row from {@link CelestialBedrockOre}'s Java table with hbm-space's recipe
 * class arithmetic (here, independently of the {@code BedrockOre*Table} sources in {@code src/recipes}, which generate the
 * data files with hbm-space's own loops), then look each one up through the machine's own recipe lookup: the two paths agreeing on
 * all 54 types is the proof that the Java table, the data files and hbm-space's table are one table.
 */
public final class BedrockSatelliteGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** 54 types in hbm-space order, registered with HBM, outputs as the 1.7 table (with hand-checked rows). */
    public static final GameTests.Function TYPES = TESTS.function("bedrock_ore_celestial_types", 20,
            () -> BedrockSatelliteGameTests::types);

    /** Every per-body row exists on arc furnace, centrifuge, combination, crystallizer, electrolyser, shredder, pyro oven. */
    public static final GameTests.Function ROWS = TESTS.function("bedrock_ore_celestial_recipe_rows", 40,
            () -> BedrockSatelliteGameTests::rows);

    /** HBM's six-type rows are gone from every table (recipe_removals, and the pyro oven's code rows). */
    public static final GameTests.Function REMOVALS = TESTS.function("bedrock_ore_hbm_rows_removed", 20,
            () -> BedrockSatelliteGameTests::removals);

    /** Raw bedrock ore mined on a body carries that body's types, the 1.7 noise salted with the dimension id. */
    public static final GameTests.Function RAW_ORE = TESTS.function("bedrock_ore_raw_ore_per_body", 20,
            () -> BedrockSatelliteGameTests::rawOre);

    /** The ore density scanner's field: the body's types and its bedrock acid (Dres nitric, Eve permanganate). */
    public static final GameTests.Function FIELD = TESTS.function("bedrock_ore_scanner_field_per_body", 20,
            () -> BedrockSatelliteGameTests::field);

    /** Satellite saved data round-trips by name (HBM's and NTM: Space's types, orbit settings). */
    public static final GameTests.Function SAT_SAVE = TESTS.function("satellite_saved_data_round_trip", 20,
            () -> BedrockSatelliteGameTests::satelliteSave);

    /** Satellites stay with the body they are launched on; a relay goes to Duna; a station's are its body's. */
    public static final GameTests.Function SAT_MUN = TESTS.function("satellite_routing_per_body", 20,
            () -> BedrockSatelliteGameTests::satelliteOnMun);

    /** The satellite linker copies orbit settings between two satellite items only. */
    public static final GameTests.Function SAT_LINKER = TESTS.function("satellite_linker_copies_orbit_settings", 20,
            () -> BedrockSatelliteGameTests::satelliteLinker);

    private BedrockSatelliteGameTests() {}

    public static void init() {}

    // ---------------------------------------------------------------------------------------------- bedrock ores

    private static void types(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        List<BedrockOreType> all = CelestialBedrockOre.getAllTypes();
        if (all.size() != 54) errors.add("expected 54 types, got " + all.size());
        int[] perBody = {4, 5, 5, 6, 6, 6, 6, 4, 6, 6};
        for (int i = 0; i < CelestialBedrockOre.BODIES.size(); i++) {
            List<BedrockOreType> types = CelestialBedrockOre.get(CelestialBedrockOre.BODIES.get(i));
            if (types == null || types.size() != perBody[i]) errors.add(CelestialBedrockOre.BODIES.get(i) + " has " + types);
        }
        for (int i = 0; i < all.size(); i++) {
            BedrockOreType type = all.get(i);
            if (CelestialBedrockOre.index(type) != i) errors.add(type + " index " + CelestialBedrockOre.index(type));
            if (BedrockOreType.byName(type.name()) != type) errors.add(type + " not in HBM's registry");
            if (type.outputs().size() != 4) errors.add(type + " has " + type.outputs().size() + " outputs");
            for (BedrockOreOutput out : type.outputs()) {
                if (ModAutogenItems.get(MaterialShapes.FRAGMENT, out.material()) == null) {
                    errors.add(type + ": no bedrock ore fragment of " + out.material().name);
                }
            }
            for (Grade grade : BedrockOreItems.grades()) {
                if (ModBedrockOreItems.get(grade, type) != BedrockOreItems.get(grade, type)) {
                    errors.add(type + " " + grade + " not mapped into HBM's lookup");
                }
            }
        }
        // hand-checked rows of hbm-space's ItemBedrockOreNew.CelestialBedrockOre
        expectRow(errors, "kerbin", "light", Mats.MAT_IRON, 18, Mats.MAT_COPPER, 9, Mats.MAT_CRYOLITE, 6, Mats.MAT_SODIUM, 3);
        expectRow(errors, "duna", "actinide", Mats.MAT_THORIUM, 18, Mats.MAT_RADIUM, 9, Mats.MAT_POLONIUM, 6, Mats.MAT_U233, 3);
        expectRow(errors, "eve", "schrabidic", Mats.MAT_SCHRABIDIUM, 18, Mats.MAT_SOLINIUM, 9, Mats.MAT_GHIORSIUM, 6,
                Mats.MAT_SCHRABIDIUM, 3);
        expectRow(errors, "tekto", "plastic", Mats.MAT_POLYMER, 18, Mats.MAT_RUBBER, 9, Mats.matByName.get("semtex"), 6,
                Mats.MAT_PVC, 3);
        expectRow(errors, "dres", "light", Mats.matByName.get("nickel"), 18, Mats.MAT_TITANIUM, 9, Mats.MAT_CADMIUM, 6,
                Mats.matByName.get("gallium"), 3);
        // 1.7 getTotalTypeCount and the first index of each body (the noise salt Populate17 uses)
        int[] start = {0, 4, 9, 14, 20, 26, 32, 38, 42, 48};
        for (int i = 0; i < start.length; i++) {
            BedrockOreType first = CelestialBedrockOre.get(CelestialBedrockOre.BODIES.get(i)).get(0);
            if (CelestialBedrockOre.index(first) != start[i]) errors.add(first + " starts at " + CelestialBedrockOre.index(first));
        }
        finish(helper, errors);
    }

    private static void expectRow(List<String> errors, String body, String suffix, NTMMaterial p, int pa, NTMMaterial a,
                                  int aa, NTMMaterial s, int sa, NTMMaterial r, int ra) {
        BedrockOreType type = BedrockOreType.byName("ntmspace:" + body + "_" + suffix);
        if (type == null) {
            errors.add("no type " + body + "_" + suffix);
            return;
        }
        if (!same(type.primary1, p, pa) || !same(type.byproductAcid1, a, aa) || !same(type.byproductSolvent1, s, sa)
                || !same(type.byproductRad1, r, ra)) {
            errors.add(type + " outputs " + type.outputs());
        }
    }

    private static boolean same(BedrockOreOutput out, NTMMaterial mat, int amount) {
        return out != null && out.material() == mat && out.amount() == amount;
    }

    private static ItemStack ore(Grade grade, BedrockOreType type) {
        return new ItemStack(BedrockOreItems.get(grade, type));
    }

    private static ItemStack ore(Grade grade, BedrockOreType type, int count) {
        return new ItemStack(BedrockOreItems.get(grade, type), count);
    }

    /** 1.7 {@code ItemBedrockOreNew.toFluid}: only a smeltable material melts, ceil(FRAGMENT.q(amount) * mult). */
    private static Mats.@Nullable MaterialStack toFluid(BedrockOreOutput out, double mult) {
        if (out.material().smeltable != NTMMaterial.SmeltingBehavior.SMELTABLE) return null;
        return new Mats.MaterialStack(out.material(), (int) Math.ceil((double) MaterialShapes.FRAGMENT.quantity * out.amount() * mult));
    }

    /** 1.7 {@code ItemBedrockOreNew.extract}: min(ceil(amount * mult), 64) fragments. */
    private static ItemStack extract(BedrockOreOutput out, double mult) {
        return new ItemStack(ModAutogenItems.get(MaterialShapes.FRAGMENT, out.material()),
                Math.min((int) Math.ceil(out.amount() * mult), 64));
    }

    private static boolean equal(@Nullable ItemStack a, ItemStack b) {
        return a != null && !a.isEmpty() && a.getItem() == b.getItem() && a.getCount() == b.getCount();
    }

    private static boolean equal(Mats.@Nullable MaterialStack a, Mats.@Nullable MaterialStack b) {
        if (a == null || b == null) return a == b;
        return a.material == b.material && a.amount == b.amount;
    }

    private static void rows(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        for (BedrockOreType t : CelestialBedrockOre.getAllTypes()) {
            BedrockOreOutput primary = t.primary1;

            // ArcFurnaceRecipes
            arcSolid(errors, t, Grade.SULFURIC_BYPRODUCT, Grade.SULFURIC_ARC, 2);
            arcSolid(errors, t, Grade.SULFURIC_ROASTED, Grade.SULFURIC_ARC, 4);
            arcSolid(errors, t, Grade.SOLVENT_BYPRODUCT, Grade.SOLVENT_ARC, 2);
            arcSolid(errors, t, Grade.SOLVENT_ROASTED, Grade.SOLVENT_ARC, 4);
            arcSolid(errors, t, Grade.RAD_BYPRODUCT, Grade.RAD_ARC, 2);
            arcSolid(errors, t, Grade.RAD_ROASTED, Grade.RAD_ARC, 4);
            arcMolten(errors, t, Grade.PRIMARY_FIRST, toFluid(primary, 5));
            arcMolten(errors, t, Grade.CRUMBS, toFluid(primary, 1));
            arcMolten(errors, t, Grade.SULFURIC_WASHED, toFluid(t.byproductAcid1, 3));
            arcMolten(errors, t, Grade.SOLVENT_WASHED, toFluid(t.byproductSolvent1, 3));
            arcMolten(errors, t, Grade.RAD_WASHED, toFluid(t.byproductRad1, 3));

            // CentrifugeRecipes
            ItemStack gravel = new ItemStack(Items.GRAVEL);
            centrifuge(errors, t, Grade.BASE, ore(Grade.PRIMARY, t), gravel);
            centrifuge(errors, t, Grade.BASE_ROASTED, ore(Grade.PRIMARY, t), gravel);
            centrifuge(errors, t, Grade.BASE_WASHED, ore(Grade.PRIMARY, t), ore(Grade.PRIMARY, t), gravel);
            centrifuge(errors, t, Grade.PRIMARY_SULFURIC, ore(Grade.PRIMARY_NOSULFURIC, t, 2), ore(Grade.SULFURIC_BYPRODUCT, t, 2));
            centrifuge(errors, t, Grade.PRIMARY_SOLVENT, ore(Grade.PRIMARY_NOSOLVENT, t, 2), ore(Grade.SULFURIC_BYPRODUCT, t, 2),
                    ore(Grade.SOLVENT_BYPRODUCT, t, 2));
            centrifuge(errors, t, Grade.PRIMARY_RAD, ore(Grade.PRIMARY_NORAD, t, 2), ore(Grade.SULFURIC_BYPRODUCT, t, 2),
                    ore(Grade.SOLVENT_BYPRODUCT, t, 2), ore(Grade.RAD_BYPRODUCT, t, 2));
            centrifuge(errors, t, Grade.PRIMARY, extract(primary, 1));
            centrifuge(errors, t, Grade.PRIMARY_ROASTED, extract(primary, 1));
            centrifuge(errors, t, Grade.PRIMARY_NOSULFURIC, extract(primary, 1), ore(Grade.CRUMBS, t));
            centrifuge(errors, t, Grade.PRIMARY_NOSOLVENT, extract(primary, 1), ore(Grade.CRUMBS, t));
            centrifuge(errors, t, Grade.PRIMARY_NORAD, extract(primary, 1), ore(Grade.CRUMBS, t));
            centrifuge(errors, t, Grade.PRIMARY_FIRST, extract(primary, 1), extract(primary, 1), ore(Grade.CRUMBS, t));
            centrifuge(errors, t, Grade.SULFURIC_WASHED, extract(t.byproductAcid1, 1), ore(Grade.CRUMBS, t));
            centrifuge(errors, t, Grade.SOLVENT_WASHED, extract(t.byproductSolvent1, 1), ore(Grade.CRUMBS, t));
            centrifuge(errors, t, Grade.RAD_WASHED, extract(t.byproductRad1, 1), ore(Grade.CRUMBS, t));

            // CombinationRecipes and PyroOvenRecipes: roasting, 50 mB vitriol
            Grade[][] roast = {{Grade.BASE, Grade.BASE_ROASTED}, {Grade.PRIMARY, Grade.PRIMARY_ROASTED},
                    {Grade.SULFURIC_BYPRODUCT, Grade.SULFURIC_ROASTED}, {Grade.SOLVENT_BYPRODUCT, Grade.SOLVENT_ROASTED},
                    {Grade.RAD_BYPRODUCT, Grade.RAD_ROASTED}};
            for (Grade[] pair : roast) {
                CombinationRecipes.CombinationRecipe combo = CombinationRecipes.getOutput(ore(pair[0], t));
                if (combo == null || !equal(combo.itemOutput(), ore(pair[1], t)) || combo.fluidOutput() == null
                        || combo.fluidOutput().type != Fluids.VITRIOL || combo.fluidOutput().fill != 50) {
                    errors.add("combination " + t + " " + pair[0]);
                }
                PyroOvenRecipes.PyroOvenRecipe pyro = pyro(ore(pair[0], t));
                if (pyro == null || !equal(pyro.outputItem, ore(pair[1], t)) || pyro.outputFluid == null
                        || pyro.outputFluid.type != Fluids.VITRIOL || pyro.outputFluid.fill != 50 || pyro.duration != 10) {
                    errors.add("pyrolysis " + t + " " + pair[0]);
                }
            }

            // CrystallizerRecipes
            crystallizer(errors, t, Grade.BASE, Fluids.WATER, Grade.BASE_WASHED, 100, 1, 250);
            crystallizer(errors, t, Grade.BASE_ROASTED, Fluids.WATER, Grade.BASE_WASHED, 100, 1, 250);
            crystallizer(errors, t, Grade.PRIMARY, Fluids.SULFURIC_ACID, Grade.PRIMARY_SULFURIC, 200, 1, 250);
            crystallizer(errors, t, Grade.PRIMARY_ROASTED, Fluids.SULFURIC_ACID, Grade.PRIMARY_SULFURIC, 200, 1, 250);
            for (Grade in : new Grade[] {Grade.PRIMARY, Grade.PRIMARY_ROASTED, Grade.PRIMARY_NOSULFURIC}) {
                crystallizer(errors, t, in, Fluids.SOLVENT, Grade.PRIMARY_SOLVENT, 200, 1, 250);
            }
            for (Grade in : new Grade[] {Grade.PRIMARY, Grade.PRIMARY_ROASTED, Grade.PRIMARY_NOSULFURIC, Grade.PRIMARY_NOSOLVENT}) {
                crystallizer(errors, t, in, Fluids.RADIOSOLVENT, Grade.PRIMARY_RAD, 200, 1, 250);
            }
            Grade[][] wash = {{Grade.SULFURIC_BYPRODUCT, Grade.SULFURIC_ROASTED, Grade.SULFURIC_ARC, Grade.SULFURIC_WASHED},
                    {Grade.SOLVENT_BYPRODUCT, Grade.SOLVENT_ROASTED, Grade.SOLVENT_ARC, Grade.SOLVENT_WASHED},
                    {Grade.RAD_BYPRODUCT, Grade.RAD_ROASTED, Grade.RAD_ARC, Grade.RAD_WASHED}};
            for (Grade[] chain : wash) {
                for (int i = 0; i < 3; i++) crystallizer(errors, t, chain[i], Fluids.WATER, chain[3], 100, 4, 250);
            }
            for (Grade in : new Grade[] {Grade.PRIMARY, Grade.PRIMARY_ROASTED, Grade.PRIMARY_SULFURIC,
                    Grade.PRIMARY_NOSULFURIC, Grade.PRIMARY_SOLVENT, Grade.PRIMARY_NOSOLVENT, Grade.PRIMARY_RAD,
                    Grade.PRIMARY_NORAD}) {
                crystallizer(errors, t, in, Fluids.HYDROGEN, Grade.PRIMARY_FIRST, 200, 1, 250);
            }
            crystallizer(errors, t, Grade.CRUMBS, Fluids.SLOP, Grade.BASE, 200, 64, 1000);

            // ElectrolyserMetalRecipes.makeBedrockOreProduct
            electrolysis(errors, t, Grade.PRIMARY_FIRST, primary, 8, ore(Grade.CRUMBS, t));
            electrolysis(errors, t, Grade.CRUMBS, primary, 2, null);

            // ShredderRecipes: bedrock_ore, any metadata, to gravel
            for (Grade grade : BedrockOreItems.grades()) {
                if (!ShredderRecipes.getShredderResult(ore(grade, t)).is(Items.GRAVEL)) {
                    errors.add("shredder " + t + " " + grade);
                    break;
                }
            }
        }

        // hand-computed spot values (1.7 arithmetic, FRAGMENT = 8, INGOT = 72)
        BedrockOreType dunaLight = BedrockOreType.byName("ntmspace:duna_light");
        ItemStack[] dunaPrimary = CentrifugeRecipes.getOutput(ore(Grade.PRIMARY, dunaLight));
        if (dunaPrimary == null || dunaPrimary.length != 1 || dunaPrimary[0].getCount() != 18
                || dunaPrimary[0].getItem() != ModAutogenItems.get(MaterialShapes.FRAGMENT, Mats.MAT_IRON)) {
            errors.add("Duna light primary is not 18 iron fragments");
        }
        BedrockOreType eveSchrab = BedrockOreType.byName("ntmspace:eve_schrabidic");
        ElectrolyserMetalRecipes.ElectrolysisMetalRecipe schrab = ElectrolyserMetalRecipes.getRecipe(ore(Grade.PRIMARY_FIRST, eveSchrab));
        if (schrab == null || schrab.output1 == null || schrab.output1.material != Mats.MAT_SCHRABIDIUM || schrab.output1.amount != 1152) {
            errors.add("Eve schrabidic first fraction does not melt to 1152 schrabidium");
        }
        BedrockOreType kerbinNonmetal = BedrockOreType.byName("ntmspace:kerbin_nonmetal");
        ElectrolyserMetalRecipes.ElectrolysisMetalRecipe coal = ElectrolyserMetalRecipes.getRecipe(ore(Grade.CRUMBS, kerbinNonmetal));
        if (coal == null || coal.output1.material != Mats.MAT_SLAG || coal.output1.amount != 144 || coal.byproduct.length != 1
                || coal.byproduct[0].getCount() != 36) {
            errors.add("Kerbin non-metal crumbs are not slag 144 + 36 coal fragments");
        }
        finish(helper, errors);
    }

    private static void arcSolid(List<String> errors, BedrockOreType t, Grade in, Grade out, int count) {
        ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(ore(in, t), false);
        if (recipe == null || !equal(recipe.solidOutput, ore(out, t, count))) errors.add("arc furnace " + t + " " + in);
    }

    private static void arcMolten(List<String> errors, BedrockOreType t, Grade in, Mats.@Nullable MaterialStack expected) {
        ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(ore(in, t), true);
        if (expected == null) {
            if (recipe != null) errors.add("arc furnace " + t + " " + in + " melts but 1.7 had no output");
            return;
        }
        if (recipe == null || recipe.fluidOutput.length != 1 || !equal(recipe.fluidOutput[0], expected)) {
            errors.add("arc furnace " + t + " " + in + " molten");
        }
    }

    private static void centrifuge(List<String> errors, BedrockOreType t, Grade in, ItemStack... expected) {
        ItemStack[] out = CentrifugeRecipes.getOutput(ore(in, t));
        boolean ok = out != null && out.length == expected.length;
        for (int i = 0; ok && i < expected.length; i++) ok = equal(out[i], expected[i]);
        if (!ok) errors.add("centrifuge " + t + " " + in);
    }

    private static void crystallizer(List<String> errors, BedrockOreType t, Grade in, FluidType fluid, Grade out,
                                     int duration, int count, int acid) {
        CrystallizerRecipes.CrystallizerRecipe recipe = CrystallizerRecipes.getOutput(ore(in, t, count), fluid);
        if (recipe == null || !equal(recipe.output, ore(out, t)) || recipe.duration != duration
                || recipe.input.stacksize != count || recipe.acidAmount != acid) {
            errors.add("crystallizer " + t + " " + in + " with " + fluid.getName());
        }
    }

    private static void electrolysis(List<String> errors, BedrockOreType t, Grade in, BedrockOreOutput primary, int mult,
                                     @Nullable ItemStack extra) {
        ElectrolyserMetalRecipes.ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(ore(in, t));
        if (recipe == null) {
            errors.add("electrolyser " + t + " " + in + " missing");
            return;
        }
        Mats.MaterialStack melt = toFluid(primary, mult);
        List<ItemStack> solid = new ArrayList<>();
        if (melt == null) solid.add(extract(primary, mult));
        if (extra != null) solid.add(extra);
        Mats.MaterialStack first = melt != null ? melt : new Mats.MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.quantity * 2);
        boolean ok = equal(recipe.output1, first) && recipe.output2 == null && recipe.duration == 20
                && recipe.byproduct.length == solid.size();
        for (int i = 0; ok && i < solid.size(); i++) ok = equal(recipe.byproduct[i], solid.get(i));
        if (!ok) errors.add("electrolyser " + t + " " + in);
    }

    private static PyroOvenRecipes.@Nullable PyroOvenRecipe pyro(ItemStack input) {
        for (PyroOvenRecipes.PyroOvenRecipe recipe : PyroOvenRecipes.getAll()) {
            if (recipe.inputFluid == null && recipe.inputItem != null && recipe.inputItem.matchesRecipe(input, true)) return recipe;
        }
        return null;
    }

    private static void removals(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        for (BedrockOreType type : BedrockOreType.VALUES) {
            if (BedrockOreChainRecipes.chainTypes().contains(type)) errors.add("pyro oven still covers " + type);
            ItemStack base = ModBedrockOreItems.make(Grade.BASE, type);
            ItemStack primary = ModBedrockOreItems.make(Grade.PRIMARY, type);
            ItemStack crumbs = ModBedrockOreItems.make(Grade.CRUMBS, type);
            ItemStack sulfuric = ModBedrockOreItems.make(Grade.SULFURIC_BYPRODUCT, type);
            if (CentrifugeRecipes.getOutput(base) != null) errors.add("centrifuge keeps HBM's " + type);
            if (CombinationRecipes.getOutput(base) != null) errors.add("combination keeps HBM's " + type);
            if (CrystallizerRecipes.getOutput(primary, Fluids.SULFURIC_ACID) != null) errors.add("crystallizer keeps HBM's " + type);
            if (ElectrolyserMetalRecipes.getRecipe(crumbs) != null) errors.add("electrolyser keeps HBM's " + type);
            if (ArcFurnaceRecipes.getOutput(sulfuric, false) != null) errors.add("arc furnace keeps HBM's " + type);
            if (ShredderRecipes.getShredderResult(base).is(Items.GRAVEL)) errors.add("shredder keeps HBM's " + type);
            if (pyro(base) != null) errors.add("pyro oven keeps HBM's " + type);
        }
        finish(helper, errors);
    }

    /*
     * The GameTest server has the overworld only, so the per-body checks read HBM's field by dimension key and seed
     * (ItemBedrockOreBase.field(dimension, seed, x, z), what setOreAmount(level, ...) reads), and setOreAmount itself
     * runs on the overworld (Kerbin). The harness scenario BedrockSatellites samples on Duna for real.
     */

    private static void rawOre(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        long seed = overworld.getSeed();
        fieldOf(errors, SpaceDimensions.DUNA, seed, "duna", 413_016, 100, -200);
        fieldOf(errors, Level.OVERWORLD, seed, "kerbin", 0, 3_000, 77);
        // Thatmo: Kerbin's types, its own dimension id as the salt (1.7 CelestialBody.getEnum fell back to Kerbin)
        fieldOf(errors, SpaceDimensions.THATMO, seed, "kerbin", 413_025, -5, 9);
        fieldOf(errors, Level.NETHER, seed, "kerbin", -1, 40, 40);

        // a stack mined on Kerbin: the composition replaces HBM's six densities
        int x = 3_000, z = 77;
        double mult = 1.2D;
        ItemStack stack = new ItemStack(ModOreBlocks.bedrock_ore_base.get());
        ItemBedrockOreBase.setOreAmount(overworld, stack, x, z, mult);
        ItemBedrockOreBase.Composition composition = ItemBedrockOreBase.COMPOSITION.get(stack).orElse(null);
        if (composition == null || !composition.origin().equals("body.kerbin")) {
            errors.add("composition " + composition);
            finish(helper, errors);
            return;
        }
        if (ItemBedrockOreBase.DENSITY.has(stack)) errors.add("the stack still carries HBM's six densities");
        List<BedrockOreType> kerbin = CelestialBedrockOre.get("kerbin");
        if (!ItemBedrockOreBase.types(stack).equals(kerbin)) errors.add("types " + ItemBedrockOreBase.types(stack));
        for (BedrockOreType type : kerbin) {
            double expected = Populate17.oreLevel(seed, 0, x, z, CelestialBedrockOre.index(type)) * mult;
            if (Double.doubleToLongBits(ItemBedrockOreBase.getOreAmount(stack, type)) != Double.doubleToLongBits(expected)) {
                errors.add(type + ": " + ItemBedrockOreBase.getOreAmount(stack, type) + " != " + expected);
            }
        }
        for (BedrockOreType type : BedrockOreType.VALUES) {
            if (ItemBedrockOreBase.getOreAmount(stack, type) != 0D) errors.add("the stack has HBM's " + type);
        }
        if (!ItemBedrockOreBase.COMPOSITION.get(stack.copy()).equals(ItemBedrockOreBase.COMPOSITION.get(stack))) {
            errors.add("the composition did not survive a copy");
        }
        Tag saved = ItemBedrockOreBase.Composition.CODEC.encodeStart(NbtOps.INSTANCE, composition).result().orElseThrow();
        if (!saved.toString().contains("ntmspace:kerbin_light")) errors.add("composition not saved by type name: " + saved);

        // the slopper pays a Duna raw ore out as Duna BASE ores
        for (BedrockOreType type : CelestialBedrockOre.get("duna")) {
            if (ModProduction2Blocks.bedrockOreBase(type) != BedrockOreItems.get(Grade.BASE, type)) {
                errors.add("slopper output of " + type);
            }
        }
        finish(helper, errors);
    }

    private static void fieldOf(List<String> errors, net.minecraft.resources.ResourceKey<Level> dimension, long seed,
                                String body, int salt, int x, int z) {
        ItemBedrockOreBase.OreField field = ItemBedrockOreBase.field(dimension, seed, x, z);
        if (!("body." + body).equals(field.origin())) errors.add(dimension + ": origin " + field.origin());
        if (SpaceBedrockOres.dimensionId(dimension) != salt) {
            errors.add(dimension + ": salt " + SpaceBedrockOres.dimensionId(dimension));
        }
        List<BedrockOreType> types = CelestialBedrockOre.get(body);
        if (!new ArrayList<>(field.levels().keySet()).equals(types)) {
            errors.add(dimension + ": types " + field.levels().keySet());
            return;
        }
        for (BedrockOreType type : types) {
            double expected = Populate17.oreLevel(seed, salt, x, z, CelestialBedrockOre.index(type));
            if (Double.doubleToLongBits(field.levels().get(type)) != Double.doubleToLongBits(expected)) {
                errors.add(dimension + " " + type + ": " + field.levels().get(type) + " != " + expected);
            }
        }
    }

    private static void field(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        long seed = helper.getLevel().getSeed();
        ItemBedrockOreBase.OreField dres = ItemBedrockOreBase.field(SpaceDimensions.DRES, seed, 10, 10);
        ItemBedrockOreBase.OreField eve = ItemBedrockOreBase.field(SpaceDimensions.EVE, seed, 10, 10);
        ItemBedrockOreBase.OreField mun = ItemBedrockOreBase.field(SpaceDimensions.MUN, seed, 10, 10);
        if (dres.boreFluid() == null || dres.boreFluid().type != Fluids.NITRIC_ACID || dres.boreFluid().fill != 500) {
            errors.add("Dres bore fluid " + dres.boreFluid());
        }
        if (eve.boreFluid() == null || eve.boreFluid().type != SpaceFluids.KMnO4 || eve.boreFluid().fill != 500) {
            errors.add("Eve bore fluid " + eve.boreFluid());
        }
        if (mun.boreFluid() != null) errors.add("the Mun names a bore fluid");
        if (!new ArrayList<>(mun.levels().keySet()).equals(CelestialBedrockOre.get("mun"))) errors.add("Mun types " + mun.levels());
        double total = 0;
        for (double level : mun.levels().values()) total += level;
        if (mun.average() != total / mun.levels().size()) errors.add("Mun average " + mun.average());
        // the scanner's field where a player stands on the overworld is Kerbin's, never HBM's six
        ItemBedrockOreBase.OreField kerbin = ItemBedrockOreBase.field(helper.getLevel(), 0, 0);
        if (!new ArrayList<>(kerbin.levels().keySet()).equals(CelestialBedrockOre.get("kerbin"))) {
            errors.add("Kerbin field " + kerbin.levels());
        }
        finish(helper, errors);
    }

    // ---------------------------------------------------------------------------------------------- satellites

    private static void satelliteSave(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        SatelliteSavedData data = new SatelliteSavedData();
        SatelliteRailgun railgun = (SatelliteRailgun) SpaceSatellites.RAILGUN.create();
        railgun.lastOp = 1234L;
        railgun.setTarget(7, -9);
        railgun.tx = "7;-9";
        SatelliteOrbit settings = new SatelliteOrbit(42F, 110F, 400F, true, 2F, "Dev", 0.25F, 0.5F, 0.75F);
        settings.write(railgun.extra);
        data.put(11, railgun);
        data.put(12, SpaceSatellites.DYSON_RELAY.create());
        data.put(13, SatelliteType.MAPPER.create());

        Tag saved = SatelliteSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();
        String text = saved.toString();
        if (!text.contains("ntmspace:railgun") || !text.contains("ntmspace:dyson_relay") || !text.contains("mapper")) {
            errors.add("not saved by name: " + text);
        }
        SatelliteSavedData read = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, saved).result().orElseThrow();
        Satellite back = read.getSatFromFreq(11);
        if (!(back instanceof SatelliteRailgun gun) || gun.lastOp != 1234L || gun.targetX != 7 || gun.targetZ != -9
                || !gun.tx.equals("7;-9")) {
            errors.add("railgun read back as " + back);
        } else {
            SatelliteOrbit orbit = SpaceSatellites.orbit(gun);
            // 1.7: the phase is normalised (400 -> 40) and the blink period clamped to [0.3, 1]
            if (!orbit.equals(settings) || orbit.phaseOffset() != 40F || orbit.blinkPeriod() != 1F) {
                errors.add("orbit settings read back as " + orbit);
            }
        }
        if (read.getSatFromFreq(12) == null || read.getSatFromFreq(12).type() != SpaceSatellites.DYSON_RELAY) errors.add("dyson relay");
        if (read.getSatFromFreq(13) == null || read.getSatFromFreq(13).type() != SatelliteType.MAPPER) errors.add("mapper");

        finish(helper, errors);
    }

    private static int freeFrequency(ServerLevel level) {
        java.util.Random random = new java.util.Random();
        while (true) {
            int freq = 500_000 + random.nextInt(400_000);
            if (!SatelliteSavedData.get(level).isFreqTaken(freq)) return freq;
        }
    }

    /*
     * The GameTest server has the overworld only: the routing is checked by level key (the Environment.SATELLITES
     * answer), and a real launch on the level the test runs in stays there. The harness scenario BedrockSatellites
     * launches on the Mun and lists the satellites on the Mun and on Kerbin.
     */
    private static void satelliteOnMun(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        MinecraftServer server = helper.getLevel().getServer();
        ItemStack mapper = XSatelliteRegistry.stackFor(SatelliteType.MAPPER);
        ItemStack relay = XSatelliteRegistry.stackFor(SatelliteType.RELAY);
        ItemStack lunar = XSatelliteRegistry.stackFor(SatelliteType.LUNAR_MINER);
        ItemStack miner = XSatelliteRegistry.stackFor(SatelliteType.MINER);
        BlockPos ground = new BlockPos(0, 80, 0);

        // launched on the Mun: a mapper stays on the Mun (its own data), a relay goes to Duna, miners to the Mun / Dres
        if (SpaceSatellites.satelliteLevelKey(server, SpaceDimensions.MUN, ground, mapper) != null) errors.add("a Mun mapper leaves the Mun");
        if (SpaceSatellites.satelliteLevelKey(server, SpaceDimensions.MUN, ground, ItemStack.EMPTY) != null) {
            errors.add("the Mun reads another level's satellites");
        }
        if (!SpaceDimensions.DUNA.equals(SpaceSatellites.satelliteLevelKey(server, SpaceDimensions.MUN, ground, relay))) {
            errors.add("relay not to Duna");
        }
        if (!SpaceDimensions.MUN.equals(SpaceSatellites.satelliteLevelKey(server, Level.OVERWORLD, ground, lunar))) {
            errors.add("lunar miner not to the Mun");
        }
        if (!SpaceDimensions.DRES.equals(SpaceSatellites.satelliteLevelKey(server, SpaceDimensions.MUN, ground, miner))) {
            errors.add("miner not to Dres");
        }

        // a station orbiting the Mun reads the Mun's satellites
        StationData stations = StationData.get(server);
        int grid = 3_000 + new java.util.Random().nextInt(1_000);
        stations.addStation(grid, grid, SolarSystem.body("mun"));
        BlockPos inStation = new BlockPos(grid * 1024 + 512, 130, grid * 1024 + 512);
        if (!SpaceDimensions.MUN.equals(SpaceSatellites.satelliteLevelKey(server, Orbit.ORBIT, inStation, ItemStack.EMPTY))) {
            errors.add("a station orbiting the Mun does not read the Mun's satellites");
        }

        // a real launch on this level: listed here, with the chip's orbit settings
        ServerLevel level = helper.getLevel();
        int freq = freeFrequency(level);
        SpaceSatellites.ORBIT.set(mapper, new SatelliteOrbit(30F, 90F, 0F, false, 0.3F, "Dev", 1F, 0F, 0F));
        XSatelliteRegistry.orbit(level, mapper, freq, 0, 80, 0);
        Satellite sat = SatelliteSavedData.get(level).getSatFromFreq(freq);
        if (sat == null || sat.type() != SatelliteType.MAPPER) errors.add("the mapper is not in this level's satellites");
        if (sat != null && SpaceSatellites.orbit(sat).inclination() != 30F) errors.add("orbit settings not copied at launch");
        SatelliteSavedData.get(level).descend(freq);

        if (XSatelliteRegistry.typeFor(new ItemStack(SpaceSatellites.SAT_WAR.get())) != SpaceSatellites.RAILGUN
                || XSatelliteRegistry.typeFor(new ItemStack(SpaceSatellites.SAT_DYSON_RELAY.get())) != SpaceSatellites.DYSON_RELAY) {
            errors.add("the new satellite items launch the wrong satellites");
        }
        finish(helper, errors);
    }

    private static void satelliteLinker(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ItemStack source = XSatelliteRegistry.stackFor(SatelliteType.RADAR);
        ItemStack copy = XSatelliteRegistry.stackFor(SatelliteType.SCANNER);
        SatelliteOrbit settings = new SatelliteOrbit(-45F, 120F, 90F, true, 0.5F, "Linker", 0.1F, 0.2F, 0.3F);
        SpaceSatellites.ORBIT.set(source, settings);
        if (!com.hbm.tileentity.machine.TileEntityMachineSatLinker.CHIP_COPY.invoker().copy(source, copy)) {
            errors.add("the linker hook reported no change");
        }
        if (!settings.equals(SpaceSatellites.ORBIT.get(copy).orElse(null))) errors.add("settings not copied");
        if (com.hbm.tileentity.machine.TileEntityMachineSatLinker.CHIP_COPY.invoker().copy(source, copy)) {
            errors.add("an unchanged copy reported a change");
        }
        ItemStack chip = new ItemStack(ModSatelliteItems.sat_chip.get());
        if (SpaceSatellites.copyItemData(source, chip) || SpaceSatellites.ORBIT.has(chip)) {
            errors.add("settings copied onto an ID chip");
        }
        // defaults: the satellite's registered colour (1.7 XSatelliteRegistry)
        SatelliteOrbit radar = SpaceSatellites.orbit(XSatelliteRegistry.stackFor(SatelliteType.RADAR));
        if (radar.colorR() != 0.134F || radar.colorG() != 1.0F || radar.altitude() != 100F || !radar.owner().equals("None")) {
            errors.add("radar defaults " + radar);
        }
        finish(helper, errors);
    }

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (!errors.isEmpty()) {
            helper.fail(errors.size() + " problem(s): " + String.join("; ", errors.subList(0, Math.min(12, errors.size()))));
            return;
        }
        helper.succeed();
    }
}
