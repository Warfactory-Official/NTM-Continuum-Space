package com.ntmspace.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.handler.NTMToolHandler;
import com.hbm.inventory.material.MaterialDistribution;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CompressorRecipes;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.ReformingRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.inventory.recipes.StorageDrumRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.ntmspace.NtmSpace;
import dev.golem.core.data.DataFiles;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.RecipeTables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/**
 * Proof for the recipe-change family (hbm-space's changes to base HBM's machine tables, expressed as addon data): every
 * file under {@code data/ntmspace/recipe/<hbm table>/} - this family's and every other addon family's - is a row of that
 * table while its requirements hold, equal to the file through the table's own codec; disabled rows stay absent, and
 * every id listed in any
 * {@code data/<ns>/recipe_removals/*.json} of this addon is gone from its table. The per-table classes
 * ({@code RecipeChange*GameTests}) check the rows' values against hbm-space's source.
 */
public final class RecipeChangeGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function ROWS_LOAD = TESTS.function("recipe_change_addon_rows_load", 40,
            () -> RecipeChangeGameTests::rowsLoad);

    public static final GameTests.Function REMOVALS = TESTS.function("recipe_change_removals_absent", 40,
            () -> RecipeChangeGameTests::removalsAbsent);

    private RecipeChangeGameTests() {
    }

    public static void init() {
        // sub-agent test classes (one line each)
        com.ntmspace.gametest.RecipeChangeSpecialGameTests.init();
        com.ntmspace.gametest.RecipeChangeMetalGameTests.init();
        com.ntmspace.gametest.RecipeChangeChemGameTests.init();
        com.ntmspace.gametest.RecipeChangeCraftingGameTests.init();
        com.ntmspace.gametest.RecipeChangeNuclearGameTests.init();
        com.ntmspace.gametest.RecipeChangeAssemblyGameTests.init();
        com.ntmspace.gametest.RecipeChangeReplacementGameTests.init();
    }

    /**
     * HBM's tables by folder name ({@code hbm:<name>}).
     */
    static Map<String, RecipeTables.Table<?>> tables() {
        Map<String, RecipeTables.Table<?>> t = new LinkedHashMap<>();
        t.put("ammo_press", AmmoPressRecipes.TABLE.table);
        t.put("annihilator", AnnihilatorRecipes.TABLE.table);
        t.put("anvil_construction", AnvilRecipes.CONSTRUCTION.table);
        t.put("anvil_smithing", AnvilRecipes.SMITHING.table);
        t.put("arc_furnace", ArcFurnaceRecipes.TABLE.table);
        t.put("arc_welder", ArcWelderRecipes.TABLE.table);
        t.put("assembly_machine", AssemblyMachineRecipes.INSTANCE.table);
        t.put("blast_furnace", BlastFurnaceRecipesNT.INSTANCE.table);
        t.put("breeder", BreederRecipes.TABLE.table);
        t.put("centrifuge", CentrifugeRecipes.TABLE.table);
        t.put("chemical_plant", ChemicalPlantRecipes.INSTANCE.table);
        t.put("coker", CokerRecipes.TABLE.table.table);
        t.put("combination", CombinationRecipes.TABLE.table);
        t.put("compressor", CompressorRecipes.TABLE.table);
        t.put("cracking", CrackingRecipes.TABLE.table.table);
        t.put("crucible", CrucibleRecipes.TABLE.table);
        t.put("crystallizer", CrystallizerRecipes.TABLE.table);
        t.put("cyclotron", CyclotronRecipes.TABLE.table);
        t.put("difurnace", DiFurnaceRecipes.INSTANCE.table);
        t.put("electrolyser_fluid", ElectrolyserFluidRecipes.TABLE.table.table);
        t.put("electrolyser_metal", ElectrolyserMetalRecipes.TABLE.table);
        t.put("exposure_chamber", ExposureChamberRecipes.INSTANCE.table);
        t.put("fluid_breeder", FluidBreederRecipes.INSTANCE.table);
        t.put("fraction", FractionRecipes.TABLE.table);
        t.put("fusion", FusionRecipes.INSTANCE.table);
        t.put("gas_centrifuge", GasCentrifugeRecipes.DISPLAY.table);
        t.put("pseudo_fluid", GasCentrifugeRecipes.PSEUDO_FLUIDS.table);
        t.put("hydrotreating", HydrotreatingRecipes.TABLE.table.table);
        t.put("lemegeton", LemegetonRecipes.TABLE.table);
        t.put("liquefaction", LiquefactionRecipes.TABLE.table);
        t.put("magic", MagicRecipes.TABLE.table);
        t.put("material_distribution", MaterialDistribution.TABLE.table);
        t.put("mixer", MixerRecipes.INSTANCE.table);
        t.put("outgasser", OutgasserRecipes.INSTANCE.table);
        t.put("particle_accelerator", ParticleAcceleratorRecipes.TABLE.table);
        t.put("pedestal", PedestalRecipes.TABLE.table);
        t.put("plasma_forge", PlasmaForgeRecipes.INSTANCE.table);
        t.put("precision_assembler", PrecAssRecipes.INSTANCE.table);
        t.put("press", PressRecipes.TABLE.table);
        t.put("purex", PUREXRecipes.INSTANCE.table);
        t.put("pyrolysis", PyroOvenRecipes.TABLE.table);
        t.put("radiolysis", RadiolysisRecipes.TABLE.table.table);
        t.put("refinery", RefineryRecipes.TABLE.table.table);
        t.put("vacuum_distill", RefineryRecipes.VACUUM.table.table);
        t.put("reforming", ReformingRecipes.TABLE.table.table);
        t.put("rotary_furnace", RotaryFurnaceRecipes.TABLE.table);
        t.put("sawmill", SawmillRecipes.TABLE.table);
        t.put("shredder", ShredderRecipes.TABLE.table);
        t.put("silex", SILEXRecipes.TABLE.table);
        t.put("soldering", SolderingRecipes.TABLE.table);
        t.put("solidification", SolidificationRecipes.TABLE.table.table);
        t.put("storage_drum", StorageDrumRecipes.TABLE.table);
        t.put("tool_conversion", NTMToolHandler.TABLE.table);
        return t;
    }

    private static void rowsLoad(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        HolderLookup.Provider registries = server.registryAccess();
        Map<String, RecipeTables.Table<?>> tables = tables();
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<String, JsonElement> file : DataFiles.read(DataFiles.recipeFolder(), server.getResourceManager())
                                                            .entrySet()) {
            String id = file.getKey();
            if (!id.startsWith(NtmSpace.MOD_ID + ":")) continue;
            String path = id.substring(id.indexOf(':') + 1);
            int slash = path.indexOf('/');
            if (slash < 0) continue;
            RecipeTables.Table<?> table = tables.get(path.substring(0, slash));
            if (table == null || !file.getValue().isJsonObject()) continue;
            JsonObject json = file.getValue().getAsJsonObject();
            if (!json.has("type") || !json.get("type").getAsString().startsWith("hbm:")) continue;
            checked++;
            String problem = compare(table, id, json, registries);
            if (problem != null) errors.add(problem);
        }
        if (checked == 0) errors.add("no addon recipe files found");
        NtmSpace.LOG.info("recipe_change_addon_rows_load: {} addon table rows checked, {} wrong", checked,
                errors.size());
        finish(helper, errors);
    }

    private static <T> String compare(RecipeTables.Table<T> table, String id, JsonObject authored,
                                      HolderLookup.Provider registries) {
        T loaded = null;
        for (RecipeTables.Entry<T> entry : table.entries()) {
            if (entry.id().equals(id)) {
                loaded = entry.value();
                break;
            }
        }
        JsonObject stripped = new JsonObject();
        for (Map.Entry<String, JsonElement> e : authored.entrySet()) {
            if (!e.getKey().equals("type") && !e.getKey().contains(":")) stripped.add(e.getKey(), e.getValue());
        }
        try {
            JsonElement expected = table.toJson(table.fromJson(stripped, registries), registries);
            List<String> literals = new ArrayList<>();
            var requires = authored.get(RecipeTables.REQUIRES);
            if (requires != null) {
                if (requires.isJsonArray())
                    for (var literal : requires.getAsJsonArray()) literals.add(literal.getAsString());
                else literals.add(requires.getAsString());
            }
            if (!RecipeTables.holds(literals, id))
                return loaded == null ? null : id + " loaded while its requirements fail";
            if (loaded == null) return id + " not loaded into " + table.id();
            JsonElement actual = table.toJson(loaded, registries);
            return expected.equals(actual) ? null : id + " differs from its file: " + actual;
        } catch (RuntimeException e) {
            return id + " does not decode: " + e.getMessage();
        }
    }

    private static void removalsAbsent(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Set<String> removed = new HashSet<>();
        for (Map.Entry<String, JsonElement> file : DataFiles.read(RecipeTables.REMOVALS, server.getResourceManager())
                                                            .entrySet()) {
            if (!file.getKey().startsWith(NtmSpace.MOD_ID + ":") || !file.getValue().isJsonObject()) continue;
            for (JsonElement id : file.getValue().getAsJsonObject().getAsJsonArray("recipes"))
                removed.add(id.getAsString());
        }
        List<String> errors = new ArrayList<>();
        if (removed.isEmpty()) errors.add("no removal lists found");
        Set<String> present = new HashSet<>();
        for (RecipeTables.Table<?> table : tables().values()) {
            for (RecipeTables.Entry<?> entry : table.entries()) present.add(entry.id());
        }
        // a removal must name a recipe file that exists (a typo would otherwise pass as "absent")
        Set<String> files = DataFiles.read(DataFiles.recipeFolder(), server.getResourceManager()).keySet();
        for (String id : removed) {
            if (present.contains(id)) errors.add(id + " is still in its table");
            else if (!files.contains(id)) errors.add(id + " names no recipe file");
            // removal lists apply to the whole recipe manager (any type), not only to the machine tables
            String[] parts = id.split(":", 2);
            if (parts.length == 2 && dev.golem.core.world.Recipes.hasRecipe(helper.getLevel(), parts[0], parts[1])) {
                errors.add(id + " is still in the recipe manager");
            }
        }
        NtmSpace.LOG.info("recipe_change_removals_absent: {} removal ids checked", removed.size());
        finish(helper, errors);
    }

    static void finish(GameTestHelper helper, List<String> errors) {
        if (errors.isEmpty()) {
            helper.succeed();
            return;
        }
        String message = errors.size() + " problem(s): " + String.join("; ",
                errors.subList(0, Math.min(20, errors.size())));
        NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
