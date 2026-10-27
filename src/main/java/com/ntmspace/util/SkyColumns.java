package com.ntmspace.util;

import dev.golem.core.world.LightLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;

/** The vanilla skylight source boundary, including light engines that omit the cached sources. */
public final class SkyColumns {
    private SkyColumns() {}

    public static int lowestSourceY(ChunkAccess chunk, int x, int z) {
        var sources = chunk.getSkyLightSources();
        return sources != null ? sources.getLowestSourceY(x & 15, z & 15) : scan(chunk, x, z);
    }

    /** Walks the same two-face occlusion rule used by ChunkSkyLightSources. */
    public static int scan(ChunkAccess chunk, int x, int z) {
        var localX = x & 15;
        var localZ = z & 15;
        var pos = new BlockPos.MutableBlockPos(x, 0, z);
        //? if <1.21.2 {
        /*var abovePos = new BlockPos.MutableBlockPos(x, 0, z);
        *///?}
        BlockState above = Blocks.AIR.defaultBlockState();
        for (var y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ); y >= chunk.getMinY(); y--) {
            var state = chunk.getBlockState(pos.setY(y));
            //? if >=1.21.2 {
            var blocked = LightLevels.opacity(state) != 0 || Shapes.faceShapeOccludes(
                    LightEngine.getOcclusionShape(above, Direction.DOWN),
                    LightEngine.getOcclusionShape(state, Direction.UP));
            //?} else {
            /*var blocked = LightLevels.opacity(chunk, pos) != 0 || Shapes.faceShapeOccludes(
                    LightEngine.getOcclusionShape(chunk, abovePos.setY(y + 1), above, Direction.DOWN),
                    LightEngine.getOcclusionShape(chunk, pos, state, Direction.UP));
            *///?}
            if (blocked) return y + 1;
            above = state;
        }
        return Integer.MIN_VALUE;
    }
}
