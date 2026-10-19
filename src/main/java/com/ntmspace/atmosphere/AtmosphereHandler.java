package com.ntmspace.atmosphere;

import com.hbm.api.environment.Environment;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.BlockBreaks;
import dev.golem.core.world.BlockPlaces;
import dev.golem.core.world.Vegetation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code ChunkAtmosphereHandler} with its event wiring ({@code ChunkAtmosphereManager}): every sealed room
 * ({@link AtmosphereBlob}) of every level, the air at a position (the body's air plus the rooms it is in), the effects of
 * air on blocks (torches and fire need oxygen, water needs pressure, plants need air), and the slow spread of grass the
 * chemical thrower's seed slurry starts ("terraforming"). "Misnomer, this isn't actually chunk based, but instead a global
 * handler" (1.7).
 *
 * <p>Events, as in 1.7: a placed block may close a room or be snuffed by the air it lands in, a broken block may open a
 * room, a door finishing its move updates its cells ({@link Sealing}). Not reached: explosions opening rooms and saplings
 * growing in a room (Golem Core has no detonation or tree-growth event yet; reported).
 */
public final class AtmosphereHandler {

    private AtmosphereHandler() {}

    private static final Map<ResourceKey<Level>, LinkedHashMap<AtmosphereProvider, AtmosphereBlob>> worldBlobs = new HashMap<>();
    private static final Map<ResourceKey<Level>, Queue<Growth>> growthMap = new HashMap<>();
    private static final int MAX_BLOB_RADIUS = 256;

    // How much CO2 is converted into O2 from various growing. Balanced around these amounts of plants providing for a single
    // pressurized room: 25 trees (955s to grow), OR 200 crops (300s per stage). (1.7)
    public static final int TREE_GROWTH_CONVERSION = 400; // per sapling -> tree
    public static final int CROP_GROWTH_CONVERSION = 15; // per stage
    public static final int GRASS_GROWTH_CONVERSION = 100; // per block

    /** 1.7 {@code getHorizon()} of a default world: lights above it count for the body's night side. */
    public static final int HORIZON = 63;

    public static void init() {
        Lifecycle.onLevelUnload((server, level) -> {
            worldBlobs.remove(level.dimension());
            growthMap.remove(level.dimension());
        });
        Lifecycle.onServerStopping(server -> {
            worldBlobs.clear();
            growthMap.clear();
            Decompressions.clear();
        });
        Lifecycle.onServerTick(AtmosphereHandler::serverTick);
        BlockPlaces.onPlace((placer, level, pos, placed) -> blockPlaced(level, pos.immutable(), placed));
        BlockBreaks.onBreak((player, level, pos, state) -> blockBroken(level, pos.immutable(), state));
        Sealing.init();
    }

    private static LinkedHashMap<AtmosphereProvider, AtmosphereBlob> blobs(Level level) {
        return worldBlobs.computeIfAbsent(level.dimension(), key -> new LinkedHashMap<>());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The air somewhere

    /** The air at an entity's eyes. */
    public static @Nullable CBT_Atmosphere getAtmosphere(Entity entity) {
        return getAtmosphere(entity.level(), BlockPos.containing(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ()), null);
    }

    public static @Nullable CBT_Atmosphere getAtmosphere(Level level, BlockPos pos) {
        return getAtmosphere(level, pos, null);
    }

    /**
     * The air at {@code pos}: the body's air with every room the position is in added (a room's negative pressure
     * reduces it), or null for no air at all. Client levels have no rooms and answer null (1.7: the handler only knew
     * server worlds).
     */
    public static @Nullable CBT_Atmosphere getAtmosphere(Level level, BlockPos pos, @Nullable AtmosphereBlob excludeBlob) {
        if (level.isClientSide()) return null;
        return combine(getCelestialAtmosphere(level), level, pos, excludeBlob);
    }

    /** {@code atmosphere} (modified) with the rooms at {@code pos} added; null if that leaves no air. */
    public static @Nullable CBT_Atmosphere combine(CBT_Atmosphere atmosphere, Level level, BlockPos pos, @Nullable AtmosphereBlob excludeBlob) {
        for (AtmosphereBlob blob : blobs(level).values()) {
            if (blob == excludeBlob) continue;
            if (blob.contains(pos)) {
                double pressure = blob.handler.getFluidPressure();
                if (pressure < 0) {
                    atmosphere.reduce(pressure);
                } else {
                    atmosphere.add(blob.handler.getFluidType(), pressure);
                }
            }
        }
        if (atmosphere.fluids.isEmpty()) return null;
        return atmosphere;
    }

    /** The body's air, safe for modification (none in orbit). */
    public static CBT_Atmosphere getCelestialAtmosphere(Level level) {
        if (Environment.inOrbit(level)) return new CBT_Atmosphere();
        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        return atmosphere == null ? new CBT_Atmosphere() : atmosphere.copy();
    }

    public static List<AtmosphereBlob> getBlobs(Level level, BlockPos pos) {
        List<AtmosphereBlob> inBlobs = new ArrayList<>();
        if (level.isClientSide()) return inBlobs;
        for (AtmosphereBlob blob : blobs(level).values()) {
            if (blob.contains(pos)) inBlobs.add(blob);
        }
        return inBlobs;
    }

    /** Whether {@code pos} is inside any sealed room. */
    public static boolean hasAtmosphere(Level level, BlockPos pos) {
        if (level.isClientSide()) return false;
        for (AtmosphereBlob blob : blobs(level).values()) {
            if (blob.contains(pos)) return true;
        }
        return false;
    }

    static List<AtmosphereBlob> getBlobsWithinRadius(Level level, BlockPos pos) {
        return getBlobsWithinRadius(level, pos, MAX_BLOB_RADIUS);
    }

    static List<AtmosphereBlob> getBlobsWithinRadius(Level level, BlockPos pos, int radius) {
        List<AtmosphereBlob> list = new LinkedList<>();
        double radiusSqr = (double) radius * radius;
        for (AtmosphereBlob blob : blobs(level).values()) {
            if (blob.getRootPosition().distSqr(pos) <= radiusSqr) list.add(blob);
        }
        return list;
    }

    /** Assuming 21% AIR / 9% OXY is required for a breathable atmosphere (1.7). */
    public static boolean canBreathe(@Nullable CBT_Atmosphere atmosphere) {
        return atmosphere != null && (atmosphere.hasFluid(SpaceFluids.EARTHAIR, 0.21) || atmosphere.hasFluid(Fluids.OXYGEN, 0.09));
    }

    /** Is the air pressure high enough to support liquids. */
    public static boolean hasLiquidPressure(@Nullable CBT_Atmosphere atmosphere) {
        return atmosphere != null && atmosphere.getPressure() >= 0.19D;
    }

    public static boolean willCorrode(@Nullable CBT_Atmosphere atmosphere) {
        return atmosphere != null && atmosphere.hasTrait(FT_Corrosive.class, 0.2);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Actions to rectify world status based on atmosphere

    /**
     * Torches and fire go out without oxygen, water boils off without pressure, a plant dies without the air it breathes
     * ({@link PlantBreathing}); the block drops as an item. True if the block was removed.
     */
    public static boolean runEffectsOnBlock(@Nullable CBT_Atmosphere atmosphere, Level level, BlockState state, BlockPos pos) {
        return runEffectsOnBlock(atmosphere, level, state, pos, false);
    }

    public static boolean runEffectsOnBlock(Level level, BlockState state, BlockPos pos) {
        return runEffectsOnBlock(null, level, state, pos, true);
    }

    private static boolean runEffectsOnBlock(@Nullable CBT_Atmosphere atmosphere, Level level, BlockState state, BlockPos pos,
                                             boolean fetchAtmosphere) {
        Block block = state.getBlock();
        boolean requiresPressure = block == Blocks.WATER;
        boolean requiresO2 = (block instanceof TorchBlock && !(block instanceof RedstoneTorchBlock)) || block instanceof BaseFireBlock;
        boolean requiresCO2 = isPlantable(state);

        if (!requiresO2 && !requiresCO2 && !requiresPressure) return false;

        if (fetchAtmosphere) atmosphere = getAtmosphere(level, pos);

        boolean canExist = true;
        if (requiresO2) {
            // Check for an atmosphere and destroy torches if there is insufficient oxygen
            canExist = !(atmosphere == null || (!atmosphere.hasFluid(Fluids.OXYGEN, 0.09) && !atmosphere.hasFluid(SpaceFluids.EARTHAIR, 0.21)));
        } else if (requiresPressure) {
            canExist = hasLiquidPressure(atmosphere);
        } else if (requiresCO2) {
            // TODO (1.7): Make plants rely on CO2 once CO2 is more readily available
            PlantBreathing.Rule rule = PlantBreathing.of(block);
            if (rule != null) {
                canExist = rule.canBreathe(atmosphere);
            } else {
                canExist = !(atmosphere == null || (!atmosphere.hasFluid(Fluids.OXYGEN, 0.01) && !atmosphere.hasFluid(SpaceFluids.EARTHAIR, 0.1)));
            }
        }

        if (canExist) return false;

        if (level instanceof ServerLevel server) Block.dropResources(state, server, pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return true;
    }

    /** 1.7 {@code instanceof IPlantable}: plants, sugar cane and cacti. */
    static boolean isPlantable(BlockState state) {
        Block block = state.getBlock();
        return Vegetation.isPlant(state) || block instanceof SugarCaneBlock || block instanceof CactusBlock
                || PlantBreathing.of(block) != null;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Registration

    public static void registerAtmosphere(AtmosphereProvider handler) {
        LinkedHashMap<AtmosphereProvider, AtmosphereBlob> blobs = blobs(handler.getLevel());
        AtmosphereBlob blob = blobs.get(handler);
        if (blob == null) {
            blob = new AtmosphereBlob(handler);
            blobs.put(handler, blob);
            blob.addBlock(handler.getRootPosition());
        }
    }

    public static void unregisterAtmosphere(AtmosphereProvider handler) {
        LinkedHashMap<AtmosphereProvider, AtmosphereBlob> blobs = worldBlobs.get(handler.getLevel().dimension());
        if (blobs != null) blobs.remove(handler);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Hooks to update our volumes as necessary

    static void onSealableChange(ServerLevel level, BlockPos pos, List<AtmosphereBlob> nearbyBlobs) {
        if (AtmosphereBlob.isBlockSealed(level, pos)) {
            for (AtmosphereBlob blob : nearbyBlobs) {
                if (blob.contains(pos)) {
                    blob.removeBlock(pos);
                } else if (!blob.contains(blob.getRootPosition())) {
                    blob.runDepress = false;
                    blob.addBlock(blob.getRootPosition());
                }
            }
        } else {
            onBlockRemoved(level, pos);
        }
    }

    /** A block was placed at {@code pos} by something other than an entity (1.7 only saw entity placements; for tests and machines). */
    public static void notifyPlaced(Level level, BlockPos pos) {
        if (!level.isClientSide()) onBlockPlaced(level, pos.immutable());
    }

    /** A block at {@code pos} is about to go or went (1.7: the break event). */
    public static void notifyRemoved(Level level, BlockPos pos) {
        if (!level.isClientSide()) onBlockRemoved(level, pos.immutable());
    }

    /** The rooms of {@code level} (for tests and the look of providers). */
    public static java.util.Collection<AtmosphereBlob> blobsOf(Level level) {
        return java.util.Collections.unmodifiableCollection(blobs(level).values());
    }

    static void onBlockPlaced(Level level, BlockPos pos) {
        if (!AtmosphereBlob.isBlockSealed(level, pos)) return;
        for (AtmosphereBlob blob : getBlobsWithinRadius(level, pos)) {
            if (blob.contains(pos)) {
                blob.removeBlock(pos);
            } else if (!blob.contains(blob.getRootPosition())) {
                blob.runDepress = false;
                blob.addBlock(blob.getRootPosition());
            }
        }
    }

    static void onBlockRemoved(Level level, BlockPos pos) {
        for (AtmosphereBlob blob : getBlobsWithinRadius(level, pos)) {
            // Make sure that a block can actually be attached to the blob
            for (Direction dir : Direction.values()) {
                if (blob.contains(pos.relative(dir))) {
                    blob.runDepress = true;
                    blob.depressDir = dir;
                    blob.addBlock(pos);
                    break;
                }
            }
        }
    }

    /** 1.7 {@code receiveBlockPlaced} + {@code ModEventHandler.onBlockPlaced}: the seal, then the air's effect, then lights. */
    private static void blockPlaced(ServerLevel level, BlockPos pos, BlockState placed) {
        onBlockPlaced(level, pos);
        boolean placeCancelled = runEffectsOnBlock(level, placed, pos);
        if (placeCancelled) return;
        if (placed.getBlock() instanceof BonemealableBlock) trackPlant(level, pos);
        SpaceLights.placed(level, pos, placed);
    }

    /** 1.7 {@code receiveBlockBroken} (the break event comes before the block is gone) + the lights of the body. */
    private static void blockBroken(ServerLevel level, BlockPos pos, BlockState state) {
        onBlockRemoved(level, pos);
        SpaceLights.broken(level, pos, state);
    }

    public static void trackPlant(Level level, BlockPos pos) {
        for (AtmosphereBlob blob : getBlobsWithinRadius(level, pos)) {
            if (blob.contains(pos)) blob.addPlant(level, pos);
        }
    }

    /** 1.7 {@code receiveTreeGrow}: a sapling grown in a room returns 400 of its waste gas as air. */
    public static void treeGrown(Level level, BlockPos pos) {
        for (AtmosphereBlob blob : getBlobsWithinRadius(level, pos, MAX_BLOB_RADIUS)) {
            if (blob.contains(pos)) {
                blob.produce(TREE_GROWTH_CONVERSION);
                break;
            }
        }
    }

    private static void serverTick(MinecraftServer server) {
        Decompressions.tick();
        for (ServerLevel level : server.getAllLevels()) {
            tickTerraforming(level);
            if (level.getGameTime() % 20 != 0) continue;
            LinkedHashMap<AtmosphereProvider, AtmosphereBlob> blobs = worldBlobs.get(level.dimension());
            if (blobs != null) for (AtmosphereBlob blob : new ArrayList<>(blobs.values())) blob.checkGrowth();
            // 1.7 ModEventHandler world tick: once per second, run atmospheric chemistry
            CelestialTraits.updateChemistry(level);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Terraforming: grass (or another block) spreading from where seed slurry landed

    private static void tickTerraforming(ServerLevel level) {
        Queue<Growth> growths = growthMap.get(level.dimension());
        if (growths == null) return;

        for (int g = 0; g < 64; g++) {
            Growth growth = growths.poll();
            if (growth == null) return;

            for (int i = 0; i < 4; i++) {
                Direction dir = Direction.values()[level.getRandom().nextInt(4) + 2];
                int x = growth.x + dir.getStepX();
                int z = growth.z + dir.getStepZ();

                for (int y = growth.y + 1; y >= growth.y - 1; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    boolean shouldReplace = growth.from != null
                            ? state.is(growth.from)
                            : state.is(Blocks.DIRT) || state.is(ModFalloutBlocks.waste_earth.get())
                                    || state.is(ModFallingBlocks.dirt_dead.get()) || state.is(ModFallingBlocks.dirt_oily.get());
                    if (shouldReplace) {
                        level.setBlock(pos, growth.into.defaultBlockState(), 3);
                        if (growth.into == Blocks.GRASS_BLOCK) level.levelEvent(2005, pos.above(), 0); // bone meal particles
                        if (growth.time > 0) addGrowth(level, growth.from, growth.into, x, y, z, growth.time - 1);
                        return; // 1.7: one successful growth ends this tick's terraforming

                    }
                }
            }
        }
    }

    public static void addGrowth(Level level, Block into, int x, int y, int z, int size) {
        addGrowth(level, null, into, x, y, z, size);
    }

    public static void addGrowth(Level level, Block into, int x, int y, int z, int count, int size) {
        addGrowth(level, null, into, x, y, z, count, size);
    }

    public static void addGrowth(Level level, @Nullable Block from, Block into, int x, int y, int z, int size) {
        growthMap.computeIfAbsent(level.dimension(), key -> new ArrayDeque<>()).add(new Growth(from, into, x, y, z, size));
    }

    public static void addGrowth(Level level, @Nullable Block from, Block into, int x, int y, int z, int count, int size) {
        Queue<Growth> growths = growthMap.computeIfAbsent(level.dimension(), key -> new ArrayDeque<>());
        for (int i = 0; i < count; i++) growths.add(new Growth(from, into, x, y, z, size));
    }

    private record Growth(@Nullable Block from, Block into, int x, int y, int z, int time) {}

    /** The pressure of the air at {@code pos} in atmospheres (for Environment.PRESSURE), 0 without any. */
    public static double pressure(Level level, BlockPos pos) {
        // a client knows no rooms: the body's air as synced
        CBT_Atmosphere atmosphere = level.isClientSide() ? getCelestialAtmosphere(level) : getAtmosphere(level, pos);
        return atmosphere == null ? 0 : atmosphere.getPressure();
    }
}
