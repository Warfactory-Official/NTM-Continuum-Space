package com.ntmspace.fork.antimatter;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.explosion.ExplosionBalefire;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.items.special.ItemCell;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.ntmspace.NtmSpace;
import dev.golem.core.Ids;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's antimatter balefire (1.7 {@code EntityBalefire.antimatter()} / {@code ExplosionBalefire(..., antimatter)}),
 * through HBM's balefire variant and hooks (ADDON-API-INVENTORY H-65..H-68, V-170..V-172):
 * <ul>
 *   <li>crater ({@code ntmspace:antimatter}): no balefire fire and no schrabidium-cluster stop; snow and ice in a
 *       column burst into a gas flame and stay; each of the five floor layers has a 1-in-{@code dist} chance to put
 *       volcanic lava at the crater floor;</li>
 *   <li>entity: for its first nine ticks an antimatter balefire irradiates every living thing within twice its range
 *       ({@code 40000 * range} RAD over the summed block resistance and the squared distance; 500 RAD or more kills);</li>
 *   <li>every balefire (hbm-space changed {@code EntityBalefire} itself, not only the antimatter mode): an explosion
 *       sound on one tick in five and a thunderclap on every tick until the crater is done;</li>
 *   <li>the custom nuke's antimatter stage spawns it with the rainbow cloud ({@code startFacAnti}, Torex type 2),
 *       and antimatter arms without the 50-nuke floor; a dropped antimatter cell blasts at 5 (1.7: 3); antimatter in a
 *       matter barrel is an {@code ExplosionVNT(5).makeAmat()} at the barrel's corner.</li>
 * </ul>
 * Not ported: the barrel's {@code IOverpressurable} antimatter/aschrab blast ({@code hasExploded} is never set in
 * hbm-space, dead code) and the {@code particle_amat} drop blast ({@code particle_amat} is a plain {@code Item}, so
 * {@code ItemDrop.onEntityItemUpdate} never runs for it).
 */
public final class AntimatterBalefire {

    /** The crater variant name, saved on the balefire entity. */
    public static final Identifier ID = Ids.of(NtmSpace.MOD_ID, "antimatter");

