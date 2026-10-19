package com.ntmspace.block.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.tileentity.machine.TileEntityMachineCryoDistill;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineCryoDistill}: offset 3, two blocks up; {@code getDimensions} {3, 2, 3, 3, 2, 2} is only the
 * space check, the structure is five boxes (the midpoint plane, both sides of the walkway, the two top tanks). Six
 * {@code TileEntityProxyCombo().fluid().power()} cells on the bottom layer: four front outputs and two side inputs
 * (fluid at the front one, power behind it). Opens the GUI on use.
 */
public class MachineCryoDistill extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    /** The five 1.7 {@code fillSpace} boxes around the core. */
    static final int[][] BOXES = {
            {0, 0, 3, 3, 2, 2},
            {0, 2, 3, 0, 2, 2},
            {0, 2, -2, 3, 2, 2},
            {2, 0, 3, -1, 2, 2},
            {3, 0, -2, 3, 2, 2},
    };

    public MachineCryoDistill(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCryoDistill(pos, state);
    }

    // Not used in filling, but checks that this whole space is safe (1.7)
    @Override public int[] getDimensions() { return new int[] {3, 2, 3, 3, 2, 2}; }
    @Override public int getOffset() { return 3; }
    @Override public int getHeightOffset() { return 2; }

    static boolean isCell(BlockPos core, Direction facing, BlockPos pos) {
        for (int[] box : BOXES) if (MachinePorts.inBox(core, box, facing, pos)) return true;
        return false;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int[] box : BOXES) MultiblockHandlerXR.visitBox(core, box, facing, visitor);
        TileEntityMachineCryoDistill.Ports ports = TileEntityMachineCryoDistill.ports(core, MachinePorts.dir(facing));
        MachinePorts.Cells cells = pos -> isCell(core, facing, pos);
        MachinePorts.ports(core, facing, cells, ports.input(), ROLE_FLUID_IN, visitor);
        MachinePorts.ports(core, facing, cells, ports.outputs(), ROLE_FLUID_OUT, visitor);
        MachinePorts.ports(core, facing, cells, ports.power(), ROLE_POWER_IN, visitor);
    }

    /** The six {@code makeExtra} cells: {@code TileEntityProxyCombo().fluid().power()}. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (BlockPos extra : extras(core, facing)) visitor.passiveCell(extra, MASK_ALL, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
    }

    /** 1.7 {@code fillSpace}'s {@code makeExtra} positions, relative to the core (the clicked block is core + 3 dir). */
    static List<BlockPos> extras(BlockPos core, Direction facing) {
        ForgeDirection dir = MachinePorts.dir(facing), rot = MachinePorts.rot(facing);
        List<BlockPos> out = new ArrayList<>();
        // front face outputs
        for (int side : new int[] {-2, -1, 1, 2}) out.add(MachinePorts.offset(core, dir, 3, rot, side, -2));
        // side inputs
        out.add(MachinePorts.offset(core, dir, -1, rot, -2, -2));
        out.add(MachinePorts.offset(core, dir, -2, rot, -2, -2));
        return out;
    }

    @Override
    public int[][] getAllDimensions() {
        return BOXES.clone();
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineCryoDistill distill)) return null;

        ForgeDirection dir = MachinePorts.dir(level.getBlockState(core).getValue(FACING));
        List<String> text = new ArrayList<>();
        if (hitCheck(dir, core, -1, -2, -2, pos)) {
            text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + distill.tanks[0].getTankType().getDisplayNameString());
        }
        if (hitCheck(dir, core, -2, -2, -2, pos)) {
            text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + "Power");
        }
        if (hitCheck(dir, core, 3, -2, -2, pos) || hitCheck(dir, core, 3, -1, -2, pos)
                || hitCheck(dir, core, 3, 1, -2, pos) || hitCheck(dir, core, 3, 2, -2, pos)) {
            for (int i = 1; i <= 4; i++) {
                text.add(ChatFormatting.RED + "<- " + ChatFormatting.RESET + distill.tanks[i].getTankType().getDisplayNameString());
            }
        }
        if (text.isEmpty()) return null;
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /** 1.7 {@code hitCheck}. */
    static boolean hitCheck(ForgeDirection dir, BlockPos core, int exDir, int exRot, int exY, BlockPos hit) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return MachinePorts.offset(core, dir, exDir, rot, exRot, exY).equals(hit);
    }
}
