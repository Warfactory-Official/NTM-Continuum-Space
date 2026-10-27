package com.ntmspace.tileentity.dyson;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ISatChip;
import com.hbm.lib.ForgeDirection;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.ntmspace.dyson.CustomDamageHandlerDyson;
import com.ntmspace.dyson.DysonConverter;
import com.ntmspace.dyson.DysonConverters;
import com.ntmspace.registry.DysonContent;
import com.ntmspace.satellite.SatelliteDysonRelay;
import com.ntmspace.trait.CBT_Dyson;
import dev.golem.core.world.Compat;
import dev.golem.core.world.LightLevels;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * hbm-space's {@code TileEntityDysonReceiver}: connects to a Dyson swarm by its ID-chip and, while the sun shines on
 * it (or a Dyson relay satellite of that frequency is in orbit) and nothing taller than ten blocks above it stands in
 * the 7 x 7 columns around it, fires the swarm's energy as a beam out of its back, nine to 23 blocks, one block up.
 * Each tick the first converter the beam meets (from the eleventh block on) takes
 * {@code getEnergyOutput(members) / consumers}; anything solid or any other block entity in the way is blown up
 * (a size 3 fire explosion) and stops the beam; any living thing in the beam is hit by a size 3 explosion whose
 * damage is {@code energy / 1000}.
 *
 * <p>"Gompertz is a funne name": the output is {@code Long.MAX_VALUE / 10 * exp(-32 exp(-1.3 members / 1024)) / 20} HE
 * per tick, shared between every receiver on the swarm (its consumers, counted per tick by {@link CBT_Dyson}).
 *
 * <p>1.7's dummy blocks had light opacity 0, so a receiver's own tower and dish neither shaded its core nor counted in
 * {@code getHeightValue}. HBM's multiblocks have opacity 0 too, so the sky light at the core
 * is the stored one; 1.7's height value (one above the highest block with any opacity) is a scan, since modern height
 * maps track other things.
 */
public class TileEntityDysonReceiver extends TileEntityMachineBase {

    // Connects to a dyson swarm via ID, receiving energy during the day
    // also receives energy at night if a satellite relay is in orbit around the planet

    // The energy received is fired as a violently powerful beam,
    // converters can collect this beam and turn it into HE/TU or used for analysis, crafting, etc.

    public boolean isReceiving;
    public int swarmId;
    public int swarmCount;
    public int swarmConsumers;
    public int beamLength;

    private AudioWrapper audio;

    /** Client effects (the flares over the dish), installed by the client. */
    public static volatile java.util.function.Consumer<TileEntityDysonReceiver> CLIENT_EFFECTS = null;

    public TileEntityDysonReceiver(BlockPos pos, BlockState state) {
        super(DysonContent.RECEIVER_BE.get(), pos, state, 1);
    }

    // Sun luminosity is 4*10^26, which we can't represent in any Java integer primitive
    // therefore the upper bound for power generation is higher than a FEnSU, effectively
    // reality doesn't provide any interesting solutions that make the system engaging to use
    // so we're going to build our own power curve.
    // We need to encourage players to build large swarms, so single satellites must suck but together they produce enormous power
    // Gompertz is a funne name
    public static long getEnergyOutput(int swarmCount) {
        double adjustedDensity = (double) swarmCount / 1024.0D;
        long maxOutput = Long.MAX_VALUE / 10;
        double b = 32.0D;
        double c = 1.3D;
        double gompertz = Math.exp(-b * Math.exp(-c * adjustedDensity));
        return (long) (maxOutput * gompertz) / 20;
    }

