package com.ntmspace.world;

import com.ntmspace.NtmSpace;
import com.ntmspace.world.gen.legacy.Block17;
import com.ntmspace.world.gen.legacy.CelestialTerrain17;
import com.ntmspace.world.gen.legacy.LayeredBodyTerrain17;
import dev.golem.core.world.CompatChunkGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * The chunk generator of a celestial body with a 1.7 biome map (Eve, Moho, Minmus): the body's 1.7 terrain
 * ({@link LayeredBodyTerrain17}) copied block for block into each chunk at the NOISE step, as {@link MunChunkGenerator}
 * does for the Mun. Heights are 1.7's, 0 to 255 ({@code min_y} 0, {@code height} 256).
 *
 * <p>1.7 generated one chunk at a time on the server thread and the ported generators keep state between chunks, so
 * generation is serialised on the terrain instance; a small cache of finished primers answers height queries.
 *
 * <p>The terrain is given a session {@code Random}: it stands in for 1.7's unseeded {@code world.rand} (and
 * {@code Math.random()}), which made parts of these bodies (Eve's cracks, Moho's plateaus) differ from one game session
 * to the next. GameTests pin it with {@link #useSessionSeed}.
 */
public abstract class LayeredChunkGenerator extends CompatChunkGenerator {

    private LayeredBodyTerrain17 terrain;
    private Long sessionSeed;
    private final Map<Long, CelestialTerrain17.Primer> primers = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CelestialTerrain17.Primer> eldest) {
            return size() > 32;
        }
    };

    protected LayeredChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /** The body's 1.7 terrain for {@code seed}, given the session {@code Random} (see the class comment). */
    protected abstract LayeredBodyTerrain17 createTerrain(long seed, Random session);

    /** Seeds the session {@code Random} (tests); call before the first chunk. Returns this generator. */
    public synchronized LayeredChunkGenerator useSessionSeed(long seed) {
        if (terrain != null) throw new IllegalStateException("the terrain already exists");
        this.sessionSeed = seed;
        return this;
    }

    /** The body's terrain (created on first use). */
    public synchronized LayeredBodyTerrain17 terrain() {
        if (terrain == null) terrain = createTerrain(seed(), sessionSeed == null ? new Random() : new Random(sessionSeed));
        return terrain;
    }

    /** The 1.7 primer for a chunk, generated once and cached (GameTests and the check command compare against it). */
    public synchronized CelestialTerrain17.Primer primer(int chunkX, int chunkZ) {
        long key = (long) chunkX << 32 ^ (chunkZ & 0xFFFFFFFFL);
        CelestialTerrain17.Primer primer = primers.get(key);
        if (primer == null) {
            primer = terrain().provide(chunkX, chunkZ);
            primers.put(key, primer);
        }
        return primer;
    }

    /** The block a primer entry becomes (null for air); a block not ported yet becomes its stand-in. */
    public static BlockState state(Block17 block, byte meta) {
        if (block == null) return Blocks.AIR.defaultBlockState();
        String standIn = STAND_INS.get(block);
        if (standIn != null && !registered(block.id)) return StandIns.of(block, standIn);
        return LegacyBlocks.state(block, meta);
    }

    /**
     * Blocks these bodies generate whose port is another workstream's: Eve's natural gas deposit {@code ore_gas} (the
     * per-body fluid ores). While one is unregistered its stand-in is placed, with one warning; the primer keeps the 1.7
     * block either way. (Eve's sea, {@code ntmspace:mercury_block}, is registered by {@code SpaceFluidBlocks}.)
     */
    private static final Map<Block17, String> STAND_INS = Map.of(
            Block17.ORE_GAS, "ntmspace:eve_rock");

    private static boolean registered(String id) {
        return BuiltInRegistries.BLOCK.containsKey(dev.golem.core.Ids.parse(id));
    }

    @Override
    protected void fill(ChunkAccess chunk) {
        int chunkX = chunk.getPos().getMinBlockX() >> 4;
        int chunkZ = chunk.getPos().getMinBlockZ() >> 4;
        CelestialTerrain17.Primer primer = primer(chunkX, chunkZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int column = (x * 16 + z) * 256;
                for (int y = 0; y < 256; y++) {
                    Block17 block = primer.blocks[column + y];
                    if (block == null) continue;
                    set(chunk, pos.set(chunkX * 16 + x, y, chunkZ * 16 + z), state(block, primer.metas[column + y]));
                }
            }
        }
    }

    private BlockState[] column(int x, int z) {
        CelestialTerrain17.Primer primer = primer(x >> 4, z >> 4);
        int column = ((x & 15) * 16 + (z & 15)) * 256;
        BlockState[] states = new BlockState[256];
        for (int y = 0; y < 256; y++) states[y] = state(primer.blocks[column + y], primer.metas[column + y]);
        return states;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] states = column(x, z);
        for (int y = 255; y >= 0; y--) {
            if (type.isOpaque().test(states[y])) return y + 1;
        }
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(0, column(x, z));
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    /** 1.7's {@code ChunkProviderCelestial.seaLevel} default, which Eve, Moho and Minmus keep. */
    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    protected int minimumY() {
        return 0;
    }

    private static final class StandIns {

        private static final Map<Block17, BlockState> STATES = new java.util.concurrent.ConcurrentHashMap<>();

        static BlockState of(Block17 block, String standIn) {
            return STATES.computeIfAbsent(block, b -> {
                NtmSpace.LOG.warn("{} is not registered yet: generating {} in its place", b.id, standIn);
                return BuiltInRegistries.BLOCK.getOptional(dev.golem.core.Ids.parse(standIn)).orElseThrow().defaultBlockState();
            });
        }
    }
}
