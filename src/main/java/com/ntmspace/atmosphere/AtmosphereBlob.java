package com.ntmspace.atmosphere;

import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.FluidType;
import com.ntmspace.NtmSpace;
import com.ntmspace.trait.CBT_Atmosphere;
import dev.golem.core.world.Vegetation;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * hbm-space's {@code AtmosphereBlob} ("somewhat based on the Advanced-Rocketry implementation, but extended to define the
 * gases and gas pressure inside the enclosed volume"): the air-filled volume a provider keeps, found by a depth-first flood
 * fill from the provider's root through every block that does not seal ({@link #isBlockSealed}). If the fill reaches
 * further than the provider's radius the room is not sealed: the blob is voided (with a decompression blast if it was a
 * seal breaking).
 *
 * <p>Threading: hbm-space ran the fill on a thread pool by default ({@code enableThreadedAtmospheres}). Modern chunk access
 * off the server thread is not available without the server's internals ({@code getChunkNow} answers null off-thread), so
 * the port runs the fill on the server thread - hbm-space's own non-threaded path. The fill is depth first (a stack, as in
 * 1.7), so an open room fails after about a radius worth of steps.
 */
public class AtmosphereBlob implements Runnable {

    /** The graph containing the enclosed area. */
    protected final AdjacencyGraph<BlockPos> graph;

    /** Handler, provides atmosphere information and receives callbacks. */
    protected final AtmosphereProvider handler;

    private static final Map<BlockState, Boolean> FULL_BOUNDS = new ConcurrentHashMap<>();

    private boolean executing;
    private BlockPos blockPos;

    /** If true, run depressurization effects on blobbing failure. */
    public boolean runDepress;
    public Direction depressDir = Direction.UP;

    private final LinkedHashMap<BlockPos, Integer> plants = new LinkedHashMap<>();

    public AtmosphereBlob(AtmosphereProvider handler) {
        this.handler = handler;
        this.graph = new AdjacencyGraph<>();
    }

    public boolean isPositionAllowed(Level level, BlockPos pos) {
        return !isBlockSealed(level, pos);
    }

    /**
     * hbm-space's {@code isBlockSealed}, rule for rule: outside the world and air do not seal, an unloaded chunk does
     * (so the fill never loads chunks), farmland and fences do not, a {@link BlockSealable} (or an HBM door, or a block in
     * {@code #ntmspace:unsealable}) answers for itself, HBM's multiblock machines never seal ("almost all have gaps"),
     * liquids and leaves do not, and anything else seals when its collision box is the full block.
     *
     * <p>One deliberate difference: 1.7 cached the full-box answer per block (the first metadata it met decided for all
     * of them, so a bottom slab could decide for a double slab); the port caches per block state.
     */
    public static boolean isBlockSealed(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;

        // Prevent loading new chunks
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return true;

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (state.isAir()) return false; // Air obviously doesn't seal
        if (state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) return false; // 1.7 BlockFarmland
        if (block instanceof FenceBlock) return false;
        if (block instanceof BlockSealable sealable) return sealable.isSealed(level, pos); // custom semi-sealables
        if (block instanceof BlockDoorGeneric door) return !door.hasExtra(state); // 1.7: !getBlocksMovement = closed
        if (state.is(Sealing.UNSEALABLE)) return false; // 1.7 IBlockSealable blocks answering false (furnaces, diesel)
        if (block instanceof BlockMultiblockCore || block instanceof BlockMultiblockCell) return false; // machines have gaps

        // liquids need to know what pressurized atmosphere they're in to determine evaporation (1.7: material.isLiquid;
        // its !material.isSolid() half is covered by the full-box test below: no non-solid material has a full box)
        if (block instanceof net.minecraft.world.level.block.LiquidBlock) return false;
        if (Vegetation.isLeaves(state)) return false; // leaves never block air

        return FULL_BOUNDS.computeIfAbsent(state, AtmosphereBlob::fullCollision);
    }

    private static boolean fullCollision(BlockState state) {
        try {
            // as 1.7, asked without a world: a block whose box depends on its neighbours does not count
            VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            return Block.isShapeFullBlock(shape);
        } catch (RuntimeException ex) {
            return false; // no collision, can't seal (like lamps)
        }
    }

