package com.ntmspace.block.terrain;

import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.HbmEffectNT;
import com.ntmspace.registry.FloraBlocks;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code geysir_chloric} ("DCM Geysir", {@code BlockGeysierDCM}): the core of a Tekto vinyl volcano, drawn
 * as basalt and glowing (light 15), not an opaque cube. Its entity ({@link Vent}, 1.7 {@code TileEntityDCM}) every tick:
 * <ul>
 *   <li>a rising pale-cyan DCM puff: the missile contrail effect at the block's foot, scale 1.5, rising 4 blocks a tick,
 *       100 to 119 ticks long, colour {@code 0xA4D7DD};</li>
 *   <li>acid damage of a tenth of the maximum health, 3 to 20, to everything living in the column from half a block
 *       above it to 60 blocks up (one block wide, half a block more on each side), with a fizz on every hit. (hbm-space's
 *       comment: "copy pasting without reading makes you create a fluid geyser that... zaps you to death".)</li>
 * </ul>
 * 1.7 posted the puff through {@code proxy.effectNT} from the server thread, which only the integrated client's proxy
 * drew (a dedicated server showed nothing); here the client spawns it every tick itself, as single player showed it.
 */
public class BlockGeysirChloric extends CompatBlock implements TickingBlock {

    public BlockGeysirChloric(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Vent(FloraBlocks.GEYSIR_CHLORIC_BE.get(), pos, state);
    }

    /** 1.7 {@code TileEntityDCM}. */
    public static final class Vent extends CompatBlockEntity {

        public Vent(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        public static void clientTick(Level level, BlockPos pos, BlockState state, Vent vent) {
            CompoundTag data = new CompoundTag();
            data.putFloat("scale", 1.5F);
            data.putDouble("moX", 0);
            data.putDouble("moY", 4);
            data.putDouble("moZ", 0);
            data.putInt("maxAge", 100 + level.getRandom().nextInt(20));
            data.putInt("color", 0xA4D7DD);
            HbmEffectNT.MissileContrail.summonParticle(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, data);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Vent vent) {
            if (!(level instanceof ServerLevel server)) return;
            AABB column = new AABB(pos.getX() - 0.5, pos.getY() + 0.5, pos.getZ() - 0.5,
                    pos.getX() + 1.5, pos.getY() + 60, pos.getZ() + 1.5);
            for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, column)) {
                float damage = Mth.clamp(living.getMaxHealth() * 0.1F, 3, 20);
                if (Compat.hurt(living, server, ModDamageSources.source(server, ModDamageSources.ACID), damage)) {
                    server.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }
}
