package com.ntmspace.block.station;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.lib.DirPos;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Connection geometry shared by the station machines. 1.7 subscribed a machine to the networks at its
 * {@code getConPos} points, positions just outside the structure. {@link #ports} opens, for each 1.7 point, every face of
 * the structure adjacent to it, where the block entity's ports sit.
 */
final class StationMachines {

    private StationMachines() {}

    /** Whether {@code pos} is a cell of the box {@code dims} around {@code core} turned to {@code facing} (core included). */
    static boolean inBox(BlockPos core, int[] dims, Direction facing, BlockPos pos) {
        int[] rot = MultiblockHandlerXR.rotate(dims, facing);
        int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
        return dx >= -rot[4] && dx <= rot[5] && dy >= -rot[1] && dy <= rot[0] && dz >= -rot[2] && dz <= rot[3];
    }

    /**
     * For each 1.7 connection point, the faces of the structure touching it, for {@code roles}. A point touching the
     * core itself is the core's business ({@link #coreFaces}).
     */
    static void ports(BlockPos core, int[] dims, Direction facing, List<DirPos> conPos, int roles,
                      BlockMultiblockCore.CellVisitor visitor) {
        for (DirPos point : conPos) {
            BlockPos at = point.getPos();
            for (Direction toCell : Direction.values()) {
                BlockPos cell = at.relative(toCell);
                if (cell.equals(core) || !inBox(core, dims, facing, cell)) continue;
                visitor.cell(cell, maskOf(MultiblockMaskTable.toLocal(toCell.getOpposite(), facing)), roles);
            }
        }
    }

    /** The local faces of the core that touch a connection point (for {@code coreMask}). */
    static int coreFaces(BlockPos core, Direction facing, List<DirPos> conPos) {
        int mask = BlockMultiblockCore.MASK_NONE;
        for (DirPos point : conPos) {
            for (Direction toCell : Direction.values()) {
                if (point.getPos().relative(toCell).equals(core)) {
                    mask |= maskOf(MultiblockMaskTable.toLocal(toCell.getOpposite(), facing));
                }
            }
        }
        return mask;
    }

    static int maskOf(Direction local) {
        return switch (local) {
            case NORTH -> BlockMultiblockCore.MASK_NORTH;
            case SOUTH -> BlockMultiblockCore.MASK_SOUTH;
            case WEST -> BlockMultiblockCore.MASK_WEST;
            case EAST -> BlockMultiblockCore.MASK_EAST;
            case UP -> BlockMultiblockCore.MASK_UP;
            case DOWN -> BlockMultiblockCore.MASK_DOWN;
        };
    }
}
