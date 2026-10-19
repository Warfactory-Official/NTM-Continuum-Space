package com.ntmspace.block.station;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.tileentity.station.TileEntityMachineHTRF4;
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
 * hbm-space's {@code MachineHTRF4}: 21 x 5 x 5 ({@code {2, 2, 2, 2, 11, 9}}, offset 11, two above the clicked block,
 * turned a quarter from the player's facing), three power and fluid proxies across the far end of the long axis, power
 * taken at the two blocks beside them.
 */
public class MachineHTRF4 extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    static final int[] DIMS = {2, 2, 2, 2, 11, 9};

    public MachineHTRF4(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineHTRF4(pos, state);
    }

    @Override public int[] getDimensions() { return DIMS.clone(); }
    @Override public int getOffset() { return 11; }
    @Override public int getHeightOffset() { return 2; }

    @Override
    public Direction getDirModified(Direction direction) {
        return ForgeDirection.getOrientation(direction).getRotation(ForgeDirection.DOWN).toDirection();
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        StationMachines.ports(core, DIMS, facing, List.of(TileEntityMachineHTRF4.conPos(core, ForgeDirection.getOrientation(facing))),
                ROLE_POWER_IN, visitor);
    }

    /** 1.7 {@code makeExtra} x3 at the far end: {@code TileEntityProxyCombo(false, true, true)}. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (BlockPos extra : extras(core, facing)) visitor.passiveCell(extra, MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
    }

    static List<BlockPos> extras(BlockPos core, Direction facing) {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos end = core.offset(-rot.offsetX * 9, 0, -rot.offsetZ * 9);
        return List.of(end, end.offset(dir.offsetX, 0, dir.offsetZ), end.offset(-dir.offsetX, 0, -dir.offsetZ));
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!Orbit.inOrbit(level)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineHTRF4 thruster)) return null;

        List<String> text = new ArrayList<>();
        if (!thruster.isFacingPrograde()) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.engineFacing").getString() + " ! ! !");
        } else {
            text.add((thruster.power == 0 ? ChatFormatting.RED : ChatFormatting.GREEN) + BobMathUtil.getShortNumber(thruster.power) + "HE");
            for (FluidTankNTM tank : thruster.tanks) {
                text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tank.getTankType().getDisplayNameString() + ": "
                        + tank.getFill() + "/" + tank.getMaxFill() + "mB");
            }
            if (extras(core, level.getBlockState(core).getValue(FACING)).contains(pos)) {
                if (core.getX() == pos.getX() || core.getZ() == pos.getZ()) {
                    text.add("Connect to Plasma Heater from here");
                } else {
                    text.add("Connect to power from here");
                }
            }
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
