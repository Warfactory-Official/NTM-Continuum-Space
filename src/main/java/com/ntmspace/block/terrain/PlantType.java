package com.ntmspace.block.terrain;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * The 1.7 metadata types of hbm-space's Laythe and Tekto flora, as block state values (HBM's rule for 1.7 block
 * families: one block, a named state). Each enum keeps the 1.7 order.
 */
public final class PlantType {

    private PlantType() {}

    /** {@code BlockCoral.EnumCoral}: {@code laythe_coral} and {@code laythe_coral_block}. */
    public enum Coral implements StringRepresentable {
        TUBE, BRAIN, BUBBLE, FIRE, HORN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * The type 1.7 drew for metadata {@code meta}: {@code BlockEnumMulti} picks its icon by {@code meta % 5}
         * (hbm-space's coral generator writes metadata 8 to 12).
         */
        public static Coral ofMeta(int meta) {
            return values()[Math.floorMod(meta, values().length)];
        }
    }

    /** {@code BlockRubberPlant.EnumRubberPlantType}: {@code rubber_plant}. */
    public enum RubberPlant implements StringRepresentable {
        GRASS, FERN, SHRUB;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** {@code BlockRubberCacti.EnumBushType}: {@code rubber_tall} (1.7's {@code spike_cacti}). */
    public enum RubberTall implements StringRepresentable {
        CACT, BUSH, FLOWER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** {@code BlockNTSapling.EnumSapling}: {@code sapling} (vinyl: the vinyl tree; PVC: the PVC tree). */
    public enum Sapling implements StringRepresentable {
        VINYL, PVC;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
