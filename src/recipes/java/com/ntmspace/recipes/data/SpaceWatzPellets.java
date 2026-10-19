package com.ntmspace.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.WatzPellet;
import com.hbm.util.Function;
import com.hbm.util.Function.FunctionLinear;
import com.hbm.util.Function.FunctionQuadratic;
import com.hbm.util.Function.FunctionSqrt;
import com.hbm.util.Function.FunctionSqrtFalling;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * hbm-space {@code ItemWatzPellet.EnumWatzType}: the pellets hbm-space adds, and HBM's with hbm-space's yield of
 * 1,000,000,000 (1.7's 500,000,000). The constructor's {@code mudContent} is halved as 1.7's constructor did; the bar colours
 * are its {@code colorLight} and {@code colorDark}.
 */
public final class SpaceWatzPellets extends DataMapSource<WatzPellet> {

    /** hbm-space {@code ItemWatzPellet}'s yield. */
    private static final double YIELD = 1_000_000_000D;

    public SpaceWatzPellets() {
        super(ReactorDataMaps.WATZ_PELLET, true);
    }

    @Override
    protected void values() {
        // the types hbm-space adds
        type("ntmspace:watz_pellet_pu241", 0x78817E, 394240, 1_950, 25, 0.0025D, new FunctionLinear(1.30D), new FunctionSqrt(2.66D / 18D).withOff(24D * 24D), null);
        type("ntmspace:watz_pellet_amrg", 0x93767B, 0x66474D, 2_888, 48, 0.0035D, new FunctionLinear(1.33D), new FunctionSqrt(4.33D / 25.5D).withOff(28D * 28D), null);
        type("ntmspace:watz_pellet_amf", 0x93767B, 0x66474D, 2_333, 44, 0.003D, new FunctionLinear(1.33D), new FunctionSqrt(4.11D / 22.2D).withOff(27D * 27D), null);
        type("ntmspace:watz_pellet_cmrg", 0xD8C2C4, 0xAD9799, 2_999, 50, 0.005D, new FunctionLinear(1.5D), new FunctionSqrt(5.5D / 25.5D).withOff(30D * 28D), null);
        type("ntmspace:watz_pellet_cmf", 0xD8C2C4, 0xAD9799, 2_444, 48, 0.0045D, new FunctionLinear(1.8D), new FunctionSqrt(5.0D / 20D).withOff(26D * 24D), null);
        type("ntmspace:watz_pellet_bk247", 0xC2C9C7, 0x8D9592, 3_000, 55, 0.012D, new FunctionLinear(1.5D), new FunctionSqrt(6.0D / 23.5D).withOff(10D * 10D), null);
        type("ntmspace:watz_pellet_cf251", 0x7879B4, 0x4D4E89, 1_250, 60, 0.001D, new FunctionLinear(1.7D), new FunctionSqrt(6.65D / 23.5D).withOff(10D * 10D), null);
        type("ntmspace:watz_pellet_cf252", 0x7879B4, 0x4D4E89, 1_050, 120, 0.0015D, new FunctionLinear(1.8D), new FunctionSqrt(8.85D / 28.8D).withOff(10D * 10D), null);
        type("ntmspace:watz_pellet_es253", 0xB9BFB2, 0x594E44, 3_750, 70, 0.0001D, new FunctionLinear(1.3D), new FunctionSqrt(7.0D / 27.7D).withOff(10D * 10D), null);

        // HBM's types, unchanged but for the yield
        type("hbm:watz_pellet_schrabidium", 0x32FFFF, 0x005C5C, 2_000, 20D, 0.01D, new FunctionLinear(1.5D), new FunctionSqrtFalling(10D), null);
        type("hbm:watz_pellet_hes", 0x66DCD6, 0x023933, 1_750, 20D, 0.005D, new FunctionLinear(1.25D), new FunctionSqrtFalling(15D), null);
        type("hbm:watz_pellet_mes", 0xCBEADF, 0x28473C, 1_500, 15D, 0.0025D, new FunctionLinear(1.15D), new FunctionSqrtFalling(15D), null);
        type("hbm:watz_pellet_les", 0xABB4A8, 0x0C1105, 1_250, 15D, 0.00125D, new FunctionLinear(1D), new FunctionSqrtFalling(20D), null);
        type("hbm:watz_pellet_hen", 0xA6B2A6, 0x030F03, 0, 10D, 0.0005D, new FunctionSqrt(100), new FunctionSqrtFalling(10D), null);
        type("hbm:watz_pellet_meu", 0xC1C7BD, 0x2B3227, 0, 10D, 0.0005D, new FunctionSqrt(75), new FunctionSqrtFalling(10D), null);
        type("hbm:watz_pellet_mep", 0x9AA3A0, 0x111A17, 0, 15D, 0.0005D, new FunctionSqrt(150), new FunctionSqrtFalling(10D), null);
        type("hbm:watz_pellet_lead", 0xA6A6B2, 0x03030F, 0, 0, 0.0025D, null, null, new FunctionSqrt(10)); // standard absorber, negative coefficient
        type("hbm:watz_pellet_boron", 0xBDC8D2, 0x29343E, 0, 0, 0.0025D, null, null, new FunctionLinear(10)); // improved absorber, linear
        type("hbm:watz_pellet_du", 0xC1C7BD, 0x2B3227, 0, 0, 0.0025D, null, null, new FunctionQuadratic(1D, 1D).withDiv(100)); // absorber with positive coefficient
        type("hbm:watz_pellet_nqd", 0x4B4B4B, 0x121212, 2_000, 20, 0.01D, new FunctionLinear(2D), new FunctionSqrt(1D / 25D).withOff(25D * 25D), null);
        type("hbm:watz_pellet_nqr", 0x2D2D2D, 0x0B0B0B, 2_500, 30, 0.01D, new FunctionLinear(1.5D), new FunctionSqrt(1D / 25D).withOff(25D * 25D), null);
    }

    /** {@code EnumWatzType(colorLight, colorDark, passive, heatEmission, mudContent, burn, heatDiv, absorb)}. */
    private void type(String pellet, int colorLight, int colorDark, double passive, double heatEmission, double mudContent,
                      Function burn, Function heatDiv, Function absorb) {
        put(pellet, new WatzPellet(colorLight, colorDark, passive, heatEmission, mudContent / 2D, Optional.ofNullable(burn),
                Optional.ofNullable(heatDiv), Optional.ofNullable(absorb), YIELD, pellet.replace("watz_pellet_", "watz_pellet_depleted_")));
    }
}