    public int getBlobMaxRadius() {
        return handler.getMaxBlobRadius();
    }

    public boolean hasFluid(FluidType fluid) {
        return hasFluid(fluid, 0.001);
    }

    public boolean hasFluid(FluidType fluid, double abovePressure) {
        if (handler.getFluidType() != fluid) return false;
        return handler.getFluidPressure() >= abovePressure;
    }

    public boolean hasPressure(double abovePressure) {
        return handler.getFluidPressure() >= abovePressure;
    }

    public void consume(int amount) {
        handler.consume(amount);
    }

    public void produce(int amount) {
        handler.produce(amount);
    }

    public AtmosphereProvider provider() {
        return handler;
    }

    /** Recursively checks for contiguous blocks and adds them to the graph. */
    public void addBlock(BlockPos blockPos) {
        blockPos = blockPos.immutable();
        if (!this.contains(blockPos) && (this.graph.size() == 0 || anyNeighbourContained(blockPos))) {
            if (!executing) {
                this.blockPos = blockPos;
                executing = true;
                this.run();
            }
        }
    }

    private boolean anyNeighbourContained(BlockPos pos) {
        // 1.7 order: UP, DOWN, EAST, WEST, NORTH, SOUTH (any order answers the same)
        return contains(pos.above()) || contains(pos.below()) || contains(pos.east()) || contains(pos.west())
                || contains(pos.north()) || contains(pos.south());
    }

    private void addSingleBlock(BlockPos blockPos) {
        if (!graph.contains(blockPos)) {
            graph.add(blockPos, getPositionsToAdd(blockPos));
            if (handler.getLevel().getBlockState(blockPos).getBlock() instanceof BonemealableBlock) {
                addPlant(handler.getLevel(), blockPos);
            }
        }
    }

    /** The root of the blob. */
    public BlockPos getRootPosition() {
        return handler.getRootPosition();
    }

