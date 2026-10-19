package com.ntmspace.breathing;

import com.hbm.api.environment.Environment;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ArmorDNT;
import com.hbm.lib.ModDamageSources;
import com.ntmspace.NtmSpace;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.atmosphere.Sealing;
import com.ntmspace.trait.CBT_Atmosphere;
import dev.golem.core.data.EntityData;
import dev.golem.core.world.Compat;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Breathing for living entities, hbm-space's {@code EntityEffectHandler.handleOxy} / {@code handleCorrosion},
 * {@code ArmorUtil.checkForOxy} / {@code getOxygenTank} / {@code checkForCorrosion} and the oxygen of
 * {@code HbmLivingProps}, run from HBM's {@link Environment#LIVING_TICK}.
 *
 * <p>Every living thing on the server keeps an oxygen count (100 = five seconds of breath). Where it cannot breathe
 * (no air, or air without 0.21 atm of earth air or 0.09 atm of oxygen) the count falls by one a tick; at 0 and below it
 * takes {@code oxyprime} damage ("forgot their PLSS"), faster and harder the further it falls. Breathable air refills it
 * at once. A player in a fully sealed suit ({@code #ntmspace:sealed_armor}) with a filled PLSS
 * ({@link ItemModOxy}) in the helmet breathes from the tank; creative players and a full DNT suit always breathe;
 * {@code #ntmspace:suffocation_immune} mobs never suffocate. The state is a Golem {@link EntityData} attachment
 * ({@code ntmspace:breath}), saved by name.
 */
public final class Breathing {

    private Breathing() {}

    /** Per-entity breath: 1.7 {@code HbmLivingProps} oxygen, cached atmosphere and "has gravity" (= has an atmosphere). */
    public static final class State {
        public int oxygen = 100;
        @Nullable CBT_Atmosphere atmosphere;
        boolean gravity;
    }

    public static final EntityData<State> STATE = EntityData.of(NtmSpace.MOD_ID, "breath", LivingEntity.class, State::new,
            (state, out) -> {
                out.putInt("oxygen", state.oxygen);
                out.putBoolean("gravity", state.gravity);
            },
            (state, in) -> {
                state.oxygen = in.getIntOr("oxygen", 100);
                state.gravity = in.getBooleanOr("gravity", false);
            });

    public static final ResourceKey<DamageType> OXYPRIME =
            ResourceKey.create(Registries.DAMAGE_TYPE, dev.golem.core.Ids.of(NtmSpace.MOD_ID, "oxyprime"));
    public static final ResourceKey<DamageType> ACID_KEY = ResourceKey.create(Registries.DAMAGE_TYPE, dev.golem.core.Ids.of("hbm", "acid"));

    /**
     * 1.7 skipped entities riding its rideable rocket ("breathe easy in your ship"); the rocket port answers true for
     * its vehicles here.
     */
    public static volatile Predicate<Entity> SHELTERED_VEHICLE = vehicle -> false;

    public static void init() {
        Environment.LIVING_TICK.register(Breathing::livingTick);
    }

    private static void livingTick(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        CBT_Atmosphere atmosphere = getAtmosphereCached(entity);
        handleOxy(entity, atmosphere);
        handleCorrosion(entity, atmosphere);
    }

    /** 1.7: players every tick, other living entities once per second. */
    private static @Nullable CBT_Atmosphere getAtmosphereCached(LivingEntity entity) {
        State state = STATE.get(entity);
        if (entity instanceof ServerPlayer || entity.tickCount % 20 == 0) {
            CBT_Atmosphere atmosphere = AtmosphereHandler.getAtmosphere(entity);
            state.atmosphere = atmosphere;
            state.gravity = atmosphere != null;
            return atmosphere;
        }
        return state.atmosphere;
    }

    /** 1.7 {@code HbmLivingProps.hasGravity}: the entity was last in some air (the orbit uses it for station gravity). */
    public static boolean hasGravity(LivingEntity entity) {
        return STATE.get(entity).gravity;
    }

    public static int getOxy(LivingEntity entity) {
        return STATE.get(entity).oxygen;
    }

    /** 1.7 {@code HbmLivingProps.setOxy}: at 0 and below the entity takes oxyprime damage. */
    public static void setOxy(LivingEntity entity, int oxygen) {
        if (oxygen <= 0) {
            if (entity.tickCount < 20) oxygen = 0; // limit overdamage effects when relogging

            int damageInterval = Mth.clamp(oxygen / 60 + 8, 1, 8);
            int damageAmount = Mth.clamp(-oxygen / 40 - 10, 1, Integer.MAX_VALUE);

            // Only damage every few ticks, giving the player more time to react
            if (entity.tickCount % damageInterval == 0 && entity.level() instanceof ServerLevel level) {
                Compat.hurt(entity, level, oxyprime(level), damageAmount);
            }
        }
        State state = STATE.get(entity);
        int old = state.oxygen;
        state.oxygen = oxygen;
        if (old != oxygen && entity instanceof ServerPlayer player) OxygenSync.send(player, oxygen);
    }

    public static DamageSource oxyprime(ServerLevel level) {
        return ModDamageSources.source(level, OXYPRIME);
    }

    /** 1.7 {@code handleOxy}. */
    public static void handleOxy(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
        if (entity.level().isClientSide()) return;
        if (entity.getType().builtInRegistryHolder().is(Sealing.SUFFOCATION_IMMUNE)) return;
        Entity vehicle = entity.getVehicle();
        if (vehicle != null && SHELTERED_VEHICLE.test(vehicle)) return; // breathe easy in your ship

        if (!checkForOxy(entity, atmosphere)) {
            setOxy(entity, getOxy(entity) - 1);
        } else {
            setOxy(entity, 100); // 5 seconds until vacuum damage
        }
    }

    /** 1.7 {@code ArmorUtil.checkForOxy}. */
    public static boolean checkForOxy(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
        if (!(entity instanceof Player player)) return AtmosphereHandler.canBreathe(atmosphere);

        if (checkSpecialBreathing(player)) return true;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack tank = getOxygenTank(player);
        if (tank.isEmpty()) return AtmosphereHandler.canBreathe(atmosphere);

        // If we have an oxygen tank, block drowning
        boolean isInWater = entity.getAirSupply() < 300;
        boolean canBreatheTank = ((ItemModOxy) tank.getItem()).attemptBreathing(entity, tank, atmosphere, isInWater);
        // 1.7 changed the tank inside the helmet's saved mods directly (a shared NBT tag); the port writes it back
        ArmorModHandler.applyMod(helmet, tank);

        if (isInWater && canBreatheTank) entity.setAirSupply(300);
        return canBreatheTank;
    }

    /** 1.7 {@code getOxygenTank}: every armour piece sealed and a PLSS in the helmet, else empty. */
    public static ItemStack getOxygenTank(Player player) {
        // Check that all the armor pieces are sealed
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.is(Sealing.SEALED_ARMOR)) return ItemStack.EMPTY;
        }
        // Check for a non-empty oxygen tank
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (ArmorModHandler.hasMods(helmet)) {
            ItemStack tankMod = ArmorModHandler.pryMod(helmet, ArmorModHandler.plate_only);
            if (tankMod.isEmpty() || !(tankMod.getItem() instanceof ItemModOxy)) return ItemStack.EMPTY;
            return tankMod;
        }
        return ItemStack.EMPTY;
    }

    /** 1.7: creative mode, or a full DNT suit. The port counts spectators as creative (1.7 had none). */
    private static boolean checkSpecialBreathing(Player player) {
        if (player.isCreative() || player.isSpectator()) return true;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            if (!(player.getItemBySlot(slot).getItem() instanceof ArmorDNT)) return false;
        }
        return true;
        // 1.7 checkModBreathing (Fisk's Superheroes) has no counterpart: that mod does not exist on these versions
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Corrosive atmospheres melt your suit, without appropriate protection (1.7 handleCorrosion)

    private static void handleCorrosion(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
        if (entity.level().isClientSide()) return;
        // 1.7: glyphids and cyber crabs; the port names the same mobs by type id
        String id = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        if (id.startsWith("hbm:entity_glyphid") || id.equals("hbm:entity_cyber_crab") || id.equals("hbm:entity_taint_crab")
                || id.equals("hbm:entity_tesla_crab")) return;
        Entity vehicle = entity.getVehicle();
        if (vehicle != null && SHELTERED_VEHICLE.test(vehicle)) return;

        // If we should corrode but we have armor, damage it heavily; once it runs out, start damaging the player
        if (checkForCorrosion(entity, atmosphere) && entity.level() instanceof ServerLevel level) {
            Compat.hurt(entity, level, ModDamageSources.source(level, ACID_KEY), 1);
        }
    }

    /** 1.7 {@code ArmorUtil.checkForCorrosion}. */
    public static boolean checkForCorrosion(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
        if (!AtmosphereHandler.willCorrode(atmosphere)) return false;
        if (!(entity instanceof Player player)) return true;

        ItemStack insert = getCorrosionProtection(player);
        if (!insert.isEmpty()) return false;

        boolean isSealed = true; // safe, for now...
        RandomSource rand = entity.getRandom();
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.is(Sealing.SEALED_ARMOR)) isSealed = false;
            if (stack.isEmpty()) continue;

            if (rand.nextInt(2) == 0 && stack.isDamageableItem()) stack.setDamageValue(stack.getDamageValue() + 1);

            boolean enabled = !(stack.getItem() instanceof com.hbm.items.gear.ArmorFSB fsb) || fsb.isArmorEnabled(stack);
            if ((stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage())
                    || !(stack.getItem() instanceof com.hbm.items.gear.ArmorFSB) || !enabled) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2F, 1F);
            }
        }
        // 1.7 also showed the red "info.corrosion" notice here; HBM has no inform toast yet
        return !isSealed;
    }

    /** 1.7 {@code getCorrosionProtection}: a chestplate insert that protects against corrosion ({@code #ntmspace:corrosion_protection}). */
    public static ItemStack getCorrosionProtection(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && ArmorModHandler.hasMods(chest)) {
            ItemStack insertMod = ArmorModHandler.pryMod(chest, ArmorModHandler.kevlar);
            if (insertMod.isEmpty() || !insertMod.is(Sealing.CORROSION_PROTECTION)) return ItemStack.EMPTY;
            return insertMod;
        }
        return ItemStack.EMPTY;
    }
}
