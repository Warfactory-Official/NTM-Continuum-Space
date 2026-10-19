package com.ntmspace.probe;

import com.hbm.api.environment.Environment;
import com.hbm.inventory.fluid.FluidType;
import dev.golem.core.hook.Hook;
import dev.golem.core.hook.Verdict;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The probe on every common hook NTM: Space listens to (scratchpad m2/probe-hook-families.md), each listener passing
 * unless {@link ProbeMarks} says otherwise. Two orderings, both the documented rule:
 * <ul>
 *   <li>{@link #FIRST}: the probe's own phase, ordered before the default phase NTM: Space registers in; used on the
 *       "first answer decides" hooks, so a marked answer overrides NTM: Space's;</li>
 *   <li>the default phase: registered after NTM: Space's (Golem Core runs every mod's {@code declare()} dependencies
 *       first), so NTM: Space's answer stands wherever it gives one and the probe's applies where it passes.</li>
 * </ul>
 * The GameTests in {@link ProbeHookGameTests} check both the listener order and the combined answers.
 */
public final class ProbeHooks {

    /** The probe's phase that runs before the default phase. */
    public static final String FIRST = "ntmprobe:first";

    /** Ids {@code ModCustomMissileItems.PART_PROPERTIES} asked the probe about (after NTM: Space). */
    public static final Set<String> MISSILE_PARTS_SEEN = ConcurrentHashMap.newKeySet();

    private ProbeHooks() {}

    private static <T> void first(Hook<T> hook, T listener) {
        hook.addPhaseOrdering(FIRST, Hook.DEFAULT_PHASE);
        hook.register(FIRST, listener);
    }

    private static Verdict verdict(Object answer) {
        return answer instanceof Verdict v ? v : Verdict.PASS;
    }

