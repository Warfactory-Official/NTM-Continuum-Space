package com.ntmspace.world.structure;

import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.generic.BlockPedestal;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.util.LootGenerator;
import com.ntmspace.registry.StructureBlocks;
import com.ntmspace.world.populate.PopulateWorld;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * The two small pieces of hbm-space's planet generators that write into the decorated chunks directly (no structure):
 * Duna's broken behemoth lantern and Ike's pedestal re-stocking. Both run at the point of their generator 1.7 ran them
 * ({@code BodyGenerators17}), on the level of the modern FEATURES step ({@code LevelPopulateWorld}).
 */
public final class PlanetFeatures17 {

    private PlanetFeatures17() {}

    /**
     * {@code WorldGeneratorDuna}, after its {@code rand.nextInt(1234) == 0} and the column draws:
     * <pre>
     * y = getHeightValue(x, z)
     * if block(x, y-1, z).canPlaceTorchOnTop and block(x, y, z).isReplaceable:
     *     lantern_behemoth, metadata 12, fillSpace {4, 0, 0, 0, 0, 0} NORTH; isBroken = true
     *     if rand.nextInt(2) == 0: LootGenerator.setBlock + lootBooklet at (x, y, z - 2)
     * </pre>
     * Metadata 12 is {@code BlockDummyable}'s core facing {@code ForgeDirection} 2 (north), as HBM's own overworld
     * lantern ({@code DungeonBlockFeature}) places it; 1.7's {@code LootGenerator.setBlock} is the {@code deco_loot} block.
     */
    public static void brokenLantern(WorldGenLevel level, PopulateWorld world, int x, int z, Random rand) {
        int y = world.heightValue(x, z);
        if (!world.exists(x, y, z) || !world.exists(x, y - 1, z) || !world.exists(x, y, z - 2)) return;
        placeLantern(level, new BlockPos(x, y, z), rand);
    }

    /** The lantern's tests and placement at {@code pos} (1.7's {@code (x, getHeightValue, z)}); true if it stands. */
    public static boolean placeLantern(WorldGenLevel level, BlockPos pos, Random rand) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (!Block.canSupportCenter(level, pos.below(), Direction.UP) || !level.getBlockState(pos).canBeReplaced()) return false;

        var block = ModDungeonBlocks.lantern_behemoth.get();
        level.setBlock(pos, block.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH), 3);
        BlockMultiblockCore.withoutTeardown(() -> block.fillSpace(level, pos, Direction.NORTH));

        if (level.getBlockEntity(pos) instanceof TileEntityLanternBehemoth lantern) {
            lantern.isBroken = true;
            lantern.setChanged();
        }

        if (rand.nextInt(2) == 0) {
            BlockPos loot = new BlockPos(x, y, z - 2);
            level.setBlock(loot, ModDungeonBlocks.deco_loot.get().defaultBlockState(), 3);
            LootGenerator.lootBooklet(level, loot);
        }
        return true;
    }

    /**
     * {@code WorldGeneratorIke} ("the HACKIEST hack i ever hacked"): every {@code pedestal} in the 16 x 16 columns of the
     * chunk at block {@code (i, j)} gets a divine shard. 1.7 walked every block of the chunk; the pedestals are the
     * chunk's block entities, which is the same set.
     */
    public static void restockPedestals(WorldGenLevel level, int i, int j) {
        ChunkAccess chunk = level.getChunk(i >> 4, j >> 4);
        for (BlockPos pos : java.util.List.copyOf(chunk.getBlockEntitiesPos())) {
            if (!chunk.getBlockState(pos).is(ModDungeonBlocks.pedestal.get())) continue;
            if (level.getBlockEntity(pos) instanceof BlockPedestal.TileEntityPedestal pedestal) {
                pedestal.setItem(new ItemStack(StructureBlocks.DIVINE_SHARD.get(), 1));
            }
        }
    }
}
