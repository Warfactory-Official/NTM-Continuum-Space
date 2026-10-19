package com.ntmspace.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.inventory.machine.ContainerMachineMilkReformer;
import com.ntmspace.registry.SpaceMachines;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityMachineMilkReformer}: every tick, 100 mB of milk (64,000 mB tank) and 10,000 HE (1 MHE
 * buffer) become 50 mB enriched milk, 35 mB condensed milk and 15 mB cream (32,000 mB tanks each). Slots: 0 battery,
 * 1 -> 2 input container, unload pairs 3/4, 5/6, 7/8 (9 and 10 unused, as in 1.7). It has no recipe table: the one
 * reaction is fixed in 1.7's code.
 */
public class TileEntityMachineMilkReformer extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {

    public final FluidTankNTM[] tanks;
    public long power;
    public static final long maxPower = 1_000_000;

    public TileEntityMachineMilkReformer(BlockPos pos, BlockState state) {
        super(SpaceMachines.MILK_REFORMER_BE.get(), pos, state, 11);
        tanks = new FluidTankNTM[4];
        tanks[0] = new FluidTankNTM(SpaceFluids.MILK, 64_000);
        tanks[1] = new FluidTankNTM(SpaceFluids.EMILK, 32_000);
        tanks[2] = new FluidTankNTM(SpaceFluids.CMILK, 32_000);
        tanks[3] = new FluidTankNTM(SpaceFluids.CREAM, 32_000);
    }

    @Override
    public String getDefaultName() {
        return "container.milkreformer";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMilkReformer te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        MachineFluidContainerIO.load(te.tanks[0], te.inventory, 1, 2);

        te.refine();

        MachineFluidContainerIO.unload(te.tanks[1], te.inventory, 3, 4);
        MachineFluidContainerIO.unload(te.tanks[2], te.inventory, 5, 6);
        MachineFluidContainerIO.unload(te.tanks[3], te.inventory, 7, 8);

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMilkReformer te) {}

    /** 1.7 {@code refine}. */
    public void refine() {
        if (power < 10_000) return;
        if (tanks[0].getFill() < 100) return;
        if (tanks[1].getFill() + 50 > tanks[1].getMaxFill()) return;
        if (tanks[2].getFill() + 35 > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + 15 > tanks[3].getMaxFill()) return;

        power -= 10_000;
        tanks[0].setFill(tanks[0].getFill() - 100);
        tanks[1].setFill(tanks[1].getFill() + 50);
        tanks[2].setFill(tanks[2].getFill() + 35);
        tanks[3].setFill(tanks[3].getFill() + 15);
    }

    private PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = MachinePorts.ports(getBlockState(), worldPosition, conPos(worldPosition));
        return cachedPorts;
    }

    /** 1.7 {@code getConPos} (world directions; the tower is symmetric). */
    public static DirPos[] conPos(BlockPos core) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        return new DirPos[] {
                new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x + 2, y, z - 1, Library.POS_X),
                new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z + 2, Library.POS_Z),
                new DirPos(x + 1, y, z - 2, Library.NEG_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z),
        };
    }

    /** 1.7 {@code getRenderBoundingBox}. */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 2, p.getY(), p.getZ() - 2, p.getX() + 3, p.getY() + 7, p.getZ() + 3);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        return true;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        tanks[0].writeToNBT(output, "input");
        for (int i = 1; i < 4; i++) tanks[i].writeToNBT(output, "m" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tanks[0].readFromNBT(input, "input");
        for (int i = 1; i < 4; i++) tanks[i].readFromNBT(input, "m" + i);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tanks[1], tanks[2], tanks[3]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tanks[0]}; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineMilkReformer(containerId, playerInventory, this);
    }
}
