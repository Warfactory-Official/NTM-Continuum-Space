package com.ntmspace.gametest;

import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.blocks.machine.MachineElectrolyser;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.hbm.tileentity.network.TileEntityCranePartitioner;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import com.hbm.util.ContaminationUtil;
import com.ntmspace.breathing.ItemModOxy;
import com.ntmspace.fork.ComplexCapacitorEntity;
import com.ntmspace.fork.ForkBaseChanges;
import com.ntmspace.fork.machine.DamagedMachines;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Part 3 of the fork-content tests: hbm-space's small changes to base-HBM classes ({@code ForkBaseChanges}), with
 * hbm-space's values ({@code BlockDummyable}, {@code CranePartitioner}, {@code PneumoTube}, {@code MachineElectrolyser},
 * {@code MachineCapacitor}, {@code IToolAreaAbility}, {@code EntityTNTPrimedBase}, {@code FluidLoaderFillableItem},
 * {@code ContaminationUtil}, {@code BlockBobble}, {@code IPersistentNBT} of the damaged machines).
 */
public final class ForkGameTests3 {

    private static final GameTests.Suite TESTS = SpaceGameTests.TESTS;

    public static final GameTests.Function SETTINGS =
            TESTS.function("fork_base_settings", 20, () -> ForkGameTests3::settings);
    public static final GameTests.Function WALL_PLACEMENT =
            TESTS.function("fork_wall_placement", 20, 8, () -> ForkGameTests3::wallPlacement);
    public static final GameTests.Function MACHINES =
            TESTS.function("fork_base_machines", 20, 8, () -> ForkGameTests3::machines);
    public static final GameTests.Function ARMOR_MODS =
            TESTS.function("fork_armor_mod_refill", 20, () -> ForkGameTests3::armorMods);
    public static final GameTests.Function DAMAGED_ITEM =
            TESTS.function("fork_damaged_machine_item", 20, 8, () -> ForkGameTests3::damagedItem);

    private ForkGameTests3() {}

    public static void init() {}

    private static void finish(GameTestHelper helper, List<String> errors) {
        if (!errors.isEmpty()) {
            String message = String.join("; ", errors);
            com.ntmspace.NtmSpace.LOG.error("GameTest failure: {}", message);
            helper.fail(message);
            return;
        }
        helper.succeed();
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElse(Blocks.AIR);
    }

    // ------------------------------------------------------------------------------------------------ settings

