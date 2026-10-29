package com.ntmspace.fork.client;

import com.hbm.blocks.ModPneumaticBlocks;
import com.hbm.client.jetpack.JetpackClientVisuals;
import com.hbm.client.render.BeamPronter;
import dev.golem.core.client.Tooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Client half of {@code ForkBaseChanges} (hbm-space's small changes to base-HBM classes):
 * <ul>
 *   <li>{@code ModelArmorWings}: worn wings fold while the wearer rides something (V-184);</li>
 *   <li>the pneumatic tube's tooltip gains hbm-space's last line, "Input air type can be changed with a fluid ID"
 *       ({@code tile.pneumatic_tube.desc}), since its compressors take any gas with NTM: Space.</li>
 * </ul>
 */
public final class ForkBaseClient {

    private ForkBaseClient() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        JetpackClientVisuals.relaxWhileRiding = true;
        JetpackClientVisuals.directionalArmorPose = true;
        BeamPronter.alpha = 0.5F;
        Tooltips.onItemTooltip((stack, lines) -> {
            if (stack.is(ModPneumaticBlocks.PNEUMATIC_TUBE.get().asItem())) {
                lines.add(Component.translatable("block.ntmspace.pneumatic_tube.desc_gas").withStyle(ChatFormatting.YELLOW));
            }
        });
    }

}
