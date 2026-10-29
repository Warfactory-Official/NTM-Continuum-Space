package com.ntmspace.gametest;

import com.hbm.inventory.FluidContainerStack;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.RecipeTables;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's changes to HBM's assembly machine table ({@code AssemblyMachineRecipes}), checked against values written
 * down from hbm-space's source: every enabled added or changed row as loaded from its file (name, output and count, duration,
 * power, item inputs, fluid inputs, expensive-mode inputs, 528 pool, condition), the row the table's own lookup
 * ({@link AssemblyMachineRecipes#getRecipe}) returns for each name under the current 528 mode, the changed rows at HBM's
 * display position, and the absence of the rows hbm-space dropped (the old custom-missile thruster and fuselage rows,
 * the lunar miner). The rows come from {@code src/recipes} ({@code AssemblyMachineTable}).
 */
public final class RecipeChangeAssemblyGameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function ROWS = TESTS.function("recipe_change_assembly_machine", 40,
            () -> RecipeChangeAssemblyGameTests::rows);

    /**
     * file|name|output|count|duration|power|inputs|fluids|expensive inputs|528 pool|requires; "-" is none. An input is
     * an item id, {@code #tag}, {@code mat:<material>/<shape>}, {@code fc:<fluid>/<mB>} (1.7 fluid dictionary) or
     * {@code fi:<item>/<fluid>} (1.7 full container by fluid meta), then {@code x<count>}. Values from hbm-space's
     * AssemblyMachineRecipes (setup(duration, power), inputItems, inputFluids, inputItemsEx, setPools528; ass.exastro's
     * fluid is xenon in 528 mode and argon otherwise, so it is two files).
     */
    private static final String[] EXPECTED = {
            "alkylation|ass.alkylation|ntmspace:machine_alkylation|1|400|100|#c:plates/stainlessx12,mat:steel/shellx6,hbm:circuit_capacitorx8,hbm:catalyst_clayx12,hbm:coil_tungstenx8|-|#c:plates_welded/stainlessx4,hbm:item_expensive_steel_platingx4,hbm:circuit_capacitorx8,hbm:catalyst_clayx12,hbm:coil_tungstenx8|528.stainless|-",
            "astroturf|ass.astroturf|ntmspace:astroturf|16|100|100|minecraft:dirtx16,#c:ingots/any_plasticx1|-|-|-|-",
            "cryodistil|ass.cryodistil|ntmspace:machine_cryo_distill|1|600|100|mat:steel/castplatex2,#c:plates/stainlessx12,#c:ingots/any_plasticx4,#c:ingots/niobiumx4|-|hbm:item_expensive_heavy_framex2,#c:plates_welded/stainlessx4,#c:ingots/niobiumx4|528.stainless|-",
            "electrolyzer|ass.electrolyzer|hbm:machine_electrolyser|1|200|100|mat:steel/castplatex8,#c:plates/copperx16,mat:titanium/shellx3,#c:ingots/rubberx8,hbm:ingot_firebrickx16,hbm:coil_copperx16,hbm:circuit_basicx8|-|hbm:item_expensive_heavy_framex4,mat:titanium/shellx3,#c:ingots/rubberx8,hbm:ingot_firebrickx16,hbm:coil_copperx16,hbm:circuit_analogx24|-|-",
            "entanglementkit|ass.entanglementkit|hbm:entanglement_kit|1|200|100|mat:dura_steel/castplatex4,#c:plates/copperx24,mat:gold/densewirex16|ntmspace:krypton/8000|-|-|-",
            "exastro|ass.exastro|ntmspace:item_expensive_astro|1|2000|100000|ntmspace:item_expensive_avionicsx2,hbm:circuit_controller_advancedx1,ntmspace:circuit_molychipx32,ntmspace:circuit_gaschipx16|ntmspace:argon/1000|-|-|!enable_528",
            "exastro_528|ass.exastro|ntmspace:item_expensive_astro|1|2000|100000|ntmspace:item_expensive_avionicsx2,hbm:circuit_controller_advancedx1,ntmspace:circuit_molychipx32,ntmspace:circuit_gaschipx16|hbm:xenon/1000|-|-|enable_528",
            "exavionics|ass.exavionics|ntmspace:item_expensive_avionics|1|1600|25000|hbm:item_expensive_circuitx2,ntmspace:circuit_capacitor_lanthaniumx16,ntmspace:circuit_avionicsx4,hbm:part_generic_glass_polarizedx32|hbm:perfluoromethyl/2000|-|-|-",
            "exstainlessbeam|ass.exstainlessbeam|ntmspace:item_expensive_stainless_beam|1|600|20000|#c:plates_welded/stainlessx8,mat:aluminium/castplatex8,mat:titanium/castplatex4,mat:steel/boltx32|ntmspace:krypton/1000|-|-|-",
            "exstainlessbeamalt|ass.exstainlessbeamalt|ntmspace:item_expensive_stainless_beam|1|600|20000|#c:plates_welded/stainlessx8,mat:aluminium/castplatex8,mat:titanium/castplatex4,mat:steel/boltx32|ntmspace:argon/1000|-|-|-",
            "gaschip|ass.gasChip|ntmspace:circuit_gaschip|1|50|250|hbm:plate_polymerx1,ntmspace:circuit_gaasx1,mat:gold/wirex1|-|-|-|-",
            "gasdock|ass.gasdock|ntmspace:gas_dock|1|200|100|mat:steel/weldedplatex5,#c:ingots/any_rubberx4,hbm:thruster_smallx1,ntmspace:circuit_avionicsx1|-|hbm:item_expensive_steel_platingx2,#c:ingots/any_rubberx4,hbm:thruster_smallx1,ntmspace:item_expensive_avionicsx1|-|-",
            "hfchip|ass.hfChip|ntmspace:circuit_hfchip|1|100|2000|ntmspace:nugget_hafniumx1,ntmspace:circuit_gaasx1,mat:gold/wirex2|-|-|-|-",
            "htr3|ass.htr3|ntmspace:machine_htr3|1|400|100|#c:plates_welded/any_resistant_alloyx4,mat:dura_steel/boltx16,mat:tungsten/weldedplatex8,hbm:motor_bismuthx1,#c:ingots/any_hardplasticx8,ntmspace:circuit_aerox2,mat:dura_steel/pipex8|-|hbm:item_expensive_lead_platingx8,mat:tungsten/weldedplatex8,hbm:motor_bismuthx4,#c:ingots/any_hardplasticx8,hbm:item_expensive_circuitx4|-|-",
            "htrf4neo|ass.htrf4neo|ntmspace:machine_htrf4neo|1|1200|100|#c:plates_welded/any_resistant_alloyx16,mat:copper/weldedplatex64,mat:schrabidate/densewirex64,#c:plates_welded/stainlessx16,hbm:circuit_bismoidx2,ntmspace:circuit_gaschipx4|-|hbm:item_expensive_ferro_platingx16,hbm:item_expensive_plasticx16,ntmspace:item_expensive_stainless_beamx16,ntmspace:circuit_gaschipx64|528.gaschip|-",
            "hydrobay|ass.hydrobay|ntmspace:hydrobay|1|200|100|#c:plates/stainlessx16,fc:minecraft:water/16000x1,mat:steel/pipex6,#c:glass_blocks/colorlessx8,minecraft:dirtx8,#c:ingots/any_plasticx2|-|ntmspace:item_expensive_stainless_beamx2,fc:minecraft:water/16000x1,mat:steel/pipex6,#c:glass_blocks/colorlessx8,minecraft:dirtx8,#c:ingots/any_plasticx4|528.stainless|-",
            "lpw2|ass.lpw2|hbm:machine_lpw2|1|600|100|mat:steel/weldedplatex16,mat:steel/boltx32,mat:titanium/shellx8,hbm:motor_deshx2,hbm:coil_goldx8,#c:ingots/any_hardplasticx8,hbm:circuit_advancedx2,mat:copper/pipex4|-|hbm:item_expensive_heavy_framex8,mat:titanium/shellx8,hbm:motor_deshx4,#c:ingots/any_hardplasticx8,hbm:item_expensive_circuitx4|-|-",
            "magma|ass.magma|ntmspace:machine_magma|1|400|100|hbm:steel_scaffoldx8,mat:tungsten/weldedplatex2,#c:plates/steelx32,hbm:drill_titaniumx1,hbm:motor_bismuthx1,ntmspace:circuit_gaschipx8|-|ntmspace:item_expensive_stainless_beamx6,mat:tungsten/weldedplatex4,hbm:drill_titaniumx1,hbm:motor_bismuthx4,ntmspace:circuit_gaschipx24|528.gaschip|-",
            "milkreformer|ass.milkreformer|ntmspace:machine_milk_reformer|1|600|100|mat:steel/castplatex14,#c:ingots/steelx2,hbm:motorx2,mat:steel/pipex8|-|hbm:item_expensive_steel_platingx4,hbm:motorx4,mat:steel/pipex8|-|-",
            "molychip|ass.molyChip|ntmspace:circuit_molychip|1|100|1500|hbm:powder_molysitex1,#c:billets/red_copperx1,mat:gold/wirex1|-|-|-|-",
            "mp_fuselage_10_15_balefire|ass.mp_fuselage_10_15_balefire|hbm:mp_fuselage_10_15_balefire|1|400|100|hbm:seg_10x1,hbm:seg_15x1,hbm:steel_scaffoldx9,#c:plates/titaniumx36,#c:plates/saturnitex9|-|-|-|-",
            "mp_fuselage_10_15_hydrogen|ass.mp_fuselage_10_15_hydrogen|hbm:mp_fuselage_10_15_hydrogen|1|400|100|hbm:seg_10x1,hbm:seg_15x1,hbm:steel_scaffoldx9,#c:plates/titaniumx36,#c:plates/ironx9|-|-|-|-",
            "mp_fuselage_10_15_kerosene|ass.mp_fuselage_10_15_kerosene|hbm:mp_fuselage_10_15_kerosene|1|400|100|hbm:seg_10x1,hbm:seg_15x1,hbm:steel_scaffoldx9,#c:plates/titaniumx36,#c:plates/steelx9|-|-|-|-",
            "mp_fuselage_10_15_solid|ass.mp_fuselage_10_15_solid|hbm:mp_fuselage_10_15_solid|1|400|100|hbm:seg_10x1,hbm:seg_15x1,hbm:steel_scaffoldx9,#c:plates/titaniumx36,#c:plates/aluminiumx9|-|-|-|-",
            "mp_fuselage_10_kerosene|ass.mp_fuselage_10_kerosene|hbm:mp_fuselage_10_kerosene|1|400|100|hbm:seg_10x2,hbm:steel_scaffoldx3,#c:plates/titaniumx12,#c:plates/steelx3|-|-|-|-",
            "mp_fuselage_10_long_kerosene|ass.mp_fuselage_10_long_kerosene|hbm:mp_fuselage_10_long_kerosene|1|400|100|hbm:seg_10x2,hbm:steel_scaffoldx6,#c:plates/titaniumx24,#c:plates/steelx6|-|-|-|-",
            "mp_fuselage_10_long_solid|ass.mp_fuselage_10_long_solid|hbm:mp_fuselage_10_long_solid|1|400|100|hbm:seg_10x2,hbm:steel_scaffoldx6,#c:plates/titaniumx24,#c:plates/aluminiumx6|-|-|-|-",
            "mp_fuselage_10_solid|ass.mp_fuselage_10_solid|hbm:mp_fuselage_10_solid|1|400|100|hbm:seg_10x2,hbm:steel_scaffoldx3,#c:plates/titaniumx12,#c:plates/aluminiumx3|-|-|-|-",
            "mp_fuselage_10_xenon|ass.mp_fuselage_10_xenon|hbm:mp_fuselage_10_xenon|1|400|100|hbm:seg_10x2,hbm:steel_scaffoldx3,#c:plates/titaniumx12,mat:copper/castplatex3|-|-|-|-",
            "mp_fuselage_15_20_kerosene|ass.mp_fuselage_15_20_kerosene|hbm:mp_fuselage_15_20_kerosene|1|400|100|hbm:seg_15x1,hbm:seg_20x1,hbm:steel_scaffoldx16,#c:plates/titaniumx64,#c:plates/steelx1|-|-|-|-",
            "mp_fuselage_15_20_solid|ass.mp_fuselage_15_20_solid|hbm:mp_fuselage_15_20_solid|1|400|100|hbm:seg_15x1,hbm:seg_20x1,hbm:steel_scaffoldx16,#c:plates/titaniumx64,#c:plates/aluminiumx1|-|-|-|-",
            "mp_fuselage_15_hydrogen|ass.mp_fuselage_15_hydrogen|hbm:mp_fuselage_15_hydrogen|1|400|100|hbm:seg_15x2,hbm:steel_scaffoldx12,#c:plates/titaniumx48,#c:plates/ironx1|-|-|-|-",
            "mp_fuselage_15_kerosene|ass.mp_fuselage_15_kerosene|hbm:mp_fuselage_15_kerosene|1|400|100|hbm:seg_15x2,hbm:steel_scaffoldx12,#c:plates/titaniumx48,#c:plates/steelx1|-|-|-|-",
            "mp_fuselage_15_solid|ass.mp_fuselage_15_solid|hbm:mp_fuselage_15_solid|1|400|100|hbm:seg_15x2,hbm:steel_scaffoldx12,#c:plates/titaniumx48,#c:plates/aluminiumx1|-|-|-|-",
            "mp_thruster_10_kerosene|ass.mp_thruster_10_kerosene|hbm:mp_thruster_10_kerosene|1|400|100|hbm:seg_10x1,mat:steel/pipex1,#c:ingots/tungstenx4,#c:plates/steelx4|-|-|-|-",
            "mp_thruster_10_solid|ass.mp_thruster_10_solid|hbm:mp_thruster_10_solid|1|400|100|hbm:seg_10x1,hbm:coil_tungstenx1,#c:ingots/dura_steelx4,#c:plates/steelx4|-|-|-|-",
            "mp_thruster_10_xenon|ass.mp_thruster_10_xenon|hbm:mp_thruster_10_xenon|1|400|100|hbm:seg_10x1,#c:plates/steelx4,mat:steel/pipex12,hbm:arc_electrode_lanthaniumx1|-|-|-|-",
            "mp_thruster_15_balefire|ass.mp_thruster_15_balefire|hbm:mp_thruster_15_balefire|1|400|100|hbm:seg_15x1,hbm:plate_polymerx16,hbm:pwr_fuelx2,#c:ingots/deshx16,#c:plates/saturnitex24,mat:copper/castplatex4,hbm:ingot_uranium_fuelx8|-|-|-|-",
            "mp_thruster_15_balefire_large|ass.mp_thruster_15_balefire_large|hbm:mp_thruster_15_balefire_large|1|400|100|hbm:seg_15x1,hbm:plate_polymerx16,hbm:pwr_fuelx2,#c:ingots/deshx24,#c:plates/saturnitex32,mat:copper/castplatex4,hbm:ingot_uranium_fuelx8|-|-|-|-",
            "mp_thruster_15_balefire_short|ass.mp_thruster_15_balefire_short|hbm:mp_thruster_15_balefire_short|1|400|100|hbm:seg_15x1,hbm:plate_polymerx8,hbm:pwr_fuelx1,#c:ingots/deshx8,#c:plates/saturnitex12,mat:copper/castplatex2,hbm:ingot_uranium_fuelx4|-|-|-|-",
            "mp_thruster_15_hydrogen|ass.mp_thruster_15_hydrogen|hbm:mp_thruster_15_hydrogen|1|400|100|hbm:seg_15x1,mat:steel/pipex1,#c:ingots/tungstenx8,#c:plates/steelx6,#c:ingots/saturnitex4|-|-|-|-",
            "mp_thruster_15_hydrogen_dual|ass.mp_thruster_15_hydrogen_dual|hbm:mp_thruster_15_hydrogen_dual|1|400|100|hbm:seg_15x1,mat:steel/pipex1,#c:ingots/tungstenx4,#c:plates/steelx6,#c:ingots/saturnitex1|-|-|-|-",
            "mp_thruster_15_kerosene|ass.mp_thruster_15_kerosene|hbm:mp_thruster_15_kerosene|1|400|100|hbm:seg_15x1,mat:steel/pipex1,#c:ingots/tungstenx8,#c:plates/steelx6,#c:ingots/deshx4|-|-|-|-",
            "mp_thruster_15_kerosene_dual|ass.mp_thruster_15_kerosene_dual|hbm:mp_thruster_15_kerosene_dual|1|400|100|hbm:seg_15x1,mat:steel/pipex1,#c:ingots/tungstenx4,#c:plates/steelx6,#c:ingots/deshx1|-|-|-|-",
            "mp_thruster_15_kerosene_triple|ass.mp_thruster_15_kerosene_triple|hbm:mp_thruster_15_kerosene_triple|1|400|100|hbm:seg_15x1,mat:steel/pipex1,#c:ingots/tungstenx6,#c:plates/steelx6,#c:ingots/deshx2|-|-|-|-",
            "mp_thruster_15_solid|ass.mp_thruster_15_solid|hbm:mp_thruster_15_solid|1|400|100|hbm:seg_15x1,#c:plates/steelx6,#c:ingots/dura_steelx6,hbm:coil_tungstenx3|-|-|-|-",
            "mp_thruster_15_solid_hexdecuple|ass.mp_thruster_15_solid_hexdecuple|hbm:mp_thruster_15_solid_hexdecuple|1|400|100|hbm:seg_15x1,#c:plates/steelx6,#c:ingots/dura_steelx12,hbm:coil_tungstenx6|-|-|-|-",
            "mp_thruster_20_kerosene|ass.mp_thruster_20_kerosene|hbm:mp_thruster_20_kerosene|1|400|100|hbm:seg_20x1,mat:steel/pipex1,#c:ingots/tungstenx8,#c:plates/steelx6,#c:ingots/deshx2|-|-|-|-",
            "mp_thruster_20_kerosene_dual|ass.mp_thruster_20_kerosene_dual|hbm:mp_thruster_20_kerosene_dual|1|400|100|hbm:seg_20x1,mat:steel/pipex1,#c:ingots/tungstenx12,#c:plates/steelx8,#c:ingots/deshx4|-|-|-|-",
            "mp_thruster_20_kerosene_triple|ass.mp_thruster_20_kerosene_triple|hbm:mp_thruster_20_kerosene_triple|1|400|100|hbm:seg_20x1,mat:steel/pipex1,#c:ingots/tungstenx16,#c:plates/steelx12,#c:ingots/deshx6|-|-|-|-",
            "mp_thruster_20_solid|ass.mp_thruster_20_solid|hbm:mp_thruster_20_solid|1|400|100|hbm:seg_20x1,hbm:coil_tungstenx8,#c:ingots/dura_steelx16,#c:plates/steelx1|-|-|-|-",
            "mp_thruster_20_solid_multi|ass.mp_thruster_20_solid_multi|hbm:mp_thruster_20_solid_multi|1|400|100|hbm:seg_20x1,hbm:coil_tungstenx12,#c:ingots/dura_steelx18,#c:plates/steelx1|-|-|-|-",
            "mp_thruster_20_solid_multier|ass.mp_thruster_20_solid_multier|hbm:mp_thruster_20_solid_multier|1|400|100|hbm:seg_20x1,hbm:coil_tungstenx16,#c:ingots/dura_steelx20,#c:plates/steelx1|-|-|-|-",
            "orbitalstationcomputer|ass.orbitalstationcomputer|ntmspace:orbital_station_computer|1|200|100|mat:aluminium/castplatex4,#c:plates/stainlessx4,#c:ingots/any_hardplasticx2,#c:dusts/quartzx4,ntmspace:circuit_molychipx4,ntmspace:circuit_aerox4|-|mat:aluminium/castplatex4,#c:plates_cast/stainlessx4,hbm:item_expensive_plasticx2,#c:dusts/quartzx16,ntmspace:item_expensive_astrox2|528.stainless|-",
            "orbitalstationport|ass.orbitalstationport|ntmspace:orbital_station_port|1|400|100|mat:titanium/weldedplatex6,hbm:motorx4,#c:glass_blocks/colorlessx8,#c:ingots/any_plasticx8,hbm:circuit_basicx4,#c:plates/stainlessx4|-|ntmspace:item_expensive_stainless_beamx8,hbm:motorx4,#c:glass_blocks/colorlessx8,#c:ingots/any_plasticx8,hbm:item_expensive_circuitx2|528.stainless|-",
            "platenickel|ass.platenickel|ntmspace:plate_nickel|1|60|100|#c:ingots/nickel_purex1|-|-|-|-",
            "platestainless|ass.platestainless|ntmspace:plate_stainless|1|60|100|#c:ingots/stainless_steelx1|-|-|-|-",
            "radiator|ass.radiator|ntmspace:machine_radiator|1|400|100|mat:aluminium/castplatex6,#c:plates/stainlessx6,mat:copper/pipex4,hbm:thermo_elementx3|-|-|-|-",
            "satdysonrelay|ass.satdysonrelay|ntmspace:sat_dyson_relay|1|600|100|#c:plates/saturnitex24,hbm:motor_bismuthx2,hbm:circuit_advancedx8,fi:hbm:fluid_barrel/hbm:kerosenex1,hbm:thruster_smallx1,mat:bscco/densewirex64,hbm:battery_lithiumx1|-|-|-|-",
            "solarpanel|ass.solarpanel|ntmspace:machine_solar|1|100|100|#c:plates/stainlessx4,hbm:photo_panelx4,#c:ingots/any_plasticx2,mat:red_copper/wirex8|-|-|-|-",
            "stardar|ass.stardar|ntmspace:machine_stardar|1|200|100|hbm:motorx4,mat:gold/castplatex16,hbm:photo_panelx32,hbm:magnetronx32,#c:concretesx16,hbm:steel_scaffoldx8,hbm:circuit_basicx4|-|hbm:motorx4,mat:gold/castplatex16,hbm:photo_panelx32,hbm:magnetronx32,hbm:item_expensive_heavy_framex2,hbm:item_expensive_circuitx4|528.aluminium|-",
            "vacuumcircuit|ass.vacuumcircuit|ntmspace:machine_vacuum_circuit|1|200|100|mat:steel/weldedplatex4,mat:tungsten/wirex16,hbm:turbine_titaniumx1,hbm:circuit_advancedx2|-|-|-|-",
    };

    /**
     * HBM rows hbm-space comments out ("we keep the old recipes for these": the old mpt/mpf parts) or drops.
     */
    private static final String[] REMOVED = {
            "ass.mpt10kero", "ass.mpt10solid", "ass.mpt10xenon", "ass.mpt15kero", "ass.mpt15kerodual",
            "ass.mpt15kerotriple", "ass.mpt15solid", "ass.mpt15solid16", "ass.mpt15hydro", "ass.mpt15hydrodual",
            "ass.mpt15bfshort", "ass.mpt15bf", "ass.mpt15bflarge", "ass.mpt20kero", "ass.mpt20kerodual",
            "ass.mpt20kerotriple", "ass.mpt20solid", "ass.mpt20solidmulti", "ass.mpt20solidmultier",
            "ass.mpf10kero", "ass.mpf10kerolong", "ass.mpf10solid", "ass.mpf10solidlong", "ass.mpf10xenon",
            "ass.mpf1015kero", "ass.mpf1015solid", "ass.mpf1015hydro", "ass.mpf1015bf", "ass.mpf15kero",
            "ass.mpf15solid", "ass.mpf15hydro", "ass.mpf1520kero", "ass.mpf1520solid", "ass.lunarminer",
    };

    private RecipeChangeAssemblyGameTests() {
    }

    public static void init() {
    }

    private static void rows(GameTestHelper helper) {
        AssemblyMachineRecipes table = AssemblyMachineRecipes.INSTANCE;
        table.registerRecipes();
        List<String> errors = new ArrayList<>();
        for (String line : EXPECTED) {
            String[] f = line.split("\\|", -1);
            String file = "ntmspace:assembly_machine/" + f[0];
            GenericRecipe recipe = entry(table, file);
            if (!inForce(f, file)) {
                if (recipe != null) errors.add(file + " loaded while its requirements fail");
                continue;
            }
            if (recipe == null) {
                errors.add(file + " not loaded");
                continue;
            }
            check(errors, file, "name", f[1], recipe.getInternalName());
            ItemStack out = recipe.outputItem != null && recipe.outputItem.length == 1 ? recipe.outputItem[0] : ItemStack.EMPTY;
            check(errors, file, "output", f[2] + "x" + f[3],
                    out.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(out.getItem()) + "x" + out.getCount());
            check(errors, file, "duration", f[4], String.valueOf(recipe.duration));
            check(errors, file, "power", f[5], String.valueOf(recipe.power));
            check(errors, file, "inputs", f[6], stacks(recipe.normalInputItems()));
            check(errors, file, "fluids", f[7], fluids(recipe.normalInputFluids()));
            check(errors, file, "expensive", f[8], recipe.inputItemEx == null ? "-" : stacks(recipe.inputItemEx));
            check(errors, file, "528 pool", f[9],
                    recipe.blueprintPools528 == null ? "-" : String.join(",", recipe.blueprintPools528));
            check(errors, file, "requires", f[10], recipe.requires == null ? "-" : String.join(",", recipe.requires));
        }
        // the table's lookup: each name resolves to the row in force under the current 528 mode
        for (String line : EXPECTED) {
            String[] f = line.split("\\|", -1);
            if (!inForce(f, f[0])) continue;
            GenericRecipe row = table.getRecipe(f[1]);
            if (row == null || !stacks(row.normalInputItems()).equals(f[6]) || !fluids(row.normalInputFluids()).equals(
                    f[7])) {
                errors.add(
                        f[1] + " in the table is not " + f[0] + ": " + (row == null ? "missing" : GenericRecipeCodec.describe(
                                row)));
            }
        }
        // a replacing row takes the position of HBM's row it replaces (orderOf in the addon's source), 1.7's line order
        checkOrder(errors, table, "ass.entanglementkit", 45 * AssemblyMachineRecipes.ORDER_STRIDE);
        checkOrder(errors, table, "ass.electrolyzer", 95 * AssemblyMachineRecipes.ORDER_STRIDE);
        for (String name : REMOVED) {
            if (table.getRecipe(name) != null) errors.add(name + " is still in the table");
        }
        RecipeChangeGameTests.finish(helper, errors);
    }

    private static boolean inForce(String[] fields, String file) {
        return RecipeTables.holds("-".equals(fields[10]) ? List.of() : List.of(fields[10].split(",")), file);
    }

    /**
     * The loaded row of an enabled file.
     */
    private static GenericRecipe entry(AssemblyMachineRecipes table, String id) {
        for (RecipeTables.Entry<GenericRecipe> entry : table.table.entries()) {
            if (entry.id().equals(id)) {
                GenericRecipe recipe = entry.value();
                recipe.bindStacks();
                return recipe;
            }
        }
        return null;
    }

    private static void checkOrder(List<String> errors, AssemblyMachineRecipes table, String name, int order) {
        GenericRecipe row = table.getRecipe(name);
        if (row == null || row.order != order)
            errors.add(name + " order " + (row == null ? "missing" : row.order) + ", HBM's is " + order);
    }

    private static void check(List<String> errors, String file, String what, String expected, String actual) {
        if (!expected.equals(actual)) errors.add(file + " " + what + ": expected " + expected + ", got " + actual);
    }

    private static String stacks(AStack[] stacks) {
        if (stacks == null || stacks.length == 0) return "-";
        List<String> out = new ArrayList<>();
        for (AStack stack : stacks) out.add(stack(stack) + "x" + stack.stacksize);
        return String.join(",", out);
    }

    private static String stack(AStack stack) {
        if (stack instanceof TagStack tag) return "#" + tag.tag.location();
        if (stack instanceof MaterialStack material) {
            return "mat:" + material.material.name + "/" + material.shape.name().toLowerCase(Locale.ROOT);
        }
        if (stack instanceof FluidContainerStack container) {
            return "fc:" + fluid(Fluids.fromName(container.fluid())) + "/" + container.amount();
        }
        if (stack instanceof RecipeIngredients.FluidItem item) {
            return "fi:" + BuiltInRegistries.ITEM.getKey(item.item) + "/" + fluid(item.fluid);
        }
        if (stack instanceof ComparableStack comparable)
            return String.valueOf(BuiltInRegistries.ITEM.getKey(comparable.item));
        return "?" + stack.getClass().getSimpleName();
    }

    private static String fluids(FluidStack[] fluids) {
        if (fluids == null || fluids.length == 0) return "-";
        List<String> out = new ArrayList<>();
        for (FluidStack stack : fluids) out.add(fluid(stack.type) + "/" + stack.fill);
        return String.join(",", out);
    }

    private static String fluid(FluidType type) {
        return String.valueOf(BuiltInRegistries.FLUID.getKey(Fluids.toVanilla(type)));
    }
}
