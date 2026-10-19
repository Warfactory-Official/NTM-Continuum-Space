package com.ntmspace.block.atmosphere;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.atmosphere.TileEntityAirScrubber;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code BlockAirScrubber} ({@code air_scrubber}, "CO2 Scrubber"): two high ({@code getDimensions}
 * {1, 0, 0, 0, 0, 0}, offset 0). 1.7 never called {@code makeExtra}, so the upper block is a plain dummy; the core takes
 * power and gives CO2 on its own faces (all but the top, where the dummy stands).
 */
public class BlockAirScrubber extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public BlockAirScrubber(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAirScrubber(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {1, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    @Override public int coreMask() { return MASK_ALL & ~MASK_UP; }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityAirScrubber scrubber)) return null;
        List<String> text = new ArrayList<>();
        text.add((scrubber.getPower() <= 200 ? ChatFormatting.RED : ChatFormatting.GREEN) + "Power: "
                + BobMathUtil.getShortNumber(scrubber.getPower()) + "HE");
        text.add(ChatFormatting.RED + "<- " + ChatFormatting.RESET + scrubber.tank.getTankType().getDisplayNameString() + ": "
                + scrubber.tank.getFill() + "/" + scrubber.tank.getMaxFill() + "mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
