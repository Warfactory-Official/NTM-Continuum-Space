package com.ntmspace.recipes.data;

import com.google.gson.JsonElement;
import com.hbm.inventory.fluid.FluidProperties;
import com.hbm.inventory.fluid.FluidType;
import dev.golem.core.world.DataSource;
import java.util.Map;
import net.minecraft.core.HolderLookup;

/**
 * hbm-space {@code Fluids.init}'s properties of NTM: Space's own fluids ({@link SpaceFluidPropertyTable}) as
 * {@code data/ntmspace/fluid_property/<fluid>.json}. Rows are {@link FluidProperties.Properties}, written and read by
 * {@link FluidProperties#write(FluidProperties.Properties)} and {@link FluidProperties#read}.
 */
public final class SpaceFluidPropertySource extends DataSource<FluidProperties.Properties> {

    public SpaceFluidPropertySource() {
        super(FluidProperties.FOLDER);
    }

    @Override
    protected void registerDefaults() {
        for (Map.Entry<FluidType, FluidProperties.Properties> row : SpaceFluidPropertyTable.rows().entrySet()) {
            String[] id = row.getKey().getRegistryName().split(":", 2);
            if (!id[0].equals("ntmspace")) throw new IllegalStateException("not an NTM: Space fluid: " + row.getKey());
            register(id[1], row.getValue());
        }
    }

    @Override
    public JsonElement encode(FluidProperties.Properties row, HolderLookup.Provider registries) {
        return FluidProperties.write(row);
    }

    @Override
    public FluidProperties.Properties decode(JsonElement json, HolderLookup.Provider registries) {
        return FluidProperties.read(json.getAsJsonObject());
    }
}
