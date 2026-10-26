package com.ntmspace.block.machine;

import com.ntmspace.data.SpaceData;
import com.ntmspace.tileentity.machine.storage.TileEntityCombatDropPod;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space {@code CombatDropPod} ({@code combat_drop}): the landed invasion pod. Drawn only by its renderer (1.7 render
 * type -1, not opaque), it drops sixteen steel ingots (the loot table, {@code DropPodContent}) and its tile entity
 * releases the payload.
 */
public class CombatDropPod extends CompatBlock implements TickingBlock {

    public CombatDropPod(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCombatDropPod(pos, state);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (SpaceData.COMBAT_POD_DESPAWN.get()) level.removeBlock(pos, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
