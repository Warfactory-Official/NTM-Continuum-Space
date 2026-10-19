package com.ntmspace.world;

import com.ntmspace.registry.LaytheTektoBlocks;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * hbm-space's {@code TTree} as a sapling grows it ({@code BlockNTSapling.func_149878_d}), the same generator Tekto's
 * decoration uses ({@code LaytheTektoDecoration.tree}), on the live level.
 */
public final class TektoTrees {

    private TektoTrees() {}

    /**
     * Grows the sapling's tree at {@code pos} (the sapling already removed): vinyl {@code TTree(false, 2, 4, 5, 3, 2,
     * false, vinyl_log, pet_leaves)}, PVC {@code TTree(true, 2, 5, 7, 4, 3, false, pvc_log, rubber_leaves)}. False when
     * the tree cannot stand there (not on rubber grass or silt, or out of height).
     */
    public static boolean sapling(ServerLevel level, BlockPos pos, boolean pvc, RandomSource random) {
        // 1.7 handed the generator world.rand, a java.util.Random
        Random rand = new Random(random.nextLong());
        World17 world = new World17(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (pvc) {
            return LaytheTektoDecoration.tree(world, rand, pos.getX(), pos.getY(), pos.getZ(), 2, 5, 7, 4, 3, false,
                    LaytheTektoBlocks.PVC_LOG.get(), LaytheTektoBlocks.RUBBER_LEAVES.get());
        }
        return LaytheTektoDecoration.tree(world, rand, pos.getX(), pos.getY(), pos.getZ(), 2, 4, 5, 3, 2, false,
                LaytheTektoBlocks.VINYL_LOG.get(), LaytheTektoBlocks.PET_LEAVES.get());
    }
}
