package com.ntmspace.block.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.tileentity.machine.TileEntityMachineStardar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineStardar}: offset 2, three blocks up; {@code getDimensions} {0, 3, 2, 2, 2, 2} is only the
 * space check. The structure is 1.7's {@code fillSpace}: the 5 x 5 dish platform at the core's level, eight legs
 * reaching three blocks down from it (every cell of the 3 x 3 grid at 2-block spacing but the middle), and a two-block
 * "rack" beside the front leg. Opens the GUI on use (1.7 {@code standardOpenBehavior}).
 */
public class MachineStardar extends BlockMultiblockMachine {

    static final int[] BODY = {0, 0, 2, 2, 2, 2};
    static final int[] LEG = {0, 3, 0, 0, 0, 0};

    public MachineStardar(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineStardar(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {0, 3, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }
    @Override public int getHeightOffset() { return 3; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        // Main body
        MultiblockHandlerXR.visitBox(core, BODY, facing, visitor);

        // Legs (world axes in 1.7; the grid is symmetric, so the facing does not matter). Each leg's origin is a
        // platform cell, which visitBox leaves out.
        for (int ox = -2; ox <= 2; ox += 2) {
            for (int oz = -2; oz <= 2; oz += 2) {
                if (ox == 0 && oz == 0) continue;
                MultiblockHandlerXR.visitBox(core.offset(ox, 0, oz), LEG, facing, visitor);
            }
        }

        // Rack: 1.7 set these two by hand (safeRem) as dummies pointing along the rotation back to the front leg
        for (BlockPos rack : rack(core, facing)) visitor.cell(rack, MASK_NONE);
    }

    /** The two rack cells: {@code core - rot + dir * 2}, two and three blocks down. */
    static BlockPos[] rack(BlockPos core, Direction facing) {
        ForgeDirection dir = MachinePorts.dir(facing), rot = MachinePorts.rot(facing);
        BlockPos top = core.offset(-rot.offsetX + dir.offsetX * 2, -2, -rot.offsetZ + dir.offsetZ * 2);
        return new BlockPos[] {top, top.below()};
    }

    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
                BODY,
                {0, 3, 0, 0, 0, 0, -2, 0, -2}, {0, 3, 0, 0, 0, 0, -2, 0, 0}, {0, 3, 0, 0, 0, 0, -2, 0, 2},
                {0, 3, 0, 0, 0, 0, 0, 0, -2}, {0, 3, 0, 0, 0, 0, 0, 0, 2},
                {0, 3, 0, 0, 0, 0, 2, 0, -2}, {0, 3, 0, 0, 0, 0, 2, 0, 0}, {0, 3, 0, 0, 0, 0, 2, 0, 2},
        };
    }
}
