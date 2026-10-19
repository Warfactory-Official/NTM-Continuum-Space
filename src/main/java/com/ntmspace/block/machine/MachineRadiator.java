package com.ntmspace.block.machine;

import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.machine.TileEntityRadiator;
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
 * hbm-space's {@code MachineRadiator} (Steam Radiator): 3 high, 3 wide, 1 deep ({@code getDimensions}
 * {1, 1, 0, 0, 1, 1}, offset 0, one block up), core in the middle. Its four corners are
 * {@code TileEntityProxyCombo(false, false, true)} fluid cells, and the faces behind them are the steam/water ports.
 */
public class MachineRadiator extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineRadiator(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadiator(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {1, 1, 0, 0, 1, 1}; }
    @Override public int getOffset() { return 0; }
    @Override public int getHeightOffset() { return 1; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        int[] dims = getDimensions();
        MachinePorts.ports(core, facing, pos -> MachinePorts.inBox(core, dims, facing, pos),
                TileEntityRadiator.conPos(core, MachinePorts.dir(facing)), ROLE_FLUID, visitor);
    }

    /** The four {@code makeExtra} corners: {@code TileEntityProxyCombo(false, false, true)}, fluid only. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        ForgeDirection dir = MachinePorts.dir(facing), rot = MachinePorts.rot(facing);
        for (int side : new int[] {1, -1}) {
            for (int y : new int[] {1, -1}) visitor.passiveCell(MachinePorts.offset(core, dir, 0, rot, side, y), MASK_ALL, PASSIVE_FLUID_IN);
        }
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityRadiator tower)) return null;

        List<String> text = new ArrayList<>();
        // never for the radiator itself (it is vacuum-optimised); 1.7 shared the line with the cooling towers
        if (!tower.vacuumOptimised && Environment.isVacuum(level, core)) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.noVacuum").getString() + " ! ! !");
        }
        for (int i = 0; i < tower.tanks.length; i++) {
            text.add((i < 1 ? (ChatFormatting.GREEN + "-> ") : (ChatFormatting.RED + "<- ")) + ChatFormatting.RESET
                    + tower.tanks[i].getTankType().getDisplayNameString() + ": " + tower.tanks[i].getFill() + "/"
                    + tower.tanks[i].getMaxFill() + "mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
