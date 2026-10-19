package com.ntmspace.block.terrain;

import com.ntmspace.block.flora.TypedItems;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code laythe_coral} ("Laythe Coral", {@code BlockCoral}): five types of coral, wet, no collision, placed
 * only under a liquid; drops one calcium powder (its loot table).
 *
 * <p>1.7 drew it with {@code RenderCoral}: crossed planes (vanilla's cross, 0.45 wide) leaning away from the first solid
 * neighbour in the order north, south, west, east, up, down ({@code Material.isSolid}), upright when there is none. That
 * attachment is computed at render time in 1.7; here it is the state {@link #FACING} (the way the plant points, away
 * from its support), kept up to date as neighbours change, and the blockstate rotates the cross onto it. World
 * generation sets the plant before all its neighbours exist, so the decoration marks it for the chunk's post-processing,
 * which runs {@link #onUpdateShape} once the chunk is complete.
 */
public class BlockLaytheCoral extends BlockWaterPlant {

    public static final EnumProperty<PlantType.Coral> TYPE = EnumProperty.create("type", PlantType.Coral.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    /** 1.7 {@code RenderCoral.directions}. */
    private static final Direction[] ATTACHMENT_ORDER = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN};

    public BlockLaytheCoral(Properties properties) {
        super(properties, below -> true, false);
        registerDefaultState(stateDefinition.any().setValue(TYPE, PlantType.Coral.TUBE).setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, FACING);
    }

    /** 1.7 {@code RenderCoral.getAttachment}, as the direction the plant points (the opposite of its support). */
    public static Direction facing(BlockGetter level, BlockPos pos) {
        for (Direction dir : ATTACHMENT_ORDER) {
            if (level.getBlockState(pos.relative(dir)).isSolid()) return dir.getOpposite();
        }
        return Direction.UP;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!liquidAbove(context.getLevel(), context.getClickedPos())) return null;
        return TypedItems.placement(this, TYPE, context).setValue(FACING, facing(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        BlockState updated = super.onUpdateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState);
        if (!updated.is(this)) return updated;
        return updated.setValue(FACING, facing(level, pos));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return TypedItems.stack(state, TYPE);
    }
}
