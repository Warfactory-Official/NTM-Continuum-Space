package com.ntmspace.item.special;

import com.hbm.packet.PlayerInformPayload;
import com.ntmspace.atmosphere.AtmosphereHandler;
import com.ntmspace.trait.CBT_Atmosphere;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space {@code ItemAtmosphereScanner}: carried anywhere in a player's inventory, every 5 ticks it shows each gas of
 * the air around the player above 0.0001 atm as an inform line ({@code <fluid>: <pressure>atm}, ids 969 + index, 4 s),
 * or a yellow "NEAR VACUUM".
 */
public class ItemAtmosphereScanner extends CompatItem {

    public ItemAtmosphereScanner(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot, boolean held) {
        if (!(entity instanceof ServerPlayer player) || level.getGameTime() % 5 != 0) return;

        CBT_Atmosphere atmosphere = AtmosphereHandler.getAtmosphere(entity);

        boolean hasAtmosphere = false;
        if (atmosphere != null) {
            for (int i = 0; i < atmosphere.fluids.size(); i++) {
                CBT_Atmosphere.FluidEntry entry = atmosphere.fluids.get(i);
                if (entry.pressure > 0.0001) {
                    String pressure = String.format("%.4f", roundDecimal(entry.pressure, 4));
                    GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC, PlayerInformPayload.of(969 + i, 4000,
                            ChatFormatting.AQUA, new PlayerInformPayload.Translated(entry.fluid.getTranslationKey()), ": ",
                            new PlayerInformPayload.Colored(pressure + "atm", ChatFormatting.RESET)));
                    hasAtmosphere = true;
                }
            }
        }

        if (!hasAtmosphere) {
            GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC,
                    PlayerInformPayload.of(969, 4000, ChatFormatting.YELLOW, "NEAR VACUUM"));
        }
    }

    /** 1.7 {@code BobMathUtil.roundDecimal}: half-up at {@code digits} places. */
    static double roundDecimal(double num, int digits) {
        return new BigDecimal(num).setScale(digits, RoundingMode.HALF_UP).doubleValue();
    }
}
