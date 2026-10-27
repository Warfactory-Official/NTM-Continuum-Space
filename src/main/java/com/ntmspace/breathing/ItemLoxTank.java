package com.ntmspace.breathing;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * hbm-space's {@code lox_tank} ({@code ItemSyringe} branch, "Oxygen Tank"): used, it fills the PLSS in the worn helmet
 * with 1000 mB of oxygen and is used up if any of it went in.
 */
public class ItemLoxTank extends CompatItem {

    private static final int CHARGE = 1000;

    public ItemLoxTank(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!ArmorModHandler.hasMods(helmet)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        var tankStack = ArmorModHandler.pryMod(helmet, ArmorModHandler.plate_only);
        if (!(tankStack.getItem() instanceof IFillableItem fillable)
                || !fillable.acceptsFluid(Fluids.OXYGEN, tankStack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (fillable.tryFill(Fluids.OXYGEN, CHARGE, tankStack) >= CHARGE) return InteractionResult.TRY_WITH_EMPTY_HAND;

        ArmorModHandler.applyMod(helmet, tankStack);
        var stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.JETPACK_TANK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Fills a worn PLSS with 1000mB of oxygen"));
    }
}
