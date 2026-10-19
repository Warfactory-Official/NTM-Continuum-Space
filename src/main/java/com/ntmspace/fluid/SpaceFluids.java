package com.ntmspace.fluid;

import com.hbm.inventory.fluid.FluidProperties;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.ntmspace.NtmSpace;
import com.ntmspace.fluid.trait.SpaceFluidTraits;
import java.util.List;

/**
 * NTM: Space's fluids: the 60 {@code FluidType}s hbm-space's {@code Fluids.init()} adds over 1.7, registered
 * with HBM's fluid registry in the {@code ntmspace} namespace ({@code ntmspace:<lower-case 1.7 name>}), in hbm-space's
 * declaration order. Every HBM tank, duct and recipe handles them like HBM's own, and each is a vanilla fluid too.
 *
 * <p>Colour, temperature and traits (including the {@code registerCalculatedFuel} values hbm-space computes after
 * the table) and 1.7's NFPA diamond ({@code p, f, r, EnumSymbol}: the {@code hazard} field) are the authored files
 * {@code data/ntmspace/fluid_property/<key>.json}, extracted mechanically from hbm-space's {@code Fluids.java} and checked
 * by the GameTests {@code ntmspace:space_fluid_properties} and {@code ntmspace:space_fluid_hazards}.
 * Containers (canister, gas tank colours) are the stacked data map {@code data/ntmspace/value_maps/patches/hbm/fluid/fluid_containers.json}.
 *
 * <p>hbm-space's changes to HBM's own fluids (rocket traits on kerosene, hydrogen, xenon, waste gas, the plasmas and
 * reformed kerosene; PWR heating steps for hydrogen, Watz mud and liquid waste; a boiler efficiency for coolant;
 * nitric acid without its pollution) are HBM {@code fluid_property_patch} files,
 * {@code data/ntmspace/fluid_property_patch/hbm/<fluid>.json}: they apply only while this mod is installed, on top of
 * HBM's own files (GameTest {@code ntmspace:space_fluid_patches}).
 */
public final class SpaceFluids {

