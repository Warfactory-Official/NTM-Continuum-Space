package com.ntmspace.probe;

import com.hbm.inventory.fluid.FluidType;
import com.ntmspace.entity.mob.EntityMoonCow;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.Registrar;
import dev.golem.core.registry.RegistryObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.cow.Cow;

/**
 * The probe's entries in every registry of records NTM: Space adds to (HBM's {@code NamedRegistry}s and the other
 * name-keyed tables, fluids, fluid traits, materials, hazard types, effects, computer components) and in the vanilla
 * registries through Golem Core (an entity type with attributes, a spawn placement and a renderer; a mob effect). Each
 * name is in the probe's namespace, so the two addons' entries sit side by side; {@link ProbeRegistryGameTests} checks
 * both are there, in registration order, and that taking a name twice is refused.
 */
public final class ProbeRecords {

    private static final String NS = ProbeEntry.MOD_ID;

    // ---------------------------------------------------------------------------------------- vanilla registries

    public static final DeferredRegister.EntityTypes ENTITY_TYPES = DeferredRegister.createEntityTypes(NS);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(() -> Registries.MOB_EFFECT, NS);
    private static final Registrar REG = Registrar.of(ENTITY_TYPES);

    /** A second moon cow type: NTM: Space's entity class and renderer under the probe's id. */
    public static final Registrar.EntityHandle<EntityMoonCow> PROBE_COW = REG.entity("probe_cow", key -> Entities.build(
            Entities.noLoot(EntityType.Builder.<EntityMoonCow>of(EntityMoonCow::new, MobCategory.CREATURE))
                    .sized(0.9F, 1.3F).clientTrackingRange(5).updateInterval(3), key));

    public static final RegistryObject<MobEffect> PROBE_EFFECT = EFFECTS.register("probe", ProbeEffect::new);

    public static final class ProbeEffect extends MobEffect {
        public ProbeEffect() {
            super(MobEffectCategory.NEUTRAL, 0x40FF40);
        }
    }

    // ---------------------------------------------------------------------------------------- HBM's registries

    public static final String NAME = NS + ":probe";

    public static com.hbm.saveddata.satellites.SatelliteType SATELLITE;
    public static com.hbm.entity.logic.BomberPayload BOMBER;
    public static com.hbm.entity.projectile.EntityBombletZeta.Kind BOMBLET;
    public static com.hbm.items.weapon.ItemCustomMissilePart.WarheadType WARHEAD;
    public static com.hbm.items.weapon.ItemCustomMissilePart.FuelType MISSILE_FUEL;
    /** Between UV (3) and NTM: Space's X-ray (4): the two spectra interleave by strength. */
    public static com.hbm.items.machine.ItemFELCrystal.EnumWavelengths WAVELENGTH;
    public static com.hbm.items.machine.ItemCassette.TrackType TRACK;
    public static com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc BURN;
    public static com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc DEPLETE;
    public static com.hbm.inventory.material.NTMMaterial MATERIAL;
    public static com.hbm.hazard.type.IHazardType HAZARD;
    public static com.hbm.particle.helper.HbmEffectNT PARTICLE;
    public static com.hbm.api.computer.ComputerComponents.Component<com.hbm.tileentity.machine.TileEntityMachineFluidTank> COMPONENT;
    /** An HBM fluid of the probe's ({@code Fluids.register}), its properties in {@code data/ntmprobe/fluid_property}. */
    public static FluidType PROBE_FUEL;

    /** Material ids are runtime numbers (never saved); NTM: Space uses 24,000 and up. */
    public static final int MATERIAL_ID = 31_000;

    private ProbeRecords() {}

