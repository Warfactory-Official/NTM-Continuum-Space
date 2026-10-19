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
import com.ntmspace.tileentity.station.TileEntityXenonThruster;
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
 * hbm-space's {@code MachineXenonThruster}: 3 x 3 x 2 ({@code {1, 1, 0, 1, 1, 1}}, offset 1, one block above the clicked
 * one), the two side cells of the core row are power and fluid proxies, and the three blocks behind the core row take
 * power and xenon.
 */
public class MachineXenonThruster extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    private static final int[] DIMS = {1, 1, 0, 1, 1, 1};

    public MachineXenonThruster(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityXenonThruster(pos, state);
    }

    @Override public int[] getDimensions() { return DIMS.clone(); }
    @Override public int getOffset() { return 1; }
    @Override public int getHeightOffset() { return 1; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        StationMachines.ports(core, DIMS, facing, List.of(TileEntityXenonThruster.conPos(core, ForgeDirection.getOrientation(facing))),
                ROLE_POWER_IN | ROLE_FLUID_IN, visitor);
    }

    @Override
    public int coreMask() {
        return StationMachines.coreFaces(BlockPos.ZERO, Direction.SOUTH,
                List.of(TileEntityXenonThruster.conPos(BlockPos.ZERO, ForgeDirection.SOUTH)));
    }

    /** 1.7 {@code makeExtra} either side of the core: {@code TileEntityProxyCombo(false, true, true)}. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        ForgeDirection rot = ForgeDirection.getOrientation(facing).getRotation(ForgeDirection.UP);
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(-rot.offsetX, 0, -rot.offsetZ), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rot.offsetX, 0, rot.offsetZ), MASK_ALL, domains);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!Orbit.inOrbit(level)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityXenonThruster thruster)) return null;

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
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
