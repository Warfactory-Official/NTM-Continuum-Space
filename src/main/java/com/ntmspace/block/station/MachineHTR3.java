package com.ntmspace.block.station;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.fluid.trait.FT_Rocket;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.tileentity.station.TileEntityMachineHTR3;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code MachineHTR3}: 11 x 7 x 7 ({@code {3, 3, 3, 3, 5, 5}}, offset 5, three above the clicked block,
 * turned a quarter from the player's facing like the LPW-2), the propellant port at the end of its long axis. A fluid
 * identifier sets the tank to the heated form of the identified fluid, if that form is a rocket propellant.
 */
public class MachineHTR3 extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    private static final int[] DIMS = {3, 3, 3, 3, 5, 5};

    public MachineHTR3(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineHTR3(pos, state);
    }

    @Override public int[] getDimensions() { return DIMS.clone(); }
    @Override public int getOffset() { return 5; }
    @Override public int getHeightOffset() { return 3; }

    /** 1.7 {@code dir.getRotation(DOWN)}. */
    @Override
    public Direction getDirModified(Direction direction) {
        return ForgeDirection.getOrientation(direction).getRotation(ForgeDirection.DOWN).toDirection();
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        StationMachines.ports(core, DIMS, facing, List.of(TileEntityMachineHTR3.conPos(core, ForgeDirection.getOrientation(facing))),
                ROLE_FLUID_IN, visitor);
    }

    /** 1.7 {@code makeExtra} at the propellant end: {@code TileEntityProxyCombo(false, false, true)}. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(extra(core, facing), MASK_ALL, PASSIVE_FLUID_IN);
    }

    static BlockPos extra(BlockPos core, Direction facing) {
        ForgeDirection rot = ForgeDirection.getOrientation(facing).getRotation(ForgeDirection.UP);
        return core.offset(-rot.offsetX * 5, 0, -rot.offsetZ * 5);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide() || player.isShiftKeyDown()) return InteractionResult.SUCCESS;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof IItemFluidId identifier)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(core) instanceof TileEntityMachineHTR3 htr3)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), held);
        FT_Heatable heatable = type.getTrait(FT_Heatable.class);

        if (heatable != null && heatable.getFirstStep().typeProduced.hasTrait(FT_Rocket.class)) {
            htr3.tanks[0].setTankType(heatable.getFirstStep().typeProduced);
            htr3.setChanged();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!Orbit.inOrbit(level)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineHTR3 thruster)) return null;

        List<String> text = new ArrayList<>();
        if (!thruster.isFacingPrograde()) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.engineFacing").getString() + " ! ! !");
        } else {
            for (FluidTankNTM tank : thruster.tanks) {
                text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tank.getTankType().getDisplayNameString() + ": "
                        + tank.getFill() + "/" + tank.getMaxFill() + "mB");
            }
            if (pos.equals(extra(core, level.getBlockState(core).getValue(FACING)))) {
                text.add("Connect to PWR from here");
            }
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
