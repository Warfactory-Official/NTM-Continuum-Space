package com.ntmspace.recipes.data;

import com.hbm.api.data.DiFurnaceDataMaps;
import com.hbm.api.data.MachineDataMaps;
import com.hbm.api.data.MachineDataMaps.NukeEntry;
import com.hbm.api.data.MachineDataMaps.RadGenFuel;
import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.RTGPellet;
import com.hbm.api.data.SatelliteDataMaps;
import com.hbm.tileentity.bomb.TileEntityNukeCustom.EnumBombType;
import com.hbm.tileentity.bomb.TileEntityNukeCustom.EnumEntryType;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/** NTM: Space's machine values, as patches of HBM's item maps. */
public final class SpaceMachineValues {

    private SpaceMachineValues() {}

    /**
     * hbm-space {@code TileEntityNukeCustom.registerBombItems}: antimatter particles and pellets feed the antimatter stage,
     * and the balefire egg and its shard are no longer ingredients.
     */
    public static final class CustomNuke extends DataMapSource<NukeEntry> {

        public CustomNuke() {
            super(MachineDataMaps.CUSTOM_NUKE, true);
        }

        @Override
        protected void values() {
            put("hbm:particle_amat", new NukeEntry(EnumBombType.AMAT, 5F, EnumEntryType.ADD));
            put("hbm:pellet_antimatter", new NukeEntry(EnumBombType.AMAT, 45F, EnumEntryType.ADD));
            removeEntry("hbm:egg_balefire");
            removeEntry("hbm:egg_balefire_shard");
        }
    }

    /** hbm-space {@code TileEntityDiFurnace.getItemPower}: {@code powder_coke} (each of its three metas) burns for 400. */
    public static final class DiFurnaceFuel extends DataMapSource<Integer> {

        public DiFurnaceFuel() {
            super(DiFurnaceDataMaps.DIFURNACE_FUEL, true);
        }

        @Override
        protected void values() {
            put("ntmspace:powder_coke_coal", 400);
            put("ntmspace:powder_coke_lignite", 400);
            put("ntmspace:powder_coke_petroleum", 400);
        }
    }

    /**
     * hbm-space {@code TileEntityMachineRadGen}: the short-lived waste of the four classes hbm-space adds (1,500 HE/t for
     * 36,000 ticks, the tiny pile a tenth for a tenth), each leaving its depleted counterpart.
     */
    public static final class RadGenFuels extends DataMapSource<RadGenFuel> {

        public RadGenFuels() {
            super(MachineDataMaps.RADGEN_FUEL, true);
        }

        @Override
        protected void values() {
            for (String waste : new String[] {"americium241", "berkelium247", "curium244", "curium245"}) {
                put("ntmspace:nuclear_waste_short_" + waste,
                        new RadGenFuel(1500, 36000, Optional.of("ntmspace:nuclear_waste_short_depleted_" + waste), 1));
                put("ntmspace:nuclear_waste_short_tiny_" + waste,
                        new RadGenFuel(150, 3600, Optional.of("ntmspace:nuclear_waste_short_depleted_tiny_" + waste), 1));
            }
        }
    }

    /** hbm-space {@code ModItems}: the RTG pellets it adds ({@code new ItemRTGPellet(heat).setDecays(material, life)}). */
    public static final class RTGPellets extends DataMapSource<RTGPellet> {

        public RTGPellets() {
            super(ReactorDataMaps.RTG_PELLET, true);
        }

        @Override
        protected void values() {
            // new ItemRTGPellet(20).setDecays(DepletedRTGMaterial.AMERICIUM, (long) (RTGUtil.getLifespan(13.8F, HalfLifeType.LONG, false) * 1.5))
            put("ntmspace:pellet_rtg_berkelium", new RTGPellet(20, Optional.empty(),
                    (long) (getLifespan(13.8F, LONG) * 1.5), "ntmspace:pellet_rtg_depleted_americium"));
            // new ItemRTGPellet(VersatileConfig.rtgDecay() ? 600 : 200).setDecays(DepletedRTGMaterial.LEAD, (long) (RTGUtil.getLifespan(1F, HalfLifeType.SHORT, false) * 2.5))
            put("ntmspace:pellet_rtg_cf251", new RTGPellet(200, Optional.of(600),
                    (long) (getLifespan(1F, SHORT) * 2.5), "hbm:pellet_rtg_depleted_lead"));
            put("ntmspace:pellet_rtg_cf252", new RTGPellet(200, Optional.of(600),
                    (long) (getLifespan(1F, SHORT) * 2.5), "hbm:pellet_rtg_depleted_lead"));
        }

        private static final int SHORT = 0;
        private static final int LONG = 2;

        /** 1.7 {@code RTGUtil.getLifespan(halfLife, type, false)}: SHORT counts days, LONG hundreds of years (float maths). */
        private static long getLifespan(float halfLife, int type) {
            float life = 0;
            switch (type) {
                case LONG -> life = (48000 * 100 * 100) * halfLife;
                case SHORT -> life = 48000 * halfLife;
                default -> {}
            }
            return (long) life;
        }
    }

    /** hbm-space {@code SatelliteRegistry}: the satellites NTM: Space's chips put into orbit. */
    public static final class Satellites extends DataMapSource<String> {

        public Satellites() {
            super(SatelliteDataMaps.SATELLITE, true);
        }

        @Override
        protected void values() {
            put("ntmspace:sat_dyson_relay", "ntmspace:dyson_relay");
            put("ntmspace:sat_war", "ntmspace:railgun");
        }
    }
}
