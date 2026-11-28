package com.ntmspace.block.machine;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Connection geometry for the progression and station machines. {@link #portFaces} and {@link #corePortFaces} open the
 * cells of a 1.7 {@code getPorts} declaration; {@link #ports} and {@link #coreFaces} derive ports from the
 * {@code getConPos} points of the machines still on them.
 */
public final class MachinePorts {

    private MachinePorts() {}

    /** A structure: whether a position is one of its cells (core included). */
    @FunctionalInterface
    public interface Cells {
        boolean contains(BlockPos pos);
    }

    /** The cells of a box {@code dims} around {@code origin} turned to {@code facing}. */
    public static boolean inBox(BlockPos origin, int[] dims, Direction facing, BlockPos pos) {
        int[] rot = MultiblockHandlerXR.rotate(dims, facing);
        int dx = pos.getX() - origin.getX(), dy = pos.getY() - origin.getY(), dz = pos.getZ() - origin.getZ();
        return dx >= -rot[4] && dx <= rot[5] && dy >= -rot[1] && dy <= rot[0] && dz >= -rot[2] && dz <= rot[3];
    }

    /**
     * For each 1.7 connection point, the faces of the structure touching it, for {@code roles}. A point touching the core
     * itself is the core's business ({@link #coreFaces}).
     */
    public static void ports(BlockPos core, Direction facing, Cells cells, DirPos[] conPos, int roles,
                             BlockMultiblockCore.CellVisitor visitor) {
        for (DirPos point : conPos) {
            BlockPos at = point.getPos();
            for (Direction toCell : Direction.values()) {
                BlockPos cell = at.relative(toCell);
                if (cell.equals(core) || !cells.contains(cell)) continue;
                visitor.cell(cell, maskOf(MultiblockMaskTable.toLocal(toCell.getOpposite(), facing)), roles);
            }
        }
    }

    /**
     * The 1.7 points as ports: each cell of the structure next to a point, the core included, is a port aimed at it.
     * 1.7 subscribed at the point itself, so a point's direction does not always say which cell is behind it.
     */
    public static PortDef[] ports(BlockGetter level, BlockState coreState, BlockPos core, DirPos... points) {
        BlockMultiblockCore block = (BlockMultiblockCore) coreState.getBlock();
        Direction facing = coreState.getValue(BlockMultiblockCore.FACING);
        Map<BlockPos, List<ForgeDirection>> byCell = new LinkedHashMap<>();
        for (DirPos point : points) {
            for (Direction toCell : Direction.values()) {
                BlockPos cell = point.getPos().relative(toCell);
                if (!cell.equals(core) && !block.claimsCell(level, core, cell, facing)) continue;
                byCell.computeIfAbsent(cell, k -> new ArrayList<>()).add(ForgeDirection.getOrientation(toCell.getOpposite()));
            }
        }
        List<PortDef> ports = new ArrayList<>();
        byCell.forEach((cell, dirs) -> ports.add(PortDef.make(cell, dirs.toArray(new ForgeDirection[0]))));
        return ports.toArray(new PortDef[0]);
    }

    /** The local faces of the core that touch a connection point (for {@code coreMask}). */
    public static int coreFaces(BlockPos core, Direction facing, DirPos[] conPos) {
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

    /** Opens each cell of 1.7's {@code getPorts} towards its connections, for {@code roles}; the core is {@link #corePortFaces}. */
    public static void portFaces(BlockPos core, Direction facing, PortDef[] ports, int roles,
                                 BlockMultiblockCore.CellVisitor visitor) {
        for (DirPos con : PortDef.connections(ports)) {
            Direction out = con.getDir().toDirection();
            BlockPos cell = con.getPos().relative(out.getOpposite());
            if (!cell.equals(core)) visitor.cell(cell, maskOf(MultiblockMaskTable.toLocal(out, facing)), roles);
        }
    }

    /** The local faces of the core among the connections of {@code ports} (for {@code coreMask}). */
    public static int corePortFaces(BlockPos core, Direction facing, PortDef[] ports) {
        int mask = BlockMultiblockCore.MASK_NONE;
        for (DirPos con : PortDef.connections(ports)) {
            Direction out = con.getDir().toDirection();
            if (con.getPos().relative(out.getOpposite()).equals(core)) mask |= maskOf(MultiblockMaskTable.toLocal(out, facing));
        }
        return mask;
    }

    public static int maskOf(Direction local) {
        return switch (local) {
            case NORTH -> BlockMultiblockCore.MASK_NORTH;
            case SOUTH -> BlockMultiblockCore.MASK_SOUTH;
            case WEST -> BlockMultiblockCore.MASK_WEST;
            case EAST -> BlockMultiblockCore.MASK_EAST;
            case UP -> BlockMultiblockCore.MASK_UP;
            case DOWN -> BlockMultiblockCore.MASK_DOWN;
        };
    }

    /** 1.7 {@code ForgeDirection.getOrientation(meta - offset)} for a facing. */
    public static ForgeDirection dir(Direction facing) {
        return ForgeDirection.getOrientation(facing);
    }

    /** 1.7 {@code dir.getRotation(ForgeDirection.UP)}. */
    public static ForgeDirection rot(Direction facing) {
        return dir(facing).getRotation(ForgeDirection.UP);
    }

    /** {@code base + dir * along + rot * side + y}, 1.7's usual offset spelling. */
    public static BlockPos offset(BlockPos base, ForgeDirection dir, int along, ForgeDirection rot, int side, int y) {
        return base.offset(dir.offsetX * along + rot.offsetX * side, y, dir.offsetZ * along + rot.offsetZ * side);
    }

    /** Visits every cell of {@code dims} around {@code origin} (the origin too, unlike {@code visitBox}). */
    public static void eachInBox(BlockPos origin, int[] dims, Direction facing, Consumer<BlockPos> out) {
        int[] rot = MultiblockHandlerXR.rotate(dims, facing);
        for (int a = origin.getX() - rot[4]; a <= origin.getX() + rot[5]; a++)
            for (int b = origin.getY() - rot[1]; b <= origin.getY() + rot[0]; b++)
                for (int c = origin.getZ() - rot[2]; c <= origin.getZ() + rot[3]; c++) out.accept(new BlockPos(a, b, c));
    }
}
