package com.ntmspace.block.atmosphere;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.machine.MachineDeuteriumTower;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericCompressor;
import com.ntmspace.trait.CBT_Atmosphere;
import com.ntmspace.trait.CelestialTraits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * hbm-space's {@code BlockAtmosphericCompressor} ({@code machine_atmo_vent}, "Atmospheric Compressor"): the deuterium
 * tower's frame four blocks high ({@code getDimensions} {3, 0, 1, 0, 0, 1}, offset 0, same base, cells and ports). A
 * screwdriver cycles the gas it collects; a fluid identifier picks one the air has ("Changed type to ...!").
 */
public class BlockAtmosphericCompressor extends MachineDeuteriumTower implements IToolable {

    public BlockAtmosphericCompressor(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAtmosphericCompressor(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[] {3, 0, 1, 0, 0, 1};
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide()) return true;
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityAtmosphericCompressor tile)) return false;
        tile.cycleGas();
        tile.setChanged();
        return true;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(stack.getItem() instanceof IItemFluidId identifier)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityAtmosphericCompressor compressor)) {
            return InteractionResult.SUCCESS;
        }
        // 1.7 asked the identifier at the clicked block, not the core
        FluidType type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        if (compressor.switchGas(type)) {
            compressor.setChanged();
            player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(type.getDisplayName()).append(Component.literal("!")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityAtmosphericCompressor tower)) return null;
        List<String> text = new ArrayList<>();
        if (!CelestialTraits.hasTrait(level, CBT_Atmosphere.class)) {
            text.add(ChatFormatting.RED + "ERROR: " + ChatFormatting.RESET + "CANNOT COLLECT IN VACUUM");
        } else {
            text.add((tower.power < tower.getMaxPower() / 20 ? ChatFormatting.RED : ChatFormatting.GREEN) + "Power: "
                    + BobMathUtil.getShortNumber(tower.power) + "HE");
            text.add(ChatFormatting.RED + "<- " + ChatFormatting.RESET + tower.tank.getTankType().getDisplayNameString() + ": "
                    + tower.tank.getFill() + "/" + tower.tank.getMaxFill() + "mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
