package com.ntmspace.block.generic;

import net.minecraft.util.RandomSource;
import com.hbm.api.block.BarrelSkin;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.world.feature.OilSpot;
import com.ntmspace.NtmSpace;
import com.ntmspace.registry.LeftoverContent;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code BrownBarrel} as {@code oil_barrel}: HBM's barrel drum ({@code RenderBarrel}, through HBM's
 * {@code client.render.BarrelRenderer} with the skin {@code barrel_oily}) that soils the ground around it. Iron, hardness 0.5,
 * resistance 2.5, nuke tab; it drops itself, but nothing when blown up ({@code canDropFromExplosion} false). Bounds
 * 2..14 px across, a full block high.
 *
 * <p>Every 20 ticks from placement ({@code onBlockAdded} scheduled {@code updateTick}, which rescheduled itself at
 * {@code tickRate} 20) it runs {@code OilSpot.generateOilSpot(world, x, z, 4, 10, false)}: ten Gaussian picks within
 * about four blocks turn grass and dirt into dead or oily dirt, sand into dirty sand and so on. 1.7's
 * {@code BrownBarrel} also had an explosion for {@code oil_barrel} ({@code explode}, strength 2.5, fire) that nothing
 * called - the destroyed-by-explosion hook tested for the yellow barrel - so it is not ported. 1.7 bound the
 * {@code TileEntityBrownBarrel} class (render distance only) but the block never made one; the port's block entity is
 * the renderer's anchor and the tick.
 */
public class BrownBarrel extends CompatBlock implements TickingBlock, BarrelSkin {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    /** 1.7 {@code tickRate}. */
    public static final int TICK_RATE = 20;

    public BrownBarrel(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code onBlockAdded}: the spill's vanilla tick, 20 ticks out. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, TICK_RATE);
    }

    /** 1.7 {@code updateTick}: ten Gaussian picks around the barrel, then the next tick in 20. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        OilSpot.generateOilSpot(level, pos.getX(), pos.getZ(), 4, 10, false);
        level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    public String barrelSkin() {
        return NtmSpace.MOD_ID + ":textures/blocks/barrel_oily.png";
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Barrel(LeftoverContent.OIL_BARREL_BE.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 1.7 {@code canDropFromExplosion} false: an explosion takes the barrel and leaves nothing. */
    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        level.removeBlock(pos, false);
        return true;
    }

    /** The renderer's anchor (1.7 bound {@code TileEntityBrownBarrel} but never made one); the spill is the block tick. */
    public static final class Barrel extends TileEntityLoadedBase {

        /**
         * Ticks since placement or load. 1.7's pending tick was saved with the chunk; this counter is not, so after a
         * load the 20-tick rhythm restarts (a phase shift of less than a second, nothing else).
         */
        public int age;

        public Barrel(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Barrel barrel) {
            barrel.age++;
        }

        public static void clientTick(Level level, BlockPos pos, BlockState state, Barrel barrel) {}
    }
}
