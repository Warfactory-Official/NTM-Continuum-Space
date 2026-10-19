package com.ntmspace.fork.bobble;

import com.hbm.creativetabs.HbmTab;
import com.hbm.tileentity.machine.TileEntityMachineShredder;
import com.ntmspace.fork.ForkContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * hbm-space's bobblehead behaviour outside the block: a Gwen bobblehead in a shredder's input blows the shredder up
 * ({@code TileEntityMachineShredder.canProcess}: the block is broken without drops and a strength-5 fiery, block-breaking
 * explosion goes off at its centre; HBM hook {@code TileEntityMachineShredder.INTERCEPT}, ADDON-API-INVENTORY H-70); the
 * creative list. Shredder rows (each to {@code hbm:scrap_plastic_board_blank}), the Gwen magic recipe and the hazard rows
 * are data.
 */
public final class SpaceBobbles {

    private SpaceBobbles() {}

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        ForkHazards.init();
        TileEntityMachineShredder.INTERCEPT.register((shredder, slot, stack) -> {
            if (!isGwen(stack)) return false;
            Level level = shredder.getLevel();
            if (level == null || level.isClientSide()) return true;
            BlockPos pos = shredder.getBlockPos();
            // 1.7: func_147480_a(x, y, z, false) then newExplosion(null, x + 0.5, y + 0.5, z + 0.5, 5, true, true)
            level.destroyBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5F, true, Level.ExplosionInteraction.TNT);
            return true;
        });
        com.hbm.creativetabs.ModTabs.addStacksTo(HbmTab.BLOCKS, () -> {
            List<ItemStack> stacks = new ArrayList<>();
            for (var item : ForkContent.BOBBLEHEAD_ITEMS) stacks.add(new ItemStack(item.get()));
            return stacks;
        });
    }

    /** hbm-space {@code bobblehead} with metadata {@code GWEN}. */
    public static boolean isGwen(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ForkContent.BOBBLEHEAD_ITEMS.get(BlockSpaceBobble.Type.GWEN.ordinal()).get();
    }
}
