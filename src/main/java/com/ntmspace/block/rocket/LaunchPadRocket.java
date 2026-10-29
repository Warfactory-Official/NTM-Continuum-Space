package com.ntmspace.block.rocket;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModFuelItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.RocketBlocks;
import com.ntmspace.tileentity.rocket.TileEntityLaunchPadRocket;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code LaunchPadRocket}: a 13 x 13 x 3 pad (1.7 {@code getDimensions {2, 0, 7, 6, 6, 6}}, offset 6) whose
 * {@code fillSpace} is the body and two side wings, with 13 input cells along the back (four columns three high and one
 * more on the right) that take power, fluids and items (1.7 {@code TileEntityProxyCombo().inventory().power().fluid()}),
 * connected from behind. The service tower and the crew platform its block entity raises for a tall rocket are further
 * cells of the multiblock. Unusable in orbit; the look overlay lists the fuels a loaded rocket still needs.
 */
public class LaunchPadRocket extends BlockMultiblockMachine implements ILookOverlay, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public static final BooleanProperty LADDER = BooleanProperty.create("ladder");
    private static final AABB[] LADDER_BOXES = new AABB[4];
    private static final int[][] TOWER = {{2, -4}, {2, -5}, {2, -6}, {3, -4}, {4, -4}, {3, -6}, {4, -6}};
    private static final int[][] WALKWAY = walkway();

    private static int[][] walkway() {
        var cells = new int[19][2];
        cells[0] = new int[]{2, -3};
        int next = 1;
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z >= -6; z--) cells[next++] = new int[]{-x, z};
        return cells;
    }

    static {
        for (var facing : Direction.Plane.HORIZONTAL) {
            var rot = ForgeDirection.getOrientation(facing).getRotation(ForgeDirection.UP);
            LADDER_BOXES[facing.get2DDataValue()] = new AABB(0.25, 0, 0.25, 0.75, 1, 0.75)
                    .move(-rot.offsetX * 0.5, 0, -rot.offsetZ * 0.5);
        }
    }

    public LaunchPadRocket(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LADDER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LADDER);
    }

    @Override
    protected BlockState cellStateFor(int localX, int localY, int localZ, Direction facing, int shapeId) {
        return super.cellStateFor(localX, localY, localZ, facing, shapeId)
                    .setValue(LADDER, localZ == -5 && localX == (localY >= 3 ? 2 : 6));
    }

    public @Nullable AABB climbBox(BlockState state) {
        return state.getValue(LADDER) ? LADDER_BOXES[state.getValue(FACING).get2DDataValue()] : null;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchPadRocket(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 7, 6, 6, 6};
    }

    @Override
    public int getOffset() {
        return 6;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);

        // Main body
        MultiblockHandlerXR.visitBox(core, new int[]{2, 0, 6, 6, 4, 4}, facing, visitor);

        MultiblockHandlerXR.visitBox(core.offset(-dir.offsetX * 2, 0, -dir.offsetZ * 2), new int[]{2, 0, 4, 0, 6, 6},
                facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(dir.offsetX * 2, 0, dir.offsetZ * 2), new int[]{2, 0, 0, 4, 6, 6},
                facing, visitor);

        // Inputs, open on their back face
        int mask = maskOf(MultiblockMaskTable.toLocal(facing.getOpposite(), facing));
        for (BlockPos input : inputs(core, facing)) {
            visitor.cell(input, mask, ROLE_POWER_IN | ROLE_FLUID_IN);
        }
    }

    @Override
    public void visitPlacedCells(BlockGetter level, BlockPos core, Direction facing, CellVisitor visitor) {
        visitCells(core, facing, visitor);
        if (level.getBlockEntity(core) instanceof TileEntityLaunchPadRocket pad)
            visitTower(core, facing, pad.towerHeight(), visitor);
    }

    @Override
    public void visitPlacementCells(BlockPos core, Direction facing, ItemStack stack, CellVisitor visitor) {
        visitCells(core, facing, visitor);
        var data = ItemStacks.blockEntityData(stack, RocketBlocks.LAUNCH_PAD_ROCKET_BE.get());
        if (data != null) visitTower(core, facing, Tags.getIntOr(data, "tower", 0), visitor);
    }

    public static void visitTower(BlockPos core, Direction facing, int height, CellVisitor visitor) {
        if (height <= 8) return;
        var pos = new BlockPos.MutableBlockPos();
        for (int y = 3; y < height + 3; y++)
            for (int[] column : TOWER) visitTowerCell(core, facing, column, y, pos, visitor);
        for (int[] cell : WALKWAY) visitTowerCell(core, facing, cell, height + 2, pos, visitor);
    }

    private static void visitTowerCell(BlockPos core, Direction facing, int[] cell, int y,
                                       BlockPos.MutableBlockPos pos, CellVisitor visitor) {
        visitor.cell(pos.setWithOffset(core, MultiblockMaskTable.worldX(cell[0], y, cell[1], facing), y,
                MultiblockMaskTable.worldZ(cell[0], y, cell[1], facing)), MASK_NONE);
    }

    private static boolean contains(int[][] cells, int x, int z) {
        for (int[] cell : cells) if (cell[0] == x && cell[1] == z) return true;
        return false;
    }

    @Override
    public boolean claimsCell(BlockGetter level, BlockPos core, BlockPos cell, Direction facing) {
        if (super.claimsCell(level, core, cell, facing)) return true;
        if (!(level.getBlockEntity(core) instanceof TileEntityLaunchPadRocket pad)) return false;
        int y = cell.getY() - core.getY(), height = pad.towerHeight();
        if (height <= 8 || y < 3 || y > height + 2) return false;
        int dx = cell.getX() - core.getX(), dz = cell.getZ() - core.getZ();
        int x = MultiblockMaskTable.localX(dx, y, dz, facing), z = MultiblockMaskTable.localZ(dx, y, dz, facing);
        return contains(TOWER, x, z) || y == height + 2 && contains(WALKWAY, x, z);
    }

    @Override
    public @Nullable BlockPos findClientCore(BlockGetter level, BlockPos cell) {
        var facing = level.getBlockState(cell).getValue(FACING);
        var ladder = new BlockPos.MutableBlockPos();
        for (int[][] layer : new int[][][]{TOWER, WALKWAY})
            for (int[] xz : layer) {
                int x = 2 - xz[0], z = -5 - xz[1];
                ladder.setWithOffset(cell, MultiblockMaskTable.worldX(x, 0, z, facing), 0,
                        MultiblockMaskTable.worldZ(x, 0, z, facing));
                if (!isLadder(level, ladder, cell)) continue;
                int foot = ladder.getY();
                while (isLadder(level, ladder.setY(foot - 1), cell)) foot--;
                var core = new BlockPos(ladder.getX() - MultiblockMaskTable.worldX(2, 0, -5, facing), foot - 3,
                        ladder.getZ() - MultiblockMaskTable.worldZ(2, 0, -5, facing));
                if (MultiblockSurface.coreClaims(level, core, cell)) return core;
            }
        return super.findClientCore(level, cell);
    }

    private boolean isLadder(BlockGetter level, BlockPos pos, BlockPos from) {
        if (level instanceof Level world) {
            if (world.isOutsideBuildHeight(pos) || !world.hasChunkAt(pos)) return false;
        } else if (Math.max(Math.abs(pos.getX() - from.getX()), Math.max(Math.abs(pos.getY() - from.getY()),
                Math.abs(pos.getZ() - from.getZ()))) > MultiblockSurface.REGION_REACH) return false;
        var state = level.getBlockState(pos);
        return state.is(this) && state.getValue(LADDER);
    }

    /**
     * 1.7's 12 + 1 input cells (and the cells its {@code getConPos} reached from behind them).
     */
    public static List<BlockPos> inputs(BlockPos core, Direction facing) {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        List<BlockPos> list = new ArrayList<>();
        for (int or = 1; or < 5; or++) {
            for (int oy = 0; oy < 3; oy++) {
                list.add(core.offset(-rot.offsetX * or - dir.offsetX * 7, oy, -rot.offsetZ * or - dir.offsetZ * 7));
            }
        }
        list.add(core.offset(rot.offsetX * 3 - dir.offsetX * 7, 0, rot.offsetZ * 3 - dir.offsetZ * 7));
        return list;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (BlockPos input : inputs(core, facing)) visitor.passiveCell(input, MASK_ALL, domains);
    }

    @Override
    protected void visitTeardownCells(ServerLevel level, BlockPos core, Direction facing, CellVisitor visitor) {
        visitCells(core, facing, visitor);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 3; dy < 260; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    visitor.cell(pos.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz), MASK_NONE);
                }
            }
        }
    }

    static int maskOf(Direction local) {
        return switch (local) {
            case NORTH -> MASK_NORTH;
            case SOUTH -> MASK_SOUTH;
            case WEST -> MASK_WEST;
            case EAST -> MASK_EAST;
            case UP -> MASK_UP;
            case DOWN -> MASK_DOWN;
        };
    }

    /**
     * A tower or platform cell at {@code at}, owned by the pad at {@code core}.
     */
    public void placeDynamicCell(Level level, BlockPos core, BlockPos at) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = at.immutable();
        Direction facing = level.getBlockState(core).getValue(FACING);
        int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
        var state = cellStateFor(MultiblockMaskTable.localX(dx, dy, dz, facing), dy,
                MultiblockMaskTable.localZ(dx, dy, dz, facing), facing, 0);
        withoutTeardown(() -> level.setBlock(pos, state, 3));
        if (isCellOf(level.getBlockState(pos))) MultiblockCoreIndex.put(server, pos, core);
    }

    public void removeDynamicCell(Level level, BlockPos core, BlockPos at) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = at.immutable();
        if (!isCellOf(level.getBlockState(pos))) return;
        long owner = MultiblockCoreIndex.lookup(server, pos);
        if (owner != Long.MIN_VALUE && owner != core.asLong()) return;
        withoutTeardown(() -> level.removeBlock(pos, false));
        MultiblockCoreIndex.remove(server, pos);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player,
                                          BlockHitResult hit) {
        if (Orbit.inOrbit(level)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.useAtCore(state, level, core, player, hit);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        String title = getName().getString();
        if (Orbit.inOrbit(level)) {
            String text = "&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.yesOrbit").getString() + " ! ! !";
            return new ILookOverlay.Data(title, 0xffff00, 0x404000, List.of(text));
        }

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return null;
        if (!(level.getBlockEntity(core) instanceof TileEntityLaunchPadRocket pad)) return null;
        if (pad.rocket == null) return null;

        if (pos.getY() - core.getY() > 2) return null; // Don't show tooltip on support tower

        List<String> text = new ArrayList<>();
        text.add("Required fuels:");

        for (FluidTankNTM tank : pad.tanks) {
            if (tank.getTankType() == Fluids.NONE) continue;
            text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tank.getTankType()
                                                                               .getDisplayNameString() + ": "
                    + tank.getFill() + "/" + tank.getMaxFill() + "mB");
        }

        if (pad.solidFuel.max > 0) {
            text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET
                    + ModFuelItems.rocket_fuel.get().getName(ModFuelItems.rocket_fuel.get().getDefaultInstance())
                                              .getString()
                    + ": " + pad.solidFuel.level + "/" + pad.solidFuel.max + "kg");
        }

        if (text.size() <= 1) return null;

        return new ILookOverlay.Data(title, 0xffff00, 0x404000, text);
    }
}
