package com.ntmspace.block.station;

import com.hbm.tileentity.ModPowerGenBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.blocks.machine.MachineLPW2;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.BobMathUtil;
import com.ntmspace.NtmSpace;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.tileentity.station.TileEntityMachineLPW2Space;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.Substitutions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hbm-space's {@code MachineLPW2}, the "LPW-2 Station Thruster": HBM's 20 x 7 x 7 LPW-2 with the core nine blocks from
 * the clicked one (1.7 offset 9, HBM's is 3), turned a quarter from the player's facing ({@code dir.getRotation(DOWN)}),
 * two propellant proxies either side of the engine three blocks up ({@code TileEntityProxyCombo(false, false, true)}),
 * and a look overlay with the tanks, or a warning when it does not face prograde. Substituted for {@code hbm:machine_lpw2}
 * in {@code SpaceEntry.substitute} with HBM's own registration properties.
 */
public class SpaceMachineLPW2 extends MachineLPW2 implements IFluidConnectorBlockMK2, ILookOverlay, ICapabilityBlock {

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModPowerGenBlockEntities.LPW2).fluidIn()
                .fluidFaces(TileEntityMachineLPW2Space.class, (be, side) -> Orbit.inOrbit(be.getLevel()));
    }

    public SpaceMachineLPW2(Properties properties) {
        super(properties);
    }

    /**
     * Replaces {@code hbm:machine_lpw2}'s block (HBM's registration properties) and its block entity type, so chunks
     * load the station engine too; the type keeps HBM's id, renderer and {@code ModPowerGenBlockEntities.LPW2} handle.
     */
    public static void substitute() {
        Substitutions.block(NtmSpace.MOD_ID, "hbm:machine_lpw2", SpaceMachineLPW2::new);
        Substitutions.<BlockEntityType<?>>entry(NtmSpace.MOD_ID, "block_entity_type", "hbm:machine_lpw2",
                key -> BlockEntities.ticking(TileEntityMachineLPW2Space::new, TileEntityMachineLPW2Space::clientTick,
                        TileEntityMachineLPW2Space::serverTick, ModPowerGenBlocks.MACHINE_LPW2.get()));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineLPW2Space(pos, state);
    }

    @Override
    public int getOffset() {
        return 9;
    }

    /** 1.7 {@code dir.getRotation(DOWN)}. */
    @Override
    public Direction getDirModified(Direction direction) {
        return ForgeDirection.getOrientation(direction).getRotation(ForgeDirection.DOWN).toDirection();
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        StationMachines.ports(core, getDimensions(), facing,
                List.of(TileEntityMachineLPW2Space.conPos(core, ForgeDirection.getOrientation(facing))), ROLE_FLUID_IN, visitor);
    }

    /** 1.7 {@code makeExtra} at {@code core +- dir * 3 - rot}, three up: the cells next to the two ports. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        visitor.passiveCell(core.offset(dir.offsetX * 3 - rot.offsetX, 3, dir.offsetZ * 3 - rot.offsetZ), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-dir.offsetX * 3 - rot.offsetX, 3, -dir.offsetZ * 3 - rot.offsetZ), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!Orbit.inOrbit(level)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineLPW2Space thruster)) return null;

        List<String> text = new ArrayList<>();
        if (!thruster.isFacingPrograde()) {
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.engineFacing").getString() + " ! ! !");
        } else {
            for (FluidTankNTM tank : thruster.tanks) {
                text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tank.getTankType().getDisplayNameString() + ": "
                        + tank.getFill() + "/" + tank.getMaxFill() + "mB");
            }
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
