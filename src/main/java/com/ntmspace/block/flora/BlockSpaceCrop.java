package com.ntmspace.block.flora;

import com.hbm.blocks.generic.BlockTerrainPlant;
import com.ntmspace.registry.FloraBlocks;
import dev.golem.core.world.CompatBonemealableBlock;
import dev.golem.core.world.CompatEnchantments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * hbm-space's {@code BlockCrop} ({@code crop_strawberry}, {@code crop_mint}, {@code crop_coffee}, {@code crop_tea},
 * {@code crop_paraffin}): a bush with eight growth stages ({@link #AGE}, 1.7's metadata), drawn as crossed planes with
 * the stage textures {@code <crop>_1 _1 _2 _2 _3 _3 _4 _5}, a quarter block tall, no collision. Its seed item plants it
 * ({@link ItemSpaceSeedFood}). Ported as 1.7 wrote it:
 *
 * <ul>
 *   <li>every random tick grows it one stage, without any light or chance check; bonemeal grows two to five stages,
 *       until stage 7;</li>
 *   <li>it stays on grass, dirt, farmland or rubber farmland (1.7: the ground's {@code canSustainPlant} for a
 *       {@code Plains} plant, which rubber farmland always answers yes to); without, it drops and goes;</li>
 *   <li>drops: four of its product when ripe, else half its stage (rounded down); the tea gives tea leaves only when
 *       ripe and tea seeds before; ripe tea and ripe wax crops add {@code 3 + fortune} rolls of one in fifteen against
 *       the stage (8/15 each) for another seed (wax: a seed and a paraffin wax tar).</li>
 * </ul>
 * The 1.7 crop also implemented {@code IPlantableBreathing}: the atmosphere port's {@code PlantBreathing} holds the
 * crops' air rules by block id.
 */
public class BlockSpaceCrop extends BlockTerrainPlant implements CompatBonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final int MAX_AGE = 7;
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);

    public final CropKind kind;

    public BlockSpaceCrop(Properties properties, CropKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    /** 1.7 {@code canSustainPlant(..., Plains)}: grass, dirt (all three kinds) and farmland; rubber farmland sustains anything. */
    public static boolean isCropGround(BlockState below) {
        return below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT)
                || below.is(Blocks.PODZOL) || below.is(Blocks.FARMLAND) || below.is(FloraBlocks.RUBBER_FARMLAND.get());
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return isCropGround(level.getBlockState(pos.below()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** 1.7 {@code updateTick}: one stage per random tick. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = Math.min(state.getValue(AGE) + 1, MAX_AGE);
        level.setBlock(pos, state.setValue(AGE, age), 2);
    }

    @Override
    public boolean canBonemeal(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) != MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    /** 1.7 {@code incrementGrowStage}: {@code getRandomIntegerInRange(rand, 2, 5)} stages. */
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = Math.min(state.getValue(AGE) + 2 + random.nextInt(4), MAX_AGE);
        level.setBlock(pos, state.setValue(AGE, age), 2);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return drops(kind, state.getValue(AGE), CompatEnchantments.fortune(params), FloraDrops.random(params));
    }

    /** 1.7 {@code quantityDropped}, {@code getItemDropped} and {@code getDrops}. */
    public static List<ItemStack> drops(CropKind kind, int age, int fortune, RandomSource random) {
        List<ItemStack> drops = new ArrayList<>();
        int count = age == MAX_AGE ? 4 : age / 2;
        Item product = kind.product(age);
        for (int i = 0; i < count; i++) drops.add(new ItemStack(product));

        if (age >= MAX_AGE && (kind == CropKind.TEA || kind == CropKind.PARAFFIN)) {
            for (int i = 0; i < 3 + fortune; ++i) {
                if (random.nextInt(15) <= age) {
                    if (kind == CropKind.TEA) {
                        drops.add(new ItemStack(FloraBlocks.TEASEEDS.get()));
                    } else {
                        drops.add(new ItemStack(FloraBlocks.PARAFFIN_SEEDS.get()));
                        drops.add(com.hbm.items.ModOilItems.tar(com.hbm.items.ItemEnums.EnumTarType.WAX, 1));
                    }
                }
            }
        }
        return drops;
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(kind.seed());
    }
}
