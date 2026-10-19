package com.ntmspace.item.special;

import com.hbm.api.environment.Environment;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ItemArmorMod;
import com.ntmspace.orbit.OrbitEnvironment;
import com.ntmspace.world.CelestialBodies;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space {@code ItemModHeavyBoots}: a boots module that, while the wearer crouches in mid-air, pulls them down until
 * their fall matches the overworld (bodies with gravity up to 1.5, counted as at least 0.2).
 *
 * <p>1.7 also let the boots be worn on their own ({@code isValidArmor} slot 3, armour texture
 * {@code hbm:textures/armor/heavy_boots.png}, its own {@code armorRender}). A non-armour item that equips into the feet
 * slot and draws an armour layer has no Golem Core facade (26.1 {@code Equippable} component, 1.20.1/1.21.1
 * {@code Equipable}/{@code ArmorItem}), and an addon may not guard eras, so the boots work as a module only for now.
 */
public class ItemModHeavyBoots extends ItemArmorMod {

    public ItemModHeavyBoots(Item.Properties properties) {
        super(ArmorModHandler.boots_only, false, false, false, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.BLUE, "Increases fall speed in low gravity"));
        tooltip.add(coloured(ChatFormatting.BLUE, "Activated by crouching"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
        tooltip.add(coloured(ChatFormatting.GOLD, "Can be worn on its own!"));
        tooltip.add(Component.literal("We take no responsibility for any deaths that may")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("occur while using these boots without a space suit.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(Component.literal("  ").append(stack.getHoverName()).append(" (")
                .append(Component.translatable("armor.fastFall")).append(")").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** 1.7 {@code onArmorTick}: if crouching in air, apply extra gravity until we match the overworld. */
    public static void heavyFall(Player player) {
        if (player.isShiftKeyDown() && !player.onGround() && !player.isInWater()) {
            float gravity = gravity(player);
            if (gravity > 1.5F) return;
            if (gravity == 0) return;
            if (gravity < 0.2F) gravity = 0.2F;

            Vec3 motion = player.getDeltaMovement();
            double y = motion.y;
            y /= 0.98F;
            y += (gravity / 20F);
            y -= (CelestialBodies.STANDARD_GRAVITY / 20F);
            y *= 0.98F;
            player.setDeltaMovement(motion.x, y, motion.z);
        }
    }

    /** 1.7 {@code CelestialBody.getGravity(entity)} for a player: the station's in orbit, else the body's surface. */
    static float gravity(Player player) {
        if (Environment.inOrbit(player.level())) return OrbitEnvironment.gravity(player);
        CelestialBodies.Body body = CelestialBodies.of(player.level());
        if (body == null) body = CelestialBodies.KERBIN; // 1.7: a level that is no body is Kerbin
        return body.playerGravity();
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity instanceof Player player)) return;
        if (ArmorModHandler.pryMod(armor, ArmorModHandler.boots_only).isEmpty()) return;
        // 1.7 then wrote the unchanged module back (ArmorModHandler.applyMod): a no-op, not repeated every tick here
        heavyFall(player);
    }
}
