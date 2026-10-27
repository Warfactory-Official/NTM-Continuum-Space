package com.ntmspace.recipes.data;

import static com.hbm.inventory.fluid.Fluids.*;

import com.google.gson.JsonElement;
import com.hbm.inventory.fluid.FluidProperties;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.ntmspace.fluid.SpaceFluids;
import com.ntmspace.fluid.trait.FT_Rocket;
import dev.golem.core.world.DataSource;
import java.util.List;
import net.minecraft.core.HolderLookup;

/**
 * hbm-space {@code Fluids.init}'s changes to HBM's own fluids (its lines that differ from 1.7's) as
 * {@code data/ntmspace/fluid_property_patch/<fluid ns>/<fluid>.json}: the {@code FT_Rocket} it adds to existing
 * declarations, its post-{@code metaOrder} heating, cooling and moderator lines for them, and nitric acid without the
 * pollution 1.7 gives it. Rows are {@link FluidProperties.Patch}, written and read by
 * {@link FluidProperties#writePatch} and {@link FluidProperties#readPatch}.
 */
public final class SpaceFluidPatchSource extends DataSource<FluidProperties.Patch> {

    public SpaceFluidPatchSource() {
        super(FluidProperties.PATCH_FOLDER);
    }

    @Override
    protected void registerDefaults() {
        // hbm-space COOLANT.addTraits(new FT_Heatable()...setEff(PWR, 1.0D).setEff(ICF, 1.0D)...)
        addTraits(COOLANT, new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.0D).addStep(300, 1, COOLANT_HOT, 1));
        // hbm-space HYDROGEN = new FluidType(...).addTraits(..., new FT_Rocket(380, 700_000)); HYDROGEN.addTraits(new FT_Heatable()...)
        addTraits(HYDROGEN, new FT_Rocket(380, 700_000), new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, SpaceFluids.SUPERHEATED_HYDROGEN, 1));
        addTraits(KEROSENE, new FT_Rocket(308, 981000));
        addTraits(KEROSENE_REFORM, new FT_Rocket(321, 1_564_000));
        // hbm-space NITRIC_ACID = new FluidType(...).addTraits(LIQUID, new FT_Corrosive(60)): no FT_Polluting
        removeTraits(NITRIC_ACID, "polluting");
        addTraits(PLASMA_BF, new FT_Rocket(50000, 700_000));
        addTraits(PLASMA_DH3, new FT_Rocket(20000, 700_000));
        addTraits(PLASMA_DT, new FT_Rocket(12000, 700_000));
        addTraits(PLASMA_HD, new FT_Rocket(7500, 700_000));
        addTraits(PLASMA_HT, new FT_Rocket(10000, 700_000));
        addTraits(PLASMA_XM, new FT_Rocket(25000, 700_000));
        // hbm-space WASTEFLUID.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, WASTEGAS, 1), new FT_PWRModerator(1.20D))
        addTraits(WASTEFLUID, new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, WASTEGAS, 1), new FT_PWRModerator(1.20D));
        // hbm-space WASTEGAS: new FT_Rocket(900, 700_000); WASTEGAS.addTraits(new FT_Coolable(WATZ, 1, 1, 300)).
        // 1.7 cools to WATZ, but WASTEFLUID heats into WASTEGAS (line above), so cooling closes the loop on
        // WASTEFLUID instead (upstream hbm-space made the same change); the imported row pins this pairing.
        addTraits(WASTEGAS, new FT_Rocket(900, 700_000), new FT_Coolable(WASTEFLUID, 1, 1, 300));
        // hbm-space WATZ.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, GAS_WATZ, 1), new FT_PWRModerator(1.40D))
        addTraits(WATZ, new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, SpaceFluids.GAS_WATZ, 1), new FT_PWRModerator(1.40D));
        addTraits(XENON, new FT_Rocket(4200, 80_000));
    }

    /** A patch adding {@code traits} to {@code fluid} (a trait it has is replaced whole). */
    private void addTraits(FluidType fluid, Object... traits) {
        register(id(fluid), new FluidProperties.Patch(List.of(), List.of(traits), null, null, null, null));
    }

    /** A patch taking the named traits from {@code fluid}. */
    private void removeTraits(FluidType fluid, String... names) {
        register(id(fluid), new FluidProperties.Patch(List.of(names), List.of(), null, null, null, null));
    }

    /** {@code <fluid ns>/<fluid path>}. */
    private static String id(FluidType fluid) {
        return fluid.getRegistryName().replace(':', '/');
    }

    @Override
    public JsonElement encode(FluidProperties.Patch row, HolderLookup.Provider registries) {
        return FluidProperties.writePatch(row);
    }

    @Override
    public FluidProperties.Patch decode(JsonElement json, HolderLookup.Provider registries) {
        return FluidProperties.readPatch(json.getAsJsonObject());
    }
}
