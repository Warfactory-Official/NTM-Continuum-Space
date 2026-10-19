package com.ntmspace.block.flora;

import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * hbm-space's {@code rubber_farmland} ("Tekto Farmland", {@code BlockRubberFarm}, a {@code BlockFarmland}): rubber silt
 * tilled with a hoe, fifteen sixteenths tall, where any plant may grow. Its top is dry or moist ({@link #MOISTURE},
 * 1.7's metadata 0 or 1), its sides are rubber silt; it drops rubber silt.
 *
 * <ul>
 *   <li>Random tick: moist when carbon tetrachloride ({@code ccl_block}) lies within four blocks across and one up, or
 *       rain falls on it, dry otherwise. Vanilla water does not moisten it, and unlike vanilla farmland it never dries
 *       back into silt (hbm-space's {@code updateTick} replaced {@code BlockFarmland}'s whole).</li>
 *   <li>A solid block above turns it into rubber silt ({@code onNeighborBlockChange}).</li>
 *   <li>Something landing on it from {@code d} blocks turns it into rubber silt with probability {@code d - 0.5}
 *       (a mob only with {@code mobGriefing}), as 1.7's {@code onFallenUpon}; the faller still takes its fall damage.</li>
 * </ul>
 */
public class BlockRubberFarmland extends CompatBlock {

    public static final IntegerProperty MOISTURE = IntegerProperty.create("moisture", 0, 1);
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

    private final Supplier<? extends Block> silt;
    private final Supplier<? extends Block> ccl;

    public BlockRubberFarmland(Properties properties, Supplier<? extends Block> silt, Supplier<? extends Block> ccl) {
        super(properties);
        this.silt = silt;
        this.ccl = ccl;
        registerDefaultState(stateDefinition.any().setValue(MOISTURE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOISTURE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** hbm-space's {@code func_149821_m}: carbon tetrachloride in x and z within four, y and one above. */
    public boolean nearCcl(LevelReader level, BlockPos pos) {
        Block fluid = ccl.get();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(p).is(fluid)) return true;
        }
        return false;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int moisture = !nearCcl(level, pos) && !level.isRainingAt(pos.above()) ? 0 : 1;
        level.setBlock(pos, state.setValue(MOISTURE, moisture), 2);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        // 1.7: getMaterial().isSolid() of the block above
        if (direction == Direction.UP && neighbourState.isSolid()) return silt.get().defaultBlockState();
        return state;
    }

    @Override
    protected void onFallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (level instanceof ServerLevel server && server.getRandom().nextFloat() < fallDistance - 0.5D
                && (entity instanceof Player || Compat.mobGriefing(server))) {
            level.setBlock(pos, silt.get().defaultBlockState(), 3);
        }
        defaultFallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }
}
