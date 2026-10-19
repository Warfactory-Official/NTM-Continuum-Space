package com.ntmspace.block.flora;

import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.CompatEnchantments;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * hbm-space's {@code BlockRubberLeaves} ({@code rubber_leaves} "Rubber Leaves" and {@code pet_leaves} "Plastic
 * Leaves"), a 1.7 {@code BlockLeaves}: always the fancy see-through leaves, no biome tint.
 *
 * <p><b>Decay</b>, as 1.7: a leaf flagged for a decay check ({@link #CHECK_DECAY}, 1.7's metadata bit 8) runs 1.7's
 * search on its random tick ({@link #connected}: a log ({@code #minecraft:logs}) within four steps through leaves
 * ({@code #minecraft:leaves}) inside the 9-cube around it); connected, the flag clears, else the leaf decays and drops
 * its items. Generated and placed leaves start unflagged. 1.7 flagged leaves within one block of a broken leaf and
 * within four of a broken log; here a broken leaf flags its 26 neighbours ({@link #onBlockRemoved}), and a leaf flags
 * itself when a face neighbour becomes air (a log or leaf going away, also vanilla's), so a cut tree's far leaves are
 * reached through their decaying neighbours rather than all at once.
 *
 * <p><b>Drops</b>, 1.7's Forge {@code BlockLeaves.getDrops} plus hbm-space's {@code dropBlockAsItemWithChance} (also on
 * decay):
 * <ul>
 *   <li>one in 20 its leaf item (rubber or plastic leaves), one in {@code max(10, 20 - (2 << fortune))} with fortune;</li>
 *   <li>then 30 %: another leaf item, and if so 50 %: a sapling (rubber leaves the PVC sapling, plastic leaves the
 *       vinyl one).</li>
 * </ul>
 * Silk touch gives nothing more ({@code canSilkHarvest} false). Shears ({@code IShearable}) drop the leaves block and the
 * normal drops still roll, as Forge's {@code harvestBlock} did.
 */
public class BlockSpaceLeaves extends CompatBlock {

    /** 1.7's metadata bit 8: this leaf runs the decay search on its next random tick. */
    public static final BooleanProperty CHECK_DECAY = BooleanProperty.create("check_decay");
    /** 1.7 {@code BlockLeaves.updateTick}'s {@code b0}: how many leaf steps a log may be away. */
    public static final int RANGE = 4;

    private final Supplier<? extends Item> leaf;
    private final Supplier<? extends Item> sapling;

    public BlockSpaceLeaves(Properties properties, Supplier<? extends Item> leaf, Supplier<? extends Item> sapling) {
        super(properties);
        this.leaf = leaf;
        this.sapling = sapling;
        registerDefaultState(stateDefinition.any().setValue(CHECK_DECAY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHECK_DECAY);
    }

    /**
     * 1.7 {@code BlockLeaves.updateTick}'s search: logs are 0, leaves -2, anything else -1 in the 9-cube around
     * {@code pos}; four rounds give every leaf next to a cell of the previous round's number the round's number. The
     * leaf is connected when it got a number.
     */
    public static boolean connected(BlockGetter level, BlockPos pos) {
        int size = 2 * RANGE + 1;
        int[] field = new int[size * size * size];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RANGE; dx <= RANGE; ++dx) {
            for (int dy = -RANGE; dy <= RANGE; ++dy) {
                for (int dz = -RANGE; dz <= RANGE; ++dz) {
                    BlockState state = level.getBlockState(cursor.setWithOffset(pos, dx, dy, dz));
                    field[index(dx, dy, dz)] = state.is(BlockTags.LOGS) ? 0 : state.is(BlockTags.LEAVES) ? -2 : -1;
                }
            }
        }
        for (int round = 1; round <= RANGE; ++round) {
            for (int dx = -RANGE; dx <= RANGE; ++dx) {
                for (int dy = -RANGE; dy <= RANGE; ++dy) {
                    for (int dz = -RANGE; dz <= RANGE; ++dz) {
                        if (field[index(dx, dy, dz)] != round - 1) continue;
                        for (Direction dir : Direction.values()) {
                            int nx = dx + dir.getStepX(), ny = dy + dir.getStepY(), nz = dz + dir.getStepZ();
                            // 1.7 indexed one cell past the cube into its scratch array; nothing it found there is a leaf of this search
                            if (Math.abs(nx) > RANGE || Math.abs(ny) > RANGE || Math.abs(nz) > RANGE) continue;
                            if (field[index(nx, ny, nz)] == -2) field[index(nx, ny, nz)] = round;
                        }
                    }
                }
            }
        }
        return field[index(0, 0, 0)] >= 0;
    }

    private static int index(int dx, int dy, int dz) {
        int size = 2 * RANGE + 1;
        return ((dx + RANGE) * size + dy + RANGE) * size + dz + RANGE;
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        if (neighbourState.isAir() && !state.getValue(CHECK_DECAY)) return state.setValue(CHECK_DECAY, true);
        return state;
    }

    /** 1.7 {@code BlockLeaves.breakBlock}: flag every leaf in the 3-cube around a removed leaf. */
    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState other = level.getBlockState(p);
            if (other.getBlock() instanceof BlockSpaceLeaves && !other.getValue(CHECK_DECAY)) {
                level.setBlock(p, other.setValue(CHECK_DECAY, true), 4);
            }
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(CHECK_DECAY);
    }

    /** 1.7 {@code updateTick}: the search, then the flag cleared or {@code removeLeaves} (drop, then air). */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CHECK_DECAY)) return;
        if (!level.hasChunksAt(pos.offset(-RANGE - 1, -RANGE - 1, -RANGE - 1), pos.offset(RANGE + 1, RANGE + 1, RANGE + 1))) return;
        if (connected(level, pos)) {
            level.setBlock(pos, state.setValue(CHECK_DECAY, false), 4);
        } else {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return drops(CompatEnchantments.fortune(params), FloraDrops.random(params));
    }

    /** The drops of one leaf block broken or decayed with {@code fortune}. */
    public List<ItemStack> drops(int fortune, RandomSource random) {
        List<ItemStack> drops = new ArrayList<>(3);
        int chance = 20; // BlockLeaves.func_150123_b
        if (fortune > 0) {
            chance -= 2 << fortune;
            if (chance < 10) chance = 10;
        }
        if (random.nextInt(chance) == 0) drops.add(new ItemStack(leaf.get()));
        // (1.7 then rolled an apple chance, which only oak leaves answer)
        if (random.nextFloat() < 0.3F) {
            drops.add(new ItemStack(leaf.get()));
            if (random.nextFloat() < 0.5F) drops.add(new ItemStack(sapling.get()));
        }
        return drops;
    }

    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && FloraDrops.shearing(player)) popResource(level, pos, new ItemStack(this));
    }
}
