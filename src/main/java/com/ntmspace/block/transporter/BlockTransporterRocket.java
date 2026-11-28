package com.ntmspace.block.transporter;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.item.ItemTransporterLinker;
import com.ntmspace.tileentity.transporter.TileEntityTransporterRocket;
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
 * <p>Fluid faces: the cargo tanks sit on 1.7's {@code getFluidPorts} and the fuel tanks on {@code getFuelPorts}; the
 * pad ({@link com.hbm.api.fluidmk2.IFluidMultiUserMK2}) hands a duct on a fuel port its separate fuel receiver, so
 * cargo and fuel never share a delivery.
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
        MachinePorts.portFaces(core, facing, TileEntityTransporterRocket.fluidPorts(core, dir), ROLE_FLUID, visitor);
        MachinePorts.portFaces(core, facing, TileEntityTransporterRocket.fuelPorts(core, dir), ROLE_FLUID_IN, visitor);
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
}
