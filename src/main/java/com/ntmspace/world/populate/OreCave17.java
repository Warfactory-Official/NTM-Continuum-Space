package com.ntmspace.world.populate;

import dev.golem.core.Ids;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import com.ntmspace.world.gen.noise.Noise17Perlin;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code OreCave} on a celestial body: a stratum of a resource block along cave walls around a y level, its
 * thickness per column from a 2D Perlin field, with stalactites and stalagmites in the open cells of the band and,
 * where it {@code withFluid}, pools of that fluid on the stratum. It ran on {@code DecorateBiomeEvent.Pre}, drawing from
 * the biome decorator's {@code Random} (one {@code nextBoolean} per rock cell of the band, one {@code nextInt(5)} per
 * open cell), so it runs in the decorator here, before the decorator's own work.
 *
 * <p>The noise seed is 1.7's: {@code world seed + MetaBlock.hashCode() * 31 + yLevel}, where the {@code MetaBlock} hash is
 * {@code 31 * (31 + <1.7 registry name>.hashCode()) + meta} and the registry name is what FML gave the block
 * ({@code "hbm:tile.<name>"}).
 */
public final class OreCave17 {

    /** {@code ForgeDirection.VALID_DIRECTIONS}: down, up, north, south, west, east. */
    private static final int[][] DIRECTIONS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    private final int oreHash;
    private final Supplier<BlockState> ore;
    private final double threshold;
    private final int rangeMult;
    private final int maxRange;
    private final int yLevel;
    private Supplier<BlockState> fluid;
    private Supplier<Block> override;
    private boolean ignoreWater;
    private boolean stalagmites = true;
    private Supplier<BlockState> stalactite = () -> null;
    private Supplier<BlockState> stalagmite = () -> null;

    private long noiseSeed;
    private Noise17Perlin noise;

    /**
     * @param registryName the ore block's 1.7 registry name ({@code hbm:tile.stone_resource})
     * @param meta its 1.7 metadata
     */
    public OreCave17(String registryName, int meta, Supplier<BlockState> ore, double threshold, int rangeMult, int maxRange, int yLevel) {
        this.oreHash = 31 * (31 + registryName.hashCode()) + meta;
        this.ore = ore;
        this.threshold = threshold;
        this.rangeMult = rangeMult;
        this.maxRange = maxRange;
        this.yLevel = yLevel;
    }

    public OreCave17 withFluid(Supplier<BlockState> fluid) {
        this.fluid = fluid;
        return this;
    }

    public OreCave17 blockOverride(Supplier<Block> override) {
        this.override = override;
        return this;
    }

    public OreCave17 ignoreWater() {
        this.ignoreWater = true;
        return this;
    }

    public OreCave17 noStalagmites() {
        this.stalagmites = false;
        return this;
    }

    /** The spikes of {@code BlockStalagmite.getMetaFromResource(ore)}: stalactite first, as 1.7 tried it. */
    public OreCave17 spikes(Supplier<BlockState> stalactite, Supplier<BlockState> stalagmite) {
        this.stalactite = stalactite;
        this.stalagmite = stalagmite;
        return this;
    }

    /** Configurable placement and a view of the same live producer's materials and band bounds. */
    public record Config(BlockState ore, Optional<BlockState> fluid,
                         Optional<BlockState> stalactite, Optional<BlockState> stalagmite,
                         RuleTest replaceable,
                         double threshold, int rangeMult, int maxRange, int yLevel,
                         Identifier domain, boolean exposedByFluid) {}

    /** Legacy producers own their ore-hash seed rather than a named domain; their domain is absent. */
    public Config configuration(Block stone) {
        return new Config(ore.get(), Optional.ofNullable(fluid == null ? null : fluid.get()),
                Optional.ofNullable(stalagmites ? stalactite.get() : null),
                Optional.ofNullable(stalagmites ? stalagmite.get() : null),
                new BlockMatchTest(override == null ? stone : override.get()),
                threshold, rangeMult, maxRange, yLevel, null, ignoreWater);
    }

    /** Places an explicit band through the same core as the native pre-decoration producer. */
    public static boolean place(WorldGenLevel level, RandomSource random,
                                BlockPos origin, Config config) {
        var cave = new OreCave17("configured:" + config.domain(), 0, config::ore,
                config.threshold(), config.rangeMult(), config.maxRange(), config.yLevel());
        config.fluid().ifPresent(value -> cave.withFluid(() -> value));
        cave.spikes(() -> config.stalactite().orElse(null), () -> config.stalagmite().orElse(null));
        cave.ignoreWater = config.exposedByFluid();
        var world = new FeaturePopulateWorld(level, origin, random);
        // The source's configured field salts the world seed by the actual dimension and requested noise domain.
        long dimensionSalt = identifierSalt(Ids.keyId(level.getLevel().dimension()));
        long dimensionSeed = RandomSupport.mixStafford13(level.getSeed() ^ dimensionSalt);
        long seed = RandomSupport.mixStafford13(dimensionSeed ^ identifierSalt(config.domain()));
        var sampler = new Noise17Perlin(new Random(seed), 2);
        cave.generateBand(world, origin.getX() & ~15, origin.getZ() & ~15, world.worldRandom(),
                state -> config.replaceable().test(state, random), sampler, level);
        return true;
    }