    /** Adjacent positions that exist in the blob. */
    protected HashSet<BlockPos> getPositionsToAdd(BlockPos blockPos) {
        HashSet<BlockPos> set = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos offset = blockPos.relative(direction);
            if (graph.contains(offset)) set.add(offset);
        }
        return set;
    }

    public boolean contains(BlockPos position) {
        synchronized (graph) {
            return graph.contains(position);
        }
    }

    public boolean contains(int x, int y, int z) {
        return contains(new BlockPos(x, y, z));
    }

    /** Removes the block at the given position from this blob, and every part no longer connected to the root. */
    public void removeBlock(BlockPos blockPos) {
        synchronized (graph) {
            graph.remove(blockPos);
            for (Direction direction : Direction.values()) {
                BlockPos newBlock = blockPos.relative(direction);
                if (graph.contains(newBlock) && !graph.doesPathExist(newBlock, handler.getRootPosition())) {
                    runEffectOnWorldBlocks(handler.getLevel(), graph.removeAllNodesConnectedTo(newBlock));
                }
            }
        }
    }

    /** Removes all nodes from the blob. */
    public void clearBlob() {
        runEffectOnWorldBlocks(handler.getLevel(), getLocations());
        graph.clear();
    }

    public Set<BlockPos> getLocations() {
        return graph.getKeys();
    }

    public int getBlobSize() {
        return graph.size();
    }

    @Override
    public void run() {
        Deque<BlockPos> stack = new ArrayDeque<>();
        stack.push(blockPos);

        final int maxSize = this.getBlobMaxRadius();
        final HashSet<BlockPos> addableBlocks = new HashSet<>();
        final BlockPos root = this.getRootPosition();

        // Depth first search (1.7's comment says breadth first; it is a stack); non recursive
        while (!stack.isEmpty()) {
            BlockPos stackElement = stack.pop();
            addableBlocks.add(stackElement);

            for (Direction dir : Direction.values()) {
                BlockPos searchNextPosition = stackElement.relative(dir);

                // Don't path areas we have already scanned
                if (!graph.contains(searchNextPosition) && !addableBlocks.contains(searchNextPosition)) {
                    try {
                        if (isPositionAllowed(handler.getLevel(), searchNextPosition)) {
                            if (searchNextPosition.distSqr(root) <= (double) maxSize * maxSize
                                    && !openToSky(handler.getLevel(), searchNextPosition)) {
                                stack.push(searchNextPosition);
                                addableBlocks.add(searchNextPosition);
                            } else {
                                // BANG HISSS
                                if (runDepress) decompress(blockPos, depressDir);

                                // Failed to seal, void
                                clearBlob();
                                executing = false;
                                return;
                            }
                        }
                    } catch (RuntimeException e) {
                        NtmSpace.LOG.info("Error: AtmosphereBlob has failed to form correctly due to an error. CurrentBlock: {} NextPos: {} Dir: {} StackSize: {}",
                                stackElement, searchNextPosition, dir, stack.size(), e);
                        clearBlob();
                        executing = false;
                        return;
                    }
                }
            }
        }

        synchronized (graph) {
            for (BlockPos addableBlock : addableBlocks) addSingleBlock(addableBlock);
            handler.onBlobCreated(this);
        }

        executing = false;
    }

    /**
     * A shortcut with 1.7's outcome: a position at or above the top motion-blocking block of its column (or outside the
     * world's height) has nothing sealing above it - a sealing block is a full collision box, which blocks motion - and
     * everything above the world's height is unsealed too (1.7 {@code y > 256}), so the straight column up from it leaves
     * any radius and the fill is certain to fail. 1.7 found that out only after flooding every loaded layer of open air
     * below (on its worker thread); the port, on the server thread, stops at the first such position.
     */
    private static boolean openToSky(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return true;
        return pos.getY() >= level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
    }

    /** Runs the atmosphere's effects (torches out, water boiling, plants dying) on positions that lost this blob's air. */
    protected void runEffectOnWorldBlocks(Level level, Collection<BlockPos> blocks) {
        BlockPos root = handler.getRootPosition();
        CBT_Atmosphere newAtmosphere = AtmosphereHandler.getAtmosphere(level, root, this);
        for (BlockPos pos : blocks.toArray(new BlockPos[0])) {
            AtmosphereHandler.runEffectsOnBlock(newAtmosphere, level, level.getBlockState(pos), pos);
        }
    }

    /**
     * 1.7 spawned an {@code EntityDepress} here: for 20 ticks it pulls everything within 8 blocks towards the breach (and
     * pushes what is on the far side), with white rocket-flame particles. The push is {@link Decompressions}; the
     * particles belong to the particle port.
     */
    public void decompress(BlockPos pos, Direction dir) {
        Level level = handler.getLevel();
        if (!(level instanceof ServerLevel server)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        Decompressions.add(server, x, y, z, dir.getOpposite(), 20);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 1.6F);
        level.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.25F);
    }

    /** Every second: a plant that grew a stage in the room turns some of the waste gas back into air. */
    public void checkGrowth() {
        Level level = handler.getLevel();
        Iterator<Map.Entry<BlockPos, Integer>> iterator = plants.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            BlockState state = level.getBlockState(entry.getKey());
            if (!(state.getBlock() instanceof BonemealableBlock)) {
                iterator.remove();
                continue;
            }
            int newMeta = growthStage(state);
            int oldMeta = entry.getValue();
            if (newMeta != oldMeta) {
                entry.setValue(newMeta);
                produce(Math.max(newMeta - oldMeta, 0) * AtmosphereHandler.CROP_GROWTH_CONVERSION);
            }
        }
    }

    public void addPlant(Level level, BlockPos pos) {
        plants.put(pos.immutable(), growthStage(level.getBlockState(pos)));
    }

    /**
     * 1.7 compared the plant's metadata, which for crops is the growth stage and for saplings carries the grown stage as
     * bit 8. The modern spelling is the {@code age} property, or a sapling's {@code stage} times 8.
     */
    static int growthStage(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer) {
                if (property.getName().equals("age")) return state.getValue(integer);
                if (property.getName().equals("stage")) return state.getValue(integer) * 8;
            }
        }
        return 0;
    }
}
