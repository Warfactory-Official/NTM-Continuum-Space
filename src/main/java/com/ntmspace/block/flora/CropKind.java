package com.ntmspace.block.flora;

import com.ntmspace.registry.FloraBlocks;
import java.util.Locale;
import net.minecraft.world.item.Item;

/**
 * hbm-space's five {@code BlockCrop}s ({@link BlockSpaceCrop}): block {@code crop_<name>}, stage textures
 * {@code <texture>_1..5}, and the items 1.7's {@code getItemDropped} named.
 */
public enum CropKind {
    STRAWBERRY("strawberry"),
    MINT("mint"),
    COFFEE("coffee"),
    TEA("tea"),
    PARAFFIN("paraffin");

    /** 1.7 {@code setBlockTextureName}: the stage textures' stem. */
    public final String texture;

    CropKind(String texture) {
        this.texture = texture;
    }

    public String id() {
        return "crop_" + name().toLowerCase(Locale.ROOT);
    }

    /** 1.7 {@code registerBlockIcons}: stages 0..7 drew {@code _1 _1 _2 _2 _3 _3 _4 _5}. */
    public static int textureStage(int age) {
        return new int[] {1, 1, 2, 2, 3, 3, 4, 5}[age];
    }

    /** 1.7 {@code getItemDropped}: the tea gives tea leaves only ripe, tea seeds before. */
    public Item product(int age) {
        return switch (this) {
            case STRAWBERRY -> FloraBlocks.STRAWBERRY.get();
            case MINT -> FloraBlocks.MINT_LEAVES.get();
            case COFFEE -> FloraBlocks.BEAN_RAW.get();
            case TEA -> age == BlockSpaceCrop.MAX_AGE ? FloraBlocks.TEA_LEAF.get() : FloraBlocks.TEASEEDS.get();
            case PARAFFIN -> FloraBlocks.PARAFFIN_SEEDS.get();
        };
    }

    /** The seed food that plants it, and what pick-block gives. */
    public Item seed() {
        return switch (this) {
            case STRAWBERRY -> FloraBlocks.STRAWBERRY.get();
            case MINT -> FloraBlocks.MINT_LEAVES.get();
            case COFFEE -> FloraBlocks.BEAN_RAW.get();
            case TEA -> FloraBlocks.TEASEEDS.get();
            case PARAFFIN -> FloraBlocks.PARAFFIN_SEEDS.get();
        };
    }
}
