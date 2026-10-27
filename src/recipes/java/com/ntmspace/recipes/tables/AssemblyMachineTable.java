package com.ntmspace.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.ModSecurityBlocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.inventory.FluidContainerStack;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.fork.ForkContent;
import com.ntmspace.inventory.material.SpaceOreMaterials;
import com.ntmspace.registry.AtmosphereMachines;
import com.ntmspace.registry.DriveProcessorBlocks;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.registry.FloraBlocks;
import com.ntmspace.registry.LeftoverContent;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.SpaceItems;
import com.ntmspace.registry.SpaceMachineItems;
import com.ntmspace.registry.SpaceMachines;
import com.ntmspace.registry.SpaceMachinesB;
import com.ntmspace.registry.SpaceMachinesC;
import com.ntmspace.registry.SpaceMachinesD;
import com.ntmspace.registry.StationBlocks;
import com.ntmspace.registry.StationLauncherBlocks;
import com.ntmspace.registry.TransporterBlocks;
import com.ntmspace.satellite.SpaceSatellites;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import com.hbm.items.ModMachineItems;

/**
 * hbm-space {@code AssemblyMachineRecipes.registerDefaults}: the rows hbm-space adds to HBM's table, and the two it changes
 * ({@code ass.electrolyzer}, {@code ass.entanglementkit}: they replace HBM's rows, removed by
 * {@code recipe_removals/assembly_machine.json}, and take HBM's {@code order}). Added rows carry no {@code order}: they
 * sort after HBM's rows, by id. 1.7's {@code GeneralConfig.enable528 ? a : b} fluid is one row per branch, the second
 * named {@code <id>_528}, each carrying its branch as {@code requires}. Rows hbm-space changes only by a port spelling
 * (its {@code sat_*} chips are HBM's {@code satellite_*} items) are HBM's rows and not repeated. hbm-space's rocket parts
 * {@code rp_capsule_20}, {@code rp_fuselage_20_*}, {@code rp_legs_20} and {@code rp_station_core_20} are the port's
 * {@code rp_c_20}, {@code rp_f_20_*} ({@code rp_fuselage_20_12_hydrazine}: {@code mp_fuselage_20_hydrazine}),
 * {@code rp_l_20} and {@code rp_sc_20} ({@link RocketItems}). Replaces the former {@code gen_recipe_changes_assembly.py}'s
 * rows and the rocket family's authored rows.
 */
public final class AssemblyMachineTable extends GenericTableSource<GenericRecipe> {

    private static final String NO_528 = "!enable_528";
    private static final String YES_528 = "enable_528";

    // hbm-space OreDictManager names HBM's OreDictManager has no alias for: their c: tags
    // hbm-space OreDictManager's NI: Space's pure-nickel material (nickel_pure tags).
    private static final NTMMaterial STAINLESS = new NTMMaterial(-1, "stainless");
    private static final NTMMaterial FIBER = new NTMMaterial(-1, "fiberglass");
    private static final NTMMaterial QUARTZ = new NTMMaterial(-1, "quartz");
    private static final TagKey<Item> ANY_CONCRETE = tag("c", "concretes");
    private static final TagKey<Item> KEY_CLEARGLASS = tag("c", "glass_blocks/colorless");
    private static final TagKey<Item> KEY_ANYGLASS = tag("c", "glass_blocks");

    public AssemblyMachineTable() {
        super(AssemblyMachineRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        String autoPlate = "autoswitch.plates";

        // commented out in hbm-space: ass.lunarminer
        remove("hbm:assembly_machine/lunarminer");

        this.register(new GenericRecipe("ass.platenickel").setup(60, 100).outputItems(new ItemStack(SpaceItems.PLATE_NICKEL.get(), 1))
                .inputItems(SpaceOreMaterials.NICKEL.ingot())
                .setPools(GenericRecipes.POOL_PREFIX_ALT + "plates")
                .setGroup(autoPlate, this));

        this.register(new GenericRecipe("ass.platestainless").setup(60, 100).outputItems(new ItemStack(SpaceItems.PLATE_STAINLESS.get(), 1))
                .inputItems(new TagStack(tag("c", "ingots/stainless_steel")))
                .setPools(GenericRecipes.POOL_PREFIX_ALT + "plates")
                .setGroup(autoPlate, this));

        this.register(new GenericRecipe("ass.exstainlessbeam").setup(600, 20_000).outputItems(new ItemStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 1))
                .inputItems(
                        shapeTag(STAINLESS, MaterialShapes.WELDEDPLATE).count(8),
                        AL.plateCast().count(8),
                        TI.plateCast().count(4),
                        STEEL.bolt().count(32))
                .inputFluids(new FluidStack(SpaceFluids.KRYPTON, 1_000)));

        this.register(new GenericRecipe("ass.exstainlessbeamalt").setup(600, 20_000).outputItems(new ItemStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 1))
                .inputItems(
                        shapeTag(STAINLESS, MaterialShapes.WELDEDPLATE).count(8),
                        AL.plateCast().count(8),
                        TI.plateCast().count(4),
                        STEEL.bolt().count(32))
                .inputFluids(new FluidStack(SpaceFluids.ARGON, 1_000)));

