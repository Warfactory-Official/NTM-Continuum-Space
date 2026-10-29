package com.ntmspace.fork.client;

import com.ntmspace.fork.ForkContent;
import dev.golem.core.Ids;
import dev.golem.core.client.Hud;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * hbm-space's two effect overlays ({@code ModEventHandlerClient}): while {@code flashbang} is active the whole screen is
 * covered by {@code overlay_flashbang.png} ({@code RenderScreenOverlay.renderFlashbangOverlay}, alpha-blended); while
 * {@code nitan} is active the health row is drawn over by the 81 x 12 "taint" bar of hbm-space's {@code overlay_misc.png}
 * (its cell 146,18; {@code renderTaintBar}, at {@code width / 2 - 92, height - 41}, after the hearts).
 */
public final class ForkHud {

    private ForkHud() {}

    private static final Identifier FLASHBANG = Ids.of("ntmspace", "textures/misc/overlay_flashbang.png");
    private static final Identifier TAINT_BAR = Ids.of("ntmspace", "textures/misc/overlay_taint_bar.png");

    public static void init() {
        Hud.addBeforeChat("ntmspace", "fork_effects", ForkHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // 1.7 drew it in the HEALTH Post event, only where hearts are drawn (survival / adventure)
        if (dev.golem.core.world.Effects.hasRegistered(player, ForkContent.NITAN_EFFECT.get())
                && !player.isCreative() && !player.isSpectator()) {
            GuiDraw.blit(graphics, TAINT_BAR, width / 2 - 92, height - 41, 0F, 0F, 81, 12, 81, 12);
        }
        if (dev.golem.core.world.Effects.hasRegistered(player, ForkContent.FLASHBANG_EFFECT.get())) {
            GuiDraw.blitUvFullscreen(graphics, FLASHBANG, 0F, 0F, 1F, 1F);
        }
    }
}
