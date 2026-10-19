package com.ntmspace.fork.client;

import com.hbm.blocks.machine.BlockRefueler;
import com.hbm.blocks.ModPneumaticBlocks;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.machine.TileEntityRefueler;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Client half of {@code ForkBaseChanges} (hbm-space's small changes to base-HBM classes):
 * <ul>
 *   <li>{@code ModelArmorWings}: worn wings fold while the wearer rides something (V-184);</li>
 *   <li>{@code BlockRefueler}: looking at a refueler shows its tank's fluid in the fluid's colour ({@code ILookOverlay},
 *       title in yellow on dark yellow; HBM's {@code ILookOverlay.PROVIDERS}, C-26);</li>
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
        com.hbm.client.jetpack.JetpackClientVisuals.relaxWhileRiding = true;
        ILookOverlay.PROVIDERS.register(ForkBaseClient::refueler);
        dev.golem.core.client.Tooltips.onItemTooltip((stack, lines) -> {
            if (stack.is(ModPneumaticBlocks.PNEUMATIC_TUBE.get().asItem())) {
                lines.add(Component.translatable("block.ntmspace.pneumatic_tube.desc_gas").withStyle(ChatFormatting.YELLOW));
            }
        });
    }

    /** hbm-space {@code BlockRefueler.printHook}. */
    static ILookOverlay.Data refueler(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                                      net.minecraft.world.entity.player.Player player) {
        if (!(level.getBlockState(pos).getBlock() instanceof BlockRefueler block)
                || !(level.getBlockEntity(pos) instanceof TileEntityRefueler refueler)) return null;
        var type = refueler.tank.getTankType();
        return new ILookOverlay.Data(block.getName().getString(), 0xffff00, 0x404000,
                List.of("&[" + type.getColor() + "&]" + type.getDisplayNameString()));
    }
}
