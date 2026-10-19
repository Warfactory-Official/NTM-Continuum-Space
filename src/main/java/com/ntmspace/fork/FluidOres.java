package com.ntmspace.fork;

import com.hbm.api.data.DepositDataMaps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import com.ntmspace.block.BlockBodyOre;
import com.ntmspace.block.BodyOreItem;
import dev.golem.core.Ids;
import dev.golem.core.hook.Verdict;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * hbm-space's fluid ores outside the drills ({@code BlockOreFluid}): what a mining laser does with a mined deposit and
 * what its nullifier destroys ({@code TileEntityMachineMiningLaser.suckDrops}, through HBM's {@code PICKUP} hook,
 * ADDON-API-INVENTORY H-69). A deposit is any block with an HBM fluid deposit ({@code hbm:fluid_deposit} or
 * {@code hbm:fluid_deposit_states}); its reserve type follows the deposit's primary fluid (gas, brine, else oil).
 */
public final class FluidOres {

    private FluidOres() {}

    /** hbm-space {@code BlockOreFluid.ReserveType}. */
    public enum Reserve {
        OIL(250, ""), GAS(100, "_gas"), BRINE(0, "_brine");

        /** {@code getBlockFluidAmount}: what one mined block gives a laser's tank. */
        public final int blockAmount;
        /** {@code getUnlocalizedReserveType}: the oil detector's message suffix. */
        public final String suffix;

        Reserve(int blockAmount, String suffix) {
            this.blockAmount = blockAmount;
            this.suffix = suffix;
        }
    }

    /** The deposit's reserve type, from its primary fluid. */
    public static Reserve reserve(DepositDataMaps.Deposit deposit) {
        FluidType primary = deposit.primaryFluid();
        if (primary == Fluids.GAS) return Reserve.GAS;
        if (deposit.primary().endsWith(":brine")) return Reserve.BRINE;
        return Reserve.OIL;
    }

    /** The block state an item stands for (its body's for a per-body ore item), or null for a non-block item. */
    public static @Nullable BlockState stateOf(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BodyOreItem body && body.getBlock().defaultBlockState().hasProperty(BlockBodyOre.BODY)) {
            return body.getBlock().defaultBlockState().setValue(BlockBodyOre.BODY, body.body());
        }
        return item instanceof BlockItem block ? block.getBlock().defaultBlockState() : null;
    }

    private static final TagKey<Item> STONES = TagKey.create(Registries.ITEM, Ids.of("c", "stones"));
    private static final TagKey<Item> COBBLESTONES = TagKey.create(Registries.ITEM, Ids.of("c", "cobblestones"));
    private static final TagKey<Item> SANDS = TagKey.create(Registries.ITEM, Ids.of("c", "sands"));

    /**
     * hbm-space's nullifier list ({@code bad}): dirt, the {@code stone}/{@code cobblestone}/{@code sand} ore dictionary
     * names (vanilla's blocks plus the common tags), sandstone, gravel, basalt, gneiss, flint, snowballs, seeds, tektite.
     * (1.7 compared items only and had no tektite; HBM's port also nullifies grass, netherrack, end stone and
     * sticks, which hbm-space does not.)
     */
    public static boolean isBad(ItemStack stack) {
        if (stack.is(STONES) || stack.is(COBBLESTONES) || stack.is(SANDS)) return true;
        Item item = stack.getItem();
        if (item == Items.DIRT || item == Items.STONE || item == Items.COBBLESTONE || item == Items.SAND || item == Items.RED_SAND
                || item == Items.SANDSTONE || item == Items.GRAVEL || item == Items.FLINT || item == Items.SNOWBALL
                || item == Items.WHEAT_SEEDS) {
            return true;
        }
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        return List.of("hbm:basalt", "hbm:stone_gneiss", "hbm:tektite").contains(id);
    }

    /** hbm-space {@code suckDrops} for one item: nullifier first, then a fluid ore fills the tank and is consumed. */
    public static Verdict pickUp(TileEntityMachineMiningLaser laser, ItemEntity entity, boolean nullifier) {
        ItemStack stack = entity.getItem();
        if (nullifier && isBad(stack)) {
            entity.discard();
            return Verdict.ALLOW;
        }
        BlockState state = stateOf(stack);
        DepositDataMaps.Deposit deposit = state == null ? null : DepositDataMaps.deposit(state);
        if (deposit != null) {
            int toFill = reserve(deposit).blockAmount;
            if (toFill > 0) {
                // 1.7 filled once per item entity, whatever its stack size
                laser.tank.setTankType(deposit.primaryFluid());
                laser.tank.setFill(Math.min(laser.tank.getFill() + toFill, laser.tank.getMaxFill()));
            }
            entity.discard();
            return Verdict.ALLOW;
        }
        // hbm-space's rules replace HBM's (1.12.2-style) scrap list and ore_oil rule: buffer everything else
        return Verdict.DENY;
    }

    private static boolean started;

    public static synchronized void init() {
        if (started) return;
        started = true;
        TileEntityMachineMiningLaser.PICKUP.register(FluidOres::pickUp);
    }
}
