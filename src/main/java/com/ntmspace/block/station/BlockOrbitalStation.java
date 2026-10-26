package com.ntmspace.block.station;

import com.ntmspace.registry.StationBlocks;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.BobMathUtil;
import com.ntmspace.atmosphere.BlockSealable;
import com.ntmspace.orbit.Orbit;
import com.ntmspace.orbit.StationDock;
import com.ntmspace.tileentity.station.TileEntityOrbitalStation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * hbm-space's {@code BlockOrbitalStation}: a 5 x 5 docking port one block thick with a raised rim
 * ({@code getDimensions {1, 0, 2, 2, 2, 2}}, offset 2). Two blocks share the class: {@code orbital_station}, the core
 * every station is launched with (unbreakable, cannot be placed by hand), and {@code orbital_station_port}, extra ports
 * a player builds. Both only work in orbit.
 *
 * <p>The twelve rim cells ({@code makeExtra}) take fluid (the port's hydrazine tank) and items, 1.7's
 * {@code TileEntityProxyCombo(true, false, true)}; the core's own connections face outward from the rim.
 *
 * <p>Use: stored items come back first; then a docked craft is boarded (or, sneaking with an empty hand, stowed into
 * the port's slots); then a held craft item is docked ({@link StationDock#SPAWNERS}). Clicks on the rim are ignored so
 * blocks can be placed against it.
 */
public class BlockOrbitalStation extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay, BlockSealable, ICapabilityBlock {

    private final boolean core;

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(StationBlocks.ORBITAL_STATION_BE).fluidIn().itemsAtCells()
                .fluidFaces(TileEntityOrbitalStation.class, (be, side) -> be.needsFuel);
    }

    public BlockOrbitalStation(Properties properties, boolean core) {
        super(properties);
        this.core = core;
    }

    /** Whether this is the station core ({@code orbital_station}) rather than an extra port. */
    public boolean isCore() {
        return core;
    }

    /** Core cells are the same block as their unbreakable owner, so every part has the same removal rule. */
    public static boolean refusesRemoval(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof BlockOrbitalStation station && station.isCore();
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityOrbitalStation(pos, state);
    }

    @Override public int[] getDimensions() { return new int[] {1, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return core ? null : super.getStateForPlacement(context);
    }

    /** 1.7 {@code canPlaceBlockAt}: no extra main ports by hand (use the dedicated sub-ports!). */
    @Override
    protected boolean checkRequirement(Level level, BlockPos corePos, Direction facing, BlockPos clicked) {
        if (core) return false;
        return super.checkRequirement(level, corePos, facing, clicked);
    }

    /** The twelve rim cells of 1.7's {@code fillSpace}, each open on its outward face. */
    @Override
    protected void visitCells(BlockPos corePos, Direction facing, CellVisitor visitor) {
        super.visitCells(corePos, facing, visitor);
        for (int i = -1; i <= 1; i++) {
            rim(visitor, corePos.offset(2, 1, i), Direction.EAST, facing);
            rim(visitor, corePos.offset(-2, 1, i), Direction.WEST, facing);
            rim(visitor, corePos.offset(i, 1, 2), Direction.SOUTH, facing);
            rim(visitor, corePos.offset(i, 1, -2), Direction.NORTH, facing);
        }
    }

    /** 1.7 {@code getConPos} (world axes): the twelve rim cells, each aimed out of its outward face. */
    public static PortDef[] rimPorts(BlockPos corePos) {
        PortDef[] ports = new PortDef[12];
        int n = 0;
        for (int i = -1; i <= 1; i++) {
            ports[n++] = PortDef.make(corePos.offset(i, 1, 2), ForgeDirection.SOUTH);
            ports[n++] = PortDef.make(corePos.offset(i, 1, -2), ForgeDirection.NORTH);
            ports[n++] = PortDef.make(corePos.offset(2, 1, i), ForgeDirection.EAST);
            ports[n++] = PortDef.make(corePos.offset(-2, 1, i), ForgeDirection.WEST);
        }
        return ports;
    }

    private static void rim(CellVisitor visitor, BlockPos pos, Direction outward, Direction facing) {
        visitor.cell(pos, maskOf(MultiblockMaskTable.toLocal(outward, facing)), ROLE_FLUID_IN);
    }

    /** 1.7 {@code TileEntityProxyCombo(true, false, true)}: inventory and fluid on every rim cell. */
    @Override
    protected void visitPassiveCells(BlockPos corePos, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int i = -1; i <= 1; i++) {
            visitor.passiveCell(corePos.offset(2, 1, i), MASK_ALL, domains);
            visitor.passiveCell(corePos.offset(-2, 1, i), MASK_ALL, domains);
            visitor.passiveCell(corePos.offset(i, 1, 2), MASK_ALL, domains);
            visitor.passiveCell(corePos.offset(i, 1, -2), MASK_ALL, domains);
        }
    }

    static int maskOf(Direction local) {
        return switch (local) {
            case NORTH -> MASK_NORTH;
            case SOUTH -> MASK_SOUTH;
            case WEST -> MASK_WEST;
            case EAST -> MASK_EAST;
            case UP -> MASK_UP;
            case DOWN -> MASK_DOWN;
        };
    }

    @Override
    public boolean isSealed(Level level, BlockPos pos) {
        return true;
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos corePos, Player player, BlockHitResult hit) {
        if (!Orbit.inOrbit(level)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        // If activating the side blocks, ignore, to allow placing
        BlockPos clicked = hit.getBlockPos();
        if (Math.abs(corePos.getX() - clicked.getX()) >= 2 || Math.abs(corePos.getZ() - clicked.getZ()) >= 2) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(corePos) instanceof TileEntityOrbitalStation station)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (station.hasStoredItems()) {
            station.giveStoredItems(player);
        } else if (station.hasDocked) {
            if (!station.hasRider) {
                if (player.isShiftKeyDown()) {
                    if (player.getMainHandItem().isEmpty()) {
                        station.despawnRocket();
                        station.giveStoredItems(player);
                    }
                } else {
                    station.enterCapsule(player);
                }
            }
        } else {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty()) {
                StationDock dock = StationDock.SPAWNERS.invoker().spawn(station, held);
                if (dock != null) {
                    station.dockRocket(dock);
                    held.shrink(1);
                }
            }
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

        BlockPos corePos = MultiblockSurface.coreOfAny(level, pos);
        if (corePos == null) return null;
        if (Math.abs(corePos.getX() - pos.getX()) >= 2 || Math.abs(corePos.getZ() - pos.getZ()) >= 2) return null;
        if (!(level.getBlockEntity(corePos) instanceof TileEntityOrbitalStation station)) return null;

        List<String> text = new ArrayList<>();
        for (String stored : station.storedNames) {
            text.add(ChatFormatting.RED + "<- " + ChatFormatting.RESET + stored);
        }

        if (!text.isEmpty()) {
            text.add(Component.translatable("station.retrieveRocket").getString());
        } else {
            if (station.hasDocked) {
                if (!station.hasRider) {
                    if (player.isShiftKeyDown()) {
                        if (player.getMainHandItem().isEmpty()) text.add(Component.translatable("station.removeRocket").getString());
                    } else {
                        text.add(Component.translatable("station.enterRocket").getString());
                    }
                } else {
                    text.add(ChatFormatting.YELLOW + Component.translatable("station.occupiedRocket").getString());
                }

                if (station.needsFuel) {
                    for (FluidTankNTM tank : station.tanks) {
                        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + tank.getTankType().getDisplayNameString() + ": "
                                + tank.getFill() + "/" + tank.getMaxFill() + "mB");
                    }
                    if (!station.hasFuel) text.add(ChatFormatting.RED + Component.translatable("station.emptyRocket").getString());
                }
            } else if (!player.isShiftKeyDown()) {
                ItemStack held = player.getMainHandItem();
                // 1.7 named the craft items (fuelled rocket_custom, rp_capsule_20, rp_pod_20); here: anything a spawner takes
                if (!held.isEmpty() && TileEntityOrbitalStation.isCraftItem(held)) {
                    text.add(Component.translatable("station.placeRocket").getString());
                }
            }
        }

        if (text.isEmpty()) return null;
        return new ILookOverlay.Data(title, 0xffff00, 0x404000, text);
    }
}
