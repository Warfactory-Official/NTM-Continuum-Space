package com.ntmspace.block.generic;

import com.hbm.tileentity.TileEntityLoadedBase;
import com.ntmspace.registry.LeftoverContent;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code BlockOrrery} ({@code orrery}, "Orrery"): a steel block with no model of its own (render type -1)
 * whose tile entity is drawn by {@code RenderOrrery} - a toy model of the whole solar system, the bodies' orbits squeezed
 * so every one fits within about fifty blocks (client {@code RenderOrrery}, {@code SolarSystem.calculatePositionsOrrery}).
 * Hardness 10, resistance 20, iron (pickaxe), machine tab; it drops itself. The tile entity does nothing but give the
 * renderer its box ({@code x - 49 .. x + 50}, {@code y - 19 .. y + 20}) and the 256-block view distance.
 */
public class BlockOrrery extends CompatBlock implements EntityBlock {

    public BlockOrrery(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityOrrery(LeftoverContent.ORRERY_BE.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** 1.7 {@code BlockOrrery.TileEntityOrrery}: no state, only the render box and distance (the renderer's). */
    public static final class TileEntityOrrery extends TileEntityLoadedBase {

        public TileEntityOrrery(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
    }
}