    @Override
    public String getDefaultName() {
        return "container.machineDysonReceiver";
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel) swarmId = ISatChip.getFreqS(inventory.getStackInSlot(0));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDysonReceiver te) {
        if (!(level instanceof ServerLevel server)) return;
        int swarmId = ISatChip.getFreqS(te.inventory.getStackInSlot(0));
        Satellite sat = SatelliteSavedData.get(server, pos).getSatFromFreq(swarmId);
        int sun = skyLight(level, pos) - level.getSkyDarken() - 11;
        te.receive(level, swarmId, sat instanceof SatelliteDysonRelay || sun > 0, isOccluded(level, pos));
    }

    /** 1.7 {@code getSavedLightValue(Sky, x, y, z)} at the core. */
    public static int skyLight(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.SKY, pos);
    }

    /**
     * 1.7: {@code worldObj.getHeightValue(x + dx, z + dz) > yCoord + 10} for any dx, dz in -3..3, the height value being
     * one above the highest block with any light opacity.
     */
    public static boolean isOccluded(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() + x, pos.getZ() + z);
                for (int y = top - 1; y >= pos.getY() + 10; y--) {
                    at.set(pos.getX() + x, y, pos.getZ() + z);
                    if (blocksLight17(level, at)) return true;
                }
            }
        }
        return false;
    }

    /** 1.7 light opacity above 0 (multiblock cells have none, as 1.7's {@code BlockDummyable}s). */
    private static boolean blocksLight17(Level level, BlockPos pos) {
        return LightLevels.opacity(level, pos) > 0;
    }

    /**
     * One 1.7 server {@code updateEntity} past the chip and the light queries: {@code swarmId} is the chip's frequency,
     * {@code lit} "the sun shines or a relay is in orbit", {@code occluded} the 7 x 7 height check.
     */
    public void receive(Level level, int swarmId, boolean lit, boolean occluded) {
        ForgeDirection dir = MachineCore.facing(getBlockState()).getOpposite();
        this.swarmId = swarmId;

        swarmCount = CBT_Dyson.count(level, swarmId);
        swarmConsumers = CBT_Dyson.consumers(level, swarmId);

        isReceiving = lit && swarmId > 0 && !occluded && swarmCount > 0 && swarmConsumers > 0;

        if (isReceiving) {
            long energyOutput = getEnergyOutput(swarmCount) / swarmConsumers;
            int maxLength = 24;
            int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();

            beamLength = maxLength;
            for (int i = 9; i < maxLength; i++) {
                int x = xCoord + dir.offsetX * i;
                int y = yCoord + 1;
                int z = zCoord + dir.offsetZ * i;
                BlockPos p = new BlockPos(x, y, z);

                BlockState state = level.getBlockState(p);

                // two block gap minimum
                boolean detonate = true;
                BlockEntity te = null;
                if (i > 10) {
                    if (state.getBlock() instanceof BlockMultiblockCore) {
                        te = MultiblockSurface.coreEntity(level, p);
                    } else {
                        te = level.getBlockEntity(p);
                    }

                    DysonConverter converter = DysonConverters.of(te);
                    if (converter != null) {
                        detonate = !converter.provideEnergy(x, y, z, energyOutput);
                    }
                }

                if (Compat.isSolidRender(state, level, p) || te != null) {
                    if (detonate) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);

                        ExplosionVNT vnt = new ExplosionVNT(level, x, y, z, 3, null);
                        vnt.setBlockAllocator(new BlockAllocatorStandard());
                        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
                        vnt.setEntityProcessor(new EntityProcessorStandard().allowSelfDamage());
                        vnt.setPlayerProcessor(new PlayerProcessorStandard());
                        vnt.setSFX(new ExplosionEffectStandard());
                        vnt.explode();
                    }

                    beamLength = i;
                    break;
                }
            }

            double blx = Math.min(xCoord, xCoord + dir.offsetX * beamLength) + 0.2;
            double bux = Math.max(xCoord, xCoord + dir.offsetX * beamLength) + 0.8;
            double bly = Math.min(yCoord, 1 + yCoord + dir.offsetY * beamLength) + 0.2;
            double buy = Math.max(yCoord, 1 + yCoord + dir.offsetY * beamLength) + 0.8;
            double blz = Math.min(zCoord, zCoord + dir.offsetZ * beamLength) + 0.2;
            double buz = Math.max(zCoord, zCoord + dir.offsetZ * beamLength) + 0.8;

            List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(blx, bly, blz, bux, buy, buz));

            for (LivingEntity entity : list) {
                ExplosionVNT vnt = new ExplosionVNT(level, entity.getX() - dir.offsetX, entity.getY() + 1.5, entity.getZ() - dir.offsetZ, 3, null);
                vnt.setBlockAllocator(new BlockAllocatorStandard());
                vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
                vnt.setEntityProcessor(new EntityProcessorStandard().withDamageMod(new CustomDamageHandlerDyson(energyOutput)));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectStandard());
                vnt.explode();
            }
        }

        networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityDysonReceiver te) {
        ForgeDirection dir = MachineCore.facing(state).getOpposite();
        if (te.isReceiving) {
            if (te.audio == null) {
                te.audio = AudioSystem.getLoopedSound(DysonContent.DYSON_BEAM.get(), SoundSource.BLOCKS,
                        pos.getX() + dir.offsetX * 8, pos.getY(), pos.getZ() + dir.offsetZ * 8, 0.75F, 20F, 1.0F, 20);
                if (te.audio != null) te.audio.startSound();
            }

            if (te.audio != null) {
                te.audio.keepAlive();
                te.audio.updatePitch(0.85F);
            }

            if (level.getRandom().nextInt(10) == 0 && CLIENT_EFFECTS != null) CLIENT_EFFECTS.accept(te);
        } else {
            te.stopAudio();
        }
    }

    private void stopAudio() {
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        stopAudio();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isReceiving);
        buf.writeInt(swarmId);
        buf.writeInt(swarmCount);
        buf.writeInt(swarmConsumers);
        buf.writeInt(beamLength);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isReceiving = buf.readBoolean();
        swarmId = buf.readInt();
        swarmCount = buf.readInt();
        swarmConsumers = buf.readInt();
        beamLength = buf.readInt();
    }

    /** 1.7 {@code runTests}: what 1, 2, 4 ... 4096 members produce, in HE/s. */
    public static String testTable() {
        StringBuilder out = new StringBuilder();
        for (int i = 1; i < 5000; i *= 2) {
            out.append(i).append(" dyson swarm members produces: ")
                    .append(com.hbm.util.BobMathUtil.getShortNumber(getEnergyOutput(i) * 20)).append("HE/s\n");
        }
        return out.toString();
    }
}
