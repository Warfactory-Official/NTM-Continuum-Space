package com.ntmspace.block.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.tileentity.machine.TileEntityMachineAlkylation;
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

/**
 * hbm-space's {@code MachineAlkylation}: 3 x 4 x 5 ({@code {3, 0, 2, 2, 1, 1}}, offset 2), no GUI. Six
 * {@code TileEntityProxyCombo(false, true, true)} cells (power and fluid) either side of the long axis; the ten 1.7
 * connection points take power and both input fluids and send both outputs. Using a fluid identifier on it (not sneaking)
 * sets the primary input's type and says so in chat.
 */
public class MachineAlkylation extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    private static final int[] DIMS = {3, 0, 2, 2, 1, 1};

    public MachineAlkylation(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAlkylation(pos, state);
    }

    @Override public int[] getDimensions() { return DIMS.clone(); }
    @Override public int getOffset() { return 2; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MachinePorts.ports(core, facing, pos -> MachinePorts.inBox(core, DIMS, facing, pos),
                TileEntityMachineAlkylation.conPos(core, MachinePorts.dir(facing)), ROLE_POWER_IN | ROLE_FLUID, visitor);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        ForgeDirection dir = MachinePorts.dir(facing), rot = MachinePorts.rot(facing);
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        for (int side : new int[] {1, -1}) {
            for (int along : new int[] {0, 2, -2}) {
                visitor.passiveCell(MachinePorts.offset(core, dir, along, rot, side, 0), MASK_ALL, domains);
            }
        }
    }

    /** 1.7 {@code onBlockActivated}: a fluid identifier sets the input type; anything else does nothing. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(stack.getItem() instanceof IItemFluidId ident)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineAlkylation alkylation)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        FluidType type = ident.getType(level, core.getX(), core.getY(), core.getZ(), stack);
        alkylation.tanks[0].setTankType(type);
        alkylation.setChanged();
        player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                .append(type.getDisplayName()).append(Component.literal("!")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineAlkylation alkylation)) return null;
        List<String> text = new ArrayList<>();
        text.add((alkylation.power < alkylation.getMaxPower() / 20 ? ChatFormatting.RED : ChatFormatting.GREEN)
                + "Power: " + BobMathUtil.getShortNumber(alkylation.power) + "HE");
        for (int i = 0; i < alkylation.tanks.length; i++) {
            if (alkylation.tanks[i].getTankType() == Fluids.NONE) continue;
            text.add((i < 2 ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ") + ChatFormatting.RESET
                    + alkylation.tanks[i].getTankType().getDisplayNameString() + ": " + alkylation.tanks[i].getFill() + "/"
                    + alkylation.tanks[i].getMaxFill() + "mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