    private static void settings(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        if (!BlockMultiblockCore.wallPlacement || !BlockMultiblockCore.walkOutAfterPlacing) errors.add("wall placement is off");
        if (TileEntityCranePartitioner.inputCount != 96 || TileEntityCranePartitioner.outputCount != 8) {
            errors.add("partitioner " + TileEntityCranePartitioner.inputCount + "+" + TileEntityCranePartitioner.outputCount + ", hbm-space 96+8");
        }
        if (!TileEntityPneumoTube.anyGas) errors.add("compressors take air only");
        if (!MachineElectrolyser.sidePorts) errors.add("electrolyser side ports off");
        if (!com.hbm.entity.item.EntityTntNtm.spin) errors.add("charges do not tumble");
        if (!com.hbm.entity.mob.EntityGhost.watchPlayers) errors.add("ghosts do not watch players");
        if (!com.hbm.tileentity.machine.TileEntityGeiger.probabilisticClicks) errors.add("geiger click gate off");
        if (!com.hbm.items.tool.ItemDosimeter.probabilisticClicks) errors.add("dosimeter click gate off");
        if (!com.hbm.items.tool.ItemGeigerCounter.probabilisticClicks) errors.add("handheld geiger click gate off");
        if (com.hbm.blocks.generic.BlockGasBase.ashGlassesParticleChance != 9) {
            errors.add("ash-glasses cloud chance " + com.hbm.blocks.generic.BlockGasBase.ashGlassesParticleChance + ", hbm-space 1 in 9");
        }
        // hbm-space's dosimeter at 0.2 RAD/s: 1 - 0.2 * 0.5 of the click ticks are silent (plain 1.7: half)
        net.minecraft.util.RandomSource dosimeterRand = net.minecraft.util.RandomSource.create(4815162342L);
        int silent = 0;
        for (int i = 0; i < 100_000; i++) if (com.hbm.items.tool.ItemDosimeter.click(0.2F, dosimeterRand, dosimeterRand) == 0) silent++;
        if (silent < 88_000 || silent > 92_000) errors.add("dosimeter at 0.2 RAD/s silent on " + silent + " of 100000 click ticks, hbm-space ~90000");
        if (!MachineFluidContainerIO.fillArmorMods) errors.add("armor modules are not refilled");
        var stone = com.hbm.handler.ability.IToolAreaAbility.RECURSION_STONE.get();
        if (!stone.test(Blocks.STONE.defaultBlockState()) || !stone.test(Blocks.COBBLESTONE.defaultBlockState())) {
            errors.add("the vein miner must stop at stone and cobblestone");
        }
        if (stone.test(Blocks.DIRT.defaultBlockState())) errors.add("the vein miner stops at dirt");
        // hbm-space's immunity: only the ocelot (and the cat) lost it
        var level = helper.getLevel();
        if (ContaminationUtil.isRadImmune(new net.minecraft.world.entity.animal.feline.Ocelot(EntityType.OCELOT, level))) {
            errors.add("the ocelot is still immune");
        }
        if (ContaminationUtil.isRadImmune(new net.minecraft.world.entity.animal.feline.Cat(EntityType.CAT, level))) {
            errors.add("the cat is still immune");
        }
        if (!ContaminationUtil.isRadImmune(new net.minecraft.world.entity.monster.zombie.Zombie(EntityType.ZOMBIE, level))) {
            errors.add("the zombie lost its immunity");
        }
        if (!"Coilgun, Leadburster, Congo Lake models, and the 737".equals(BobbleType.PEEP.contribution)
                || !"CPU_CLOCK".equals(BobbleType.PEEP.scrap) || !"BRIDGE_BIOS".equals(BobbleType.MICROWAVE.scrap)
                || !BobbleType.MELLOW.contribution.startsWith("Celestial mechanics")) {
            errors.add("bobblehead credits are not hbm-space's");
        }
        // a Mellow figure shreds to hbm-space's scrap
        ItemStack peep = com.hbm.inventory.recipes.DecoCrateMachineRecipes.bobbleScrap(new ItemStack(
                BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", "bobblehead_peep")).orElse(net.minecraft.world.item.Items.AIR)));
        if (peep != null && !BuiltInRegistries.ITEM.getKey(peep.getItem()).getPath().equals("scrap_plastic_cpu_clock")) {
            errors.add("Peep shreds to " + peep);
        }
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ placement

    /** hbm-space {@code BlockDummyable.onBlockPlacedBy}: the chemical plant ({2, 0, 1, 1, 1, 1}, offset 1) on a wall and a ceiling. */
    private static void wallPlacement(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        Block plant = com.hbm.blocks.machine.ModMachineBlocks.machine_chemical_plant.get();
        Player player = GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 12, 0))));
        player.setYRot(0.0F);

        // wall: the north face of a block at (4, 2, 6); the plant faces north with its south side on (4, 2, 5)
        BlockPos wall = helper.absolutePos(new BlockPos(4, 2, 6));
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        use(player, plant, wall, Direction.NORTH);
        check(errors, level, helper.absolutePos(new BlockPos(4, 2, 4)), plant, Direction.NORTH, "wall");