    public static final FluidType NITROGEN = Fluids.register(NtmSpace.MOD_ID, "NITROGEN", 0xB3C6D2);
    public static final FluidType AMMONIA = Fluids.register(NtmSpace.MOD_ID, "AMMONIA", 0x00A0F7);
    public static final FluidType HYDRAZINE = Fluids.register(NtmSpace.MOD_ID, "HYDRAZINE", 0x31517D);
    public static final FluidType BLOODGAS = Fluids.register(NtmSpace.MOD_ID, "BLOODGAS", 0x591000);
    public static final FluidType EARTHAIR = Fluids.register(NtmSpace.MOD_ID, "EARTHAIR", 0xD1CEEE);
    public static final FluidType HCL = Fluids.register(NtmSpace.MOD_ID, "HCL", 0x00D452);
    public static final FluidType MINSOL = Fluids.register(NtmSpace.MOD_ID, "MINSOL", 0xFADF6A);
    public static final FluidType NEON = Fluids.register(NtmSpace.MOD_ID, "NEON", 0xF1F600);
    public static final FluidType ARGON = Fluids.register(NtmSpace.MOD_ID, "ARGON", 0xFD70D0);
    public static final FluidType KRYPTON = Fluids.register(NtmSpace.MOD_ID, "KRYPTON", 0x9AC6E6);
    public static final FluidType COFFEE = Fluids.register(NtmSpace.MOD_ID, "COFFEE", 0x57493D);
    public static final FluidType TEA = Fluids.register(NtmSpace.MOD_ID, "TEA", 0x76523C);
    public static final FluidType HONEY = Fluids.register(NtmSpace.MOD_ID, "HONEY", 0xD99A02);
    public static final FluidType MILK = Fluids.register(NtmSpace.MOD_ID, "MILK", 0xCFCFCF);
    public static final FluidType SMILK = Fluids.register(NtmSpace.MOD_ID, "SMILK", 0xF5DEE4);
    public static final FluidType CCL = Fluids.register(NtmSpace.MOD_ID, "CCL", 0x0C3B2F);
    public static final FluidType EVEAIR = Fluids.register(NtmSpace.MOD_ID, "EVEAIR", 0xDCABF8);
    public static final FluidType KMnO4 = Fluids.register(NtmSpace.MOD_ID, "KMnO4", 0x560046);
    public static final FluidType CHLOROMETHANE = Fluids.register(NtmSpace.MOD_ID, "CHLOROMETHANE", 0xD3CF9E);
    public static final FluidType METHANOL = Fluids.register(NtmSpace.MOD_ID, "METHANOL", 0x88739F);
    public static final FluidType BROMINE = Fluids.register(NtmSpace.MOD_ID, "BROMINE", 0xAF2214);
    public static final FluidType CHLOROETHANE = Fluids.register(NtmSpace.MOD_ID, "CHLOROETHANE", 0xBBA9A0);
    public static final FluidType POLYTHYLENE = Fluids.register(NtmSpace.MOD_ID, "POLYTHYLENE", 0x35302E);
    public static final FluidType FLUORINE = Fluids.register(NtmSpace.MOD_ID, "FLUORINE", 0xC5C539);
    public static final FluidType TEKTOAIR = Fluids.register(NtmSpace.MOD_ID, "TEKTOAIR", 0x245F46);
    public static final FluidType ELBOWGREASE = Fluids.register(NtmSpace.MOD_ID, "ELBOWGREASE", 0xCBC433);
    public static final FluidType NMASSTETRANOL = Fluids.register(NtmSpace.MOD_ID, "NMASSTETRANOL", 0xF1DB0F);
    public static final FluidType NMASS = Fluids.register(NtmSpace.MOD_ID, "NMASS", 0x53A9F4);
    public static final FluidType SCUTTERBLOOD = Fluids.register(NtmSpace.MOD_ID, "SCUTTERBLOOD", 0x6C166C);
    public static final FluidType HTCO4 = Fluids.register(NtmSpace.MOD_ID, "HTCO4", 0x675454);
    public static final FluidType JOOLGAS = Fluids.register(NtmSpace.MOD_ID, "JOOLGAS", 0x96B596);
    public static final FluidType SARNUSGAS = Fluids.register(NtmSpace.MOD_ID, "SARNUSGAS", 0xE47D5C);
    public static final FluidType UGAS = Fluids.register(NtmSpace.MOD_ID, "UGAS", 0x718C9A);
    public static final FluidType NGAS = Fluids.register(NtmSpace.MOD_ID, "NGAS", 0xA37BA3);
    public static final FluidType EMILK = Fluids.register(NtmSpace.MOD_ID, "EMILK", 0xCFCFCF);
    public static final FluidType CMILK = Fluids.register(NtmSpace.MOD_ID, "CMILK", 0xCFCFCF);
    public static final FluidType CREAM = Fluids.register(NtmSpace.MOD_ID, "CREAM", 0xCFCFCF);
    public static final FluidType MAGMA = Fluids.register(NtmSpace.MOD_ID, "MAGMA", 0xFF3300);
    public static final FluidType DICYANOACETYLENE = Fluids.register(NtmSpace.MOD_ID, "DICYANOACETYLENE", 0x675A9F);
    public static final FluidType BRINE = Fluids.register(NtmSpace.MOD_ID, "BRINE", 0xD1A73E);
    public static final FluidType CONGLOMERA = Fluids.register(NtmSpace.MOD_ID, "CONGLOMERA", 0x364D47);
    public static final FluidType DUNAAIR = Fluids.register(NtmSpace.MOD_ID, "DUNAAIR", 0xD4704E);
    public static final FluidType SUPERHEATED_HYDROGEN = Fluids.register(NtmSpace.MOD_ID, "SUPERHEATED_HYDROGEN", 0xE39393);
    public static final FluidType GAS_WATZ = Fluids.register(NtmSpace.MOD_ID, "GAS_WATZ", 0x86653E);
    public static final FluidType URANIUM_BROMIDE = Fluids.register(NtmSpace.MOD_ID, "URANIUM_BROMIDE", 0xD1CEBE);
    public static final FluidType AQUEOUS_COPPER = Fluids.register(NtmSpace.MOD_ID, "AQUEOUS_COPPER", 0x4CC2A2);
    public static final FluidType COPPERSULFATE = Fluids.register(NtmSpace.MOD_ID, "COPPERSULFATE", 0x55E5CF);
    public static final FluidType THORIUM_BROMIDE = Fluids.register(NtmSpace.MOD_ID, "THORIUM_BROMIDE", 0x7A5542);
    public static final FluidType GASEOUS_URANIUM_BROMIDE = Fluids.register(NtmSpace.MOD_ID, "GASEOUS_URANIUM_BROMIDE", 0xD1CEBE);
    public static final FluidType GASEOUS_PLUTONIUM_BROMIDE = Fluids.register(NtmSpace.MOD_ID, "GASEOUS_PLUTONIUM_BROMIDE", 0x4C4C4C);
    public static final FluidType GASEOUS_SCHRABIDIUM_BROMIDE = Fluids.register(NtmSpace.MOD_ID, "GASEOUS_SCHRABIDIUM_BROMIDE", 0x006B6B);
    public static final FluidType GASEOUS_THORIUM_BROMIDE = Fluids.register(NtmSpace.MOD_ID, "GASEOUS_THORIUM_BROMIDE", 0x7A5542);
    public static final FluidType HGAS = Fluids.register(NtmSpace.MOD_ID, "HGAS", 0x999368);
    public static final FluidType AQUEOUS_NICKEL = Fluids.register(NtmSpace.MOD_ID, "AQUEOUS_NICKEL", 0xDACEBA);
    public static final FluidType VINYL = Fluids.register(NtmSpace.MOD_ID, "VINYL", 0xA2A2A2);
    public static final FluidType TCRUDE = Fluids.register(NtmSpace.MOD_ID, "TCRUDE", 0x051914);
    public static final FluidType CBENZ = Fluids.register(NtmSpace.MOD_ID, "CBENZ", 0x91C6BB);
    public static final FluidType HALOLIGHT = Fluids.register(NtmSpace.MOD_ID, "HALOLIGHT", 0xB6F9CF);
    public static final FluidType LITHYDRO = Fluids.register(NtmSpace.MOD_ID, "LITHYDRO", 0xD1CEBE);
    public static final FluidType LITHCARBONATE = Fluids.register(NtmSpace.MOD_ID, "LITHCARBONATE", 0xD1CEBE);

