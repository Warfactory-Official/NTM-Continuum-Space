package com.ntmspace.block.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.ntmspace.tileentity.machine.TileEntityMachineGasDock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code MachineGasDock} (Gas Giant Harvester): a 3x3 pad ({@code getDimensions} {0, 0, 1, 1, 1, 1},
 * offset 1), 12 px tall on every cell ({@code setBlockBoundsBasedOnState}), core in the middle. Every cell that is not
 * the core is a {@code TileEntityProxyCombo().fluid()}. The ports are the nine blocks below the pad and the twelve
 * around it (1.7 {@code getConPos}, in world axes). No GUI.
 */
public class MachineGasDock extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineGasDock(Properties properties) {
        super(properties);
        // 1.7 setBlockBounds(0, 0, 0, 1, 12/16, 1) on the core and on every dummy of the pad
        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, 1.5D, 0.75D, 1.5D));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineGasDock(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {0, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        int[] dims = getDimensions();
        MachinePorts.ports(core, facing, pos -> MachinePorts.inBox(core, dims, facing, pos),
                TileEntityMachineGasDock.conPos(core), ROLE_FLUID, visitor);
    }

    /** The core touches the ports below it: its bottom face. */
    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    /** Every dummy: {@code TileEntityProxyCombo().fluid()}. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_FLUID_IN);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineGasDock dock)) return null;

        List<String> text = new ArrayList<>();
        for (int i = 0; i < dock.tanks.length; i++) {
            text.add((i < 1 ? (ChatFormatting.RED + "<- ") : (ChatFormatting.GREEN + "-> ")) + ChatFormatting.RESET
                    + dock.tanks[i].getTankType().getDisplayNameString() + ": " + dock.tanks[i].getFill() + "/"
                    + dock.tanks[i].getMaxFill() + "mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
