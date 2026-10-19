package com.ntmspace.block.transporter;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.item.ItemTransporterLinker;
import com.ntmspace.tileentity.transporter.TileEntityTransporterRocket;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code BlockTransporterRocket}: a {@code BlockDummyable} with dimensions {@code {1, 0, 1, 1, 1, 2}} and
 * offset 1, filled as three boxes (a 4 x 3 floor, and the two rows across it at the core and two blocks to the side one
 * block high) with six fluid proxies ({@code makeExtra}): the four floor corners next to the core and the two cells by
 * the fuel connections. Use opens the pad's screen unless the player holds the linker, whose own use then runs.
 *
 * <p>Fluid faces: 1.7 subscribed the cargo tanks at {@code getConPos} and the fuel tanks at {@code getTankPos}, both
 * points just outside the structure; here the structure faces next to those points open for ducts, and the pad
 * ({@link com.hbm.api.fluidmk2.IFluidMultiUserMK2}) hands a duct at a fuel point its separate fuel receiver, as 1.7's
 * second subscription did, so cargo and fuel never share a delivery.
 */
public class BlockTransporterRocket extends BlockMultiblockMachine implements IFluidConnectorBlockMK2 {

    private static final int[] DIMS = {1, 0, 1, 1, 1, 2};
    /** 1.7 {@code fillSpace}: the three {@code MultiblockHandlerXR.fillSpace} boxes. */
    private static final int[][] BOXES = {
            {0, 0, 1, 1, 1, 2},
            {1, 0, 1, 1, 0, 0},
            {1, 0, 1, 1, -2, 2},
    };

    public BlockTransporterRocket(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityTransporterRocket(pos, state);
    }

    @Override public int[] getDimensions() { return DIMS.clone(); }
    @Override public int getOffset() { return 1; }

    /** 1.7: any click opens the screen, except with the linker in hand (then the linker's use runs). */
    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.getMainHandItem().getItem() instanceof ItemTransporterLinker) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.useAtCore(state, level, core, player, hit);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int[] box : BOXES) MultiblockHandlerXR.visitBox(core, box, facing, visitor);
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        ports(core, facing, List.of(TileEntityTransporterRocket.conPos(core, dir)), ROLE_FLUID, visitor);
        ports(core, facing, List.of(TileEntityTransporterRocket.tankPos(core, dir)), ROLE_FLUID_IN, visitor);
    }

    /** 1.7 {@code makeExtra}: {@code TileEntityProxyCombo().fluid()} at the four floor corners and by the fuel points. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int domains = PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(1, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(1, 0, -1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-1, 0, 1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-1, 0, -1), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dir.offsetX - rot.offsetX * 2, 0, dir.offsetZ - rot.offsetZ * 2), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dir.offsetX - rot.offsetX * 2, 0, -dir.offsetZ - rot.offsetZ * 2), MASK_ALL, domains);
    }

    private static boolean inStructure(BlockPos core, Direction facing, BlockPos pos) {
        for (int[] box : BOXES) {
            int[] rot = MultiblockHandlerXR.rotate(box, facing);
            int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
            if (dx >= -rot[4] && dx <= rot[5] && dy >= -rot[1] && dy <= rot[0] && dz >= -rot[2] && dz <= rot[3]) return true;
        }
        return false;
    }

    /**
     * For each 1.7 connection point, the faces of the structure that touch it, open for {@code roles}. The mask table
     * keeps the last mask written per cell and role, so the faces of one cell (a floor corner touches a side point and
     * the point above it) are gathered first and written once.
     */
    private static void ports(BlockPos core, Direction facing, List<DirPos> points, int roles, CellVisitor visitor) {
        java.util.Map<BlockPos, Integer> masks = new java.util.LinkedHashMap<>();
        for (DirPos point : points) {
            BlockPos at = point.getPos();
            for (Direction toCell : Direction.values()) {
                BlockPos cell = at.relative(toCell);
                if (cell.equals(core) || !inStructure(core, facing, cell)) continue;
                masks.merge(cell, maskOf(MultiblockMaskTable.toLocal(toCell.getOpposite(), facing)), (x, y) -> x | y);
            }
        }
        masks.forEach((cell, mask) -> visitor.cell(cell, mask, roles));
    }

    private static int maskOf(Direction local) {
        return switch (local) {
            case NORTH -> MASK_NORTH;
            case SOUTH -> MASK_SOUTH;
            case WEST -> MASK_WEST;
            case EAST -> MASK_EAST;
            case UP -> MASK_UP;
            case DOWN -> MASK_DOWN;
        };
    }
}