    private SpaceFluids() {}

    private static boolean initialised;

    /** Mod construction: the trait format, the files, the vanilla fluids. */
    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        SpaceFluidTraits.register();
        List<FluidType> all = List.of(
            NITROGEN, AMMONIA, HYDRAZINE, BLOODGAS, EARTHAIR, HCL, MINSOL, NEON, ARGON, KRYPTON, COFFEE, TEA, HONEY,
            MILK, SMILK, CCL, EVEAIR, KMnO4, CHLOROMETHANE, METHANOL, BROMINE, CHLOROETHANE, POLYTHYLENE, FLUORINE,
            TEKTOAIR, ELBOWGREASE, NMASSTETRANOL, NMASS, SCUTTERBLOOD, HTCO4, JOOLGAS, SARNUSGAS, UGAS, NGAS, EMILK,
            CMILK, CREAM, MAGMA, DICYANOACETYLENE, BRINE, CONGLOMERA, DUNAAIR, SUPERHEATED_HYDROGEN, GAS_WATZ,
            URANIUM_BROMIDE, AQUEOUS_COPPER, COPPERSULFATE, THORIUM_BROMIDE, GASEOUS_URANIUM_BROMIDE,
            GASEOUS_PLUTONIUM_BROMIDE, GASEOUS_SCHRABIDIUM_BROMIDE, GASEOUS_THORIUM_BROMIDE, HGAS, AQUEOUS_NICKEL,
            VINYL, TCRUDE, CBENZ, HALOLIGHT, LITHYDRO, LITHCARBONATE);
        // methanol.json is gaseous AND liquid (hbm-space: new FluidType("METHANOL", ...).addTraits(GASEOUS) ...
        // .addTraits(new FT_Combustible(HIGH, 600_000), LIQUID); registerCalculatedFuel then gives 375,000 TU and
        // 937,000 HE): a property file's "type" may list several phases.
        FluidProperties.applyBundled(SpaceFluids.class, all);
        // hbm-space's changes to HBM's own fluids, data/ntmspace/fluid_property_patch/hbm/*.json (see the class comment)
        FluidProperties.applyBundledPatches(SpaceFluids.class, NtmSpace.MOD_ID);
        Fluids.declareVanilla(NtmSpace.MOD_ID, all);
    }
}
