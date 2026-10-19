package com.ntmspace.block.flora;

import com.hbm.blocks.generic.BlockTerrainPlant;
import com.ntmspace.block.terrain.PlantType;
import com.ntmspace.registry.FloraBlocks;
import com.ntmspace.registry.LaytheTektoBlocks;
import com.ntmspace.world.TektoTrees;
import dev.golem.core.world.CompatBonemealableBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code sapling} ({@code BlockNTSapling}, a 1.7 {@code BlockSapling}): the vinyl sapling ("Vinyl Sapling")
 * and the PVC one ("Plastic Sapling"), one block item each ({@link TypedItems}); they drop themselves.
 *
 * <ul>
 *   <li>Growth: on a random tick with light 9 or more above, one in seven; bonemeal succeeds 45 % of the time and grows
 *       the tree at once (hbm-space skipped vanilla's two-stage sapling). The tree is Tekto's {@code TTree}
 *       ({@link TektoTrees}): vinyl {@code TTree(2, 4, 5, 3, 2)} of vinyl log and plastic leaves, PVC
 *       {@code TTree(2, 5, 7, 4, 3)} of PVC log and rubber leaves. It needs rubber grass or rubber silt under the
 *       sapling, else the sapling is put back.</li>
 *   <li>Placing: on rubber silt, rubber grass or rubber farmland, or on any ground sustaining a plains plant (grass, dirt,
 *       farmland) ({@code canPlaceBlockOnSide}).</li>
 *   <li><b>Ported oddity</b> ({@code canBlockStay}): it stays only with light 8 or sky and on ground that
 *       {@code canSustainPlant}s it, which rubber silt and rubber grass do not: a sapling on them breaks (dropping
 *       itself) as soon as a neighbour changes, though it grows there if nothing does. On rubber farmland it stays but can
 *       never grow.</li>
 * </ul>
 * The 1.7 sapling also implemented {@code IPlantableBreathing} (Tekto air or chlorine): the atmosphere port's
 * {@code PlantBreathing} holds that rule by block id.
 */
public class BlockSpaceSapling extends BlockTerrainPlant implements CompatBonemealableBlock {

    public static final EnumProperty<PlantType.Sapling> TYPE = EnumProperty.create("type", PlantType.Sapling.class);

    public BlockSpaceSapling(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, PlantType.Sapling.VINYL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    /** 1.7 {@code canSustainPlant(..., Plains)} for a sapling: grass, dirt and farmland; rubber farmland sustains anything. */
    private static boolean sustains(BlockState soil) {
        return soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.COARSE_DIRT) || soil.is(Blocks.PODZOL)
                || soil.is(Blocks.FARMLAND) || soil.is(FloraBlocks.RUBBER_FARMLAND.get());
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return (level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos)) && sustains(level.getBlockState(pos.below()));
    }

    /** 1.7 {@code canPlaceBlockOnSide}. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState soil = context.getLevel().getBlockState(context.getClickedPos().below());
        if (!soil.is(LaytheTektoBlocks.RUBBER_GRASS.get()) && !soil.is(LaytheTektoBlocks.RUBBER_SILT.get()) && !sustains(soil)) {
            return null;
        }
        return TypedItems.placement(this, TYPE, context);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) grow(level, pos, state, random);
    }

    @Override
    public boolean canBonemeal(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    /** 1.7 {@code BlockSapling.func_149852_a}. */
    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.45F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, pos, state, random);
    }

    /** 1.7 {@code func_149878_d}: air in the sapling's place, the tree, or the sapling back. */
    public static void grow(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (!TektoTrees.sapling(level, pos, state.getValue(TYPE) == PlantType.Sapling.PVC, random)) {
            level.setBlock(pos, state, 2);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(TypedItems.stack(state, TYPE));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return TypedItems.stack(state, TYPE);
    }
}
