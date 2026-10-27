package com.ntmspace.breathing;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.armor.ItemArmorMod;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.registry.AtmosphereContent;
import com.ntmspace.trait.CBT_Atmosphere;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * hbm-space's {@code ItemModOxy}, the PLSS ({@code oxy_plss}): an oxygen tank armour module for a helmet (installed in the
 * chestplate-type slot, as 1.7's {@code ItemArmorMod(plate_only, helmet only)}). Worn with a fully sealed suit it supplies
 * breath where the air does not: {@code consumption} mB every {@code rate} ticks out of {@code maxFuel} (16,000 / 10 / 1,
 * so a full tank lasts 160,000 ticks). It is filled with oxygen like any fluid container, or with a {@code lox_tank}.
 * Fuel and use time are item components ({@code ntmspace:plss_fuel}, {@code ntmspace:plss_ticks}); 1.7's NBT keys
 * {@code fuel}/{@code ticks}.
 */
public class ItemModOxy extends ItemArmorMod implements IFillableItem {

    private final FluidType fuel;
    private final int maxFuel;
    private final int rate;
    private final int consumption;

    public ItemModOxy(int maxFuel, int rate, int consumption, Item.Properties properties) {
        super(ArmorModHandler.plate_only, true, false, false, false, properties);
        this.maxFuel = maxFuel;
        this.rate = rate;
        this.consumption = consumption;
        this.fuel = Fluids.OXYGEN;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(fuel.getDisplayNameString() + ": " + getFuel(stack) + "mB / " + maxFuel + "mB")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("armor.mustSeal").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.RED, stack, " (" + fuel.getDisplayNameString() + ": " + getFuel(stack) + "mB / " + maxFuel + "mB"));
    }

    /**
     * Client: the PLSS breathing loop ({@code ntmspace:player.plss_breathing}), told every tick whether the wearer breathes
     * from the tank; set by the client start-up, which plays it only for the local player out of creative (1.7).
     */
    public static volatile @Nullable BiConsumer<LivingEntity, Boolean> CLIENT_BREATHING;

    /** 1.7 {@code modUpdate}: client only, the breathing sound while the tank has been in use for more than a second. */
    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide()) return;
        BiConsumer<LivingEntity, Boolean> breathing = CLIENT_BREATHING;
        if (breathing == null) return;
        breathing.accept(entity, Breathing.isTankInUse(entity));
    }

    /**
     * Returns true if the entity can breathe, either via the contained air, or via the atmosphere itself; if contained air
     * is used, it is decremented here (1.7).
     */
    public boolean attemptBreathing(LivingEntity entity, ItemStack stack, @Nullable CBT_Atmosphere atmosphere, boolean forceConsume) {
        if (!forceConsume && AtmosphereHandler.canBreathe(atmosphere)) {
            Breathing.setTankInUse(entity, false);
            return true;
        }
        if (entity.tickCount % rate == 0) setFuel(stack, Math.max(getFuel(stack) - consumption, 0));
        boolean hasFuel = getFuel(stack) > 0;
        Breathing.setTankInUse(entity, hasFuel);
        return hasFuel;
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == fuel;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;
        int fill = getFuel(stack);
        int toFill = Math.min(amount, maxFuel - fill);
        setFuel(stack, fill + toFill);
        return amount - toFill;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return false;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return 0;
    }

    @Override
    public @Nullable FluidType getFirstFluidType(ItemStack stack) {
        return null;
    }

    /** 1.7 answered 0 here (the gauge reads {@link #getFuel}). */
    @Override
    public int getFill(ItemStack stack) {
        return 0;
    }

    /** The breathing sound plays while in use for more than a second (1.7 {@code ticks > 20}). */
    public static boolean getInUse(ItemStack stack) {
        return AtmosphereContent.PLSS_TICKS.getOr(stack, 0) > 20;
    }

    public static void setInUse(ItemStack stack, boolean inUse) {
        AtmosphereContent.PLSS_TICKS.set(stack, inUse ? AtmosphereContent.PLSS_TICKS.getOr(stack, 0) + 1 : 0);
    }

    public static int getFuel(ItemStack stack) {
        return AtmosphereContent.PLSS_FUEL.getOr(stack, 0);
    }

    public static void setFuel(ItemStack stack, int fuel) {
        AtmosphereContent.PLSS_FUEL.set(stack, fuel);
    }

    public int getMaxFuel() {
        return maxFuel;
    }
}
