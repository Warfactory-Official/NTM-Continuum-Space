package com.ntmspace.fork;

import com.hbm.api.data.DepositDataMaps;
import com.hbm.items.tool.ItemOilDetector;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's {@code ItemOilDetector} over HBM's {@code oil_detector} (substituted under its own id): it finds any fluid
 * deposit ({@code BlockOreFluid}: oil, gas, brine, bedrock oil on every body) and names the reserve: "bullseye" for one
 * in the player's own column, "detected" for one in the 5- and 10-block ring around it (searched in hbm-space's order,
 * the first found decides), with {@code _gas} / {@code _brine} for those reserves. Colours: dark green, gold, red.
 *
 * <p>The column scan runs from 15 above the player down to 5 above the world's floor (1.7: {@code i > 5}, with a floor
 * at 0). Messages go to chat with NTM: Space keys ({@code item.ntmspace.oil_detector.*}); 1.7 sent them as a HUD notice.
 */
public class SpaceOilDetector extends ItemOilDetector {

    public SpaceOilDetector(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            int x = Mth.floor(player.getX());
            int y = Mth.floor(player.getY());
            int z = Mth.floor(player.getZ());

            boolean direct = false;
            DepositDataMaps.Deposit reserve = searchDirect(level, x, y, z);
            if (reserve != null) {
                direct = true;
            } else {
                reserve = search(level, x, y, z);
            }
            String type = reserve == null ? "" : FluidOres.reserve(reserve).suffix;

            if (direct) {
                player.sendSystemMessage(Component.translatable("item.ntmspace.oil_detector.bullseye" + type).withStyle(ChatFormatting.DARK_GREEN));
            } else if (reserve != null) {
                player.sendSystemMessage(Component.translatable("item.ntmspace.oil_detector.detected" + type).withStyle(ChatFormatting.GOLD));
            } else {
                player.sendSystemMessage(Component.translatable("item.ntmspace.oil_detector.noOil").withStyle(ChatFormatting.RED));
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    /** hbm-space {@code search}: the 5-ring, the 10-ring, then the 5-diagonals; the first column with a deposit wins. */
    public static DepositDataMaps.@Nullable Deposit search(Level level, int x, int y, int z) {
        int[][] offsets = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}, {10, 0}, {-10, 0}, {0, 10}, {0, -10}, {5, 5}, {-5, 5}, {5, -5}, {-5, -5}};
        for (int[] o : offsets) {
            DepositDataMaps.Deposit reserve = searchDirect(level, x + o[0], y, z + o[1]);
            if (reserve != null) return reserve;
        }
        return null;
    }

    /** hbm-space {@code searchDirect}: from {@code y + 15} down to 5 above the floor, the first deposit block. */
    public static DepositDataMaps.@Nullable Deposit searchDirect(Level level, int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = level.getMinY() + 5;
        for (int i = y + 15; i > floor; i--) {
            DepositDataMaps.Deposit deposit = DepositDataMaps.deposit(level.getBlockState(pos.set(x, i, z)));
            if (deposit != null) return deposit;
        }
        return null;
    }
}
