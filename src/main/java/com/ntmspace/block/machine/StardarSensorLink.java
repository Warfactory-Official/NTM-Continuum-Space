package com.ntmspace.block.machine;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.machine.ItemReactorSensor;
import com.hbm.sound.ModSounds;
import com.ntmspace.tileentity.machine.TileEntityDishControl;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's additions to {@code ItemReactorSensor.onItemUse}, through HBM's {@link ItemReactorSensor#USE} hook: used
 * on a StarDar the sensor remembers the clicked position (the same {@code hbm:reactor_sensor_target} component as for a
 * research reactor, a packed block position); used on a remote access console it links the console to that StarDar.
 * Sneaking skips the block's own use (the StarDar would open its GUI), as 1.7's sneak-click did; the console's own use
 * passes the click on to the sensor while it is not linked.
 */
public final class StardarSensorLink {

    private StardarSensorLink() {}

    public static void register() {
        ItemReactorSensor.USE.register(StardarSensorLink::use);
    }

    static @Nullable InteractionResult use(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block block = level.getBlockState(pos).getBlock();

        if (block instanceof MachineStardar) {
            if (!level.isClientSide()) message(player, stack, "Position Set!");
            ItemReactorSensor.TARGET.set(stack, pos.asLong());
            boop(level, player, context);
            return InteractionResult.SUCCESS;
        }

        if (block instanceof MachineDishControl) {
            if (ItemReactorSensor.getTarget(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

            // Get Core
            BlockPos core = MultiblockSurface.coreOfAny(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof TileEntityDishControl control)) return InteractionResult.TRY_WITH_EMPTY_HAND;

            // 1.7 reports success whether or not the link took
            if (!level.isClientSide()) {
                message(player, stack, "Stardar Linked!");
                control.tryLink(stack);
            }

            boop(level, player, context);
            return InteractionResult.SUCCESS;
        }

        return null;
    }

    private static void boop(Level level, @Nullable Player player, UseOnContext context) {
        if (player == null) return;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(context.getHand());
    }

    /** 1.7 {@code SendMessage}: {@code [<sensor name>] text}, the brackets and name dark aqua, the text green. */
    private static void message(@Nullable Player player, ItemStack stack, String text) {
        if (player == null) return;
        player.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(stack.getItem().getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal(text).withStyle(ChatFormatting.GREEN)));
    }
}