        // ceiling: the bottom face of a block at (10, 6, 4), yaw 0 (north); the plant hangs with its top at y 5
        BlockPos ceiling = helper.absolutePos(new BlockPos(10, 6, 4));
        level.setBlock(ceiling, Blocks.STONE.defaultBlockState(), 3);
        use(player, plant, ceiling, Direction.DOWN);
        check(errors, level, helper.absolutePos(new BlockPos(10, 3, 5)), plant, Direction.NORTH, "ceiling");
        finish(helper, errors);
    }

    private static void use(Player player, Block block, BlockPos clicked, Direction face) {
        ItemStack stack = new ItemStack(block);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(clicked).add(face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, clicked, false)));
    }

    private static void check(List<String> errors, net.minecraft.world.level.Level level, BlockPos core, Block block,
                              Direction facing, String what) {
        var state = level.getBlockState(core);
        if (state.getBlock() != block || state.getValue(BlockMultiblockCore.FACING) != facing
                || !(level.getBlockEntity(core) != null)) {
            errors.add(what + ": no core facing " + facing + " at " + core + " (" + state + ")");
        }
    }

    // ------------------------------------------------------------------------------------------------ machines

    private static void machines(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        // partitioner: 96 queue + 8 declog slots
        BlockPos partitionerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(partitionerPos, com.hbm.blocks.ModConveyorBlocks.CRANE_PARTITIONER.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(partitionerPos) instanceof TileEntityCranePartitioner partitioner)
                || partitioner.getContainerSize() != 104 || partitioner.inputCount() != 96) {
            errors.add("the partitioner is not 96 + 8: " + level.getBlockEntity(partitionerPos));
        }
        // compressor tube: the tank's gas is the gas it takes
        BlockPos tubePos = helper.absolutePos(new BlockPos(3, 2, 1));
        level.setBlock(tubePos, com.hbm.blocks.ModPneumaticBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(tubePos) instanceof TileEntityPneumoTube tube) {
            tube.compair.setTankType(Fluids.OXYGEN);
            if (tube.gas() != Fluids.OXYGEN) errors.add("the compressor ignores its gas: " + tube.gas());
        } else {
            errors.add("no pneumatic tube block entity");
        }
        // electrolyser: four more connection points and the side cells open toward them (local frame: facing south)
        MachineElectrolyser electrolyser = com.hbm.blocks.machine.ModMachineBlocks.machine_electrolyser.get();
        if ((electrolyser.maskAt(3, 0, 0) & BlockMultiblockCore.MASK_EAST) == 0
                || (electrolyser.maskAt(-1, 0, 0) & BlockMultiblockCore.MASK_WEST) == 0
                || (electrolyser.maskAt(-1, 0, 5) & BlockMultiblockCore.MASK_WEST) == 0
                || (electrolyser.maskAt(-1, 0, -5) & BlockMultiblockCore.MASK_WEST) == 0
                || (electrolyser.maskAt(-1, 0, 5) & BlockMultiblockCore.MASK_SOUTH) == 0) {
            errors.add("electrolyser side ports missing from the mask table");
        }
        BlockPos electrolyserPos = helper.absolutePos(new BlockPos(6, 2, 6));
        level.setBlock(electrolyserPos, electrolyser.defaultBlockState(), 3);
        if (!(level.getBlockEntity(electrolyserPos) instanceof com.hbm.tileentity.machine.TileEntityMachineElectrolyser te)
                || te.getConPos().length != 18) {
            errors.add("the electrolyser does not have 1.7's sixteen connection points and hbm-space's two side cells");
        }
        // capacitor_complex: 1e17 HE in a block entity of its own, capacity lines first then the lore
        BlockPos capPos = helper.absolutePos(new BlockPos(1, 2, 4));
        level.setBlock(capPos, ForkBaseChanges.CAPACITOR_COMPLEX.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(capPos) instanceof ComplexCapacitorEntity cap)
                || cap.getMaxPower() != 100_000_000_000_000_000L) {
            errors.add("capacitor_complex block entity: " + level.getBlockEntity(capPos));
        }
        var tooltip = ForkBaseChanges.CAPACITOR_COMPLEX.get().tooltip(new ItemStack(ForkBaseChanges.CAPACITOR_COMPLEX_ITEM.get()),
                List.of(net.minecraft.network.chat.Component.literal(ForkBaseChanges.COMPLEX_LORE.get(0))));
        if (tooltip.isEmpty() || !tooltip.get(0).getString().startsWith("Stores up to")
                || !tooltip.get(5).getString().startsWith("TaCdSa236-7")) {
            errors.add("capacitor tooltip order: " + tooltip);
        }
        // TNT: a primed charge turns every tick
        var tnt = new com.hbm.entity.item.EntityTntNtm(com.hbm.entity.ModNtmEntities.TNT_NTM.get(), level);
        tnt.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 4, 6))));
        float yaw = tnt.getYRot(), pitch = tnt.getXRot();
        tnt.tick();
        if (tnt.getYRot() == yaw && tnt.getXRot() == pitch) errors.add("the primed charge does not tumble");
        tnt.discard();
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ armor modules

    /** hbm-space {@code FluidLoaderFillableItem.fill}: a chestplate in a tank's unload slot fills its oxygen tank module. */
    private static void armorMods(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ItemStack plate = new ItemStack(BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", "hazmat_plate")).orElse(net.minecraft.world.item.Items.AIR));
        ItemStack oxy = new ItemStack(BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", "oxy_plss")).orElse(net.minecraft.world.item.Items.AIR));
        ArmorModHandler.applyMod(plate, oxy);
        FluidTankNTM tank = new FluidTankNTM(Fluids.OXYGEN, 16_000);
        tank.setFill(10_000);
        NTMItemHandler inventory = new NTMItemHandler(2);
        inventory.setStackInSlot(0, plate);
        boolean moved = MachineFluidContainerIO.unload(tank, inventory, 0, 1);
        ItemStack after = inventory.getStackInSlot(0);
        ItemStack mod = ArmorModHandler.pryMod(after, ArmorModHandler.plate_only);
        if (!moved || tank.getFill() != 0 || ItemModOxy.getFuel(mod) != 10_000) {
            errors.add("module fuel " + ItemModOxy.getFuel(mod) + ", tank " + tank.getFill() + ", moved " + moved
                    + " (hbm-space: the module takes the tank's 10000 mB)");
        }
        if (!inventory.getStackInSlot(1).isEmpty()) errors.add("the chestplate left its slot (1.7 keeps it there)");
        finish(helper, errors);
    }

    // ------------------------------------------------------------------------------------------------ damaged item

    /** 1.7 {@code IPersistentNBT}: a damaged condenser drops as a damaged item, which places a damaged condenser. */
    private static void damagedItem(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        var level = helper.getLevel();
        Block condenser = block("hbm:machine_condenser_powered");
        BlockPos core = helper.absolutePos(new BlockPos(4, 2, 4));
        level.setBlock(core, condenser.defaultBlockState(), 3);
        if (!(level.getBlockEntity(core) instanceof DamagedMachines.Damageable machine)) {
            errors.add("no damageable condenser");
            finish(helper, errors);
            return;
        }
        List<ItemStack> intact = Block.getDrops(level.getBlockState(core), level, core, level.getBlockEntity(core));
        if (intact.stream().anyMatch(s -> ItemStacks.carriedBoolean(s, "damaged"))) errors.add("an intact condenser dropped damaged");
        machine.setDamaged(true);
        List<ItemStack> drops = Block.getDrops(level.getBlockState(core), level, core, level.getBlockEntity(core));
        ItemStack damaged = drops.stream().filter(s -> s.is(condenser.asItem())).findFirst().orElse(ItemStack.EMPTY);
        if (damaged.isEmpty() || !DamagedMachines.carriedDamage(damaged)) errors.add("a damaged condenser dropped " + drops);
        BlockPos other = helper.absolutePos(new BlockPos(4, 2, 10));
        level.setBlock(other, condenser.defaultBlockState(), 3);
        if (level.getBlockEntity(other) instanceof DamagedMachines.Damageable placed) {
            ItemStacks.applyComponents(level.getBlockEntity(other), damaged);
            if (!placed.isDamaged()) errors.add("the damaged item placed an intact condenser");
        } else {
            errors.add("no second condenser");
        }
        finish(helper, errors);
    }
}