    private static double number(Object answer) {
        return answer instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    public static void init() {
        environment();
        hbm();
        golem();
    }

    // ---------------------------------------------------------------------------------------------- environment

    private static void environment() {
        // first answer decides: the probe's phase runs first (BREATHING and PRESSURE: ProbeContent's sealed phase)
        first(Environment.COMPRESSION, (level, pos, amount) -> verdict(ProbeMarks.at("compression", level, pos)));
        first(Environment.GROWTH, (level, pos) -> verdict(ProbeMarks.at("growth", level, pos)));
        first(Environment.GROUNDWATER, (level, pos) -> ProbeMarks.at("groundwater", level, pos) instanceof FluidType f ? f : null);
        first(Environment.GRAVITY, entity -> number(ProbeMarks.entity("gravity", entity)));
        first(Environment.SUNLIGHT, (level, pos) -> number(ProbeMarks.at("sunlight", level, pos)));
        first(dev.golem.core.world.Furnaces.IGNITION, (level, pos, fuel) -> verdict(ProbeMarks.at("ignition", level, pos)));

        // default phase, after NTM: Space: its answer stands where it gives one
        Environment.SURFACE.register(level -> verdict(ProbeMarks.level("surface", level)));
        Environment.ORBIT.register(level -> verdict(ProbeMarks.level("orbit", level)));
        Environment.SATELLITES.register((level, pos, launched) ->
                ProbeMarks.level("satellites", level) instanceof net.minecraft.resources.ResourceKey<?> key
                        ? level.getServer().getLevel(levelKey(key)) : null);
        com.hbm.api.environment.SkyOwnership.OWNERS.register(level -> ProbeMarks.level("sky_owner", level) instanceof Boolean b && b);
        dev.golem.core.world.Weathers.register(level ->
                ProbeMarks.level("weather", level) instanceof dev.golem.core.world.Weathers.State s ? s : null);
        // NTM: Space's sky modifier is in its own phase before the default one: the probe receives its answer
        dev.golem.core.world.Skies.MODIFIERS.register(new dev.golem.core.world.Skies.Modifier() {
            @Override
            public boolean active(Level level) {
                return ProbeMarks.level("skies", level) != null;
            }

            @Override
            public float daylight(Level level, float daylight) {
                ProbeMarks.flag("skies_input", daylight);
                return daylight * (float) number(ProbeMarks.level("skies", level));
            }

            @Override
            public net.minecraft.world.phys.Vec3 skyColor(Level level, net.minecraft.world.phys.Vec3 color) {
                ProbeMarks.flag("skies_input_color", color);
                return color.scale(number(ProbeMarks.level("skies", level)));
            }
        });

        // everyone is told, in order
        Environment.LIVING_TICK.register(entity -> {
            if (ProbeMarks.entity("living_tick", entity) != null) ProbeMarks.call("living_tick");
        });
        Environment.GAS_RELEASE.register((level, pos, type, mB) -> ProbeMarks.call("gas_release"));
        Environment.IMPACT.register((level, pos) -> ProbeMarks.call("impact"));
        Environment.LIGHT_ON.register((level, pos, state) -> ProbeMarks.call("light_on"));
        Environment.NUCLEAR_BLAST.register((level, x, z, radius) -> ProbeMarks.call("nuclear_blast"));
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.resources.ResourceKey<Level> levelKey(net.minecraft.resources.ResourceKey<?> key) {
        return (net.minecraft.resources.ResourceKey<Level>) key;
    }

    // ---------------------------------------------------------------------------------------------- HBM's own hooks

    private static void hbm() {
        // first non-PASS decides (owner loops in phase order): the probe's phase first
        first(com.hbm.util.ContaminationUtil.RAD_IMMUNE, entity -> verdict(ProbeMarks.entity("rad_immune", entity)));
        // any listener may say yes
        com.hbm.handler.ArmorUtil.HAZMAT.register(entity -> ProbeMarks.entity("hazmat", entity) != null);
        // summed
        com.hbm.hazard.type.HazardTypeRadiation.STACK_RADIATION.register(stack -> number(ProbeMarks.flag("stack_radiation")) > 0
                ? number(ProbeMarks.flag("stack_radiation")) : 0D);
        // first non-empty product: NTM: Space's blood rag comes first
        com.hbm.items.special.ItemRag.USE.register(player ->
                ProbeMarks.entity("rag", player) != null ? new ItemStack(Items.FEATHER) : ItemStack.EMPTY);
        com.hbm.items.special.ItemEnergy.EMPTY.register(id -> {
            ProbeMarks.call("energy_empty");
            return null;
        });
        // every listener prints, in order
        com.hbm.util.ContaminationUtil.GEIGER_LINES.register((player, out) -> {
            if (ProbeMarks.flag("geiger") != null) out.accept(net.minecraft.network.chat.Component.literal("ntmprobe geiger line"));
        });
        // every listener appends, in order: one feather at the end of the special catalogue
        com.hbm.handler.BobmazonOfferFactory.OFFERS.register((standard, special) -> special.add(new com.hbm.handler.BobmazonOfferFactory.Offer(
                new ItemStack(Items.FEATHER), com.hbm.handler.BobmazonOfferFactory.Requirement.STEEL, 1, 0)));
        com.hbm.items.ModCustomMissileItems.PART_PROPERTIES.register((id, properties) -> {
            MISSILE_PARTS_SEEN.add(id);
            return properties;
        });

        // told, or asked with an answer the probe never gives
        com.hbm.items.special.ItemBedrockOreBase.SOURCE.register((dimension, seed, x, z) -> null);
        com.hbm.saveddata.satellites.XSatelliteRegistry.ORBITED.register((level, stack, freq, sat) -> ProbeMarks.call("orbited"));
        com.hbm.tileentity.machine.TileEntityMachineSatLinker.CHIP_COPY.register((source, copy) -> false);
        com.hbm.tileentity.TileEntityDoorGeneric.MOVED.register((level, core, door) -> ProbeMarks.call("door_moved"));
        com.hbm.tileentity.machine.BlockEntityDecon.DECONTAMINATED.register((level, pos, entities) -> ProbeMarks.call("decon"));
        com.hbm.tileentity.machine.TileEntityStorageDrum.SLOT_TICK.register((drum, slot, stack) -> ProbeMarks.call("drum"));
        com.hbm.tileentity.machine.TileEntityMachineShredder.INTERCEPT.register((shredder, slot, stack) -> false);
        com.hbm.tileentity.machine.TileEntityReactorResearch.RADIATING.register((level, pos, reactor, rad) -> ProbeMarks.call("research_reactor"));
        com.hbm.blocks.machine.DemonLamp.TileEntityDemonLamp.IRRADIATED.register((level, lamp, entity, dose) -> ProbeMarks.call("demon_lamp"));
        com.hbm.entity.logic.EntityBalefire.TICK.register((balefire, finished) -> ProbeMarks.call("balefire"));
        com.hbm.explosion.ExplosionNukeCustom.ANTIMATTER.register((level, x, y, z, amat) -> false);
        com.hbm.tileentity.machine.storage.TileEntityBarrel.ANTIMATTER_BLAST.register((level, pos, type) -> false);
        com.hbm.entity.logic.EntityBomber.LOOP_SOUND.register(bomber -> null);
        com.hbm.entity.logic.EntityPlaneBase.CRASH.register((plane, x, y, z) -> ProbeMarks.call("plane_crash"));
        com.hbm.tileentity.machine.storage.TileEntityFileCabinet.TICK.register(cabinet -> {});
        com.hbm.items.machine.ItemReactorSensor.USE.register(context -> null);
        com.hbm.tileentity.machine.TileEntityMachineMiningLaser.PICKUP.register((laser, item, nullifier) -> Verdict.PASS);
    }

    // ---------------------------------------------------------------------------------------------- Golem Core

    private static void golem() {
        dev.golem.core.world.BlockPlaces.onPlace((placer, level, pos, placed) -> {
            ProbeMarks.call("block_place");
            if (placed.is(net.minecraft.world.level.block.Blocks.LAPIS_BLOCK)) ProbeEntry.LOG.info("probe_place {}", pos.toShortString());
        });
        dev.golem.core.world.BlockBreaks.onBreak((player, level, pos, state) -> {
            ProbeMarks.call("block_break");
            if (state.is(net.minecraft.world.level.block.Blocks.LAPIS_BLOCK)) ProbeEntry.LOG.info("probe_break {}", pos.toShortString());
        });
        dev.golem.core.world.Deaths.onDeath((entity, source) -> {
            if (ProbeMarks.entity("death", entity) != null) ProbeMarks.call("death");
        });
        dev.golem.core.world.ItemUses.onFinish((entity, stack) -> ProbeMarks.call("item_finish"));
        dev.golem.core.platform.Lifecycle.onPlayerJoin((player, firstTime) ->
                ProbeEntry.LOG.info("probe_join {} first={}", player.getName().getString(), firstTime));
        dev.golem.core.platform.Lifecycle.onEntityLoad((entity, level) -> {
            if (entity.getType() == ProbeRecords.PROBE_COW.get()) ProbeMarks.call("probe_cow_load");
        });
        dev.golem.core.platform.Lifecycle.onServerStarted(server -> ProbeMarks.call("server_started"));
        dev.golem.core.platform.Lifecycle.onServerTick(server -> ProbeMarks.call("server_tick"));
        dev.golem.core.platform.Lifecycle.onServerStopping(server -> ProbeMarks.call("server_stopping"));
        dev.golem.core.platform.Lifecycle.onLevelUnload((server, level) -> ProbeMarks.call("level_unload"));
        dev.golem.core.platform.Lifecycle.onPlayerCopy((oldPlayer, newPlayer, alive) -> ProbeMarks.call("player_copy"));
    }
}
