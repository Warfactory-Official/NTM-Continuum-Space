package com.ntmspace.rocket;

import com.hbm.client.ClientEffects;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.WarheadType;
import com.ntmspace.fluid.SpaceFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The missile fuel and payload kinds hbm-space added to HBM's {@code ItemCustomMissilePart} enums, registered into HBM's
 * {@link FuelType} / {@link WarheadType} registries (ADDON-API-INVENTORY E-021, E-022) under {@code ntmspace:} names.
 *
 * <ul>
 *   <li>{@code ANY}: space-grade fuselages that take whatever their thruster burns (no fluid of its own).</li>
 *   <li>{@code HYDRAZINE}: hydrazine, no oxidiser; the kerosene contrail (1.7 {@code EntityMissileCustom}).</li>
 *   <li>{@code METHALOX}: natural gas + oxygen; the kerosene contrail.</li>
 *   <li>{@code KEROLOX}: kerosene + oxygen; no contrail (1.7's switch had no case for it).</li>
 *   <li>{@code APOLLO} / {@code SATELLITE}: the capsule and satellite payloads; on a plain missile they do nothing on
 *       impact (1.7's switch had no case for them).</li>
 * </ul>
 * A missile launcher fills its tanks from a {@code FuelType}'s fluids (HBM's design); 1.7's compact launcher knew only
 * HYDRAZINE of the four, so a methalox or kerolox custom missile can be fuelled in the port where 1.7 left its tanks
 * untyped. ANY has no fluid and, being "solid" to HBM's launchers, never pairs with a thruster in a missile.
 */
public final class RocketFuels {

    public static final FuelType ANY = FuelType.register("ntmspace:any", "any", ChatFormatting.GRAY, null, null, null);
    public static final FuelType HYDRAZINE = FuelType.register("ntmspace:hydrazine", "hydrazine", ChatFormatting.AQUA,
            ClientEffects.Contrail.KEROSENE, () -> SpaceFluids.HYDRAZINE, null);
    public static final FuelType METHALOX = FuelType.register("ntmspace:methalox", "methalox", ChatFormatting.YELLOW,
            ClientEffects.Contrail.KEROSENE, () -> Fluids.GAS, () -> Fluids.OXYGEN);
    public static final FuelType KEROLOX = FuelType.register("ntmspace:kerolox", "kerolox", ChatFormatting.LIGHT_PURPLE,
            null, () -> Fluids.KEROSENE, () -> Fluids.OXYGEN);

    // 1.7 flickered both labels gold/red off the wall clock at 2Hz
    public static final WarheadType APOLLO = WarheadType.register("ntmspace:apollo",
            () -> label("apollo"), missile -> {}, null);
    public static final WarheadType SATELLITE = WarheadType.register("ntmspace:satellite",
            () -> label("satellite"), missile -> {}, null);

    private RocketFuels() {}

    private static Component label(String name) {
        return Component.translatable("item.warhead.desc." + name)
                .withStyle(System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.GOLD : ChatFormatting.RED);
    }

    /** The HBM fuel type of a 1.7 {@code FuelType} name. */
    public static FuelType fuel(String name) {
        return switch (name) {
            case "KEROSENE" -> FuelType.KEROSENE;
            case "SOLID" -> FuelType.SOLID;
            case "HYDROGEN" -> FuelType.HYDROGEN;
            case "XENON" -> FuelType.XENON;
            case "BALEFIRE" -> FuelType.BALEFIRE;
            case "ANY" -> ANY;
            case "HYDRAZINE" -> HYDRAZINE;
            case "METHALOX" -> METHALOX;
            case "KEROLOX" -> KEROLOX;
            default -> throw new IllegalArgumentException(name);
        };
    }

    public static void init() {}
}
