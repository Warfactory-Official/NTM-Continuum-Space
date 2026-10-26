package com.ntmspace.recipes.data;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.api.data.FluidDataMaps.Container;
import com.hbm.api.data.FluidDataMaps.Containers;
import com.ntmspace.block.BlockBodyOre.Body;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/** NTM: Space's fluid containers, as patches of HBM's maps. */
public final class SpaceFluidContainers {

    private SpaceFluidContainers() {}

    /** hbm-space {@code Fluids.init()}: {@code addContainers(new CD_Canister(c))} and {@code new CD_Gastank(bottle, label)}. */
    public static final class Colors extends DataMapSource<Containers> {

        public Colors() {
            super(FluidDataMaps.FLUID_CONTAINERS, true);
        }

        @Override
        protected void values() {
            canister("ntmspace:hydrazine", 0x31517D);
            canister("ntmspace:bloodgas", 0x591000);
            gastank("ntmspace:earthair", 0xC3BEE8, 0xFFFFFF);
            gastank("ntmspace:neon", 0xF1F600, 0x303030);
            gastank("ntmspace:argon", 0xFD70D0, 0x303030);
            gastank("ntmspace:krypton", 0x9AC6E6, 0x303030);
            gastank("ntmspace:eveair", 0x8C21FF, 0xFFFFFF);
            gastank("ntmspace:tektoair", 0x388C69, 0x303030);
            canister("ntmspace:elbowgrease", 0xCBC433);
            canister("ntmspace:nmasstetranol", 0xF1DB0F);
            gastank("ntmspace:joolgas", 0x78B878, 0xFF8400);
            gastank("ntmspace:sarnusgas", 0xB56847, 0xFF8400);
            gastank("ntmspace:ugas", 0x5099BF, 0xFF8400);
            gastank("ntmspace:ngas", 0xBE92E0, 0xFF8400);
            gastank("ntmspace:dunaair", 0xCF5B34, 0xFFFFFF);
        }

        private void canister(String fluid, int color) {
            put(fluid, new Containers(Optional.of(color), Optional.empty(), Optional.empty()));
        }

        private void gastank(String fluid, int bottle, int label) {
            put(fluid, new Containers(Optional.empty(), Optional.of(bottle), Optional.of(label)));
        }
    }

    /** hbm-space {@code FluidContainerRegistry.register()}: the fixed containers NTM: Space adds. */
    public static final class Items extends DataMapSource<Container> {

        public Items() {
            super(FluidDataMaps.FLUID_CONTAINER, true);
        }

        @Override
        protected void values() {
            // 1.7 FluidContainerRegistry registers the same consumed item contents for every body.
            ore("ore_oil", "hbm:oil", 250);
            ore("ore_gas", "hbm:gas", 100);
            container("ntmspace:bucket_mercury", "hbm:mercury", 1000, "minecraft:bucket");
            container("ntmspace:bucket_bromine", "ntmspace:bromine", 1000, "minecraft:bucket");
            container("ntmspace:bucket_ccl", "ntmspace:ccl", 1000, "minecraft:bucket");
            container("minecraft:milk_bucket", "ntmspace:milk", 1000, "minecraft:bucket");
            container("ntmspace:glass_smilk", "ntmspace:smilk", 100, "ntmspace:glass_empty");
            container("hbm:coffee", "ntmspace:coffee", 100, "ntmspace:cmug_empty");
            container("ntmspace:teacup", "ntmspace:tea", 100, "ntmspace:teacup_empty");
            container("ntmspace:bottle_honey", "ntmspace:honey", 100, "minecraft:glass_bottle");
            // dry ice is used up (no empty container)
            container("ntmspace:dry_ice", "hbm:carbondioxide", 1000, null);
        }

        private void ore(String family, String fluid, int amount) {
            for (var body : Body.values()) {
                container("ntmspace:" + body.itemPath(family), fluid, amount, null);
            }
        }

        private void container(String item, String fluid, int amount, String empty) {
            put(item, new Container(fluid, amount, Optional.ofNullable(empty)));
        }
    }
}
