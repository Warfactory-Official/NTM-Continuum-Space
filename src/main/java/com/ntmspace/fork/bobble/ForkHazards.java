package com.ntmspace.fork.bobble;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.type.IHazardType;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.Ids;
import dev.golem.core.world.Effects;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's two new hazard types ({@code HazardRegistry.AUTISM}, {@code GLITCH}), registered with HBM's hazard system as
 * {@code ntmspace:autism} and {@code ntmspace:glitch}. Both give nausea for {@code ceil(level)} ticks unless the head is
 * protected against light (and not at all with {@code disableBlinding}); AUTISM's tooltip says "[Autism]", GLITCH's
 * shows a random item's name, changing every half second, or "WORLD" one time in ten. hbm-space gives AUTISM to
 * {@code sat_gerald} and GLITCH to the bobblehead of its metadata 25 (MICROWAVE in hbm-space's reordered list) - the
 * rows are in {@code data/ntmspace/value_maps/patches/hbm/item/hazards.json} ({@code tools/gen_fork_hazards.py}).
 */
public final class ForkHazards {

    private ForkHazards() {}

    public static final IHazardType AUTISM = HazardRegistry.registerType(Ids.of("ntmspace", "autism"), new Autism());
    public static final IHazardType GLITCH = HazardRegistry.registerType(Ids.of("ntmspace", "glitch"), new Glitch());

    public static void init() {}

    /** Shared {@code onUpdate}: 1.7 re-applied nausea every tick; padded by one hazard interval like HBM's blinding. */
    static void nausea(LivingEntity target, double level, int rate) {
        if (RadiationConfig.disableBlinding) return;
        if (!ArmorRegistry.hasProtection(target, EquipmentSlot.HEAD, HazardClass.LIGHT)) {
            Effects.add(target, MobEffects.NAUSEA, (int) Math.ceil(level) + rate, 0);
        }
    }

    /** hbm-space {@code HazardTypeAutism}. */
    static final class Autism implements IHazardType {
        @Override
        public void onUpdate(LivingEntity target, double level, ItemStack stack) {
            nausea(target, level, hazardRate);
        }

        @Override
        public void updateEntity(ItemEntity item, double level) {}

        @Override
        public void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers) {
            list.add(Component.literal("[").append(Component.translatable("trait.ntmspace.confusion")).append("]")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /** hbm-space {@code HazardTypeGlitch}. */
    static final class Glitch implements IHazardType {
        @Override
        public void onUpdate(LivingEntity target, double level, ItemStack stack) {
            nausea(target, level, hazardRate);
        }

        @Override
        public void updateEntity(ItemEntity item, double level) {}

        @Override
        public void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers) {
            // 1.7 rolled the player's world random; tooltips built without a player (the creative search tree) roll a fresh one
            boolean world = player != null ? player.level().getRandom().nextInt(10) == 0 : new Random().nextInt(10) == 0;
            if (world) {
                list.add(Component.literal("WORLD").withStyle(ChatFormatting.DARK_RED));
                return;
            }
            // 1.7: a random item id below the registry's size, seeded by the half-second; an empty id said STACKTRACE0x00<r>
            Random rand = new Random(System.currentTimeMillis() / 500);
            int size = BuiltInRegistries.ITEM.size();
            int r = rand.nextInt(size);
            Item item = BuiltInRegistries.ITEM.byId(r);
            if (item != null) list.add(new ItemStack(item).getHoverName());
            else list.add(Component.literal("STACKTRACE0x00" + r).withStyle(ChatFormatting.RED));
        }
    }
}