    private AntimatterBalefire() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        ExplosionBalefire.registerVariant(ID, new Crater());
        EntityBalefire.TICK.register(AntimatterBalefire::tick);
        // hbm-space NukeCustom: bf at the (already centred) position + 0.5, startFacAnti five blocks up
        ExplosionNukeCustom.ANTIMATTER.register((level, x, y, z, amat) -> {
            spawn(level, x + 0.5, y + 0.5, z + 0.5, (int) amat);
            cloud(level, x + 0.5, y + 5, z + 0.5, amat);
            return true;
        });
        // hbm-space TileEntityBarrel.checkFluidInteraction: new ExplosionVNT(world, xCoord, yCoord, zCoord, 5).makeAmat()
        TileEntityBarrel.ANTIMATTER_BLAST.register((level, pos, type) -> {
            new ExplosionVNT(level, pos.getX(), pos.getY(), pos.getZ(), 5F).makeAmat().explode();
            return true;
        });
        // hbm-space TileEntityNukeCustom: "//if(nuke < 50) amat = 0;"
        TileEntityNukeCustom.amatNukeFloor = 0F;
        // hbm-space ItemDrop: cell_antimatter ExplosionVNT 5F (1.7 3F)
        ItemCell.antimatterDropStrength = 5F;
    }

    /** Spawns an antimatter balefire of {@code range} at the position (1.7 {@code new EntityBalefire(w).antimatter()}). */
    public static EntityBalefire spawn(Level level, double x, double y, double z, int range) {
        EntityBalefire bf = new EntityBalefire(ModNtmEntities.BALEFIRE.get(), level);
        bf.setPos(x, y, z);
        bf.destructionRange = range;
        bf.variant = ID;
        level.addFreshEntity(bf);
        return bf;
    }

    /** hbm-space {@code EntityNukeTorex.startFacAnti}: type 2, scale {@code clamp(scale * 0.01, 0.5, 5)} (no square root). */
    public static EntityNukeTorex cloud(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = EntityNukeTorex.statFacExact(level, x, y, z, Mth.clamp(scale * 0.01F, 0.5F, 5F), 2);
        level.addFreshEntity(torex);
        return torex;
    }

    /** 1.7 {@code Material.craftedSnow/snow/ice/packedIce}. */
    static boolean isSnowOrIce(BlockState state) {
        return state.is(BlockTags.ICE) || state.is(BlockTags.SNOW) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }

    /** hbm-space {@code ExplosionBalefire.breakColumn} with {@code antimatter} set. */
    static final class Crater implements ExplosionBalefire.Variant {
        @Override
        public boolean keep(ExplosionBalefire explosion, BlockPos pos, BlockState state) {
            if (!isSnowOrIce(state)) return false;
            // hbm-space: ParticleUtil.spawnGasFlame(world, pX, y, pZ, x, y, z) - the column offset and the ABSOLUTE height
            // as the flame's motion (kept: a fast upward jet), no scale, 150 blocks. hbm-space then "continue"d without
            // moving down, which loops forever on that block (a server hang); the port flames once and walks on, leaving
            // the snow or ice in place as hbm-space did.
            CompoundTag data = new CompoundTag();
            data.putDouble("moX", pos.getX() - explosion.posX);
            data.putDouble("moY", pos.getY());
            data.putDouble("moZ", pos.getZ() - explosion.posZ);
            HbmEffectNT.GasFlame.send(explosion.level, pos.getX(), pos.getY(), pos.getZ(), data, 150);
            return true;
        }

        @Override
        public boolean balefire() {
            return false;
        }

        @Override
        public void floor(ExplosionBalefire explosion, int x, int depth, int z, int dist, int layer) {
            // hbm-space: if(rand.nextInt(dist) == 0 && antimatter) setBlock(pX, depth, pZ, volcanic_lava_block) - at the
            // floor, not at the layer, once per layer
            if (explosion.level.getRandom().nextInt(dist) == 0) {
                explosion.level.setBlock(new BlockPos(x, depth, z),
                        com.hbm.blocks.ModFluidNetBlocks.volcanic_lava_block.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** hbm-space {@code EntityBalefire.onUpdate}'s additions, after the spiral and before the blast damage. */
    static void tick(EntityBalefire bf, boolean finished) {
        if (!(bf.level() instanceof ServerLevel level)) return;
        if (ID.equals(bf.variant) && bf.tickCount < 10) {
            radiate(level, bf, 40000F * bf.destructionRange, bf.destructionRange * 2);
        }
        if (bf.getRandom().nextInt(5) == 0) {
            level.playSound(null, bf.getX(), bf.getY(), bf.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                    10000.0F, 0.8F + bf.getRandom().nextFloat() * 0.2F);
        }
        if (!finished) {
            level.playSound(null, bf.getX(), bf.getY(), bf.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                    10000.0F, 0.8F + bf.getRandom().nextFloat() * 0.2F);
        }
    }

    /** hbm-space {@code EntityBalefire.radiate} (float arithmetic as in 1.7; it also counts {@code age} up once more). */
    public static void radiate(ServerLevel level, EntityBalefire bf, float rads, double range) {
        double px = bf.getX(), py = bf.getY(), pz = bf.getZ();
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(px, py, pz, px, py, pz).inflate(range));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (LivingEntity e : entities) {
            double vx = e.getX() - px, vy = (e.getY() + e.getEyeHeight()) - py, vz = e.getZ() - pz;
            double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (len < 1.0E-4D) {
                vx = 0; vy = 0; vz = 0; // 1.7 Vec3.normalize of a zero vector is the zero vector
            } else {
                vx /= len; vy /= len; vz /= len;
            }
            float res = 0;
            for (int i = 1; i < len; i++) {
                pos.set(Mth.floor(px + vx * i), Mth.floor(py + vy * i), Mth.floor(pz + vz * i));
                res += level.getBlockState(pos).getBlock().getExplosionResistance();
            }
            if (res < 1) res = 1;
            float eRads = rads;
            eRads /= res;
            eRads /= (float) (len * len);
            ContaminationUtil.contaminate(e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);
            if (eRads >= 500 && !(e instanceof Player player && player.isCreative())) {
                e.setHealth(0);
                ExplosionLarge.spawnShock(level, e.getX(), e.getY(), e.getZ(), 10, 10);
                // 1.7 setDead: a mob simply vanishes; a player dies (a removed player would be a broken session)
                if (e instanceof Player) e.die(level.damageSources().generic());
                else e.discard();
            }
        }
        bf.age++;
    }
}
