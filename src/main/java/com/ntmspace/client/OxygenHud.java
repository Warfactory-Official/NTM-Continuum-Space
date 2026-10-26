package com.ntmspace.client;

import net.minecraft.world.entity.LivingEntity;
import com.ntmspace.breathing.Breathing;
import com.ntmspace.breathing.ItemModOxy;
import dev.golem.core.client.Hud;
import dev.golem.core.client.VanillaHud;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * hbm-space's oxygen HUD ({@code ModEventHandlerRenderer}, the AIR overlay): while suffocating for a reason other than
 * water (oxygen below 100) a row of air bubbles stands where vanilla's would, and with a PLSS worn a thin bar under it
 * shows the tank (grey frame, red to cyan as it fills). Both sit in the right-hand column above the food bar, where
 * vanilla puts its air row.
 */
public final class OxygenHud {

    private OxygenHud() {}

    public static void init() {
        VanillaHud.hideAir(() -> {
            var player = Minecraft.getInstance().player;
            return player != null && !player.isCreative() && !player.isSpectator() && replacesAir(player);
        });
        Hud.addLast(com.ntmspace.NtmSpace.MOD_ID, "oxygen", OxygenHud::render);
    }

    /** The air row sits above additional mount-health rows in vanilla's right status column. */
    public static int vanillaAirTop(Player player, int guiHeight) {
        int hearts = 0;
        if (player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth()) {
            hearts = Math.min((int) (vehicle.getMaxHealth() + 0.5F) / 2, 30);
        }
        int rows = (int) Math.ceil(hearts / 10.0);
        return guiHeight - 49 - Math.max(0, rows - 1) * 10;
    }

    public static boolean replacesAir(Player player) {
        return Breathing.getOxy(player) < Breathing.FULL_BREATH || !Breathing.getOxygenTank(player).isEmpty();
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int rightHeight = height - vanillaAirTop(player, height);

        // If we're suffocating for a reason other than water, render the HUD bubbles
        int air = Breathing.getOxy(player);
        if (air < 100) {
            int left = width / 2 + 91;
            int top = height - rightHeight;
            int full = Mth.ceil((double) (air - 2) * 10.0D / 100.0D);
            int partial = Mth.ceil((double) air * 10.0D / 100.0D) - full;
            for (int i = 0; i < full + partial; ++i) {
                VanillaHud.airBubble(graphics, left - i * 8 - 9, top, i >= full);
            }
            rightHeight += 10;
        }

        ItemStack tankStack = Breathing.getOxygenTank(player);
        if (!tankStack.isEmpty()) {
            ItemModOxy tank = (ItemModOxy) tankStack.getItem();
            float tot = (float) ItemModOxy.getFuel(tankStack) / (float) tank.getMaxFuel();
            int right = width / 2 + 91;
            int top = height - rightHeight + 3;
            // 1.7 drew half-pixel quads (frame at -0.5..+4.5); whole pixels here
            GuiDraw.fill(graphics, right - 82, top - 1, right + 1, top + 5, 0xFF404040);
            int r = (int) ((1F - tot) * 255), gb = (int) (tot * 255);
            GuiDraw.fill(graphics, right - (int) (81 * tot), top, right, top + 4, 0xFF000000 | r << 16 | gb << 8 | gb);
        }
    }
}
