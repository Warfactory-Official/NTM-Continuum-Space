package com.ntmspace.fork.machine;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import com.hbm.api.block.IToolable;
import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.blocks.machine.MachineChungus;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code MachineChungus} damage rules on HBM's Leviathan (substituted for {@code hbm:machine_chungus}, block
 * and block entity type, with HBM's registration properties): explosions damage it and then remove it
 * ({@link DamagedMachines#hit}), a blowtorch repairs it ({@code IToolable}), a damaged turbine shows its repair list and
 * ignores the lever. The turbine body, lever and look overlay are otherwise HBM's (see {@link TileEntityChungusSpace}).
 */
public class SpaceMachineChungus extends MachineChungus implements IBlockExplodedVNT, IToolable {

    public SpaceMachineChungus(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DamagedMachines.DAMAGED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DamagedMachines.DAMAGED);
    }

    @Override public int[] getDimensions() { return new int[]{4, 0, 10, 3, 2, 2}; }

    @Override protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockHandlerXR.checkSpace(level, core, CROWN, clicked, facing);
    }

    @Override protected boolean canChangeTierWhileRunning() { return true; }

    @Override public boolean dropFromExplosion(Explosion explosion) { return false; }

    /** Replaces {@code hbm:machine_chungus}'s block and its block entity type (HBM's id, renderer and handle stay). */
    public static void substitute() {
        Substitutions.block(NtmSpace.MOD_ID, "hbm:machine_chungus", SpaceMachineChungus::new);
        Substitutions.<BlockEntityType<?>>entry(NtmSpace.MOD_ID, "block_entity_type", "hbm:machine_chungus",
                key -> BlockEntities.ticking(TileEntityChungusSpace::new, TileEntityChungusSpace::clientTick,
                        TileEntityChungusSpace::serverTick, ModPowerGenBlocks.MACHINE_CHUNGUS.get()));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityChungusSpace(pos, state);
    }

    /** 1.7: {@code if(entity != null && !entity.damaged)} around the lever; a damaged turbine swallows the click. */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && DamagedMachines.damaged(level, pos) != null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

    /** 1.7 {@code onBlockExploded} (vanilla explosions): never broken or dropped by the blast itself. */
    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        return DamagedMachines.hit(level, pos, explosion) || super.onExplosion(state, level, pos, explosion, onHit);
    }

    /** 1.7 {@code onBlockExploded} for HBM's own explosions. */
    @Override
    public boolean onVNTExploded(ServerLevel level, BlockPos pos, BlockState state, ExplosionVNT explosion) {
        return DamagedMachines.hit(level, pos, explosion);
    }

    /** 1.7 {@code onScrew}: only the blowtorch, and only on a damaged turbine. */
    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.TORCH) return false;
        return DamagedMachines.repair(level, pos, player);
    }

    /** 1.7 {@code printHook}: a damaged turbine shows its repair ({@code IRepairable.addGenericOverlay}), else HBM's. */
    @Override
    public ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (DamagedMachines.damaged(level, pos) != null) {
            return DamagedMachines.overlay(level, pos, player, getName().getString());
        }
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
