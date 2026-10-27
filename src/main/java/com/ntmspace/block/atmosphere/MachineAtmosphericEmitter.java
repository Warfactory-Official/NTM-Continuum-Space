package com.ntmspace.block.atmosphere;

import com.ntmspace.atmosphere.AtmosphereText;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.block.machine.MachinePorts;
import com.ntmspace.tileentity.atmosphere.TileEntityAtmosphericEmitter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * hbm-space's {@code MachineAtmosphericEmitter} ({@code machine_atmo_emitter}): a 2x2 base three high
 * ({@code getDimensions} {2, 0, 1, 0, 1, 0}, offset 0), the three base cells besides the core
 * {@code TileEntityProxyCombo().power().fluid()}, eight ports (power and the gas) around the base. A fluid identifier sets
 * the gas, only a gaseous one ("Changed type to ...!").
 *
 * <p>1.7's {@code fillSpace} mixed x and z in two of its three {@code makeExtra} positions; worked through for every
 * facing they still name exactly the three non-core base cells, which is what is ported.
 */
public class MachineAtmosphericEmitter extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    static final int[] DIMS = {2, 0, 1, 0, 1, 0};

    public MachineAtmosphericEmitter(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAtmosphericEmitter(pos, state);
    }

    @Override public int[] getDimensions() { return DIMS.clone(); }
    @Override public int getOffset() { return 0; }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        orPorts(core, facing, TileEntityAtmosphericEmitter.conPos(core, MachinePorts.dir(facing)), visitor);
    }

    /**
     * {@link MachinePorts#ports} with every face of a cell gathered first: a base corner touches two 1.7 points, and the
     * mask table keeps only the last mask written per cell and role.
     */
    static void orPorts(BlockPos core, Direction facing, DirPos[] conPos, CellVisitor visitor) {
        Map<BlockPos, Integer> masks = new LinkedHashMap<>();
        MachinePorts.ports(core, facing, pos -> MachinePorts.inBox(core, DIMS, facing, pos), conPos,
                ROLE_POWER_IN | ROLE_FLUID_IN, new CellVisitor() {
                    @Override public void cell(BlockPos pos, int mask) { masks.merge(pos.immutable(), mask, (a, b) -> a | b); }
                    @Override public void cell(BlockPos pos, int mask, int roles) { cell(pos, mask); }
                });
        masks.forEach((pos, mask) -> visitor.cell(pos, mask, ROLE_POWER_IN | ROLE_FLUID_IN));
    }

    @Override
    public int coreMask() {
        return MachinePorts.coreFaces(BlockPos.ZERO, Direction.SOUTH,
                TileEntityAtmosphericEmitter.conPos(BlockPos.ZERO, ForgeDirection.SOUTH));
    }

    /** The three base cells besides the core: {@code TileEntityProxyCombo().power().fluid()}. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        ForgeDirection dir = MachinePorts.dir(facing), rot = MachinePorts.rot(facing);
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(MachinePorts.offset(core, dir, -1, rot, 1, 0), MASK_ALL, domains);
        visitor.passiveCell(MachinePorts.offset(core, dir, -1, rot, 0, 0), MASK_ALL, domains);
        visitor.passiveCell(MachinePorts.offset(core, dir, 0, rot, 1, 0), MASK_ALL, domains);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        // as HBM's drain (same 1.7 click code): only an identifier is used here
        if (!(stack.getItem() instanceof IItemFluidId identifier)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide() || player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityAtmosphericEmitter emitter)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos core = emitter.getBlockPos();
        var type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
        AtmosphereText.retypeGaseous(type, player, emitter.tank, emitter);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return level.isClientSide() || player.isShiftKeyDown() ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityAtmosphericEmitter drain)) return null;
        List<String> text = new ArrayList<>();
        text.add((drain.isRunning() ? ChatFormatting.GREEN : ChatFormatting.RED) + "Power: "
                + BobMathUtil.getShortNumber(drain.power) + "HE");
        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + drain.tank.getTankType().getDisplayNameString() + ": "
                + drain.tank.getFill() + "/" + drain.tank.getMaxFill() + "mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
