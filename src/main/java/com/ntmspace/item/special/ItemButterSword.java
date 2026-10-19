package com.ntmspace.item.special;

import dev.golem.core.world.CompatSwordItem;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * hbm-space {@code BSword} ({@code butter_sword}, texture {@code budder_sword}): a wooden sword that blows up any squid
 * it hits, a 2-strength explosion that breaks blocks (1.7 {@code createExplosion(attacker, x, y, z, 2F, true)}).
 */
public class ItemButterSword extends CompatSwordItem {

    /** Vanilla 1.7 {@code ToolMaterial.WOOD}: level 0, 59 uses, speed 2, damage 0, enchantability 15, repaired with planks. */
    public static final ToolTier WOOD = ToolTier.of(0, 59, 2.0F, 0.0F, 15, () -> ItemTags.PLANKS);

    public ItemButterSword(Properties properties) {
        // 1.7 ItemSword: 4 + the material's damage
        super(properties, WOOD, 4.0F + WOOD.attackDamageBonus(), Tools.SWORD_SPEED, 0.0D);
    }

    @Override
    protected void compatHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Squid && !target.level().isClientSide()) {
            target.level().explode(attacker, target.getX(), target.getY(), target.getZ(), 2.0F, Level.ExplosionInteraction.TNT);
        }
        super.compatHurtEnemy(stack, target, attacker);
    }
}
