package com.ntmspace.gametest;

import com.ntmspace.NtmSpace;
import dev.golem.core.gametest.GameTests;
import java.util.ServiceLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

/** NTM: Space's GameTests (the ntmspace:* namespace). */
public final class SpaceGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(NtmSpace.MOD_ID);

    /** The addon loaded on top of HBM: an HBM block resolves from the addon's code. */
    public static final GameTests.Function LOADED = TESTS.function("addon_loaded_on_hbm", 20, () -> SpaceGameTests::loaded);

    /**
     * The Mun generator on its own (a GameTest world never has datapack dimensions; the in-world check is the harness's
     * {@code /ntmspace check mun}): for a fixed seed every primer holds only Mun blocks, has bedrock at y 0, and the
     * generator's base column (read by structures and spawn search) shows the primer's blocks.
     */
    public static final GameTests.Function MUN_GENERATOR = TESTS.function("mun_generator_primer", 200,
            () -> SpaceGameTests::munGenerator);

    /**
     * hbm-space's chemical plant, through HBM's datapack table: the four air recipes are removed
     * ({@code data/ntmspace/recipe_removals}), {@code chem.hcl} is added in the addon's own namespace, and its output is
     * the addon fluid {@code ntmspace:hcl}, also a vanilla fluid.
     */
    public static final GameTests.Function CHEMICAL_PLANT = TESTS.function("chemical_plant_space_recipes", 20,
            () -> SpaceGameTests::chemicalPlant);

    /** {@code hbm:fan} is NTM: Space's substitute (same id, same block entity), and HBM's own handle returns it. */
    public static final GameTests.Function FAN_SUBSTITUTION = TESTS.function("fan_substitution", 20,
            () -> SpaceGameTests::fanSubstitution);

    private SpaceGameTests() {}

    private static void fanSubstitution(GameTestHelper helper) {
        net.minecraft.world.level.block.Block fan = com.hbm.blocks.machine.ModMachineBlocks.fan.get();
        if (!(fan instanceof com.ntmspace.block.SpaceMachineFan)) {
            fail(helper, "hbm:fan is " + fan.getClass().getName() + ", not NTM: Space's substitute");
            return;
        }
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(pos, fan.defaultBlockState());
        // throws (failing the test) if the substitute has no HBM fan block entity
        dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, com.hbm.blocks.machine.MachineFan.TileEntityFan.class);
        helper.succeed();
    }

    private static void chemicalPlant(GameTestHelper helper) {
        com.hbm.inventory.recipes.ChemicalPlantRecipes plant = com.hbm.inventory.recipes.ChemicalPlantRecipes.INSTANCE;
        plant.registerRecipes();
        for (String off : new String[] {"chem.oxygen", "chem.xenon", "chem.xenonoxy", "chem.birkeland"}) {
            if (plant.getRecipe(off) != null) {
                fail(helper, off + " should be switched off by NTM: Space");
                return;
            }
        }
        com.hbm.inventory.recipes.loader.GenericRecipe hcl = plant.getRecipe("chem.hcl");
        if (hcl == null || hcl.outputFluid == null || hcl.outputFluid[0].type != com.ntmspace.fluid.SpaceFluids.HCL
                || hcl.outputFluid[0].fill != 1000 || hcl.inputFluid.length != 2) {
            fail(helper, "chem.hcl missing or wrong: " + (hcl == null ? null : com.hbm.inventory.recipes.loader.GenericRecipeCodec.describe(hcl)));
            return;
        }
        com.hbm.inventory.fluid.trait.FT_Corrosive corrosive =
                com.ntmspace.fluid.SpaceFluids.HCL.getTrait(com.hbm.inventory.fluid.trait.FT_Corrosive.class);
        if (corrosive == null || corrosive.getRating() != 30
                || !com.ntmspace.fluid.SpaceFluids.HCL.hasTrait(com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid.class)) {
            fail(helper, "ntmspace:hcl did not get hbm-space's traits from data/ntmspace/fluid_property/hcl.json");
            return;
        }
        net.minecraft.world.level.material.Fluid vanilla = com.hbm.inventory.fluid.Fluids.toVanilla(com.ntmspace.fluid.SpaceFluids.HCL);
        String id = String.valueOf(BuiltInRegistries.FLUID.getKey(vanilla));
        if (!id.equals("ntmspace:hcl") || com.hbm.inventory.fluid.Fluids.fromVanilla(vanilla) != com.ntmspace.fluid.SpaceFluids.HCL
                || com.hbm.inventory.fluid.Fluids.byRegistryName("ntmspace:hcl") != com.ntmspace.fluid.SpaceFluids.HCL) {
            fail(helper, "ntmspace:hcl is not registered as an HBM and a vanilla fluid: " + id);
            return;
        }
        helper.succeed();
    }

    public static void init() {}

    private static boolean initialised;

    /**
     * Declares the native suites and then development extensions on {@link #TESTS}. Fabric's {@link GameTestEntry}
     * and the Forge-family GameTestSetup call it before registration closes. The legacy Fabric generator can run
     * earlier, so it uses the same guard to prevent duplicate declarations and extension registration.
     */
    public static synchronized void initAll() {
        if (initialised) return;
        initialised = true;
        StructureGameTests.init();
        StationGameTests.init();
        RocketGameTests.init();
        StationLauncherGameTests.init();
        TransporterGameTests.init();
        Lpw2GameTests.init();
        LaytheTektoThatmoGameTests.init();
        DunaIkeDresGameTests.init();
        PlanetGameTests.init();
        SpaceFluidGameTests.init();
        LayeredBiomeGameTests.init();
        PopulateGameTests.init();
        AtmosphereGameTests.init();
        AtmosphereMachineGameTests.init();
        FloraGameTests.init();
        SpaceMobGameTests.init();
        MachineGameTests.init();
        MachineGameTestsB.init();
        MachineGameTestsC.init();
        MachineGameTestsD.init();
        PortGameTests.init();
        DropPodGameTests.init();
        FacadeGameTests.init();
        WarGameTests.init();
        NativeNetherPortalGameTests.init();
        RocketRecipeGameTests.init();
        RecipeChangeGameTests.init();
        DriveProcessorGameTests.init();
        BedrockSatelliteGameTests.init();
        DysonGameTests.init();
        ForkGameTests.init();
        LeftoverGameTests.init();
        SkyWeatherGameTests.init();
        CloudGameTests.init();
        SkyShaderGameTests.init();
        StardarThatmoGameTests.init();
        ThatmoStrikeGameTests.init();
        ServiceLoader.load(SpaceGameTestExtension.class, SpaceGameTestExtension.class.getClassLoader())
                .forEach(SpaceGameTestExtension::register);
    }

    private static void munGenerator(GameTestHelper helper) {
        com.ntmspace.world.MunChunkGenerator generator = new com.ntmspace.world.MunChunkGenerator(
                helper.getLevel().getChunkSource().getGenerator().getBiomeSource());
        generator.useSeed(4815162342L);
        java.util.Set<String> allowed = java.util.Set.of("minecraft:bedrock", "minecraft:gravel", "minecraft:lava",
                "ntmspace:moon_rock", "hbm:moon_turf", "hbm:basalt", "ntmspace:ore_brine", "hbm:stone_cracked",
                "hbm:oil_spill");
        int[][] chunks = {{0, 0}, {3, -2}, {-7, 11}, {40, 40}, {-100, 77}};
        int surfaces = 0;
        for (int[] c : chunks) {
            com.ntmspace.world.gen.legacy.CelestialTerrain17.Primer primer = generator.primer(c[0], c[1]);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int column = (x * 16 + z) * 256;
                    if (primer.blocks[column] != com.ntmspace.world.gen.legacy.Block17.BEDROCK) {
                        fail(helper, "no bedrock at y 0 in chunk " + c[0] + "," + c[1]);
                        return;
                    }
                    int top = -1;
                    for (int y = 0; y < 256; y++) {
                        com.ntmspace.world.gen.legacy.Block17 block = primer.blocks[column + y];
                        if (block == null) continue;
                        if (!allowed.contains(block.id)) {
                            fail(helper, "chunk " + c[0] + "," + c[1] + " has " + block.id + " at y " + y);
                            return;
                        }
                        if (block.isOpaqueCube()) top = y;
                    }
                    net.minecraft.world.level.NoiseColumn noise = generator.getBaseColumn(c[0] * 16 + x, c[1] * 16 + z,
                            helper.getLevel(), null);
                    String at = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(
                            noise.getBlock(top).getBlock()).toString();
                    if (top >= 0 && !at.equals(primer.blocks[column + top].id)) {
                        fail(helper, "base column says " + at + " where the primer has " + primer.blocks[column + top].id);
                        return;
                    }
                    if (top > 40) surfaces++;
                }
            }
        }
        if (surfaces < chunks.length * 256 / 2) fail(helper, "most Mun columns should reach above y 40, only " + surfaces + " did");
        helper.succeed();
    }

    private static void loaded(GameTestHelper helper) {
        boolean hbm = BuiltInRegistries.BLOCK.keySet().stream().anyMatch(id -> id.getNamespace().equals("hbm"));
        if (!hbm) fail(helper, "no hbm: block is registered");
        helper.succeed();
    }

    /** Fails the test, logging the reason too: 1.20.1 Fabric's runner prints only the failed test's name. */
    private static void fail(GameTestHelper helper, String message) {
        com.ntmspace.NtmSpace.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
