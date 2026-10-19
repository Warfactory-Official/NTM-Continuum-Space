package com.ntmspace.block.station;

import com.ntmspace.tileentity.station.TileEntityStationPropulsionCreative;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** hbm-space's {@code BlockStationPropulsionCreative}: a plain block whose block entity is an infinite engine. */
public class BlockStationPropulsionCreative extends CompatBlock implements TickingBlock {

    public BlockStationPropulsionCreative(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityStationPropulsionCreative(pos, state);
    }
}
