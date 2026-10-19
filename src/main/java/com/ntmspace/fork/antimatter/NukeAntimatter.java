package com.ntmspace.fork.antimatter;

import com.hbm.blocks.bomb.NukeBomb2Block;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space {@code NukeAntimatter} ({@code nuke_antimatter}, "Antimatter Bomb"): a rotatable {@code BlockMachineBase}
 * with a GUI, drawn by its renderer, like HBM's Balefire Bomb. Redstone or a detonator fires it at once when loaded
 * (two lead particles, two hafnium ingots and an antimatter pellet); see {@link TileEntityAntimatter}. Its
 * {@code [BOMBPL]} placement log is {@code BombPlacementLog}'s.
 */
public class NukeAntimatter extends NukeBomb2Block {

    public NukeAntimatter(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeBomb newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAntimatter(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        return explode(level, pos, detonator, detonator == null ? null : detonator.getUUID());
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator, @Nullable UUID detonatorId) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof TileEntityAntimatter bomb)) return BombReturnCode.UNDEFINED;
        if (!bomb.isLoaded()) return BombReturnCode.ERROR_MISSING_COMPONENT;
        bomb.explode();
        return BombReturnCode.DETONATED;
    }
}
