package com.ntmspace.block.station;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.ntmspace.orbit.NavDrives;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.OrbitalStation;
import com.ntmspace.orbit.Propulsion;
import com.ntmspace.tileentity.station.TileEntityOrbitalStationComputer;
import com.ntmspace.world.SolarSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code BlockOrbitalStationComputer}: one block (a {@code BlockDummyable} of size 0) that steers the station.
 * Use with a navigation drive for a body starts the journey there and keeps the drive; use with an empty hand takes the
 * drive back; otherwise it opens the station screen (name, gravity). Nothing while travelling, nothing outside orbit.
 */
public class BlockOrbitalStationComputer extends BlockMultiblockMachine implements ILookOverlay {

    public BlockOrbitalStationComputer(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityOrbitalStationComputer(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {0, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!Orbit.inOrbit(level)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof TileEntityOrbitalStationComputer computer)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (computer.isTravelling()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        ItemStack heldStack = player.getMainHandItem();
        NavDrives.Destination destination = NavDrives.destination(heldStack);

        if (destination != null) {
            if (!computer.getItem(0).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (destination.isOrbit()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            SolarSystem.Body body = SolarSystem.body(destination.body());
            if (body == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

            if (computer.travelTo(body, heldStack.copy())) {
                heldStack.setCount(0);
                level.playSound(null, hit.getBlockPos(), com.hbm.sound.ModSounds.UPGRADE_PLUG.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
        } else if (heldStack.isEmpty() && !computer.getItem(0).isEmpty()) {
            ItemStack drive = computer.getItem(0).copy();
            if (!player.getInventory().add(drive)) player.drop(drive, false);
            computer.setItem(0, ItemStack.EMPTY);
            computer.markChanged();
        } else if (player instanceof ServerPlayer serverPlayer) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, computer, core);
        }

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

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityOrbitalStationComputer computer)) return null;

        OrbitalStation station = OrbitalStation.clientStation;
        double progress = station.getUnscaledProgress(0);
        List<String> text = new ArrayList<>();

        if (!station.hasEngines) {
            text.add(ChatFormatting.RED + "No engines available");
        } else if (station.errorsAt.size() > 0) {
            for (BlockPos errorAt : station.errorsAt) {
                BlockEntity error = level.getBlockEntity(errorAt);
                if (!(error instanceof Propulsion propulsion)) continue;

                List<Component> errors = new ArrayList<>();
                propulsion.addErrors(errors);

                if (!errors.isEmpty()) {
                    text.add(ChatFormatting.RED + error.getBlockState().getBlock().getName().getString());
                    for (Component line : errors) text.add(line.getString());
                }
            }
        } else if (progress > 0) {
            String target = Component.translatable("body." + station.target.name).getString();
            if (station.state == OrbitalStation.StationState.LEAVING) {
                text.add(ChatFormatting.AQUA + Component.translatable("station.engage").getString() + ": " + ChatFormatting.RESET + target);
            } else if (station.state == OrbitalStation.StationState.ARRIVING) {
                text.add(ChatFormatting.AQUA + Component.translatable("station.disengage").getString());
            } else {
                text.add(ChatFormatting.AQUA + Component.translatable("station.travelling").getString() + ": " + ChatFormatting.RESET + target);
            }
            text.add(ChatFormatting.AQUA + Component.translatable("station.progress").getString() + ": " + ChatFormatting.RESET
                    + Math.round(progress * 100) + "%");
        } else if (computer.hasDrive) {
            if (!player.getMainHandItem().isEmpty()) {
                text.add(Component.translatable("station.removeDrive").getString());
            } else {
                text.add(Component.translatable("station.interactDrive").getString());
            }
        } else {
            text.add(Component.translatable("station.insertDrive").getString());
        }

        return new ILookOverlay.Data(title, 0xffff00, 0x404000, text);
    }
}
