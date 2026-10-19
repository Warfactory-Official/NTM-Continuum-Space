package com.ntmspace.probe;

import dev.golem.core.gametest.GameTests;
import net.minecraft.gametest.framework.GameTestHelper;

/** The probe's GameTests (the ntmprobe:* namespace). */
public final class ProbeGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(ProbeEntry.MOD_ID);

    /** Both addons started: the probe sees NTM: Space's classes and Golem Core ran its entry. */
    public static final GameTests.Function BOTH = TESTS.function("two_addons_loaded", 20, () -> ProbeGameTests::both);

    /**
     * HBM's open fluid model: the probe's own vanilla fluid, which HBM never registered, is an HBM fluid named by its
     * vanilla id; a barrel sending it through an HBM duct fills an HBM fluid tank.
     */
    public static final GameTests.Function FOREIGN_FLUID = TESTS.function("foreign_fluid_in_ducts", 100,
            () -> ProbeGameTests::foreignFluid);

    /**
     * The probe's datapack: its chemical plant recipe makes the foreign fluid, HBM's chem.cobble is removed by its
     * recipe_removals list, and two recipes with neutral conditions load or not on every loader.
     */
    public static final GameTests.Function RECIPES = TESTS.function("probe_recipes", 20, () -> ProbeGameTests::recipes);

    /** Two addons on one hook: the probe's sealed phase runs before NTM: Space's default-phase listener. */
    public static final GameTests.Function ORDER = TESTS.function("hook_phase_order", 20, () -> ProbeGameTests::order);

    private ProbeGameTests() {}

    private static com.hbm.inventory.fluid.FluidType probeGas() {
        net.minecraft.world.level.material.Fluid vanilla = net.minecraft.core.registries.BuiltInRegistries.FLUID
                .getOptional(dev.golem.core.Ids.of(ProbeEntry.MOD_ID, "probe_gas")).orElseThrow();
        return com.hbm.inventory.fluid.Fluids.fromVanilla(vanilla);
    }

    private static void foreignFluid(GameTestHelper helper) {
        com.hbm.inventory.fluid.FluidType gas = probeGas();
        if (gas == com.hbm.inventory.fluid.Fluids.NONE || !gas.isForeign() || !gas.getRegistryName().equals("ntmprobe:probe_gas")
                || com.hbm.inventory.fluid.Fluids.byRegistryName("ntmprobe:probe_gas") != gas
                || com.hbm.inventory.fluid.Fluids.byNetworkId(com.hbm.inventory.fluid.Fluids.networkId(gas)) != gas) {
            fail(helper, "the probe's fluid is not an HBM fluid by its vanilla id: " + gas);
            return;
        }
        net.minecraft.core.BlockPos tankPos = new net.minecraft.core.BlockPos(4, 2, 4);
        com.hbm.blocks.machine.MachineFluidTank block = com.hbm.blocks.machine.ModMachineBlocks.machine_fluidtank.get();
        helper.setBlock(tankPos, block.defaultBlockState());
        com.hbm.tileentity.machine.TileEntityMachineFluidTank tank = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, tankPos, com.hbm.tileentity.machine.TileEntityMachineFluidTank.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(tankPos), net.minecraft.core.Direction.NORTH);
        tank.tank.setTankType(gas);
        tank.mode = 0;
        com.hbm.lib.DirPos port = null;
        for (com.hbm.lib.DirPos candidate : tank.getConPos()) {
            if (helper.getLevel().getBlockState(candidate.getPos()).isAir()) {
                port = candidate;
                break;
            }
        }
        if (port == null) {
            fail(helper, "fluid tank exposes no free port");
            return;
        }
        net.minecraft.core.BlockPos ductPos = port.getPos();
        helper.getLevel().setBlock(ductPos, com.hbm.blocks.ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        ((com.hbm.tileentity.network.TileEntityFluidDuctNT) helper.getLevel().getBlockEntity(ductPos)).setFluidType(gas);
        net.minecraft.core.BlockPos barrelPos = ductPos.offset(port.getDir().offsetX, port.getDir().offsetY, port.getDir().offsetZ);
        helper.getLevel().setBlock(barrelPos, com.hbm.blocks.ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        com.hbm.tileentity.machine.storage.TileEntityBarrel barrel =
                (com.hbm.tileentity.machine.storage.TileEntityBarrel) helper.getLevel().getBlockEntity(barrelPos);
        barrel.tank.setTankType(gas);
        barrel.tank.setFill(8000);
        barrel.mode = 2;
        helper.succeedWhen(() -> helper.assertTrue(tank.tank.getFill() > 0, "the foreign fluid has not crossed the duct"));
    }

    private static void recipes(GameTestHelper helper) {
        com.hbm.inventory.recipes.ChemicalPlantRecipes plant = com.hbm.inventory.recipes.ChemicalPlantRecipes.INSTANCE;
        plant.registerRecipes();
        com.hbm.inventory.recipes.loader.GenericRecipe made = plant.getRecipe("chem.probe_gas");
        if (made == null || made.outputFluid == null || made.outputFluid[0].type != probeGas()) {
            fail(helper, "the probe's chemical plant recipe is missing or does not make its fluid");
            return;
        }
        // the neutral golem:conditions, rewritten per loader at build time: mod_loaded ntmspace holds, its negation not
        if (plant.getRecipe("chem.probe_conditional_on") == null || plant.getRecipe("chem.probe_conditional_off") != null) {
            fail(helper, "golem:conditions did not hold on this loader: on=" + plant.getRecipe("chem.probe_conditional_on")
                    + " off=" + plant.getRecipe("chem.probe_conditional_off"));
            return;
        }
        if (plant.getRecipe("chem.cobble") != null) {
            fail(helper, "chem.cobble should be switched off by the probe's datapack");
            return;
        }
        helper.succeed();
    }

    private static void order(GameTestHelper helper) {
        java.util.List<com.hbm.api.environment.Environment.Breathing> listeners =
                com.hbm.api.environment.Environment.BREATHING.listeners();
        net.minecraft.core.BlockPos pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        // the sealed answer must come first: seal a spot, and the first listener alone answers ALLOW there
        ProbeContent.seal(helper.getLevel(), pos);
        if (listeners.isEmpty() || listeners.get(0).breathe(helper.getLevel(), pos, 0) != dev.golem.core.hook.Verdict.ALLOW
                || com.hbm.api.environment.Environment.pressure(helper.getLevel(), pos) != 1.0D) {
            fail(helper, "the probe's sealed phase does not run first");
            return;
        }
        helper.succeed();
    }

    public static void init() {
        ProbeHookGameTests.init();
        ProbeRegistryGameTests.init();
    }

    private static void both(GameTestHelper helper) {
        if (com.ntmspace.NtmSpace.MOD_ID == null) fail(helper, "ntmspace classes missing");
        helper.succeed();
    }

    /** Fails the test, logging the reason too: 1.20.1 Fabric's runner prints only the failed test's name. */
    private static void fail(GameTestHelper helper, String message) {
        ProbeEntry.LOG.error("GameTest failure: {}", message);
        helper.fail(message);
    }
}