        this.register(new GenericRecipe("ass.exavionics").setup(1_600, 25_000).outputItems(new ItemStack(SpaceItems.ITEM_EXPENSIVE_AVIONICS.get(), 1))
                .inputItems(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 2),
                        new ComparableStack(SpaceMachineItems.circuit("CAPACITOR_LANTHANIUM"), 16),
                        new ComparableStack(SpaceMachineItems.circuit("AVIONICS"), 4),
                        new ComparableStack(ModIngredientItems.part(EnumPartType.GLASS_POLARIZED), 32))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 2_000)));

        // 1.7 new FluidStack(GeneralConfig.enable528 ? Fluids.XENON : Fluids.ARGON, 1_000): one row per branch
        this.register("exastro", new GenericRecipe("ass.exastro").setup(2_000, 100_000).outputItems(new ItemStack(SpaceItems.ITEM_EXPENSIVE_ASTRO.get(), 1))
                .inputItems(
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_AVIONICS.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 1),
                        new ComparableStack(SpaceMachineItems.circuit("MOLYCHIP"), 32),
                        new ComparableStack(SpaceMachineItems.circuit("GASCHIP"), 16))
                .inputFluids(new FluidStack(SpaceFluids.ARGON, 1_000)).requires(NO_528));
        this.register("exastro_528", new GenericRecipe("ass.exastro").setup(2_000, 100_000).outputItems(new ItemStack(SpaceItems.ITEM_EXPENSIVE_ASTRO.get(), 1))
                .inputItems(
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_AVIONICS.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 1),
                        new ComparableStack(SpaceMachineItems.circuit("MOLYCHIP"), 32),
                        new ComparableStack(SpaceMachineItems.circuit("GASCHIP"), 16))
                .inputFluids(new FluidStack(Fluids.XENON, 1_000)).requires(YES_528));

        this.register(new GenericRecipe("ass.gasChip").setup(50, 250).outputItems(new ItemStack(SpaceMachineItems.circuit("GASCHIP")))
                .inputItems(
                        new ComparableStack(ModItems.plate_polymer.get()),
                        new ComparableStack(SpaceMachineItems.circuit("GAAS"), 1),
                        GOLD.wire()));

        this.register(new GenericRecipe("ass.hfChip").setup(100, 2_000).outputItems(new ItemStack(SpaceMachineItems.circuit("HFCHIP")))
                .inputItems(
                        new ComparableStack(SpaceItems.NUGGET_HAFNIUM.get()),
                        new ComparableStack(SpaceMachineItems.circuit("GAAS"), 1),
                        GOLD.wire().count(2)));

        this.register(new GenericRecipe("ass.molyChip").setup(100, 1_500).outputItems(new ItemStack(SpaceMachineItems.circuit("MOLYCHIP")))
                .inputItems(
                        new ComparableStack(ModItems.powder_molysite.get()),
                        MINGRADE.billet(),
                        GOLD.wire()));

        // changed by hbm-space: replaces HBM's row (recipe_removals) at its position
        remove("hbm:assembly_machine/entanglementkit");
        this.register(replacing(new GenericRecipe("ass.entanglementkit").setup(200, 100).outputItems(new ItemStack(ModItems.entanglement_kit.get(), 1))
                .inputItems(
                        DURA.plateCast().count(4),
                        CU.plate().count(24),
                        GOLD.wireDense().count(16))
                .inputFluids(new FluidStack(SpaceFluids.KRYPTON, 8_000))));

        // changed by hbm-space: replaces HBM's row (recipe_removals) at its position
        remove("hbm:assembly_machine/electrolyzer");
        this.register(replacing(new GenericRecipe("ass.electrolyzer").setup(200, 100).outputItems(new ItemStack(ModMachineBlocks.machine_electrolyser.get().asItem(), 1))
                .inputItems(
                        STEEL.plateCast().count(8),
                        CU.plate().count(16),
                        TI.shell().count(3),
                        RUBBER.ingot().count(8),
                        new ComparableStack(ModItems.ingot_firebrick.get(), 16),
                        new ComparableStack(ModItems.coil_copper.get(), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 8))
                .inputItemsEx(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 4),
                        TI.shell().count(3),
                        RUBBER.ingot().count(8),
                        new ComparableStack(ModItems.ingot_firebrick.get(), 16),
                        new ComparableStack(ModItems.coil_copper.get(), 16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 24))));

        this.register(new GenericRecipe("ass.nukeantimatter").setup(400, 100).outputItems(new ItemStack(ForkContent.NUKE_ANTIMATTER.get().asItem(), 1))
                .inputItems(
                        STAINLESS.plate().count(16),
                        MINGRADE.wire().count(32),
                        new ComparableStack(SpaceItems.INGOT_HAFNIUM.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1),
                        new ComparableStack(SpaceItems.BILLET_GAAS.get(), 1),
                        new ComparableStack(ModItems.magnetron.get(), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "controller"));

        this.register(new GenericRecipe("ass.dysonmember").setup(200, 100).outputItems(new ItemStack(DysonContent.SWARM_MEMBER.get(), 1))
                .inputItems(
                        W.plateWelded(),
                        ANY_HARDPLASTIC.ingot().count(2),
                        new ComparableStack(SpaceItems.BERYLLIUM_MIRROR.get(), 1),
                        GOLD.wireDense().count(2),
                        MINGRADE.wire().count(32),
                        STAINLESS.plate().count(4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_BOARD), 2),
                        new ComparableStack(SpaceMachineItems.circuit("HFCHIP"), 1)));

        this.register(new GenericRecipe("ass.satdysonrelay").setup(600, 100).outputItems(new ItemStack(SpaceSatellites.SAT_DYSON_RELAY.get(), 1))
                .inputItems(
                        BIGMT.plate().count(24),
                        new ComparableStack(ModItems.motor_bismuth.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 8),
                        fullBarrel(Fluids.KEROSENE, 1),
                        new ComparableStack(ModItems.thruster_small.get(), 1),
                        BSCCO.wireDense().count(64),
                        new RecipeIngredients.Battery(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LITHIUM).get(), 1)));

        this.register(new GenericRecipe("ass.magma").setup(400, 100).outputItems(new ItemStack(SpaceMachinesC.MACHINE_MAGMA.get().asItem(), 1))
                .inputItems(
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 8),
                        W.plateWelded().count(2),
                        STEEL.plate().count(32),
                        new ComparableStack(ModItems.drill_titanium.get(), 1),
                        new ComparableStack(ModItems.motor_bismuth.get()),
                        new ComparableStack(SpaceMachineItems.circuit("GASCHIP"), 8))
                .inputItemsEx(
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 6),
                        W.plateWelded().count(4),
                        new ComparableStack(ModItems.drill_titanium.get(), 1),
                        new ComparableStack(ModItems.motor_bismuth.get(), 4),
                        new ComparableStack(SpaceMachineItems.circuit("GASCHIP"), 24))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "gaschip"));

        this.register(new GenericRecipe("ass.hydrobay").setup(200, 100).outputItems(new ItemStack(SpaceMachinesB.HYDROBAY.get().asItem(), 1))
                .inputItems(
                        STAINLESS.plate().count(16),
                        getDict(Fluids.WATER, 16_000),
                        STEEL.pipe().count(6),
                        new TagStack(KEY_CLEARGLASS).count(8),
                        new ComparableStack(Blocks.DIRT.asItem(), 8),
                        ANY_PLASTIC.ingot().count(2))
                .inputItemsEx(
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 2),
                        getDict(Fluids.WATER, 16_000),
                        STEEL.pipe().count(6),
                        new TagStack(KEY_CLEARGLASS).count(8),
                        new ComparableStack(Blocks.DIRT.asItem(), 8),
                        ANY_PLASTIC.ingot().count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "stainless"));

        this.register(new GenericRecipe("ass.radiator").setup(400, 100).outputItems(new ItemStack(SpaceMachinesC.MACHINE_RADIATOR.get().asItem(), 1))
                .inputItems(
                        AL.plateCast().count(6),
                        STAINLESS.plate().count(6),
                        CU.pipe().count(4),
                        new ComparableStack(ModItems.thermo_element.get(), 3)));

        this.register(new GenericRecipe("ass.milkreformer").setup(600, 100).outputItems(new ItemStack(SpaceMachines.MACHINE_MILK_REFORMER.get().asItem(), 1))
                .inputItems(
                        STEEL.plateCast().count(14),
                        STEEL.ingot().count(2),
                        new ComparableStack(ModItems.motor.get(), 2),
                        STEEL.pipe().count(8))
                .inputItemsEx(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 4),
                        new ComparableStack(ModItems.motor.get(), 4),
                        STEEL.pipe().count(8)));

        this.register(new GenericRecipe("ass.algaefilm").setup(100, 100).outputItems(new ItemStack(LeftoverContent.ALGAE_FILM.get().asItem(), 1))
                .inputItems(
                        AL.plate().count(8),
                        new ComparableStack(FloraBlocks.SALTLEAF.get(), 16),
                        new ComparableStack(ModMachineBlocks.fan.get().asItem(), 1),
                        new ComparableStack(ModDecoBlocks.steel_beam.get().asItem(), 4),
                        new ComparableStack(ModSecurityBlocks.fence_metal.get().asItem(), 2))
                .inputItemsEx(
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 1),
                        new ComparableStack(FloraBlocks.SALTLEAF.get(), 24),
                        new ComparableStack(ModSecurityBlocks.fence_metal.get().asItem(), 4)));

        this.register(new GenericRecipe("ass.airscrubber").setup(200, 100).outputItems(new ItemStack(AtmosphereMachines.AIR_SCRUBBER.get().asItem(), 1))
                .inputItems(
                        STAINLESS.plate().count(6),
                        CA.dust().count(4),
                        LI.dust().count(12),
                        new ComparableStack(ModItems.motor.get(), 1),
                        new ComparableStack(ModItems.turbine_titanium.get(), 1))
                .inputItemsEx(
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 1),
                        CA.dust().count(8),
                        LI.dust().count(16),
                        new ComparableStack(ModItems.motor.get(), 1),
                        new ComparableStack(ModItems.turbine_titanium.get(), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "stainless"));

        this.register(new GenericRecipe("ass.alkylation").setup(400, 100).outputItems(new ItemStack(SpaceMachines.MACHINE_ALKYLATION.get().asItem()))
                .inputItems(
                        STAINLESS.plate().count(12),
                        STEEL.shell().count(6),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 8),
                        new ComparableStack(ModItems.catalyst_clay.get(), 12),
                        new ComparableStack(ModItems.coil_tungsten.get(), 8))
                .inputItemsEx(
                        shapeTag(STAINLESS, MaterialShapes.WELDEDPLATE).count(4),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 4),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR), 8),
                        new ComparableStack(ModItems.catalyst_clay.get(), 12),
                        new ComparableStack(ModItems.coil_tungsten.get(), 8))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "stainless"));

        this.register(new GenericRecipe("ass.cryodistil").setup(600, 100).outputItems(new ItemStack(SpaceMachines.MACHINE_CRYO_DISTILL.get().asItem()))
                .inputItems(
                        STEEL.plateCast().count(2),
                        STAINLESS.plate().count(12),
                        ANY_PLASTIC.ingot().count(4),
                        NB.ingot().count(4))
                .inputItemsEx(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2),
                        shapeTag(STAINLESS, MaterialShapes.WELDEDPLATE).count(4),
                        NB.ingot().count(4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "stainless"));

        this.register(new GenericRecipe("ass.transporterrocket").setup(200, 100).outputItems(new ItemStack(TransporterBlocks.TRANSPORTER_ROCKET.get().asItem(), 2))
                .inputItems(
                        STEEL.plateCast().count(2),
                        TI.plateWelded().count(4),
                        new ComparableStack(ModStorageBlocks.crate_iron.get().asItem(), 2),
                        new ComparableStack(ModItems.thruster_small.get(), 1),
                        new ComparableStack(SpaceMachineItems.circuit("AERO"), 1))
                .inputItemsEx(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2),
                        TI.plateWelded().count(4),
                        new ComparableStack(ModStorageBlocks.crate_iron.get().asItem(), 2),
                        new ComparableStack(ModItems.thruster_small.get(), 1),
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_AVIONICS.get(), 1)));

        this.register(new GenericRecipe("ass.gasdock").setup(200, 100).outputItems(new ItemStack(SpaceMachinesC.GAS_DOCK.get().asItem(), 1))
                .inputItems(
                        STEEL.plateWelded().count(5),
                        ANY_RUBBER.ingot().count(4),
                        new ComparableStack(ModItems.thruster_small.get(), 1),
                        new ComparableStack(SpaceMachineItems.circuit("AVIONICS"), 1))
                .inputItemsEx(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 2),
                        ANY_RUBBER.ingot().count(4),
                        new ComparableStack(ModItems.thruster_small.get(), 1),
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_AVIONICS.get(), 1)));

        this.register(new GenericRecipe("ass.stardar").setup(200, 100).outputItems(new ItemStack(SpaceMachinesD.MACHINE_STARDAR.get().asItem(), 1))
                .inputItems(
                        new ComparableStack(ModItems.motor.get(), 4),
                        GOLD.plateCast().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 32),
                        new ComparableStack(ModItems.magnetron.get(), 32),
                        new TagStack(ANY_CONCRETE).count(16),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .inputItemsEx(
                        new ComparableStack(ModItems.motor.get(), 4),
                        GOLD.plateCast().count(16),
                        new ComparableStack(ModItems.photo_panel.get(), 32),
                        new ComparableStack(ModItems.magnetron.get(), 32),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "aluminium"));

        this.register(new GenericRecipe("ass.driveprocessor").setup(400, 100).outputItems(new ItemStack(DriveProcessorBlocks.MACHINE_DRIVE_PROCESSOR.get().asItem(), 1))
                .inputItems(
                        ANY_RUBBER.ingot().count(2),
                        CU.wire().count(8),
                        STEEL.plateCast().count(2),
                        new ComparableStack(ModItems.crt_display.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 2))
                .inputItemsEx(
                        ANY_RUBBER.ingot().count(2),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.STEEL_PLATING), 4),
                        new ComparableStack(ModItems.crt_display.get(), 2),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 1))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "aluminium"));

        this.register(new GenericRecipe("ass.vacuumcircuit").setup(200, 100).outputItems(new ItemStack(SpaceMachinesB.MACHINE_VACUUM_CIRCUIT.get().asItem(), 1))
                .inputItems(
                        STEEL.plateWelded().count(4),
                        W.wire().count(16),
                        new ComparableStack(ModItems.turbine_titanium.get(), 1),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 2)));

        this.register(new GenericRecipe("ass.solarpanel").setup(100, 100).outputItems(new ItemStack(SpaceMachinesC.MACHINE_SOLAR.get().asItem(), 1))
                .inputItems(
                        STAINLESS.plate().count(4),
                        new ComparableStack(ModItems.photo_panel.get(), 4),
                        ANY_PLASTIC.ingot().count(2),
                        MINGRADE.wire().count(8)));

        this.register(new GenericRecipe("ass.launchpadrocket").setup(400, 100).outputItems(new ItemStack(RocketBlocks.LAUNCH_PAD_ROCKET.get().asItem(), 1))
                .inputItems(
                        STEEL.plateWelded().count(12),
                        AL.pipe().count(24),
                        new TagStack(ANY_CONCRETE).count(64),
                        new TagStack(ANY_CONCRETE).count(64),
                        ANY_PLASTIC.ingot().count(16),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 64),
                        new ComparableStack(SpaceMachineItems.circuit("AERO"), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "aluminium"));

        this.register(new GenericRecipe("ass.rocketassembly").setup(200, 100).outputItems(new ItemStack(RocketBlocks.MACHINE_ROCKET_ASSEMBLY.get().asItem(), 1))
                .inputItems(
                        STEEL.plateCast().count(8),
                        STEEL.pipe().count(12),
                        new TagStack(ANY_CONCRETE).count(16),
                        ANY_PLASTIC.ingot().count(8),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 64),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "aluminium"));

        this.register(new GenericRecipe("ass.orrery").setup(400, 100).outputItems(new ItemStack(LeftoverContent.ORRERY.get().asItem(), 1))
                .inputItems(
                        new TagStack(KEY_ANYGLASS).count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 12))
                .inputFluids(new FluidStack(Fluids.TRITIUM, 2_000)));

        this.register(new GenericRecipe("ass.orbitalstationport").setup(400, 100).outputItems(new ItemStack(StationBlocks.ORBITAL_STATION_PORT.get().asItem(), 1))
                .inputItems(
                        TI.plateWelded().count(6),
                        new ComparableStack(ModItems.motor.get(), 4),
                        new TagStack(KEY_CLEARGLASS).count(8),
                        ANY_PLASTIC.ingot().count(8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 4),
                        STAINLESS.plate().count(4))
                .inputItemsEx(
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 8),
                        new ComparableStack(ModItems.motor.get(), 4),
                        new TagStack(KEY_CLEARGLASS).count(8),
                        ANY_PLASTIC.ingot().count(8),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 2))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "stainless"));

        this.register(new GenericRecipe("ass.orbitalstationlauncher").setup(400, 100).outputItems(new ItemStack(StationLauncherBlocks.ORBITAL_STATION_LAUNCHER.get().asItem(), 1))
                .inputItems(
                        TI.plateWelded().count(6),
                        new ComparableStack(ModItems.motor.get(), 4),
                        new TagStack(KEY_CLEARGLASS).count(8),
                        ANY_PLASTIC.ingot().count(8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 2),
                        STAINLESS.plate().count(4))
                .inputItemsEx(
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 8),
                        new ComparableStack(ModItems.motor.get(), 4),
                        new TagStack(KEY_CLEARGLASS).count(8),
                        ANY_PLASTIC.ingot().count(8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "stainless"));

        this.register(new GenericRecipe("ass.rpstationcore20").setup(200, 100).outputItems(new ItemStack(RocketItems.RP_SC_20.get(), 1))
                .inputItems(
                        new ComparableStack(StationBlocks.ORBITAL_STATION_PORT.get().asItem(), 1),
                        AL.plateCast().count(4),
                        new ComparableStack(SpaceMachineItems.circuit("AVIONICS"), 1))
                .inputItemsEx(
                        new ComparableStack(StationBlocks.ORBITAL_STATION_PORT.get().asItem(), 1),
                        AL.plateCast().count(4),
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_AVIONICS.get(), 1)));

        this.register(new GenericRecipe("ass.rppod20").setup(200, 100).outputItems(new ItemStack(RocketItems.RP_POD_20.get(), 1))
                .inputItems(
                        AL.shell().count(4),
                        STAINLESS.plate().count(8),
                        FIBER.ingot().count(4),
                        ANY_PLASTIC.ingot().count(2),
                        new ComparableStack(SpaceMachineItems.circuit("AVIONICS"), 1),
                        new ComparableStack(ModItems.thruster_small.get(), 4)));

        this.register(new GenericRecipe("ass.orbitalstationcomputer").setup(200, 100).outputItems(new ItemStack(StationBlocks.ORBITAL_STATION_COMPUTER.get().asItem(), 1))
                .inputItems(
                        AL.plateCast().count(4),
                        STAINLESS.plate().count(4),
                        ANY_HARDPLASTIC.ingot().count(2),
                        QUARTZ.dust().count(4),
                        new ComparableStack(SpaceMachineItems.circuit("MOLYCHIP"), 4),
                        new ComparableStack(SpaceMachineItems.circuit("AERO"), 4))
                .inputItemsEx(
                        AL.plateCast().count(4),
                        shapeTag(STAINLESS, MaterialShapes.CASTPLATE).count(4),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 2),
                        QUARTZ.dust().count(16),
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_ASTRO.get(), 2))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "stainless"));

        this.register(new GenericRecipe("ass.lpw2").setup(600, 100).outputItems(new ItemStack(ModPowerGenBlocks.MACHINE_LPW2.get().asItem(), 1))
                .inputItems(
                        STEEL.plateWelded().count(16),
                        STEEL.bolt().count(32),
                        TI.shell().count(8),
                        new ComparableStack(ModItems.motor_desh.get(), 2),
                        new ComparableStack(ModItems.coil_gold.get(), 8),
                        ANY_HARDPLASTIC.ingot().count(8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 2),
                        CU.pipe().count(4))
                .inputItemsEx(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 8),
                        TI.shell().count(8),
                        new ComparableStack(ModItems.motor_desh.get(), 4),
                        ANY_HARDPLASTIC.ingot().count(8),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 4)));

        this.register(new GenericRecipe("ass.htr3").setup(400, 100).outputItems(new ItemStack(StationBlocks.MACHINE_HTR3.get().asItem(), 1))
                .inputItems(
                        ANY_RESISTANTALLOY.plateWelded().count(4),
                        DURA.bolt().count(16),
                        W.plateWelded().count(8),
                        new ComparableStack(ModItems.motor_bismuth.get(), 1),
                        ANY_HARDPLASTIC.ingot().count(8),
                        new ComparableStack(SpaceMachineItems.circuit("AERO"), 2),
                        DURA.pipe().count(8))
                .inputItemsEx(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 8),
                        W.plateWelded().count(8),
                        new ComparableStack(ModItems.motor_bismuth.get(), 4),
                        ANY_HARDPLASTIC.ingot().count(8),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.CIRCUIT), 4)));

        this.register(new GenericRecipe("ass.htrf4neo").setup(1_200, 100).outputItems(new ItemStack(StationBlocks.MACHINE_HTRF4NEO.get().asItem(), 1))
                .inputItems(
                        ANY_RESISTANTALLOY.plateWelded().count(16),
                        CU.plateWelded().count(64),
                        SBD.wireDense().count(64),
                        shapeTag(STAINLESS, MaterialShapes.WELDEDPLATE).count(16),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BISMOID), 2),
                        new ComparableStack(SpaceMachineItems.circuit("GASCHIP"), 4))
                .inputItemsEx(
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 16),
                        new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.PLASTIC), 16),
                        new ComparableStack(SpaceItems.ITEM_EXPENSIVE_STAINLESS_BEAM.get(), 16),
                        new ComparableStack(SpaceMachineItems.circuit("GASCHIP"), 64))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "gaschip"));

        // hbm-space renames 1.7's missile part rows (ass.mpt10kero) after their item (ass.mp_thruster_10_kerosene)
        remove("hbm:assembly_machine/mpt10kero");
        this.register(new GenericRecipe("ass.mp_thruster_10_kerosene").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_10_kerosene.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(4),
                        STEEL.plate().count(4)));

        remove("hbm:assembly_machine/mpt10solid");
        this.register(new GenericRecipe("ass.mp_thruster_10_solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_10_solid.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 1),
                        new ComparableStack(ModItems.coil_tungsten.get(), 1),
                        DURA.ingot().count(4),
                        STEEL.plate().count(4)));

        remove("hbm:assembly_machine/mpt10xenon");
        this.register(new GenericRecipe("ass.mp_thruster_10_xenon").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_10_xenon.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 1),
                        STEEL.plate().count(4),
                        STEEL.pipe().count(12),
                        new ComparableStack(ModMachineItems.arc_electrode_lanthanium.get(), 1)));

        remove("hbm:assembly_machine/mpt15kero");
        this.register(new GenericRecipe("ass.mp_thruster_15_kerosene").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_kerosene.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(8),
                        STEEL.plate().count(6),
                        DESH.ingot().count(4)));

        remove("hbm:assembly_machine/mpt15kerodual");
        this.register(new GenericRecipe("ass.mp_thruster_15_kerosene_dual").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_kerosene_dual.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(4),
                        STEEL.plate().count(6),
                        DESH.ingot()));

        remove("hbm:assembly_machine/mpt15kerotriple");
        this.register(new GenericRecipe("ass.mp_thruster_15_kerosene_triple").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_kerosene_triple.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(6),
                        STEEL.plate().count(6),
                        DESH.ingot().count(2)));

        remove("hbm:assembly_machine/mpt15solid");
        this.register(new GenericRecipe("ass.mp_thruster_15_solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_solid.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        STEEL.plate().count(6),
                        DURA.ingot().count(6),
                        new ComparableStack(ModItems.coil_tungsten.get(), 3)));

        remove("hbm:assembly_machine/mpt15solid16");
        this.register(new GenericRecipe("ass.mp_thruster_15_solid_hexdecuple").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_solid_hexdecuple.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        STEEL.plate().count(6),
                        DURA.ingot().count(12),
                        new ComparableStack(ModItems.coil_tungsten.get(), 6)));

        remove("hbm:assembly_machine/mpt15hydro");
        this.register(new GenericRecipe("ass.mp_thruster_15_hydrogen").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_hydrogen.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(8),
                        STEEL.plate().count(6),
                        BIGMT.ingot().count(4)));

        remove("hbm:assembly_machine/mpt15hydrodual");
        this.register(new GenericRecipe("ass.mp_thruster_15_hydrogen_dual").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_hydrogen_dual.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(4),
                        STEEL.plate().count(6),
                        BIGMT.ingot()));

        remove("hbm:assembly_machine/mpt15bfshort");
        this.register(new GenericRecipe("ass.mp_thruster_15_balefire_short").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_balefire_short.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModItems.plate_polymer.get(), 8),
                        new ComparableStack(ModPwrBlocks.PWR_FUEL.get().asItem(), 1),
                        DESH.ingot().count(8),
                        BIGMT.plate().count(12),
                        CU.plateCast().count(2),
                        new ComparableStack(ModItems.ingot_uranium_fuel.get(), 4)));

        remove("hbm:assembly_machine/mpt15bf");
        this.register(new GenericRecipe("ass.mp_thruster_15_balefire").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_balefire.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModItems.plate_polymer.get(), 16),
                        new ComparableStack(ModPwrBlocks.PWR_FUEL.get().asItem(), 2),
                        DESH.ingot().count(16),
                        BIGMT.plate().count(24),
                        CU.plateCast().count(4),
                        new ComparableStack(ModItems.ingot_uranium_fuel.get(), 8)));

        remove("hbm:assembly_machine/mpt15bflarge");
        this.register(new GenericRecipe("ass.mp_thruster_15_balefire_large").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_15_balefire_large.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModItems.plate_polymer.get(), 16),
                        new ComparableStack(ModPwrBlocks.PWR_FUEL.get().asItem(), 2),
                        DESH.ingot().count(24),
                        BIGMT.plate().count(32),
                        CU.plateCast().count(4),
                        new ComparableStack(ModItems.ingot_uranium_fuel.get(), 8)));

        remove("hbm:assembly_machine/mpt20kero");
        this.register(new GenericRecipe("ass.mp_thruster_20_kerosene").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(8),
                        STEEL.plate().count(6),
                        DESH.ingot().count(2)));

        remove("hbm:assembly_machine/mpt20kerodual");
        this.register(new GenericRecipe("ass.mp_thruster_20_kerosene_dual").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene_dual.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(12),
                        STEEL.plate().count(8),
                        DESH.ingot().count(4)));

        remove("hbm:assembly_machine/mpt20kerotriple");
        this.register(new GenericRecipe("ass.mp_thruster_20_kerosene_triple").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_kerosene_triple.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(16),
                        STEEL.plate().count(12),
                        DESH.ingot().count(6)));

        this.register(new GenericRecipe("ass.mp_thruster_20_methalox").setup(400, 100).outputItems(new ItemStack(RocketItems.MP_THRUSTER_20_METHALOX.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(8),
                        STEEL.plate().count(6),
                        DESH.ingot().count(2)));

        this.register(new GenericRecipe("ass.mp_thruster_20_methalox_dual").setup(400, 100).outputItems(new ItemStack(RocketItems.MP_THRUSTER_20_METHALOX_DUAL.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(12),
                        STEEL.plate().count(8),
                        DESH.ingot().count(4)));

        this.register(new GenericRecipe("ass.mp_thruster_20_methalox_triple").setup(400, 100).outputItems(new ItemStack(RocketItems.MP_THRUSTER_20_METHALOX_TRIPLE.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(16),
                        STEEL.plate().count(12),
                        DESH.ingot().count(6)));

        this.register(new GenericRecipe("ass.mp_thruster_20_hydrogen").setup(400, 100).outputItems(new ItemStack(RocketItems.MP_THRUSTER_20_HYDROGEN.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(8),
                        STEEL.plate().count(6),
                        BIGMT.ingot().count(2)));

        this.register(new GenericRecipe("ass.mp_thruster_20_hydrogen_dual").setup(400, 100).outputItems(new ItemStack(RocketItems.MP_THRUSTER_20_HYDROGEN_DUAL.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(12),
                        STEEL.plate().count(8),
                        BIGMT.ingot().count(4)));

        this.register(new GenericRecipe("ass.mp_thruster_20_hydrogen_triple").setup(400, 100).outputItems(new ItemStack(RocketItems.MP_THRUSTER_20_HYDROGEN_TRIPLE.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        STEEL.pipe(),
                        W.ingot().count(16),
                        STEEL.plate().count(12),
                        BIGMT.ingot().count(6)));

        remove("hbm:assembly_machine/mpt20solid");
        this.register(new GenericRecipe("ass.mp_thruster_20_solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_solid.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        new ComparableStack(ModItems.coil_tungsten.get(), 8),
                        DURA.ingot().count(16),
                        STEEL.plate()));

        remove("hbm:assembly_machine/mpt20solidmulti");
        this.register(new GenericRecipe("ass.mp_thruster_20_solid_multi").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_solid_multi.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        new ComparableStack(ModItems.coil_tungsten.get(), 12),
                        DURA.ingot().count(18),
                        STEEL.plate()));

        remove("hbm:assembly_machine/mpt20solidmultier");
        this.register(new GenericRecipe("ass.mp_thruster_20_solid_multier").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_thruster_20_solid_multier.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        new ComparableStack(ModItems.coil_tungsten.get(), 16),
                        DURA.ingot().count(20),
                        STEEL.plate()));

        remove("hbm:assembly_machine/mpf10kero");
        this.register(new GenericRecipe("ass.mp_fuselage_10_kerosene").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_kerosene.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 3),
                        TI.plate().count(12),
                        STEEL.plate().count(3)));

        remove("hbm:assembly_machine/mpf10solid");
        this.register(new GenericRecipe("ass.mp_fuselage_10_solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_solid.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 3),
                        TI.plate().count(12),
                        AL.plate().count(3)));

        remove("hbm:assembly_machine/mpf10xenon");
        this.register(new GenericRecipe("ass.mp_fuselage_10_xenon").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_xenon.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 3),
                        TI.plate().count(12),
                        CU.plateCast().count(3)));

        remove("hbm:assembly_machine/mpf10kerolong");
        this.register(new GenericRecipe("ass.mp_fuselage_10_long_kerosene").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_long_kerosene.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 6),
                        TI.plate().count(24),
                        STEEL.plate().count(6)));

        remove("hbm:assembly_machine/mpf10solidlong");
        this.register(new GenericRecipe("ass.mp_fuselage_10_long_solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_long_solid.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 6),
                        TI.plate().count(24),
                        AL.plate().count(6)));

        remove("hbm:assembly_machine/mpf1015kero");
        this.register(new GenericRecipe("ass.mp_fuselage_10_15_kerosene").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_15_kerosene.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 1),
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 9),
                        TI.plate().count(36),
                        STEEL.plate().count(9)));

        remove("hbm:assembly_machine/mpf1015solid");
        this.register(new GenericRecipe("ass.mp_fuselage_10_15_solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_15_solid.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 1),
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 9),
                        TI.plate().count(36),
                        AL.plate().count(9)));

        remove("hbm:assembly_machine/mpf1015hydro");
        this.register(new GenericRecipe("ass.mp_fuselage_10_15_hydrogen").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_15_hydrogen.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 1),
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 9),
                        TI.plate().count(36),
                        IRON.plate().count(9)));

        remove("hbm:assembly_machine/mpf1015bf");
        this.register(new GenericRecipe("ass.mp_fuselage_10_15_balefire").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_10_15_balefire.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_10.get(), 1),
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 9),
                        TI.plate().count(36),
                        BIGMT.plate().count(9)));

        remove("hbm:assembly_machine/mpf15kero");
        this.register(new GenericRecipe("ass.mp_fuselage_15_kerosene").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_kerosene.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 12),
                        TI.plate().count(48),
                        STEEL.plate()));

        remove("hbm:assembly_machine/mpf15solid");
        this.register(new GenericRecipe("ass.mp_fuselage_15_solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_solid.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 12),
                        TI.plate().count(48),
                        AL.plate()));

        remove("hbm:assembly_machine/mpf15hydro");
        this.register(new GenericRecipe("ass.mp_fuselage_15_hydrogen").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_hydrogen.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 12),
                        TI.plate().count(48),
                        IRON.plate()));

        remove("hbm:assembly_machine/mpf1520kero");
        this.register(new GenericRecipe("ass.mp_fuselage_15_20_kerosene").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_20_kerosene.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16),
                        TI.plate().count(64),
                        STEEL.plate()));

        remove("hbm:assembly_machine/mpf1520solid");
        this.register(new GenericRecipe("ass.mp_fuselage_15_20_solid").setup(400, 100).outputItems(new ItemStack(ModCustomMissileItems.mp_fuselage_15_20_solid.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_15.get(), 1),
                        new ComparableStack(ModItems.seg_20.get(), 1),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16),
                        TI.plate().count(64),
                        AL.plate()));

        this.register(new GenericRecipe("ass.rp_fuselage_20_12").setup(400, 100).outputItems(new ItemStack(RocketItems.RP_F_20_12.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 16),
                        TI.shell().count(12),
                        AL.plateWelded().count(8)));

        this.register(new GenericRecipe("ass.rp_fuselage_20_6").setup(400, 100).outputItems(new ItemStack(RocketItems.RP_F_20_6.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 8),
                        TI.shell().count(6),
                        AL.plateWelded().count(4)));

        this.register(new GenericRecipe("ass.rp_fuselage_20_3").setup(400, 100).outputItems(new ItemStack(RocketItems.RP_F_20_3.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 4),
                        TI.shell().count(3),
                        AL.plateWelded().count(2)));

        this.register(new GenericRecipe("ass.rp_fuselage_20_1").setup(400, 100).outputItems(new ItemStack(RocketItems.RP_F_20_1.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 2),
                        new ComparableStack(ModSteelBlocks.steel_scaffold.get().asItem(), 2),
                        TI.shell(),
                        AL.plateWelded()));

        this.register(new GenericRecipe("ass.rp_fuselage_20_12_hydrazine").setup(400, 100).outputItems(new ItemStack(RocketItems.MP_FUSELAGE_20_HYDRAZINE.get(), 1))
                .inputItems(
                        new ComparableStack(ModItems.seg_20.get(), 2),
                        TI.shell().count(12),
                        AL.plateWelded().count(16),
                        POLYMER.ingot().count(8)));

        this.register(new GenericRecipe("ass.rp_capsule_20").setup(200, 100).outputItems(new ItemStack(RocketItems.RP_C_20.get(), 1))
                .inputItems(
                        new ComparableStack(ModFuelItems.rocket_fuel.get(), 8),
                        new ComparableStack(ModItems.thruster_small.get(), 4),
                        new ComparableStack(SpaceMachineItems.circuit("AERO"), 1),
                        ANY_RUBBER.ingot().count(16),
                        AL.shell().count(4),
                        FIBER.ingot().count(12)));

        this.register(new GenericRecipe("ass.rp_legs_20").setup(100, 100).outputItems(new ItemStack(RocketItems.RP_L_20.get(), 1))
                .inputItems(
                        STEEL.pipe().count(4),
                        AL.plate().count(8),
                        new ComparableStack(ModItems.motor.get(), 4)));

        this.register(new GenericRecipe("ass.astroturf").setup(100, 100).outputItems(new ItemStack(FloraBlocks.ASTROTURF.get().asItem(), 16))
                .inputItems(
                        new ComparableStack(Blocks.DIRT.asItem(), 16),
                        ANY_PLASTIC.ingot()));
    }

    /**
     * Space's authored additions occupy the gap after their NTM source anchor. Replacements retain the loaded
     * base row's order; the insertion order here is the same authored sequence as SpaceDatagen's addon groups.
     */
    @Override
    public void register(String id, GenericRecipe recipe) {
        if (recipe.order != Integer.MAX_VALUE) {
            super.register(id, recipe);
            return;
        }
        var anchor = switch (id(recipe)) {
            case "platenickel" -> "platetitanium";
            case "platestainless" -> "platesteel";
            case "exstainlessbeam", "exstainlessbeamalt", "exavionics", "exastro" -> "exgold";
            case "gaschip", "hfchip", "molychip" -> "atomicclockalt";
            case "nukeantimatter", "dysonmember", "satdysonrelay" -> "satlink";
            default -> "nitra";
        };
        registerAfter("hbm:assembly_machine/" + anchor, id, recipe, AssemblyMachineRecipes.ORDER_STRIDE);
    }

    /** Registration above supplies orders relative to base anchors instead of using this source's own indices. */
    @Override
    protected boolean ordered() {
        return false;
    }

    /** A row hbm-space changes: it takes the {@code order} of HBM's row of the same id, the one it replaces. */
    private GenericRecipe replacing(GenericRecipe recipe) {
        recipe.order = orderOf("hbm:assembly_machine/" + id(recipe));
        return recipe;
    }

    /**
     * 1.7 {@code STAINLESS.plateCast()} / {@code plateWelded()}: the ore dictionary name of that shape, here its
     * {@code c:} tag ({@code c:plates_welded/stainless}).
     */
    private static AStack shapeTag(NTMMaterial mat, MaterialShapes shape) {
        return new TagStack(mat.tag(shape));
    }

    /** 1.7 {@code fluid.getDict(amount)}: any full container of that much of the fluid. */
    private static AStack getDict(FluidType fluid, int amount) {
        return new FluidContainerStack(fluid.getName(), amount);
    }

    /** 1.7 {@code new ComparableStack(ModItems.fluid_barrel_full, count, fluid.getID())}: full barrels of the fluid. */
    private static AStack fullBarrel(FluidType fluid, int count) {
        return new RecipeIngredients.FluidItem(ModFluidItems.fluid_barrel.get(), fluid, count);
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Ids.of(namespace, path));
    }
}
