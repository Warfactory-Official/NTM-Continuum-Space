package com.ntmspace.block.terrain;

import com.hbm.lib.ModDamageSources;
import com.hbm.packet.PlasmaBlastPayload;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.ntmspace.registry.FloraBlocks;
import dev.golem.core.Ids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Sounds;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
 * hbm-space's {@code geysir_electric} ("Discharge Geyser", {@code BlockVolcanoV2}): the core of Eve's electric volcanoes,
 * drawn as basalt, glowing (light 15), not an opaque cube. Its entity ({@link Discharge}, 1.7
 * {@code TileEntityLightningVolcano}) charges for 100 to 499 ticks, then discharges: one tick before the end a white
 * plasma blast 20 blocks above it (scale 40), a thunderclap (volume 200) and electric damage of the full maximum health,
 * 3 to 20, to everything living in the 60-block column above (with the tesla sound on every hit); then the flash
 * ({@link Discharge#flash}) grows to 100 while a dark spiral and two blue arcs reach a hundred blocks up
 * ({@code client/RenderGeysirElectric}), and the next charge starts.
 *
 * <p>1.7 turned the effect by the block's metadata; nothing ever set one (Eve's {@code WorldGenElectricVolcano} and the
 * block item place metadata 0), so the discharge always rises straight up from the block's foot.
 */
public class BlockGeysirElectric extends CompatBlock implements TickingBlock {

    public BlockGeysirElectric(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Discharge(FloraBlocks.GEYSIR_ELECTRIC_BE.get(), pos, state);
    }

    /** 1.7 {@code TileEntityLightningVolcano}. */
    public static final class Discharge extends TileEntityLoadedBase {

        /** Ticks until the discharge; 0 while it flashes. Synced to clients every tick, as 1.7's buf packet. */
        public int charge;
        /** The flash's growth, 0 to 100, on both sides (the server times the next charge by it). */
        public float flash;

        public Discharge(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
            // 1.7: "Quick pre-set to prevent it immediately firing on load"
            this.charge = new Random().nextInt(400) + 100;
        }

        /** 1.7 {@code updateEntity}'s first half, on both sides. */
        private void grow() {
            if (charge > 0) {
                flash = 0;
            } else {
                flash += 0.3F;
                flash = Math.min(100.0F, flash + 0.3F * (100.0F - flash) * 0.15F);
            }
        }

        public static void clientTick(Level level, BlockPos pos, BlockState state, Discharge geyser) {
            geyser.grow();
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Discharge geyser) {
            geyser.grow();
            if (!(level instanceof ServerLevel server)) return;

            if (geyser.charge == 1) {
                // 1.7 offset its position by dir * (time / 5 % 1), a long modulo 1: always the block itself
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 20.5;
                double z = pos.getZ() + 0.5;
                PlasmaBlastPayload.send(server, x, y, z, 1F, 1F, 1F, 0F, 90F, 10 * 4);
                server.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 200,
                        0.8F + server.getRandom().nextFloat() * 0.2F);
                discharge(server, pos);
            }

            if (geyser.charge > 0) geyser.charge--;
            if (geyser.flash >= 100) geyser.charge = server.getRandom().nextInt(400) + 100;
            geyser.setChanged();
            geyser.networkPackNT();
        }

        /** 1.7 {@code vapor()}: electricity, the full maximum health clamped to 3..20. */
        static void discharge(ServerLevel server, BlockPos pos) {
            AABB column = new AABB(pos.getX() - 0.5, pos.getY() + 0.5, pos.getZ() - 0.5,
                    pos.getX() + 1.5, pos.getY() + 60, pos.getZ() + 1.5);
            for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, column)) {
                float damage = Mth.clamp(living.getMaxHealth(), 3, 20);
                if (Compat.hurt(living, server, ModDamageSources.source(server, ModDamageSources.ELECTRICITY), damage)) {
                    server.playSound(null, living.getX(), living.getY(), living.getZ(),
                            Sounds.get(Ids.of("hbm", "weapon.tesla")), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }

        @Override
        protected void saveData(DataOut out) {
            super.saveData(out);
            out.putInt("charge", charge);
        }

        @Override
        protected void loadData(DataIn in) {
            super.loadData(in);
            charge = in.getIntOr("charge", charge);
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeInt(charge);
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            charge = buf.readInt();
        }
    }
}