    private static long identifierSalt(Identifier id) {
        var hash = RandomSupport.seedFromHashOf(id.toString());
        return RandomSupport.mixStafford13(hash.seedLo() ^ Long.rotateLeft(hash.seedHi(), 29));
    }

    /** The noise seed 1.7 gave this cave in a world of {@code worldSeed}. */
    public long noiseSeed(long worldSeed) {
        return worldSeed + (oreHash * 31) + yLevel;
    }

    /**
     * {@code onDecorate} for the chunk at block origin {@code (cX, cZ)}; {@code stone} is the body's stone
     * ({@code WorldProviderCelestial.getStone}), {@code rand} the decorator's.
     */
    public void generate(PopulateWorld world, long worldSeed, int cX, int cZ, Block stone, Random rand) {
        Block replace = override != null ? override.get() : stone;
        synchronized (this) {
            if (noise == null || noiseSeed != worldSeed) {
                noise = new Noise17Perlin(new Random(noiseSeed(worldSeed)), 2);
                noiseSeed = worldSeed;
            }
        }
        generateBand(world, cX + 8, cZ + 8, rand,
                state -> PopulateWorld.normalCube(state) && state.is(replace), noise, null);
    }

    private void generateBand(PopulateWorld world, int startX, int startZ, Random rand,
                              Predicate<BlockState> replaceable, Noise17Perlin sampler,
                              WorldGenLevel liveLevel) {
        boolean configured = liveLevel != null;
        var pos = new BlockPos.MutableBlockPos();
        BlockState oreState = ore.get();
        BlockState fluidState = fluid == null ? null : fluid.get();
        boolean hasFluid = fluid != null;
        double scale = 0.01D;

        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                double n = sampler.value(x * scale, z * scale);
                if (n > threshold) {
                    int range = (int) ((n - threshold) * rangeMult);
                    if (range > maxRange) range = (maxRange * 2) - range;
                    if (range < 0) continue;

                    for (int y = yLevel - range; y <= yLevel + range; y++) {
                        BlockState genTarget = world.get(x, y, z);

                        if (replaceable.test(genTarget)) {
                            boolean shouldGen = false;
                            boolean canGenFluid = (!configured || hasFluid) && rand.nextBoolean();

                            for (int d = 0; d < 6; d++) {
                                int[] dir = DIRECTIONS[d];
                                BlockState neighbor = world.get(x + dir[0], y + dir[1], z + dir[2]);
                                if (neighbor.isAir() || (ignoreWater && (configured ? PopulateWorld.liquid(neighbor) : PopulateWorld.water(neighbor))) || spike(neighbor)) {
                                    shouldGen = true;
                                }

                                if (shouldGen && (!hasFluid || !canGenFluid)) break;

                                if (hasFluid) {
                                    switch (d) {
                                        case 1 -> { if (!neighbor.isAir() && !spike(neighbor)) canGenFluid = false; }
                                        case 0 -> { if (!PopulateWorld.normalCube(neighbor)) canGenFluid = false; }
                                        default -> {
                                            if (!PopulateWorld.normalCube(neighbor) && (fluidState == null || !neighbor.is(fluidState.getBlock()))) canGenFluid = false;
                                        }
                                    }
                                }
                            }

                            if (hasFluid && canGenFluid) {
                                world.set(x, y, z, fluidState);
                                world.set(x, y - 1, z, oreState);
                                for (int d = 2; d < 6; d++) {
                                    int[] dir = DIRECTIONS[d];
                                    if (PopulateWorld.normalCube(world.get(x + dir[0], y, z + dir[2]))) {
                                        world.set(x + dir[0], y, z + dir[2], oreState);
                                    }
                                }
                            } else if (shouldGen) {
                                world.set(x, y, z, oreState);
                            }

                        } else if (stalagmites) {
                            if ((genTarget.isAir() || !PopulateWorld.normalCube(genTarget)) && rand.nextInt(5) == 0
                                    && !PopulateWorld.liquid(genTarget)) {
                                BlockState hanging = stalactite.get();
                                BlockState standing = stalagmite.get();
                                // canPlaceBlockAt: the stalactite needs a solid underside above, the stalagmite a solid top below
                                if (hanging != null && (configured ? hanging.canSurvive(liveLevel, pos.set(x, y, z))
                                        : PopulateWorld.normalCube(world.get(x, y + 1, z)))) {
                                    world.set(x, y, z, hanging);
                                } else if (standing != null && (configured ? standing.canSurvive(liveLevel, pos.set(x, y, z))
                                        : PopulateWorld.normalCube(world.get(x, y - 1, z)))) {
                                    world.set(x, y, z, standing);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** {@code instanceof BlockStalagmite}. */
    private static boolean spike(BlockState state) {
        return state.getBlock() instanceof com.hbm.blocks.generic.BlockStalagmite;
    }
}
