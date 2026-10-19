package com.ntmspace.block.station;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.tileentity.station.TileEntityMachineHTRNeo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineHTRFNeo}: the HTR-F4's frame ({@code {2, 2, 2, 2, 11, 9}}, offset 11, two above, turned a
 * quarter), with four power and coolant proxies on the bottom layer. 1.7 offset their x by three along the facing and
 * their z by two, so for a north/south facing they sit on the structure's long sides and for an east/west facing outside
 * it (where {@code makeExtra} did nothing); kept, with the check that a proxy must lie inside the structure.
 */
public class MachineHTRFNeo extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineHTRFNeo(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineHTRNeo(pos, state);
    }

    @Override public int[] getDimensions() { return MachineHTRF4.DIMS.clone(); }
    @Override public int getOffset() { return 11; }
    @Override public int getHeightOffset() { return 2; }

    @Override
    public Direction getDirModified(Direction direction) {
        return ForgeDirection.getOrientation(direction).getRotation(ForgeDirection.DOWN).toDirection();
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        StationMachines.ports(core, MachineHTRF4.DIMS, facing, TileEntityMachineHTRNeo.conPos(core, ForgeDirection.getOrientation(facing)),
                ROLE_POWER_IN | ROLE_FLUID, visitor);
    }

    /** 1.7 {@code makeExtra} x4: {@code TileEntityProxyCombo(false, true, true)}. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (BlockPos extra : extras(core, facing)) visitor.passiveCell(extra, MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
    }

    static List<BlockPos> extras(BlockPos core, Direction facing) {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY() - 2, z = core.getZ();
        List<BlockPos> extras = new ArrayList<>();
        for (BlockPos pos : new BlockPos[] {
                new BlockPos(x - rot.offsetX * 5 - dir.offsetX * 3, y, z - rot.offsetZ * 5 - dir.offsetZ * 2),
                new BlockPos(x + rot.offsetX * 1 - dir.offsetX * 3, y, z + rot.offsetZ * 1 - dir.offsetZ * 2),
                new BlockPos(x - rot.offsetX * 5 - dir.offsetX * 3, y, z - rot.offsetZ * 5 + dir.offsetZ * 2),
                new BlockPos(x + rot.offsetX * 1 - dir.offsetX * 3, y, z + rot.offsetZ * 1 + dir.offsetZ * 2)}) {
            // 1.7 makeExtra did nothing where the block was not part of the machine
            if (StationMachines.inBox(core, MachineHTRF4.DIMS, facing, pos)) extras.add(pos);
        }
        return extras;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!Orbit.inOrbit(level)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineHTRNeo thruster)) return null;

        List<String> text = new ArrayList<>();
        if (!thruster.isFacingPrograde()) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.engineFacing").getString() + " ! ! !");
        } else {
            text.add("Plasma Energy: " + (thruster.plasmaEnergy == 0 ? ChatFormatting.RED : ChatFormatting.GREEN)
                    + BobMathUtil.getShortNumber(thruster.plasmaEnergy) + "TU");
            text.add("Power: " + (thruster.power < thruster.getMaxPower() ? ChatFormatting.RED : ChatFormatting.GREEN)
                    + BobMathUtil.getShortNumber(thruster.power) + "HE");

            int heat = (int) Math.ceil(thruster.temperature);
            String label = (heat > 123 ? ChatFormatting.RED : ChatFormatting.AQUA) + "" + heat + "K";
            text.add("Temperature: " + label);

            text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + thruster.coolantTanks[0].getTankType().getDisplayNameString() + ": "
                    + thruster.coolantTanks[0].getFill() + "/" + thruster.coolantTanks[0].getMaxFill() + "mB");
            text.add(ChatFormatting.RED + "<- " + ChatFormatting.RESET + thruster.coolantTanks[1].getTankType().getDisplayNameString() + ": "
                    + thruster.coolantTanks[1].getFill() + "/" + thruster.coolantTanks[1].getMaxFill() + "mB");

            if (!thruster.isCool()) text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! INSUFFICIENT COOLING ! ! !");

            if (extras(core, level.getBlockState(core).getValue(FACING)).contains(pos)) {
                if (core.getX() == pos.getX() || core.getZ() == pos.getZ()) text.add("Connect to Fusion Reactor from here");
            }
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
