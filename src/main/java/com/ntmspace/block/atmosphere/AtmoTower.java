package com.ntmspace.block.atmosphere;

import com.hbm.blocks.machine.MachineDeuteriumTower;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmoTower;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code AtmoTower} ({@code machine_atmo_tower}): the deuterium tower's frame seven blocks high
 * ({@code getDimensions} {6, 0, 1, 0, 0, 1}, offset 0, the same 2x2 base, three {@code TileEntityProxyCombo(false, true,
 * true)} base cells and eight ports), so the frame, cells and ports are HBM's {@link MachineDeuteriumTower}'s.
 */
public class AtmoTower extends MachineDeuteriumTower {

    public AtmoTower(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAtmoTower(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[] {6, 0, 1, 0, 0, 1};
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityAtmoTower tower)) return null;
        List<String> text = new ArrayList<>();
        text.add((tower.power < tower.getMaxPower() / 20 ? ChatFormatting.RED : ChatFormatting.GREEN) + "Power: "
                + BobMathUtil.getShortNumber(tower.power) + "HE");
        for (int i = 0; i < tower.tanks.length; i++) {
            text.add((i < 1 ? (ChatFormatting.GREEN + "-> ") : (ChatFormatting.RED + "<- ")) + ChatFormatting.RESET
                    + tower.tanks[i].getTankType().getDisplayNameString() + ": " + tower.tanks[i].getFill() + "/"
                    + tower.tanks[i].getMaxFill() + "mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
