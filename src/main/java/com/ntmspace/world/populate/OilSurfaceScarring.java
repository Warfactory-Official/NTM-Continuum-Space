package com.ntmspace.world.populate;

import net.minecraft.util.Mth;
import com.ntmspace.world.LayeredChunkGenerator;
import com.ntmspace.world.RandomSourceRandom;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.MapGenBedrockOil17;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Bedrock oil's shared column dressing against a loaded world, with the caller's explicit surface targets. */
public final class OilSurfaceScarring {
    private OilSurfaceScarring() {}

    public static void addBedrockOilSpot(WorldGenLevel level, RandomSource random, int x, int z, int width, int count,
                                        List<OreConfiguration.TargetBlockState> scars) {
        var pos = new BlockPos.MutableBlockPos();
        //? if >=1.21.2 {
        int bottom = level.getMinY();
        //?} else {
        /*int bottom = level.getMinY();
        *///?}
        int top = bottom + level.getHeight() - 1;
        var surface = new MapGenBedrockOil17.Surface() {
            @Override public boolean fullCube(int x, int y, int z) {
                return PopulateWorld.normalCube(level.getBlockState(pos.set(x, y, z)));
            }
            @Override public boolean is(int x, int y, int z, Block17 block) {
                var state = level.getBlockState(pos.set(x, y, z));
                if (block == Block17.SAND && state.is(Blocks.RED_SAND)) return true;
                if (block == Block17.DIRT && (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT))) return true;
                if (block == Block17.STONE && (state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE))) return true;
                return state.is(LayeredChunkGenerator.state(block, (byte) 0).getBlock());
            }
            @Override public int meta(int x, int y, int z) { return level.getBlockState(pos.set(x, y, z)).is(Blocks.RED_SAND) ? 1 : 0; }
            @Override public void set(int x, int y, int z, Block17 block, int meta) {
                level.setBlock(pos.set(x, y, z), LayeredChunkGenerator.state(block, (byte) meta), 2);
            }
            @Override public void replace(int x, int y, int z, Block17 block) { set(x, y, z, block, 0); }
            @Override public boolean scar(int x, int y, int z) {
                var state = level.getBlockState(pos.set(x, y, z));
                for (var scar : scars) {
                    if (scar.target.test(state, random)) { level.setBlock(pos, scar.state, 2); return true; }
                }
                return false;
            }
        };
        var adapted = new RandomSourceRandom(random);
        for (int i = 0; i < count; i++) {
            int rx = x + Mth.clamp((int) (random.nextGaussian() * width), -15, 15);
            int rz = z + Mth.clamp((int) (random.nextGaussian() * width), -15, 15);
            MapGenBedrockOil17.scarColumn(surface, adapted, rx, rz, top, bottom, true);
        }
    }
}
