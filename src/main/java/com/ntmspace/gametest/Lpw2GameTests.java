package com.ntmspace.gametest;

import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.ModPowerGenBlockEntities;
import com.ntmspace.block.station.SpaceMachineLPW2;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.tileentity.station.TileEntityMachineLPW2Space;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's LPW-2 station thruster, substituted for {@code hbm:machine_lpw2}. Expected fuel costs are hand-computed
 * from hbm-space's {@code SolarSystem.getFuelCost} (ve = isp * 9.81; propellant = mass * (e^(dv / ve) - 1) / 2, rounded
 * up to whole 100 mB): 200,000 kg by 2,000 m/s at ISP 321 (reformed kerosene) = 88,724.9 -> 88,800 mB; by 1,000 m/s at
 * the fallback ISP 300 = 40,465.2 -> 40,500 mB; by 4,000 m/s at ISP 321 = 256,170.9 -> 256,200 mB (over a full tank).
 */
public final class Lpw2GameTests {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    /** {@code hbm:machine_lpw2} is NTM: Space's block (offset 9, quarter turn) and its block entity is the station engine. */
    public static final GameTests.Function SUBSTITUTED = TESTS.function("lpw2_substituted", 20,
            () -> Lpw2GameTests::substituted);

    /** Burn check, fuel use, errors, thrust and start/stop times against hbm-space's values. */
    public static final GameTests.Function BURN = TESTS.function("lpw2_burn_matches_space", 20,
            () -> Lpw2GameTests::burn);

    /** Outside orbit the engine does nothing (1.7 returns first): no propulsion registration, no clock, no overlay. */
    public static final GameTests.Function OFF_ORBIT = TESTS.function("lpw2_inert_off_orbit", 20,
            () -> Lpw2GameTests::offOrbit);

    private Lpw2GameTests() {}

    public static void init() {}

    private static void substituted(GameTestHelper helper) {
        Block block = ModPowerGenBlocks.MACHINE_LPW2.get();
        helper.assertTrue(block instanceof SpaceMachineLPW2, "hbm:machine_lpw2 is " + block.getClass().getName());
        helper.assertTrue("hbm:machine_lpw2".equals(BuiltInRegistries.BLOCK.getKey(block).toString()), "block id");
        helper.assertTrue("hbm:machine_lpw2".equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ModPowerGenBlockEntities.LPW2.get()).toString()),
                "block entity type id");
        SpaceMachineLPW2 lpw2 = (SpaceMachineLPW2) block;
        helper.assertTrue(lpw2.getOffset() == 9, "hbm-space offset 9");
        helper.assertTrue(lpw2.getDirModified(Direction.NORTH) == Direction.WEST && lpw2.getDirModified(Direction.EAST) == Direction.NORTH,
                "dir.getRotation(DOWN): north -> west, east -> north");
        TileEntityMachineLPW2Space te = core(helper, new BlockPos(1, 2, 1), Direction.SOUTH);
        helper.assertTrue(te.getType() == ModPowerGenBlockEntities.LPW2.get(), "the core's type is hbm:machine_lpw2");
        helper.assertTrue(ModPowerGenBlockEntities.LPW2.get().create(te.getBlockPos(), te.getBlockState()) instanceof TileEntityMachineLPW2Space,
                "the type's factory (chunk loading) builds the station engine");
        helper.assertTrue(te.isFacingPrograde(), "facing south is prograde");
        helper.succeed();
    }

    private static void burn(GameTestHelper helper) {
        TileEntityMachineLPW2Space te = core(helper, new BlockPos(1, 2, 1), Direction.SOUTH);
        helper.assertTrue(te.tanks.length == 2 && te.tanks[0].getTankType() == Fluids.KEROSENE_REFORM && te.tanks[1].getTankType() == Fluids.OXYGEN
                && te.tanks[0].getMaxFill() == 256_000 && te.tanks[1].getMaxFill() == 256_000, "256,000 mB of reformed kerosene and oxygen");
        FT_Rocket rocket = Fluids.KEROSENE_REFORM.getTrait(FT_Rocket.class);
        helper.assertTrue(rocket != null && rocket.getISP() == 321, "reformed kerosene ISP 321");
        helper.assertTrue(te.getThrust() == 2_000_000_000F, "2 GN (F1 thrust)");

        te.tanks[0].setFill(100_000);
        te.tanks[1].setFill(80_000);
        helper.assertTrue(!te.canPerformBurn(200_000, 2000) && te.fuelCost == 88_800, "88,800 mB each, oxygen short: " + te.fuelCost);
        List<Component> errors = new ArrayList<>();
        te.addErrors(errors);
        helper.assertTrue(errors.size() == 1 && errors.get(0).getString().contains("needs 88800mB"), "one error, the oxygen: " + errors);

        te.tanks[1].setFill(256_000);
        helper.assertTrue(te.canPerformBurn(200_000, 2000), "both tanks hold 88,800 mB");
        helper.assertTrue(te.startBurn() == 120 && te.isOn && te.tanks[0].getFill() == 11_200 && te.tanks[1].getFill() == 167_200,
                "start: 120 ticks, 88,800 mB of each burned");
        helper.assertTrue(te.endBurn() == 120 && !te.isOn, "stop: 120 ticks of cooldown");

        te.tanks[0].setFill(256_000);
        te.tanks[1].setFill(256_000);
        helper.assertTrue(!te.canPerformBurn(200_000, 4000) && te.fuelCost == 256_200, "4,000 m/s needs 256,200 mB, over full tanks");

        te.tanks[0].setTankType(Fluids.WATER); // empties the tank (1.7 setTankType)
        te.tanks[0].setFill(256_000);
        helper.assertTrue(te.canPerformBurn(200_000, 1000) && te.fuelCost == 40_500, "no rocket trait: ISP 300, 40,500 mB");
        helper.succeed();
    }

    private static void offOrbit(GameTestHelper helper) {
        TileEntityMachineLPW2Space te = core(helper, new BlockPos(1, 2, 1), Direction.SOUTH);
        BlockPos pos = te.getBlockPos();
        te.isOn = true;
        for (int i = 0; i < 5; i++) {
            TileEntityMachineLPW2Space.serverTick(helper.getLevel(), pos, te.getBlockState(), te);
            TileEntityMachineLPW2Space.clientTick(helper.getLevel(), pos, te.getBlockState(), te);
        }
        OrbitalStation station = OrbitalStation.getStationFromPosition(helper.getLevel().getServer(), pos.getX(), pos.getZ());
        helper.assertTrue(!station.engines().contains(te), "no propulsion registered outside orbit");
        helper.assertTrue(te.isOn && te.soundtime == 0 && te.time == 0 && te.lastTime == 0,
                "nothing runs outside orbit (isOn untouched, no sound, the clock stands)");
        helper.assertTrue(te.animationTime(0.5F) == 0, "the renderer's clock stands outside orbit");
        helper.assertTrue(((SpaceMachineLPW2) ModPowerGenBlocks.MACHINE_LPW2.get()).getLookOverlay(helper.getLevel(), pos, null) == null,
                "no look overlay outside orbit");
        helper.succeed();
    }

    private static TileEntityMachineLPW2Space core(GameTestHelper helper, BlockPos relative, Direction facing) {
        BlockState state = ModPowerGenBlocks.MACHINE_LPW2.get().defaultBlockState().setValue(BlockMultiblockCore.FACING, facing);
        helper.setBlock(relative, state);
        return GameTestHelpers.blockEntity(helper, relative, TileEntityMachineLPW2Space.class);
    }
}
