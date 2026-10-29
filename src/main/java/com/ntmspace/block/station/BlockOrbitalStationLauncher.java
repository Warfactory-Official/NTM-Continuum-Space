package com.ntmspace.block.station;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModFuelItems;
import com.hbm.util.BobMathUtil;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.registry.StationLauncherBlocks;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationLauncher;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code BlockOrbitalStationLauncher}: the docking port's 5 x 5 shape and rim ({@link BlockOrbitalStation},
 * an extra port: placeable, hardness 1) with a launcher core. Only works in orbit. Use: a docked rocket is boarded
 * (nothing happens while it is occupied); otherwise the launcher's screen opens. Clicks on the rim are ignored so
 * blocks can be placed against it. The look overlay names the fuels a valid rocket still needs, or the docked rocket's
 * state.
 *
 * <p>1.7's rim cells were {@code TileEntityProxyCombo(true, true, true)} (items, power, fluid); the launcher takes no
 * power, so the rim keeps the port's items and fluid.
 */
public class BlockOrbitalStationLauncher extends BlockOrbitalStation {

    public BlockOrbitalStationLauncher(Properties properties) {
        super(properties, false);
    }

    /**
     * The launcher owns its fuel tanks; only its subscriptions require an assembled rocket.
     */
    @Override
    public MachineCaps caps() {
        return MachineCaps.of(StationLauncherBlocks.ORBITAL_STATION_LAUNCHER_BE).fluidIn().itemsAtCells();
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityOrbitalStationLauncher(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos corePos, Player player,
                                          BlockHitResult hit) {
        if (!Orbit.inOrbit(level)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        // If activating the side blocks, ignore, to allow placing
        BlockPos clicked = hit.getBlockPos();
        if (Math.abs(corePos.getX() - clicked.getX()) >= 2 || Math.abs(corePos.getZ() - clicked.getZ()) >= 2) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!(level.getBlockEntity(corePos) instanceof TileEntityOrbitalStationLauncher station))
            return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (station.getDocked() != null) {
            if (player.isShiftKeyDown()) {
                if (player.getMainHandItem().isEmpty()) station.retrieveDockedCraft(player);
            } else {
                station.enterCapsule(player);
            }
            return InteractionResult.SUCCESS;
        }

        return openMenu(state, level, corePos, player, hit);
    }

    /**
     * 1.7 {@code standardOpenBehavior}: the multiblock's menu.
     */
    private InteractionResult openMenu(BlockState state, Level level, BlockPos corePos, Player player,
                                       BlockHitResult hit) {
        if (!(level.getBlockEntity(corePos) instanceof net.minecraft.world.MenuProvider menu))
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer))
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        dev.golem.core.gui.Menus.openWithPos(serverPlayer, menu, corePos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        String title = getName().getString();
        if (!Orbit.inOrbit(level)) {
            List<String> text = new ArrayList<>();
            text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                    + Component.translatable("atmosphere.noOrbit").getString() + " ! ! !");
            return new ILookOverlay.Data(title, 0xffff00, 0x404000, text);
        }

        BlockPos corePos = MultiblockSurface.coreOfAny(level, pos);
        if (corePos == null) return null;
        if (!(level.getBlockEntity(corePos) instanceof TileEntityOrbitalStationLauncher pad)) return null;

        List<String> text = new ArrayList<>();
        if (pad.hasDocked) {
            if (!pad.hasRider) {
                text.add(Component.translatable("station.enterRocket").getString());
            } else {
                text.add(ChatFormatting.YELLOW + Component.translatable("station.occupiedRocket").getString());
            }

            return new ILookOverlay.Data(title, 0xffff00, 0x404000, text);
        }

        if (pad.rocket == null || !pad.rocket.validate()) return null;

        text.add("Required fuels:");

        for (FluidTankNTM tank : pad.tanks) {
            if (tank.getTankType() == Fluids.NONE) continue;
            text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tank.getTankType()
                                                                               .getDisplayNameString() + ": "
                    + tank.getFill() + "/" + tank.getMaxFill() + "mB");
        }

        if (pad.solidFuel.max > 0) {
            text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET
                    + ModFuelItems.rocket_fuel.get().getName(ModFuelItems.rocket_fuel.get().getDefaultInstance())
                                              .getString()
                    + ": " + pad.solidFuel.level + "/" + pad.solidFuel.max + "kg");
        }

        if (text.size() <= 1) return null;

        return new ILookOverlay.Data(title, 0xffff00, 0x404000, text);
    }
}
