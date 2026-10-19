package com.ntmspace.fork;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.logic.EntityPlaneBase;
import com.hbm.explosion.ExplosionTom;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.special.ItemEnergy;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.tileentity.machine.storage.TileEntityFileCabinet;
import dev.golem.core.Ids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Small changes hbm-space made to base HBM classes, each through an HBM hook or tunable so they hold only while NTM:
 * Space is installed (fork-content triage, part 2 item 7):
 * <ul>
 *   <li>{@code ExplosionTom}: the lava sheet under the old terrain only within 500 blocks of the impact, like the
 *       clearing above it ({@code ExplosionTom.lavaRadius}, V-174);</li>
 *   <li>{@code EntityPlaneBase}: a shot-down plane's crash also composes the large explosion effect
 *       ({@code ExplosionCreator.composeEffectLarge}; {@code EntityPlaneBase.CRASH}, H-73);</li>
 *   <li>{@code TileEntityFileCabinet}: a cabinet holding a Soyuz ({@code missile_soyuz}, any of its three forms) explodes
 *       every tick it does - {@code ExplosionVNT} 15 with a 48-ray allocator, no drops and slag debris, the cross entity
 *       processor (7.5, range x3), the standard player processor and the standard effect
 *       ({@code TileEntityFileCabinet.TICK}, H-74);</li>
 *   <li>{@code ItemEnergy}: coffee leaves its mug ({@code ntmspace:cmug_empty}; {@code ItemEnergy.EMPTY}, H-75).</li>
 * </ul>
 */
public final class ForkTweaks {

    private ForkTweaks() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        ExplosionTom.lavaRadius = 500;
        EntityPlaneBase.CRASH.register((plane, x, y, z) -> ExplosionCreator.composeEffectLarge(plane.level(), x, y, z));
        TileEntityFileCabinet.TICK.register(ForkTweaks::soyuz);
        ItemEnergy.EMPTY.register(id -> id.equals("coffee")
                ? BuiltInRegistries.ITEM.getOptional(Ids.of("ntmspace", "cmug_empty")).orElse(null) : null);
    }

    /** hbm-space {@code TileEntityFileCabinet.updateEntity}'s soyuz check. */
    static void soyuz(TileEntityFileCabinet cabinet) {
        Level level = cabinet.getLevel();
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < cabinet.getContainerSize(); i++) {
            ItemStack slot = cabinet.getItem(i);
            if (slot.isEmpty() || !BuiltInRegistries.ITEM.getKey(slot.getItem()).toString().startsWith("hbm:missile_soyuz")) continue;
            BlockPos pos = cabinet.getBlockPos();
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            ExplosionVNT xnt = new ExplosionVNT(level, x, y, z, 15);
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop()
                    .withBlockEffect(new BlockMutatorDebris(() -> ModBlocks.block_slag.get().defaultBlockState())));
            xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(3));
            xnt.setPlayerProcessor(new PlayerProcessorStandard());
            xnt.explode();
            // 1.7 composeEffect(15, 5, 1, 45, 10, 16, 50, 1, 3, -2, 200) = composeEffectStandard
            ExplosionCreator.composeEffectStandard(level, x, y, z);
            if (cabinet.isRemoved()) return;
        }
    }
}
