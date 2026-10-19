package com.ntmspace.block.dyson;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.ntmspace.tileentity.dyson.TileEntityDysonConverterAnatmogenesis;
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
 * hbm-space's {@code MachineDysonConverterAnatmogenesis}: {@code getDimensions} {2, 0, 5, 5, 1, 1}, offset 5; the beam's
 * intake is the front end (five ahead of the core, one up). A fluid identifier sets its gas (gases only, "Changed type
 * to ...!"), a screwdriver switches between emitting and capturing; the look overlay shows the rate in atm per hour,
 * the gas's current partial pressure on this body and the mode.
 */
public class MachineDysonConverterAnatmogenesis extends BlockMultiblockMachine implements ILookOverlay, IToolable {

    public MachineDysonConverterAnatmogenesis(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonConverterAnatmogenesis(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {2, 0, 5, 5, 1, 1}; }
    @Override public int getOffset() { return 5; }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityDysonConverterAnatmogenesis converter)) return null;

        CBT_Atmosphere atmosphere = CelestialTraits.getTrait(level, CBT_Atmosphere.class);
        double pressure = atmosphere != null ? atmosphere.getPressure(converter.fluid) : 0;
        if (pressure < 0.0001) pressure = 0;
        pressure = Math.round(pressure * 1_000.0) / 1_000.0;

        List<String> text = new ArrayList<>();
        text.add("Current rate: " + ((double) converter.gasProduced * 20 * 60 * 60 / CelestialTraits.MB_PER_ATM) + "atm per hour");
        text.add("Current gas: " + converter.fluid.getDisplayNameString() + " - " + pressure);
        text.add("Current mode: " + (converter.isEmitting ? "EMITTING" : "CAPTURING"));
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide()) return true;
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityDysonConverterAnatmogenesis converter)) return false;

        converter.isEmitting = !converter.isEmitting;
        converter.setChanged();
        return true;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemFluidId identifier)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityDysonConverterAnatmogenesis converter)) {
            return InteractionResult.FAIL;
        }

        FluidType type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        if (CelestialTraits.isGas(type)) {
            converter.fluid = type;
            converter.setChanged();
            player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(type.getDisplayName()).append(Component.literal("!")));
        }
        return InteractionResult.SUCCESS;
    }
}
