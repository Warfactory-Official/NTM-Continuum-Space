package com.ntmspace.tileentity.machine;

import com.ntmspace.block.flora.CropKind;
import com.ntmspace.block.flora.ItemSpaceSeedFood;
import com.ntmspace.registry.FloraBlocks;
import dev.golem.core.world.Vegetation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * What the greenhouse plants, in modern terms. 1.7 took any {@code IPlantable} item whose plant type is {@code Crop},
 * whose plant is an {@code IGrowable}, not a {@code BlockStem}, and, for hbm-space's own {@code BlockCrop}, one with
 * {@code canHydro} ("single block crops only"). Modern items carry no plant type; the crops are:
 * <ul>
 *   <li>any seed item that places a {@link CropBlock} (wheat, carrots, potatoes, beetroot, torchflower; stems and the
 *       two-block pitcher crop are not {@code CropBlock}s);</li>
 *   <li>hbm-space's seed foods ({@link ItemSpaceSeedFood}) through {@link CropKind#seed}, except the chlorinated seeds:
 *       1.7 {@code crop_paraffin} has {@code canHydro = false}.</li>
 * </ul>
 * Growth stage: 1.7 compared metadata; for crops that is the {@code age} property (a sapling's {@code stage} counted
 * eight, as the atmosphere port reads it).
 */
public final class HydroponicPlants {

    private HydroponicPlants() {}

    /** 1.7 {@code isItemValidForSlot(0)}: {@code instanceof IPlantable}, i.e. any item that places a plant. */
    public static boolean isPlantable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemSpaceSeedFood) return true;
        return stack.getItem() instanceof BlockItem block && Vegetation.isPlant(block.getBlock().defaultBlockState());
    }

    /** The plant 1.7 would place for this seed, or null if it is not a single-block crop the greenhouse takes. */
    public static @Nullable BlockState plantFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof ItemSpaceSeedFood) {
            for (CropKind kind : CropKind.values()) {
                if (kind == CropKind.PARAFFIN || kind.seed() != stack.getItem()) continue; // canHydro false
                return crop(kind).defaultBlockState();
            }
            return null;
        }
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock crop) return crop.defaultBlockState();
        return null;
    }

    private static Block crop(CropKind kind) {
        return switch (kind) {
            case STRAWBERRY -> FloraBlocks.CROP_STRAWBERRY.get();
            case MINT -> FloraBlocks.CROP_MINT.get();
            case COFFEE -> FloraBlocks.CROP_COFFEE.get();
            case TEA -> FloraBlocks.CROP_TEA.get();
            case PARAFFIN -> FloraBlocks.CROP_PARAFFIN.get();
        };
    }

    /** 1.7 {@code getBlockMetadata} of a plant. */
    public static int stage(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer) {
                if (property.getName().equals("age")) return state.getValue(integer);
                if (property.getName().equals("stage")) return state.getValue(integer) * 8;
            }
        }
        return 0;
    }

    /** 1.7 {@code setBlockMetadataWithNotify(meta)} for a plant: the state with that stage, clamped to the property. */
    public static BlockState withStage(BlockState state, int stage) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer) {
                if (property.getName().equals("age")) return withValue(state, integer, stage);
                if (property.getName().equals("stage")) return withValue(state, integer, stage >= 8 ? 1 : 0);
            }
        }
        return state;
    }

    private static BlockState withValue(BlockState state, IntegerProperty property, int value) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (Integer allowed : property.getPossibleValues()) {
            min = Math.min(min, allowed);
            max = Math.max(max, allowed);
        }
        return state.setValue(property, Math.max(min, Math.min(max, value)));
    }
}
