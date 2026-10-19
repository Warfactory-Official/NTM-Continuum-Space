package com.ntmspace.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.inventory.machine.ContainerMachineCryoDistill;
import com.ntmspace.recipe.CryoRecipes;
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
 * hbm-space's {@code TileEntityMachineCryoDistill}: separates an atmosphere (64,000 mB input, Earth air by default) into
 * four 24,000 mB fractions by its {@code ntmspace:cryo_distill} recipe, 100 mB and 20,000 HE (1 MHE buffer) per tick.
 * Eleven slots: 0 battery, 7 fluid identifier, and four unload pairs 1/2, 3/4, 5/6, 8/9 (slot 10 is unused, as in 1.7).
 *
 * <p>Not ported: 1.7 {@code IPersistentNBT} (the tanks kept in the dropped item), deferred like HBM's own machines.
 */
public class TileEntityMachineCryoDistill extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {

    public long power;
    public static final long maxPower = 1_000_000;

    public final FluidTankNTM[] tanks;

    public TileEntityMachineCryoDistill(BlockPos pos, BlockState state) {
        super(SpaceMachines.CRYO_DISTILL_BE.get(), pos, state, 11);
        tanks = new FluidTankNTM[5];
        tanks[0] = new FluidTankNTM(SpaceFluids.EARTHAIR, 64_000);
        tanks[1] = new FluidTankNTM(SpaceFluids.NITROGEN, 24_000);
        tanks[2] = new FluidTankNTM(Fluids.OXYGEN, 24_000);
        tanks[3] = new FluidTankNTM(SpaceFluids.KRYPTON, 24_000);
        tanks[4] = new FluidTankNTM(Fluids.CARBONDIOXIDE, 24_000);
    }

    @Override
    public String getDefaultName() {
        return "container.cryoDistillator";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCryoDistill te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        if (MachineSlots.setTypeFromId(te.tanks[0], te.inventory, 7, level, pos)) te.setChanged();

        if (te.cachedPowerPorts == null) {
            Ports ports = ports(pos, MachineCore.facing(state));
            te.cachedPowerPorts = MachinePorts.ports(state, pos, ports.power());
            te.setupFluidInPorts(te.getReceivingTanks(), PortDef.combine(MachinePorts.ports(state, pos, ports.input())));
            te.setupFluidOutPorts(te.getSendingTanks(), PortDef.combine(MachinePorts.ports(state, pos, ports.outputs())));
        }
        te.setupPowerPorts(te.cachedPowerPorts);
        te.updatePortPIFIFO();
        te.distill();

        MachineFluidContainerIO.unload(te.tanks[1], te.inventory, 1, 2);
        MachineFluidContainerIO.unload(te.tanks[2], te.inventory, 3, 4);
        MachineFluidContainerIO.unload(te.tanks[3], te.inventory, 5, 6);
        MachineFluidContainerIO.unload(te.tanks[4], te.inventory, 8, 9);

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCryoDistill te) {}

    /** 1.7 {@code distill}: one recipe step per tick. */
    void distill() {
        CryoRecipes.Output out = CryoRecipes.INSTANCE.getOutput(tanks[0].getTankType());
        if (out == null) {
            for (int i = 1; i < 5; i++) tanks[i].setTankType(Fluids.NONE);
            return;
        }

        FluidStack[] fractions = out.fractions();
        for (int i = 0; i < 4; i++) tanks[i + 1].setTankType(fractions[i].type);

        if (power < out.power()) return;
        if (tanks[0].getFill() < out.input()) return;
        for (int i = 0; i < 4; i++) {
            if (tanks[i + 1].getFill() + fractions[i].fill > tanks[i + 1].getMaxFill()) return;
        }

        tanks[0].setFill(tanks[0].getFill() - out.input());
        for (int i = 0; i < 4; i++) tanks[i + 1].setFill(tanks[i + 1].getFill() + fractions[i].fill);
        power -= out.power();
    }

    private PortDef[] cachedPowerPorts;

    /** 1.7 {@code getConPos} split by role: [0] input, [1..4] outputs, [5] power. */
    public record Ports(DirPos[] input, DirPos[] outputs, DirPos[] power) {}

    public static Ports ports(BlockPos core, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        DirPos input = new DirPos(x + dir.offsetX * -1 + rot.offsetX * -3, y - 2, z + dir.offsetZ * -1 + rot.offsetZ * -3, rot.getOpposite());
        DirPos[] outputs = {
                new DirPos(x + dir.offsetX * 4 + rot.offsetX * -2, y - 2, z + dir.offsetZ * 4 + rot.offsetZ * -2, dir),
                new DirPos(x + dir.offsetX * 4 + rot.offsetX * -1, y - 2, z + dir.offsetZ * 4 + rot.offsetZ * -1, dir),
                new DirPos(x + dir.offsetX * 4 + rot.offsetX * 1, y - 2, z + dir.offsetZ * 4 + rot.offsetZ * 1, dir),
                new DirPos(x + dir.offsetX * 4 + rot.offsetX * 2, y - 2, z + dir.offsetZ * 4 + rot.offsetZ * 2, dir),
        };
        DirPos power = new DirPos(x + dir.offsetX * -2 + rot.offsetX * -3, y - 2, z + dir.offsetZ * -2 + rot.offsetZ * -3, rot.getOpposite());
        return new Ports(new DirPos[] {input}, outputs, new DirPos[] {power});
    }

    public static Ports ports(BlockPos core, net.minecraft.core.Direction facing) {
        return ports(core, ForgeDirection.getOrientation(facing));
    }

    /** 1.7 {@code getRenderBoundingBox}. */
    public AABB renderBounds() {
        BlockPos p = worldPosition;
        return new AABB(p.getX() - 4, p.getY() - 2, p.getZ() - 4, p.getX() + 6, p.getY() + 5, p.getZ() + 4);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 7) return stack.getItem() instanceof IItemFluidId;
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
        for (int i = 1; i < 5; i++) tanks[i].writeToNBT(output, "o" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        tanks[0].readFromNBT(input, "input");
        for (int i = 1; i < 5; i++) tanks[i].readFromNBT(input, "o" + i);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tanks[1], tanks[2], tanks[3], tanks[4]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tanks[0]}; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineCryoDistill(containerId, playerInventory, this);
    }
}
