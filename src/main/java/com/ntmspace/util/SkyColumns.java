package com.ntmspace.util;

import dev.golem.core.world.LightLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** The vanilla skylight source boundary, including light engines that omit the cached sources. */
public final class SkyColumns {
    private SkyColumns() {}

    public static int lowestSourceY(ChunkAccess chunk, int x, int z) {
        var sources = chunk.getSkyLightSources();
        return sources != null ? sources.getLowestSourceY(x & 15, z & 15) : scan(chunk, x, z);
    }

    /** Walks the same two-face occlusion rule used by ChunkSkyLightSources. */
    public static int scan(ChunkAccess chunk, int x, int z) {
        var pos = new BlockPos.MutableBlockPos(x, 0, z);
        BlockState above = Blocks.AIR.defaultBlockState();
        for (var y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15); y >= chunk.getMinY(); y--) {
            var state = chunk.getBlockState(pos.setY(y));
            if (LightLevels.stopsSkylight(chunk, pos, above, state)) return y + 1;
            above = state;
        }
        return Integer.MIN_VALUE;
    }
}
