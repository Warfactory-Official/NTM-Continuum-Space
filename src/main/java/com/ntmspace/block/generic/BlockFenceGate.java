package com.ntmspace.block.generic;

import dev.golem.core.world.CompatDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * hbm-space's {@code BlockFenceGate} ({@code fence_gate}, "Chainlink Fence Gate"): a {@code BlockModDoor} - so a
 * two-block door placed by its own item, opened by hand or redstone, the lower half dropping the item - drawn and shaped
 * as a chain-link panel through the middle of the block instead of a door leaf on its edge. Iron, hardness 5,
 * resistance 1; open and close play {@code hbm:block.fenceGate} (here {@code ntmspace:block.fencegate}, volume 1, pitch
 * 0.9 to 1.0, as {@code BlockModDoor.getSFX} did for both directions).
 *
 * <p>1.7 bounds ({@code func_150011_b}, door direction {@code j} = modern facing EAST, SOUTH, WEST, NORTH; the hinge
 * bit = modern RIGHT hinge):
 * <ul>
 *   <li>closed: a 1/16-thick panel through the block's centre, across the doorway;</li>
 *   <li>open: the panel swung to the doorway's side, half of it out in the next block (only drawn so: see below);</li>
 *   <li>selection box and ray trace: always the closed panel (1.7 set {@code interacting} while picking, which made
 *       the bounds ignore the open flag);</li>
 *   <li>collision: the closed panel while closed, nothing while open ({@code getCollisionBoundingBoxFromPool} null).</li>
 * </ul>
 * The two drawn shapes are the authored models {@code ntmspace:block/fence_gate_*}.
 */
public class BlockFenceGate extends CompatDoorBlock {

    private static final VoxelShape ACROSS_X = Block.box(7.5, 0, 0, 8.5, 16, 16);
    private static final VoxelShape ACROSS_Z = Block.box(0, 0, 7.5, 16, 16, 8.5);

    public BlockFenceGate(BlockSetType type, Properties properties) {
        super(type, properties);
    }

    /** The closed panel: 1.7 {@code j} 0 and 2 (EAST, WEST) span z, 1 and 3 (SOUTH, NORTH) span x. */
    public static VoxelShape panel(BlockState state) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X ? ACROSS_X : ACROSS_Z;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return panel(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : panel(state);
    }
}
