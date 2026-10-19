package com.ntmspace.rocket;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.ntmspace.fluid.SpaceFluids;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * One rocket part: hbm-space's {@code render/util/MissilePart} (height, model, texture, deployed/shroud models, custom
 * renderer) joined with the rocket stats hbm-space added to {@code ItemCustomMissilePart} (mass, thrust, ISP, tank size,
 * the fuel and payload kinds). hbm-space kept the stats on the part item; here they live in {@link RocketParts}, keyed by
 * the item's registry id, so HBM's own missile parts (whose items are HBM's and keep 1.7's missile stats) and HBM's
 * satellites (which are no missile part in HBM) take part in rockets the way they did in hbm-space.
 *
 * <p>{@code fuel} is the 1.7 {@code FuelType} name (KEROSENE, SOLID, HYDROGEN, XENON, BALEFIRE, ANY, HYDRAZINE, METHALOX,
 * KEROLOX), {@code warhead} the {@code WarheadType} name (APOLLO, SATELLITE, or HBM's own), sizes are the radii of
 * 1.7's {@code PartSize}; sizes and heights are doubles, as 1.7's {@code MissilePart} and {@code PartSize} held them.
 *
 * @param renderer a custom capsule renderer ({@code "RenderDropPod"}), or null
 * @param renderInventoryModel false for the satellites, which 1.7 hid in the assembled item renderer
 */
public record RocketPart(String id, PartType type, @Nullable String fuel, @Nullable String warhead, double topRadius,
                         double bottomRadius, int tankSize, int mass, int thrust, int isp, double height, double guiHeight,
                         Identifier mesh, Identifier skin, @Nullable Identifier deployedMesh, @Nullable Identifier shroudMesh,
                         @Nullable String renderer, boolean renderInventoryModel) {

    /** The registered item, or air before the registries are filled or for a part whose item does not exist. */
    public Item item() {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
        return item == null ? Items.AIR : item;
    }

    /** 1.7 {@code getModel(deployed)}. */
    public Identifier getModel(boolean deployed) {
        if (deployed && deployedMesh != null) return deployedMesh;
        return mesh;
    }

    /** 1.7 {@code getShroud()}. */
    public Identifier getShroud() {
        if (shroudMesh != null) return shroudMesh;
        return mesh;
    }

    /** 1.7 {@code attributes[0] == WarheadType.APOLLO}. */
    public boolean isApollo() {
        return "APOLLO".equals(warhead);
    }

    /** 1.7 {@code attributes[0] == WarheadType.SATELLITE}. */
    public boolean isSatellite() {
        return "SATELLITE".equals(warhead);
    }

    /** 1.7 {@code getThrust()}: thrusters only, N. */
    public int getThrust() {
        return type == PartType.THRUSTER ? thrust : 0;
    }

    /** 1.7 {@code getISP()}: thrusters only, s. */
    public int getISP() {
        return type == PartType.THRUSTER ? isp : 0;
    }

    /** 1.7 {@code getTankSize()}: fuselages only, mB. */
    public int getTankSize() {
        return type == PartType.FUSELAGE ? tankSize : 0;
    }

    /**
     * 1.7 {@code getFuel()}: the fuel fluid, {@link Fluids#NONE} for solid fuel (the pad's solid store), null for none
     * (ANY, or not a fuelled part).
     */
    public @Nullable FluidType getFuel() {
        if (fuel == null) return null;
        return switch (fuel) {
            case "KEROSENE", "KEROLOX" -> Fluids.KEROSENE;
            case "METHALOX" -> Fluids.GAS;
            case "HYDROGEN" -> Fluids.HYDROGEN;
            case "XENON" -> Fluids.XENON;
            case "BALEFIRE" -> Fluids.BALEFIRE;
            case "HYDRAZINE" -> SpaceFluids.HYDRAZINE;
            case "SOLID" -> Fluids.NONE; // Requires non-fluid fuel
            default -> null;
        };
    }

    /** 1.7 {@code getOxidizer()}. */
    public @Nullable FluidType getOxidizer() {
        if (fuel == null) return null;
        return switch (fuel) {
            case "KEROLOX", "HYDROGEN", "METHALOX" -> Fluids.OXYGEN;
            case "KEROSENE", "BALEFIRE" -> Fluids.PEROXIDE;
            default -> null;
        };
    }
}
