package com.ntmspace.fork.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import com.hbm.api.block.IToolable;
import com.hbm.blocks.machine.MachineCondenserPowered;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.IBlockExplodedVNT;
import com.hbm.interfaces.ILookOverlay;
import com.ntmspace.NtmSpace;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.Substitutions;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code MachineCondenserPowered} damage rules on HBM's powered condenser (substituted for
 * {@code hbm:machine_condenser_powered}, block and block entity type): explosions damage it and then remove it, a
 * blowtorch repairs it, a damaged condenser shows its repair list. Everything else is HBM's.
 */
public class SpaceMachineCondenserPowered extends MachineCondenserPowered implements IBlockExplodedVNT, IToolable {

    public SpaceMachineCondenserPowered(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DamagedMachines.DAMAGED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DamagedMachines.DAMAGED);
    }

    @Override public boolean dropFromExplosion(Explosion explosion) { return false; }

    /** Replaces {@code hbm:machine_condenser_powered}'s block and block entity type (HBM's id, renderer and handle stay). */
    public static void substitute() {
        Substitutions.block(NtmSpace.MOD_ID, "hbm:machine_condenser_powered", SpaceMachineCondenserPowered::new);
        Substitutions.<BlockEntityType<?>>entry(NtmSpace.MOD_ID, "block_entity_type", "hbm:machine_condenser_powered",
                key -> BlockEntities.ticking(TileEntityCondenserPoweredSpace::new, TileEntityCondenserPoweredSpace::clientTick,
                        TileEntityCondenserPoweredSpace::serverTick, ModMachineBlocks.machine_condenser_powered.get()));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCondenserPoweredSpace(pos, state);
    }

    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        return DamagedMachines.hit(level, pos, explosion) || super.onExplosion(state, level, pos, explosion, onHit);
    }

    @Override
    public boolean onVNTExploded(ServerLevel level, BlockPos pos, BlockState state, ExplosionVNT explosion) {
        return DamagedMachines.hit(level, pos, explosion);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.TORCH) return false;
        return DamagedMachines.repair(level, pos, player);
    }

    @Override
    public ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (DamagedMachines.damaged(level, pos) != null) return DamagedMachines.overlay(level, pos, player, getName().getString());
        return super.getLookOverlay(level, pos, player);
    }

    /** 1.7 {@code IPersistentNBT.getDrops}: a damaged machine drops as a damaged item ({@link DamagedMachines#markDrops}). */
    @Override
    public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        java.util.List<ItemStack> drops = super.getDrops(state, params);
        DamagedMachines.markDrops(this, drops, params);
        return drops;
    }
}