    public static void init() {
        SATELLITE = com.hbm.saveddata.satellites.SatelliteType.register(NAME, ProbeSatellite::new,
                com.mojang.serialization.MapCodec.unit(ProbeSatellite::new), null);
        BOMBER = com.hbm.entity.logic.BomberPayload.register(NAME,
                (level, x, y, z) -> com.hbm.entity.logic.EntityBomber.create(level, x, y, z, 75, 125, 1000, NAME), bomber -> {});
        BOMBLET = com.hbm.entity.projectile.EntityBombletZeta.KINDS.register(
                new com.hbm.entity.projectile.EntityBombletZeta.Kind(NAME, zeta -> {}));
        WARHEAD = com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.register(NAME, () -> Component.literal("Probe"),
                missile -> {}, null);
        MISSILE_FUEL = com.hbm.items.weapon.ItemCustomMissilePart.FuelType.register(NAME, "probe", ChatFormatting.GREEN,
                null, null, null);
        WAVELENGTH = com.hbm.items.machine.ItemFELCrystal.EnumWavelengths.register(NS + ":far_uv", 3.5D,
                "wavelengths.name.uv", "wavelengths.waveRange.uv", 0x40FF40, 0x40FF40, ChatFormatting.GREEN, entity -> {});
        TRACK = com.hbm.items.machine.ItemCassette.TrackType.register(NS + ":PROBE", "Probe", null,
                com.hbm.items.machine.ItemCassette.SoundType.LOOP, 0x40FF40, 10);
        BURN = com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc.register(NS + ":FLAT", "trait.rbmx.flux.linear", null,
                (flux, enrichment, reactivity, selfRate) -> flux);
        DEPLETE = com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc.register(NS + ":FLAT", e -> 1D);
        MATERIAL = com.hbm.inventory.material.Mats.register(MATERIAL_ID, "probium").colors(0x40FF40, 0x103F10, 0x40FF40);
        HAZARD = com.hbm.hazard.HazardRegistry.registerType(dev.golem.core.Ids.of(NS, "probe"), new ProbeHazard());
        PARTICLE = com.hbm.particle.helper.HbmEffectNT.register(NAME);
        Map<String, com.hbm.api.computer.ComputerComponents.Method<com.hbm.tileentity.machine.TileEntityMachineFluidTank>> methods =
                new LinkedHashMap<>();
        methods.put("getFill", (tank, args) -> new Object[] {tank.tank.getFill()});
        COMPONENT = com.hbm.api.computer.ComputerComponents.register(com.hbm.tileentity.machine.TileEntityMachineFluidTank.class,
                "ntmprobe_tank", methods, () -> com.hbm.blocks.machine.ModMachineBlocks.machine_fluidtank.get());

        // fluid traits: a parameterised trait and a flag, both usable in any fluid_property file and patch
        com.hbm.inventory.fluid.FluidProperties.registerTrait(ProbeTrait.NAME, ProbeTrait.class,
                json -> new ProbeTrait(json.get("level").getAsInt()), trait -> {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("level", ((ProbeTrait) trait).level);
                    return json;
                });
        com.hbm.inventory.fluid.FluidProperties.registerFlag(NS + ":inert", ProbeFlag.INSTANCE);
        PROBE_FUEL = com.hbm.inventory.fluid.Fluids.register(NS, "probe_fuel", 0x40FF40);
        com.hbm.inventory.fluid.FluidProperties.applyBundled(ProbeRecords.class, List.of(PROBE_FUEL));
        com.hbm.inventory.fluid.FluidProperties.applyBundledPatches(ProbeRecords.class, NS);
        com.hbm.inventory.fluid.Fluids.declareVanilla(NS, List.of(PROBE_FUEL));

        // vanilla registries: attributes, placement
        dev.golem.core.registry.LivingAttributes.register(PROBE_COW, Cow::createAttributes);
        dev.golem.core.world.Spawns.placement(() -> PROBE_COW.get(), dev.golem.core.world.Spawns.Placement.ON_GROUND,
                dev.golem.core.world.Spawns.Rule.MOB);
    }

    /** The probe's fluid trait ({@code "traits": {"ntmprobe:marker": {"level": n}}}). */
    public static final class ProbeTrait {
        public static final String NAME = ProbeEntry.MOD_ID + ":marker";
        public final int level;

        public ProbeTrait(int level) {
            this.level = level;
        }
    }

    /** The probe's fluid flag ({@code "flags": ["ntmprobe:inert"]}). */
    public static final class ProbeFlag {
        public static final ProbeFlag INSTANCE = new ProbeFlag();

        private ProbeFlag() {}
    }

    public static final class ProbeSatellite extends com.hbm.saveddata.satellites.Satellite {
        @Override
        public com.hbm.saveddata.satellites.SatelliteType type() {
            return SATELLITE;
        }

        @Override
        public String getType() {
            return "Probe";
        }
    }

    /** A hazard that does nothing: only its name is under test. */
    public static final class ProbeHazard implements com.hbm.hazard.type.IHazardType {
        @Override
        public void onUpdate(net.minecraft.world.entity.LivingEntity target, double level, net.minecraft.world.item.ItemStack stack) {}

        @Override
        public void updateEntity(net.minecraft.world.entity.item.ItemEntity item, double level) {}

        @Override
        public void addHazardInformation(net.minecraft.world.entity.player.Player player, List<Component> list, double level,
                                         net.minecraft.world.item.ItemStack stack,
                                         List<com.hbm.hazard.modifier.IHazardModifier> modifiers) {}
    }
}
