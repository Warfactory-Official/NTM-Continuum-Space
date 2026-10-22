package com.ntmspace.entity.rocket;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.entity.logic.EntityChunkLoader;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ISatChip;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.saveddata.satellites.XSatelliteRegistry;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.OrbitalStation.StationState;
import com.ntmspace.orbit.SpaceTeleporter;
import com.ntmspace.orbit.StationData;
import com.ntmspace.orbit.StationDock;
import com.ntmspace.registry.RocketItems;
import com.ntmspace.registry.RocketSounds;
import com.ntmspace.rocket.DriveBody;
import com.ntmspace.rocket.ItemCustomRocket;
import com.ntmspace.rocket.ItemVOTVdrive;
import com.ntmspace.rocket.ItemVOTVdrive.Destination;
import com.ntmspace.rocket.RocketParts;
import com.ntmspace.rocket.RocketStruct;
import com.ntmspace.rocket.RocketStruct.RocketStage;
import com.ntmspace.rocket.RocketTarget;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import com.ntmspace.world.SolarSystem;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code EntityRideableRocket}: an assembled rocket on its way between bodies and stations, crewed when its
 * payload is a capsule ({@code rp_c_20}, the reusable pod {@code rp_pod_20}) and uncrewed for satellites and the station
 * core. A state machine ({@link RocketState}) drives it:
 *
 * <pre>
 * AWAITING --jump/uncrewed--> LAUNCHING --y 900--> TRANSFER (in orbit, while the "station" travels) --> LANDING/DOCKING
 * LANDING --ground--> LANDED (legs) or TIPPING (no legs, or water/lava) ; DOCKING --port--> LANDED/NEEDSFUEL
 * from a station: UNDOCKING --y 32--> TRANSFER or LANDING
 * </pre>
 *
 * <p>Port notes:
 * <ul>
 *   <li>1.7 extended {@code EntityMissileBaseNT}, whose physics the rocket overwrote every tick; only its movement step
 *       (1.7 {@code EntityThrowableNT}: a block ray from the position to {@code position + motion * motionMult}) and
 *       its chunk loading survive, both reproduced here.</li>
 *   <li>The data watcher (state, timer, drive and the rocket packed into short ids) is synched data here: the rocket
 *       travels as a {@code rocket_custom} stack, the drive as its stack, the pitch on its own (from 26.1 {@code xRot}
 *       clamps), the state by ordinal (a packet, never saved: saves name it).</li>
 *   <li>1.7 kept the rider in place with a sneak handler that zeroed the sneak; vanilla now dismounts first, so the
 *       rocket re-boards its pilot (1.7's own re-mount) and counts those attempts as 1.7's {@code forceExitTimer}.</li>
 *   <li>The rider's tilt with the rocket and the 12-block third-person camera are {@code RocketClient}'s (Golem
 *       {@code PlayerRenderHooks.POSE}, {@code CameraHooks.DISTANCE}).</li>
 * </ul>
 */
public class EntityRideableRocket extends CompatEntity implements StationDock, IRadarDetectableNT, com.hbm.interfaces.IEntityLookOverlay {

    public enum RocketState {
        AWAITING,   // Prepped for launch, once mounted will transition to launching
        LAUNCHING,  // Ascending through the atmosphere up to the target altitude, at which point it'll teleport to the target body
        LANDING,    // Descending onto the target location
        LANDED,     // Landed on the target, will not launch until the player activates the rocket, at which point it'll transition back to AWAITING
        TIPPING,    // tipping culture is a burden on modern society
        DOCKING,    // Arriving at an orbital station
        UNDOCKING,  // Leaving an orbital station
        NEEDSFUEL,  // Needs fuel, once fueled it will transition to AWAITING
        TRANSFER,   // Flying in space!
    }

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIMER = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DRIVE = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> ROCKET = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.FLOAT);

    public @Nullable ItemStack navDrive;

    public @Nullable EntityRideableRocketDummy capDummy;

    private int stateTimer = 0;

    /** 1.7 {@code sizeSet}: false until the first server tick after construction or a load. */
    private boolean sizeSet = false;
    public int decoupleTimer = 0;
    public int shroudTimer = 0;

    public int forceExitTimer = 0;

    private double rocketVelocity = 0;

    private @Nullable AudioWrapper audio;

    private RocketState lastState = RocketState.AWAITING;

    private boolean willExplode = false;

    private int satFreq = 0;

    private @Nullable TileEntityOrbitalStation targetPort;

    private @Nullable Destination destinationOverride; // for pod recalls, will ignore the current drive if set

    public @Nullable LivingEntity thrower;

    /** A screen's preview of a rocket (never added to a level): drawn deployed, without the pod's own renderer. */
    public boolean preview;

    /** 1.7's rocket pitch (0 upright, 90 lying), the server's value; the client interpolates it. */
    private float pitch;
    private float pitchO;

    /** The cached rocket of the synched stack (rebuilt when the stack changes). */
    private @Nullable ItemStack rocketStackFor;
    private @Nullable RocketStruct rocketCache;

    /** Client: where the rocket was when the contrail last ran (1.7 lastTickPos). */
    private double contrailX, contrailY, contrailZ;
    private boolean contrailStarted;

    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 1);

    public EntityRideableRocket(EntityType<? extends EntityRideableRocket> type, Level level) {
        super(type, level);

    }

    /** 1.7 {@code EntityRideableRocket(world, x, y, z, stack)}. */
    public static EntityRideableRocket create(Level level, float x, float y, float z, ItemStack stack) {
        EntityRideableRocket rocket = new EntityRideableRocket(RocketEntities.RIDEABLE_ROCKET.get(), level);
        rocket.setPos(x, y, z);
        RocketStruct struct = ItemCustomRocket.get(stack);
        if (struct == null) struct = new RocketStruct(RocketParts.get(RocketParts.CAPSULE));
        rocket.satFreq = ISatChip.getFreqS(stack);
        rocket.setRocket(struct);
        return rocket;
    }

    public EntityRideableRocket withProgram(ItemStack stack) {
        this.navDrive = stack.copy();
        return this;
    }

    public EntityRideableRocket launchedBy(@Nullable LivingEntity entity) {
        this.thrower = entity;
        return this;
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(STATE, RocketState.AWAITING.ordinal());
        data.define(TIMER, 0);
        data.define(DRIVE, ItemStack.EMPTY);
        data.define(ROCKET, ItemStack.EMPTY);
        data.define(PITCH, 0F);
    }

    // ---------------------------------------------------------------------------------------- flight

    public void beginLandingSequence(RocketTarget from, RocketTarget to) {
        setDeltaMovement(Vec3.ZERO);

        RocketStruct rocket = getRocket();
        boolean expendStage = rocket.stages.size() > 0;
        if (getState() == RocketState.UNDOCKING && from.body() == to.body()) expendStage = false;

        if (expendStage) {
            rocket.stages.remove(0);
            setRocket(rocket);
        }

        setState(RocketState.LANDING);

        Destination destination = getDestination();
        MinecraftServerAccess server = MinecraftServerAccess.of(level());

        if (destination != null && server != null) {
            int x = destination.x;
            int y = 800;
            int z = destination.z;

            ResourceKey<Level> targetLevel = destination.body.level();

            Entity rider = getFirstPassenger();

            if (canRide()) {
                if (destination.body == DriveBody.ORBIT) {
                    setState(RocketState.DOCKING);

                    // Place the station in the middle of the zone, where the docking ring will always be
                    x = x * OrbitalStation.STATION_SIZE + (OrbitalStation.STATION_SIZE / 2);
                    y = 0;
                    z = z * OrbitalStation.STATION_SIZE + (OrbitalStation.STATION_SIZE / 2);
                }

                if (!level().dimension().equals(targetLevel)) {
                    SpaceTeleporter.teleport(rider instanceof Player ? rider : this, targetLevel, x + 0.5D, y, z + 0.5D, false);
                } else {
                    setPos(x + 0.5D, y, z + 0.5D);
                }

                // After a successful warp, spawn in a station core if one doesn't yet exist
                if (destination.body == DriveBody.ORBIT) {
                    ServerLevel orbit = server.server().getLevel(Orbit.ORBIT);
                    if (orbit != null) OrbitalStation.spawn(orbit, x, z);
                }
            } else {
                if (rocket.capsule != null && rocket.capsule.isSatellite() && XSatelliteRegistry.typeFor(new ItemStack(rocket.capsule.item())) != null
                        && destination.body != DriveBody.ORBIT) {
                    ServerLevel target = server.server().getLevel(targetLevel);
                    if (target != null) {
                        ItemStack stack = new ItemStack(rocket.capsule.item());
                        XSatelliteRegistry.orbit(target, stack, satFreq, getX(), getY(), getZ());
                    }
                } else if (rocket.capsule != null && RocketParts.STATION_CORE.equals(rocket.capsule.id())) {
                    // We mark the station as travellable, but we don't actually add the station until the player travels to it
                    OrbitalStation.addStation(server.server(), x, z, Orbit.bodyOf(level()));

                    // 1.7 granted achDriveFail to a survival player whose drive was not copied first: the advancement
                    // tree is not part of this port (left for the advancements pass)
                }

                discard();
            }
        }
    }

    public void beginCelestialTransfer(RocketTarget from, RocketTarget to) {
        setDeltaMovement(Vec3.ZERO);

        setState(RocketState.TRANSFER);

        RocketStruct rocket = getRocket();
        MinecraftServerAccess server = MinecraftServerAccess.of(level());
        if (server == null || from.body() == null || to.body() == null) return;

        StationData data = StationData.get(server.server());
        OrbitalStation station = data.addStation(from.body());

        int size = 10;
        double distance = SolarSystem.calculateDistanceBetweenTwoBodies(level().getGameTime(), from.body(), to.body());
        float thrust = rocket.getThrust();

        station.setState(StationState.TRANSFER, OrbitalStation.calculateTransferTime(distance, size, thrust));
        station.orbiting = from.body();
        station.target = to.body();
        data.setDirty();

        Entity rider = getFirstPassenger();

        int x = station.dX * OrbitalStation.STATION_SIZE + (OrbitalStation.STATION_SIZE / 2);
        int y = 128;
        int z = station.dZ * OrbitalStation.STATION_SIZE + (OrbitalStation.STATION_SIZE / 2);

        if (!Orbit.inOrbit(level())) {
            SpaceTeleporter.teleport(rider instanceof Player ? rider : this, Orbit.ORBIT, x + 0.5D, y, z + 0.5D, false);
        } else {
            setPos(x + 0.5D, y, z + 0.5D);
        }
    }

    public void attemptLaunch() {
        RocketTarget from = RocketTarget.of(level(), (int) getX(), (int) getZ());
        RocketTarget to = getTarget();

        RocketState transitionTo = from.inOrbit() ? RocketState.UNDOCKING : RocketState.LAUNCHING;

        // Will only launch if the rocket has sufficient fuel
        if (getRocket().hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit())) {
            setState(transitionTo);
        }
    }

    public boolean canExitCapsule() {
        RocketState state = getState();
        return state != RocketState.LANDING
                && state != RocketState.LAUNCHING
                && state != RocketState.DOCKING
                && state != RocketState.UNDOCKING
                && state != RocketState.TRANSFER;
    }

    @Override
    public void tick() {
        super.tick();
        RocketState state = getState();

        if (!level().isClientSide()) {
            serverMove();
            if (isRemoved()) return;
            state = getState();

            if (!sizeSet) {
                sizeSet = true;
                if (state == RocketState.LANDED || state == RocketState.AWAITING || state == RocketState.NEEDSFUEL) redock();
            }

            Entity rider = getFirstPassenger();

            setYRot(-90.0F);

            if (navDrive != null && navDrive.getItem() instanceof ItemVOTVdrive) {
                ItemVOTVdrive.getTarget(navDrive, level());
                setDrive(navDrive);
            }

            if (thrower == null && rider instanceof LivingEntity living) {
                thrower = living;
            }

            // If it's a satellite launcher, launch immediately
            if (state == RocketState.AWAITING && ((rider instanceof LivingEntity living && isJumping(living)) || !canRide())) {
                attemptLaunch();

                thrower = rider instanceof LivingEntity living ? living : null;
            }

            if (thrower != null && rider == null && !canExitCapsule() && forceExitTimer < 60 && thrower.isAlive()
                    && thrower.level() == level()) {
                // 1.7's sneak handler counted the ticks a pilot tried to leave a rocket in motion; vanilla lets go first
                forceExitTimer++;
                // forced, as 1.7 mountEntity: vanilla's boarding cooldown after the dismount would refuse it
                dev.golem.core.world.Teleports.remount(thrower, this);
            }

            if (state == RocketState.LAUNCHING) {
                if (isReusable()) {
                    pitch = Mth.clamp((stateTimer - 60) * 0.3F, 0.0F, 45.0F);
                    if (rocketVelocity < 4) rocketVelocity += Mth.clamp(stateTimer / 120D * 0.05D, 0, 0.05);
                } else {
                    double acceleration = stateTimer / 120D;
                    pitch = Mth.clamp((stateTimer - 80) * 0.3F, 0.0F, 45.0F);
                    if (rocketVelocity < 4) rocketVelocity += Mth.clamp(acceleration * acceleration * 0.05D, 0, 0.05);
                }
                // 1.7 forced the pitch to 0 on OptiFine clients ("FUCK OPTIFINE"): not ported
            } else if (state == RocketState.LANDING) {
                double targetHeight = heightValue((int) getX(), (int) getZ());
                pitch = 0;

                if (isReusable()) {
                    rocketVelocity = Mth.clamp((targetHeight - getY()) * 0.01, -1.0, -0.005);
                } else {
                    rocketVelocity = Mth.clamp((targetHeight - getY()) * 0.005, -0.5, -0.005);
                }

                if (destinationOverride == null && navDrive != null && navDrive.getItem() instanceof ItemVOTVdrive) {
                    Destination destination = ItemVOTVdrive.getDestination(navDrive);

                    // Check if we're about to land on top of another rocket and adjust accordingly
                    AABB bb = getBoundingBox();
                    bb = new AABB(bb.minX, targetHeight, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
                    if (!level().getEntities(this, bb, entity -> entity instanceof EntityRideableRocket).isEmpty()) {
                        int distance = random.nextBoolean() ? -5 : 5;
                        if (random.nextBoolean()) {
                            destination.x += distance;
                        } else {
                            destination.z += distance;
                        }
                        ItemVOTVdrive.setCoordinates(navDrive, destination.x, destination.z);
                    }

                    setPos(destination.x + 0.5D, getY(), destination.z + 0.5D);
                }
            } else if (state == RocketState.TIPPING) {
                float tipTime = (float) stateTimer * 0.1F;
                pitch = tipTime * tipTime;

                if (pitch > 90) {
                    pitch = 90;

                    if (willExplode) {
                        dropNDie(null);
                        ExplosionLarge.explode(level(), null, getX(), getY(), getZ(), 5, true, false, true);
                        Vec3 motion = getDeltaMovement();
                        ExplosionLarge.spawnShrapnelShower(level(), getX(), getY(), getZ(), motion.x, motion.y, motion.z, 15, 0.075);
                        // 1.7 played "hbm:entity.pipefail", which has no sounds.json entry in hbm-space: silent
                    }
                }

                rocketVelocity = 0;
            } else if (state == RocketState.DOCKING) {
                // we have to wait for docking ports and their associated entities to load
                // waiting for loading using timers is bad, so maybe refactor this
                if (stateTimer > 20) {
                    rocketVelocity = 0.1;
                    pitch = 0;

                    if (targetPort == null) targetPort = OrbitalStation.getPort(level(), (int) getX(), (int) getZ());

                    // Just in case no ports have loaded in time, do nothing until they have
                    if (targetPort != null) {
                        BlockPos port = targetPort.getBlockPos();
                        setPos(port.getX() + 0.5D, getY(), port.getZ() + 0.5D);

                        targetPort.despawnRocket();
                        targetPort.reservePort();

                        if (getY() + getBbHeight() > port.getY() + 1.5D) {
                            setState(isReusable() ? RocketState.NEEDSFUEL : RocketState.LANDED);
                            destinationOverride = null;
                            setPos(getX(), port.getY() + 1.5D - getBbHeight(), getZ());

                            targetPort.dockRocket(this);
                            targetPort = null;
                        }
                    } else {
                        rocketVelocity = 0;
                    }
                } else {
                    rocketVelocity = 0;
                    pitch = 0;
                }
            } else if (state == RocketState.UNDOCKING) {
                rocketVelocity = -0.1;
                pitch = 0;
            } else if (state == RocketState.TRANSFER) {
                rocketVelocity = 0;
                pitch = 90;
                forceExitTimer = 0; // never allow exits in transfers under any circumstances!

                MinecraftServerAccess server = MinecraftServerAccess.of(level());
                if (server != null) {
                    OrbitalStation station = OrbitalStation.getStationFromPosition(server.server(), (int) getX(), (int) getZ());
                    station.update(level());

                    if (station.getUnscaledProgress(0) > 0.99 || station.state == StationState.ARRIVING) {
                        RocketTarget from = RocketTarget.of(level(), (int) getX(), (int) getZ());
                        RocketTarget to = getTarget();

                        beginLandingSequence(from, to);

                        StationData.get(server.server()).removeStation(station);
                    }

                    if (rider instanceof ServerPlayer player) RocketStationSync.send(player, station);
                }
            } else {
                rocketVelocity = 0;
                pitch = 0;
            }

            if (getState() == RocketState.LAUNCHING) {
                setDeltaMovement(direction(pitch, getYRot(), rocketVelocity));
            } else {
                setDeltaMovement(0, rocketVelocity, 0);
            }

            if (getState() == RocketState.LANDING) {
                BlockPos feet = BlockPos.containing(getX(), getY(), getZ());
                var fluid = level().getFluidState(feet);

                if (!fluid.isEmpty()) setState(RocketState.TIPPING);
                if (fluid.is(FluidTags.LAVA)) {
                    setRemainingFireTicks(15 * 20);
                    willExplode = true;
                }
            }

            state = getState();
            if (!isRemoved() && ((state == RocketState.LAUNCHING && getY() > 900) || (state == RocketState.UNDOCKING && getY() < 32))) {
                RocketTarget from = RocketTarget.of(level(), (int) getX(), (int) getZ());
                RocketTarget to = getTarget();

                if (!canRide() || from.body() == to.body()) {
                    beginLandingSequence(from, to);
                } else {
                    beginCelestialTransfer(from, to);
                }
            }

            if (!isRemoved()) {
                if (getBbHeight() > 8) {
                    double offset = getBbHeight() - 4;
                    if (capDummy == null || capDummy.isRemoved()) {
                        capDummy = new EntityRideableRocketDummy(RocketEntities.RIDEABLE_ROCKET_DUMMY.get(), level());
                        capDummy.setParent(this);
                        capDummy.setPos(getX(), getY() + offset, getZ());
                        level().addFreshEntity(capDummy);
                    } else {
                        capDummy.setPos(getX(), getY() + offset, getZ());
                    }
                } else if (capDummy != null) {
                    capDummy.discard();
                    capDummy = null;
                }
            }

            entityData.set(PITCH, pitch);
        } else {
            pitchO = pitch;
            pitch = entityData.get(PITCH);

            // ON state transitions
            if (state != lastState) {
                if (state == RocketState.LAUNCHING) {
                    AudioWrapper ignition = AudioSystem.getLoopedSound(RocketSounds.ROCKET_IGNITION.get(), SoundSource.PLAYERS,
                            (float) getX(), (float) getY(), (float) getZ(), 1.0F, 250.0F, 1.0F, 5);
                    if (ignition != null) {
                        ignition.setDoesRepeat(false);
                        ignition.startSound();
                    }
                }

                lastState = state;
                stateTimer = 0;
            } else {
                // We can't start audio loops at the same time as playing a sound, for some reason
                if (state == RocketState.LAUNCHING
                        || (state == RocketState.LANDING && getDeltaMovement().y > -0.4)
                        || (state == RocketState.TRANSFER && OrbitalStation.clientStation.getUnscaledProgress(0) <= 0.15)) {
                    if (audio == null || !audio.isPlaying()) {
                        var sound = getRocket().stages.size() <= 1 ? RocketSounds.ROCKET_FLY_LIGHT.get() : RocketSounds.ROCKET_FLY_HEAVY.get();
                        audio = AudioSystem.getLoopedSound(sound, SoundSource.PLAYERS, (float) getX(), (float) getY(), (float) getZ(),
                                1.0F, 250.0F, 1.0F, 5);
                        if (audio != null) audio.startSound();
                    }

                    if (audio != null) {
                        audio.updatePosition((float) getX(), (float) getY(), (float) getZ());
                        audio.keepAlive();
                    }
                } else {
                    if (audio != null) {
                        audio.stopSound();
                        audio = null;
                    }
                }
            }

            if (state == RocketState.TRANSFER) {
                OrbitalStation station = OrbitalStation.clientStation;
                station.update(level());

                if (station.getUnscaledProgress(0) > 0.2) {
                    if (decoupleTimer == 0) {
                        AudioWrapper decouple = AudioSystem.getLoopedSound(RocketSounds.ROCKET_STAGE.get(), SoundSource.PLAYERS,
                                (float) getX(), (float) getY(), (float) getZ(), 0.5F, 250.0F, 0.9F + random.nextFloat() * 0.2F, 40);
                        if (decouple != null) {
                            decouple.setDoesRepeat(false);
                            decouple.startSound();
                        }
                    }

                    if (decoupleTimer == 100 && getRocket().stages.size() > 1) {
                        AudioWrapper decouple = AudioSystem.getLoopedSound(RocketSounds.ROCKET_STAGE.get(), SoundSource.PLAYERS,
                                (float) getX(), (float) getY(), (float) getZ(), 0.5F, 250.0F, 1.2F + random.nextFloat() * 0.2F, 40);
                        if (decouple != null) {
                            decouple.setDoesRepeat(false);
                            decouple.startSound();
                        }
                    } else if (decoupleTimer > 100) {
                        shroudTimer++;
                    }

                    decoupleTimer++;
                }
            } else {
                decoupleTimer = 0;
                shroudTimer = 0;
            }

            if (contrailStarted) spawnContrail();
            contrailX = getX();
            contrailY = getY();
            contrailZ = getZ();
            contrailStarted = true;
        }

        setStateTimer(++stateTimer);
    }

    /** 1.7 {@code EntityThrowableNT}'s step: a block ray along {@code motion * motionMult}, then the move. */
    private void serverMove() {
        chunkLoader.tick();

        double mult = motionMult();
        if (mult <= 0) return;

        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement().scale(mult));
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() != HitResult.Type.MISS) onImpact(hit);
        if (isRemoved()) return;

        // 1.7 moved on by the motion whatever the impact did (it zeroes the motion when it lands)
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x * mult, getY() + motion.y * mult, getZ() + motion.z * mult);
    }

    /** 1.7 {@code motionMult}: parked rockets do not move. */
    protected double motionMult() {
        RocketState state = getState();
        if (state == RocketState.AWAITING || state == RocketState.LANDED || state == RocketState.NEEDSFUEL) return 0;
        return 4;
    }

    /**
     * 1.7 {@code sizeSet}: on its first tick a parked rocket docks itself to the station port or launcher it stands on
     * (after a reload; the port forgets its craft).
     */
    private void redock() {
        BlockPos at = BlockPos.containing(getX(), getY() + getBbHeight() - 1.0D, getZ());
        BlockPos core = com.hbm.blocks.multiblock.MultiblockSurface.coreOfAny(level(), at);
        if (core == null) return;
        if (level().getBlockEntity(core) instanceof TileEntityOrbitalStation port) {
            port.dockRocket(this);
        } else {
            DOCK_TARGETS.forEach(target -> target.accept(level(), core, this));
        }
    }

    /** Other blocks a parked rocket docks to on load (the orbital station launcher): 1.7's {@code instanceof} chain. */
    public static final List<DockTarget> DOCK_TARGETS = new java.util.concurrent.CopyOnWriteArrayList<>();

    @FunctionalInterface
    public interface DockTarget {
        void accept(Level level, BlockPos core, EntityRideableRocket rocket);
    }

    /** The game time of the pilot's last jump key press (sent by the client, {@link RocketInputs}). */
    long pilotJumpTime = Long.MIN_VALUE;

    /** 1.7 {@code rider.isJumping}: the server learnt it from the vehicle input packet; here the client says so. */
    private boolean isJumping(LivingEntity entity) {
        return entity instanceof Player && level().getGameTime() - pilotJumpTime <= 1;
    }

    private double heightValue(int x, int z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** 1.7 {@code BobMathUtil.getDirectionFromAxisAngle(pitch - 90, 180 - yaw, length)}: the rocket's axis. */
    public static Vec3 direction(float pitch, float yaw, double length) {
        float p = pitch - 90.0F;
        float y = 180.0F - yaw;
        double ox = -Mth.sin(y / 180.0F * (float) Math.PI) * Mth.cos(p / 180.0F * (float) Math.PI) * length;
        double oz = Mth.cos(y / 180.0F * (float) Math.PI) * Mth.cos(p / 180.0F * (float) Math.PI) * length;
        double oy = -Mth.sin(p / 180.0F * (float) Math.PI) * length;
        return new Vec3(ox, oy, oz);
    }

    // ---------------------------------------------------------------------------------------- crew

    /** Does this rocket accept passengers (is a capsule). */
    public boolean canRide() {
        RocketStruct rocket = getRocket();
        return rocket.capsule != null && rocket.capsule.isApollo();
    }

    @Override
    public boolean isReusable() {
        RocketStruct rocket = getRocket();
        return rocket.capsule != null && RocketParts.POD.equals(rocket.capsule.id());
    }

    public void recallPod(Destination destination) {
        thrower = null; // REALLY FUCKED UP SHIT HAPPENING
        destinationOverride = destination;
        attemptLaunch();
    }

    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        if (!canRide()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level().isClientSide() && (getFirstPassenger() == null || getFirstPassenger() == player)) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return getFirstPassenger() == null ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** The capsule dummy's click and hit, forwarded (1.7 {@code parent.interactFirst} / {@code attackEntityFrom}). */
    InteractionResult onInteractDummy(Player player, InteractionHand hand) {
        return onInteract(player, hand);
    }

    boolean onHurtDummy(DamageSource source, float amount) {
        return onHurt(source, amount);
    }

    /** 1.7 {@code getMountedYOffset}. */
    public double getMountedYOffset() {
        if (isReusable()) return getBbHeight() - 2.5;
        return getBbHeight() - 3.0;
    }

    /**
     * 1.7 {@code updateRiderPosition}: along the rocket's axis by the mounted offset plus the rider's own y offset (a
     * player's {@code yOffset - 0.5}, eye-based in 1.7, hence the 1.62 down to the feet).
     */
    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        boolean player = passenger instanceof Player;
        double length = getMountedYOffset() + (player ? 1.62F - 0.5F : 0);
        Vec3 target = direction(pitch, getYRot(), length);
        moveFunction.accept(passenger, getX() + target.x, getY() + target.y - (player ? 1.62F : 0), getZ() + target.z);
    }

    /**
     * 1.7 dismounted a sneaking pilot from {@code minY + height} with the height set to 1 (then 2 blocks along +x so
     * nobody splats from tall rockets), or to the rocket's height + 1 in orbit.
     */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (Orbit.inOrbit(level())) return new Vec3(getX(), getY() + getBbHeight() + 1.0F, getZ());
        return new Vec3(getX() + 2, getY() + 1.0F, getZ());
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    protected void onImpact(BlockHitResult hit) {
        RocketState state = getState();
        if (state != RocketState.LANDING && state != RocketState.DOCKING) return;

        setDeltaMovement(Vec3.ZERO);

        if (state == RocketState.DOCKING) return;

        // Check for a landing gear, if we don't have one, topple over catastrophically
        RocketStruct rocket = getRocket();
        if (rocket.stages.size() > 0 && rocket.stages.get(0).fins == null) {
            setState(RocketState.TIPPING);
            willExplode = true;
        } else {
            setState(RocketState.LANDED);
        }

        destinationOverride = null;

        setPos(getX(), heightValue((int) getX(), (int) getZ()), getZ());
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        if (!level().isClientSide() && !isRemoved()) {
            if (getFirstPassenger() == null && source.getEntity() instanceof Player player) {
                // A pickaxe is required to break, unless it's just the capsule (or it has tipped over)
                RocketStruct rocket = getRocket();
                boolean bareCapsule = rocket.stages.size() == 0 && (rocket.capsule == null || !RocketParts.POD.equals(rocket.capsule.id()));
                if (bareCapsule || getState() == RocketState.TIPPING) {
                    dropNDie(source);
                } else {
                    ItemStack stack = player.getMainHandItem();
                    if (!stack.isEmpty() && stack.isCorrectToolForDrops(Blocks.STONE.defaultBlockState())) {
                        dropNDie(source);
                    }
                }
            }
        }
        return true;
    }

    public void dropNDie(@Nullable DamageSource source) {
        discard();

        // Drop the rocket itself, to be taken to a pad and refueled
        // unless it's just the capsule
        RocketStruct rocket = getRocket();
        if (rocket.stages.size() == 0) {
            if (rocket.capsule != null) spawnAtLocationCompat(new ItemStack(rocket.capsule.item()));
        } else {
            // 1.7 rebuilt the item from the data watcher, which carried no satellite frequency: the frequency is lost
            ItemStack stack = ItemCustomRocket.build(rocket, true);
            spawnAtLocationCompat(stack);
        }

        // Drop the drive if it is still present
        if (navDrive != null && !navDrive.isEmpty()) {
            spawnAtLocationCompat(navDrive);
        }
    }

    private void spawnAtLocationCompat(ItemStack stack) {
        if (level() instanceof ServerLevel server) spawnAtLocation(server, stack);
    }

    @Override
    public void remove(RemovalReason reason) {
        chunkLoader.clearChunkLoader();
        if (capDummy != null) capDummy.discard();
        super.remove(reason);
    }

    // ---------------------------------------------------------------------------------------- contrail (client)

    protected void spawnContrail() {
        RocketState state = getState();

        if (state == RocketState.AWAITING
                || state == RocketState.LANDED
                || (state == RocketState.LANDING && getDeltaMovement().y <= -0.4)
                || state == RocketState.DOCKING
                || state == RocketState.UNDOCKING
                || state == RocketState.NEEDSFUEL
                || (state == RocketState.TRANSFER && OrbitalStation.clientStation.getUnscaledProgress(0) > 0.15))
            return;

        double x = getX();
        double y = getY();
        double z = getZ();

        // Offset particles when travelling upwards, preventing them spawning inside the capsule at high speeds
        if (getY() - contrailY > 0) {
            x = contrailX;
            y = contrailY;
            z = contrailZ;
        }

        RocketStruct rocket = getRocket();
        if (rocket.stages.size() == 0) {
            if (state == RocketState.TIPPING) return;

            if (isReusable()) {
                RocketEffects.gasFlame(level(), x + 0.5, y, z, 0, -1, 0);
                RocketEffects.gasFlame(level(), x - 0.5, y, z, 0, -1, 0);
                RocketEffects.gasFlame(level(), x, y, z + 0.5, 0, -1, 0);
                RocketEffects.gasFlame(level(), x, y, z - 0.5, 0, -1, 0);
            } else {
                double r = rocket.capsule != null ? rocket.capsule.bottomRadius() * 0.5 : 1.0;
                RocketEffects.gasFlame(level(), x + r, y, z + r, 0.25, -0.75, 0.25);
                RocketEffects.gasFlame(level(), x - r, y, z + r, -0.25, -0.75, 0.25);
                RocketEffects.gasFlame(level(), x + r, y, z - r, 0.25, -0.75, -0.25);
                RocketEffects.gasFlame(level(), x - r, y, z - r, -0.25, -0.75, -0.25);
            }

            double groundHeight = heightValue((int) x, (int) z);
            double distanceToGround = y - groundHeight;
            if (distanceToGround < 10) {
                RocketEffects.shock(level(), x, groundHeight + 0.5, z, 1 + random.nextInt(3), 1 + random.nextGaussian());
            }

            return;
        }

        RocketStage stage = rocket.stages.get(0);

        // the fuck is a contraol bob
        if (state == RocketState.LANDING) {
            RocketEffects.gasFlame(level(), x, y, z, 0.0, -1.0, 0.0);

            double groundHeight = heightValue((int) x, (int) z);
            double distanceToGround = y - groundHeight;
            if (distanceToGround < 10) {
                RocketEffects.shock(level(), x, groundHeight + 0.5, z, 1 + random.nextInt(3), 1 + random.nextGaussian());
            }
        } else if (state == RocketState.LAUNCHING || getStateTimer() < 200) {
            spawnContrailWithOffset(0, 0, 0);

            int cluster = stage.getCluster();
            for (int c = 1; c < cluster; c++) {
                float spin = (float) c / (float) (cluster - 1);
                double radius = stage.fuselage != null ? stage.fuselage.bottomRadius() : 0;
                double ox = Math.cos(spin * Math.PI * 2) * radius;
                double oz = Math.sin(spin * Math.PI * 2) * radius;
                spawnContrailWithOffset(ox, 0, oz);
            }
        }
    }

    /** 1.7 {@code EntityMissileBaseNT.spawnContraolWithOffset}: puffs along the last step, thrown back along the axis. */
    private void spawnContrailWithOffset(double offsetX, double offsetY, double offsetZ) {
        Vec3 step = new Vec3(contrailX - getX(), contrailY - getY(), contrailZ - getZ());
        double len = step.length();
        Vec3 vec = step.normalize();
        // 1.7: (0,1,0) rotated about Z by the pitch, then about Y by yaw + 90 (Vec3.rotateAroundZ/Y)
        Vec3 thrust = rotateY(rotateZ(new Vec3(0, 1, 0), getPitch(1F) * (float) Math.PI / 180F), (getYRot() + 90) * (float) Math.PI / 180F);

        for (int i = 0; i < Math.max(Math.min(len, 10), 1); i++) {
            double j = i - len;
            CompoundTag data = new CompoundTag();
            data.putFloat("scale", 1F);
            data.putDouble("moX", -thrust.x);
            data.putDouble("moY", -thrust.y);
            data.putDouble("moZ", -thrust.z);
            data.putInt("maxAge", 60 + random.nextInt(20));
            HbmEffectNT.MissileContrail.summonParticle(level(), getX() - vec.x * j + offsetX, getY() - vec.y * j + offsetY,
                    getZ() - vec.z * j + offsetZ, data);
        }
    }

    private static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(v.x * cos + v.z * sin, v.y, v.z * cos - v.x * sin);
    }

    private static Vec3 rotateZ(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(v.x * cos + v.y * sin, v.y * cos - v.x * sin, v.z);
    }

    // ---------------------------------------------------------------------------------------- data

    /** The rocket's pitch for rendering (1.7 {@code rotationPitch}), interpolated. */
    public float getPitch(float partialTick) {
        return Mth.lerp(partialTick, pitchO, pitch);
    }

    public RocketStruct getRocket() {
        ItemStack stack = entityData.get(ROCKET);
        if (rocketStackFor != stack || rocketCache == null) {
            RocketStruct rocket = ItemCustomRocket.get(stack);
            if (rocket == null) rocket = new RocketStruct();
            // In case of emergency, default to regular capsule
            if (rocket.capsule == null) rocket.capsule = RocketParts.get(RocketParts.CAPSULE);
            rocketCache = rocket;
            rocketStackFor = stack;
        }
        return rocketCache.copy();
    }

    public void setRocket(RocketStruct rocket) {
        ItemStack stack = ItemCustomRocket.build(rocket);
        entityData.set(ROCKET, stack);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ROCKET.equals(key)) refreshDimensions();
    }

    /** 1.7 {@code setSize(2, rocket.getHeight() + 1)}. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(2.0F, (float) getRocket().getHeight() + 1.0F);
    }

    public RocketState getState() {
        int ordinal = entityData.get(STATE);
        RocketState[] values = RocketState.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : RocketState.AWAITING;
    }

    public void setState(RocketState state) {
        // a positive signal for the harness, in dev runs: every server-side state change of a rocket in a level
        if (Platform.isDevelopment() && !level().isClientSide() && state != getState() && !isRemoved()
                && level().getEntity(getId()) == this) {
            NtmSpace.LOG.info("Rocket {} -> {} in {} at {} {} {}", getUUID(), state, level().dimension(),
                    (int) getX(), (int) getY(), (int) getZ());
        }
        entityData.set(STATE, state.ordinal());
        entityData.set(TIMER, 0);
        stateTimer = 0;
    }

    public RocketTarget getTarget() {
        if (destinationOverride != null) {
            return new RocketTarget(destinationOverride.body.getBody(), false, true);
        }

        ItemStack drive = entityData.get(DRIVE);
        return ItemVOTVdrive.getTarget(drive.isEmpty() ? null : drive, level());
    }

    public @Nullable Destination getDestination() {
        if (destinationOverride != null) return destinationOverride;

        ItemStack drive = entityData.get(DRIVE);
        return ItemVOTVdrive.getDestination(drive);
    }

    public void setDrive(ItemStack drive) {
        entityData.set(DRIVE, drive.copy());
    }

    public ItemStack getDriveStack() {
        return entityData.get(DRIVE);
    }

    public int getStateTimer() {
        return entityData.get(TIMER);
    }

    public void setStateTimer(int timer) {
        entityData.set(TIMER, timer);
    }

    @Override
    protected void loadData(DataIn input) {
        chunkLoader.markRestoredFromNbt();
        RocketState state = RocketState.AWAITING;
        String name = input.getStringOr("state", RocketState.AWAITING.name());
        for (RocketState s : RocketState.values()) if (s.name().equals(name)) state = s;
        setState(state);
        stateTimer = input.getIntOr("timer", 0);
        setStateTimer(stateTimer);

        setRocket(input.read("rocket", RocketStruct.CODEC).orElseGet(RocketStruct::new));

        navDrive = input.read("drive", ItemStack.CODEC).orElse(null);
        satFreq = input.getIntOr("freq", 0);
        pitch = input.getFloatOr("pitch", 0F);

        if (input.getBooleanOr("hasOverride", false)) {
            DriveBody body = DriveBody.byName(input.getStringOr("overrideBody", "kerbin"));
            destinationOverride = new Destination(body == null ? DriveBody.KERBIN : body, input.getIntOr("overrideX", 0), input.getIntOr("overrideZ", 0));
        } else {
            destinationOverride = null;
        }
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("timer", getStateTimer());
        output.putString("state", getState().name());
        output.store("rocket", RocketStruct.CODEC, getRocket());
        if (navDrive != null && !navDrive.isEmpty()) output.store("drive", ItemStack.CODEC, navDrive);
        output.putInt("freq", satFreq);
        output.putFloat("pitch", pitch);

        if (destinationOverride != null) {
            output.putBoolean("hasOverride", true);
            output.putString("overrideBody", destinationOverride.body.key());
            output.putInt("overrideX", destinationOverride.x);
            output.putInt("overrideZ", destinationOverride.z);
        }
    }

    // ---------------------------------------------------------------------------------------- StationDock

    @Override
    public Entity entity() {
        return this;
    }

    @Override
    public boolean hasRider() {
        return getFirstPassenger() != null;
    }

    @Override
    public boolean isUndocking() {
        return getState() == RocketState.UNDOCKING;
    }

    @Override
    public boolean targetInOrbit() {
        return getTarget().inOrbit();
    }

    @Override
    public int launchMass() {
        return getRocket().getLaunchMass();
    }

    /** 1.7 {@code TileEntityOrbitalStation.updateEntity}'s fuel check on the docked pod. */
    @Override
    public void fuelled(boolean hasFuel) {
        RocketState state = getState();
        if (hasFuel) {
            if (state == RocketState.NEEDSFUEL) {
                setState(navDrive != null ? RocketState.AWAITING : RocketState.LANDED);
            }
        } else if (state != RocketState.NEEDSFUEL) {
            setState(RocketState.NEEDSFUEL);
        }
    }

    @Override
    public void board(Player player) {
        onInteract(player, InteractionHand.MAIN_HAND);
    }

    @Override
    public List<ItemStack> stow() {
        List<ItemStack> items = new ArrayList<>();
        RocketStruct rocket = getRocket();
        // 1.7 pushed the rocket then the drive onto a stack and popped them: the drive first
        if (navDrive != null && !navDrive.isEmpty()) items.add(navDrive.copy());
        if (rocket.stages.size() > 0) {
            items.add(ItemCustomRocket.build(rocket, true));
        } else if (rocket.capsule != null) {
            items.add(new ItemStack(rocket.capsule.item()));
        }
        discard();
        return items;
    }

    @Override
    public void dropAndDie() {
        dropNDie(null);
    }

    @Override
    public boolean canRecall() {
        RocketState state = getState();
        return state == RocketState.AWAITING || state == RocketState.LANDED;
    }

    @Override
    public void recall(ResourceKey<Level> level, int x, int z) {
        DriveBody body = DriveBody.ORBIT;
        for (DriveBody candidate : DriveBody.values()) {
            if (candidate != DriveBody.ORBIT && candidate.level().equals(level)) body = candidate;
        }
        recallPod(new Destination(body, x, z));
    }

    @Override
    public void teleportedWith(ServerPlayer rider) {
        thrower = rider;
    }

    // ---------------------------------------------------------------------------------------- look overlay (client)

    /** 1.7 {@code printHook}: the rocket's state, destination and what to do next. */
    @Override
    public com.hbm.interfaces.ILookOverlay.@Nullable Data getLookOverlay(Player player) {
        RocketState state = getState();
        if (state == RocketState.LAUNCHING
                || state == RocketState.LANDING
                || state == RocketState.TIPPING
                || state == RocketState.DOCKING
                || state == RocketState.UNDOCKING)
            return null;

        List<String> text = new ArrayList<>();

        if (state == RocketState.TRANSFER) {
            OrbitalStation station = OrbitalStation.clientStation;
            double progress = station.getUnscaledProgress(0);

            text.add(net.minecraft.ChatFormatting.AQUA + net.minecraft.network.chat.Component.translatable("station.travelling").getString() + ": "
                    + net.minecraft.ChatFormatting.RESET + net.minecraft.network.chat.Component.translatable("body." + station.target.name).getString());
            text.add(net.minecraft.ChatFormatting.AQUA + net.minecraft.network.chat.Component.translatable("station.progress").getString() + ": "
                    + net.minecraft.ChatFormatting.RESET + Math.round(progress * 100) + "%");

            return new com.hbm.interfaces.ILookOverlay.Data("Rocket", 0xffff00, 0x404000, text);
        }

        RocketStruct rocket = getRocket();
        if (rocket.stages.size() == 0 && !Orbit.inOrbit(level()) && !isReusable()) return null;

        RocketTarget from = RocketTarget.of(level(), (int) getX(), (int) getZ());
        RocketTarget to = getTarget();

        boolean canLaunch = to.body() != null && state == RocketState.AWAITING;

        // Check if the stage can make the journey
        if (state == RocketState.NEEDSFUEL) {
            text.add(net.minecraft.ChatFormatting.RED + "Rocket has no fuel!");
        } else if (canLaunch && !rocket.hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit())) {
            text.add(net.minecraft.ChatFormatting.RED + "Rocket can't reach destination!");
            canLaunch = false;
        }

        Entity rider = getFirstPassenger();
        if (rider == null) {
            text.add("Interact to enter");
        } else if (rider != player) {
            text.add("OCCUPIED");
        } else {
            if (to.inOrbit()) {
                text.add("Destination: ORBITAL STATION");
            } else if (to.body() != null) {
                text.add("Destination: " + net.minecraft.network.chat.Component.translatable("body." + to.body().name).getString());
            } else {
                text.add("Destination: NO DRIVE INSTALLED");
            }

            if (canLaunch) {
                text.add("JUMP TO LAUNCH");
            } else if (state == RocketState.LANDED) {
                text.add("Insert next drive to continue");
            }

            ItemStack stack = player.getMainHandItem();
            if ((state == RocketState.LANDED || state == RocketState.AWAITING) && stack.getItem() instanceof ItemVOTVdrive) {
                if (ItemVOTVdrive.getProcessed(stack)) {
                    text.add("Interact to swap drive");
                }
            }
        }

        return new com.hbm.interfaces.ILookOverlay.Data("Rocket", 0xffff00, 0x404000, text);
    }

    // ---------------------------------------------------------------------------------------- radar (1.7 missile base)

    @Override
    public String getRadarName() {
        return "Unknown";
    }

    @Override
    public int getBlipLevel() {
        return SPECIAL;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return !(params.smartMode && getDeltaMovement().y >= 0);
    }

    /** The server of a level, for code that runs on both sides. */
    record MinecraftServerAccess(net.minecraft.server.MinecraftServer server) {
        static @Nullable MinecraftServerAccess of(Level level) {
            if (level.isClientSide() || level.getServer() == null) return null;
            return new MinecraftServerAccess(level.getServer());
        }
    }
}
