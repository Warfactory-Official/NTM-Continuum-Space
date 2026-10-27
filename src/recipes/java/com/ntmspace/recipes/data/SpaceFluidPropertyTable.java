package com.ntmspace.recipes.data;

import static com.hbm.inventory.fluid.Fluids.DELICIOUS;
import static com.hbm.inventory.fluid.Fluids.EVAP;
import static com.hbm.inventory.fluid.Fluids.GASEOUS;
import static com.hbm.inventory.fluid.Fluids.LIQUID;
import static com.hbm.inventory.fluid.Fluids.NOCON;
import static com.hbm.inventory.fluid.Fluids.NOID;
import static com.hbm.inventory.fluid.Fluids.VISCOUS;
import static com.ntmspace.fluid.SpaceFluids.*;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.FluidCombustion.FuelGrade;
import com.hbm.inventory.fluid.FluidHazard;
import com.hbm.inventory.fluid.FluidHazard.Symbol;
import com.hbm.inventory.fluid.FluidProperties;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.ntmspace.fluid.trait.FT_Rocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * hbm-space {@code Fluids.init}, the lines of the 60 fluids it adds over 1.7 ({@code SpaceFluids}): colour,
 * hazard diamond ({@code p, f, r, EnumSymbol}), temperature and traits, in {@code SpaceFluids}' order, with the trait
 * calls after {@code metaOrder} and the spreadsheet fuels ({@code registerCalculatedFuel}) that name them. These are the
 * rows of {@code data/ntmspace/fluid_property/} ({@link SpaceFluidPropertySource}). {@code addContainers} is not a fluid
 * property (it is the {@code fluid_containers} data map) and is left out. A trait replaces an earlier trait of its
 * class, as in 1.7's {@code FluidType.addTraits}.
 */
final class SpaceFluidPropertyTable {

    private SpaceFluidPropertyTable() {}

    /** hbm-space {@code POISON_EXTREME}: poison stat for horrible chemicals like red mud or phosgene. */
    static final float POISON_EXTREME = PollutionHandler.POISON_PER_SECOND * 0.025F;
    /** hbm-space {@code POISON_MINOR}: poison stat for mostly inert things like carbon dioxide. */
    static final float POISON_MINOR = PollutionHandler.POISON_PER_SECOND * 0.001F;

    /** One hbm-space {@code FluidType} as its constructor, {@code setTemp} and {@code addTraits} set it. */
    private static final class Def {
        final int color;
        final FluidHazard hazard;
        int temperature = FluidType.ROOM_TEMPERATURE;
        final Map<Class<?>, Object> traits = new LinkedHashMap<>();
        List<TagKey<Fluid>> shares = List.of();

        Def(int color, int p, int f, int r, Symbol symbol) {
            this.color = color;
            this.hazard = new FluidHazard(p, f, r, symbol);
        }

        Def setTemp(int temperature) {
            this.temperature = temperature;
            return this;
        }

        Def addTraits(Object... traits) {
            for (Object trait : traits) this.traits.put(trait.getClass(), trait);
            return this;
        }

        Def shares(String tag) {
            shares = List.of(TagKey.create(Registries.FLUID, Ids.parse(tag)));
            return this;
        }

        FluidProperties.Properties row() {
            return new FluidProperties.Properties(color, null, temperature, hazard, new ArrayList<>(traits.values()), shares);
        }
    }

    private static Map<FluidType, Def> defs;

    /** Every fluid's row, in {@code SpaceFluids}' order. */
    static synchronized Map<FluidType, FluidProperties.Properties> rows() {
        if (defs == null) {
            defs = new LinkedHashMap<>();
            init();
        }
        Map<FluidType, FluidProperties.Properties> rows = new LinkedHashMap<>();
        defs.forEach((fluid, def) -> rows.put(fluid, def.row()));
        return rows;
    }

    /** hbm-space {@code new FluidType(name, color, p, f, r, symbol)}. */
    private static Def fluid(FluidType fluid, int color, int p, int f, int r, Symbol symbol) {
        Def def = new Def(color, p, f, r, symbol);
        if (defs.putIfAbsent(fluid, def) != null) throw new IllegalStateException("fluid declared twice: " + fluid);
        return def;
    }

    /** A fluid declared above, for hbm-space's later {@code X.addTraits(...)} calls. */
    private static Def at(FluidType fluid) {
        Def def = defs.get(fluid);
        if (def == null) throw new IllegalStateException("fluid not declared: " + fluid);
        return def;
    }

    /** hbm-space {@code Fluids.registerCalculatedFuel}. */
    private static void registerCalculatedFuel(Def type, double base, double combustMult, FuelGrade grade) {

        long flammable = (long) base;
        long combustible = (long) (base * combustMult);

        flammable = round(flammable);
        combustible = round(combustible);

        type.addTraits(new FT_Flammable(flammable));

        if (combustible > 0 && grade != null)
            type.addTraits(new FT_Combustible(grade, combustible));
    }

    /** hbm-space {@code Fluids.round}. */
    private static long round(long l) {
        if (l > 10_000_000L) return l - (l % 100_000L);
        if (l > 1_000_000L) return l - (l % 10_000L);
        if (l > 100_000L) return l - (l % 1_000L);
        if (l > 10_000L) return l - (l % 100L);
        if (l > 1_000L) return l - (l % 10L);

        return l;
    }

    /** hbm-space {@code Fluids.init}. */
    private static void init() {

        fluid(NITROGEN, 0xB3C6D2, 1, 0, 0, Symbol.CRYOGENIC).setTemp(-90).addTraits(LIQUID, EVAP);
        fluid(AMMONIA, 0x00A0F7, 2, 0, 1, Symbol.ASPHYXIANT).addTraits(new FT_Poison(true, 4), GASEOUS);
        fluid(HYDRAZINE, 0x31517D, 2, 3, 2, Symbol.NONE).addTraits((new FT_Flammable(500_000)), new FT_Combustible(FuelGrade.HIGH, 1_250_000), new FT_Corrosive(30), LIQUID, new FT_Rocket(210, 600_000));
        fluid(BLOODGAS, 0x591000, 3, 1, 1, Symbol.NONE).addTraits(new FT_Flammable(1_000_000), new FT_Combustible(FuelGrade.AERO, 2_500_000)).addTraits(LIQUID);
        fluid(EARTHAIR, 0xD1CEEE, 0, 0, 0, Symbol.NONE).addTraits(GASEOUS);
        fluid(HCL, 0x00D452, 3, 0, 3, Symbol.ACID).addTraits(new FT_Corrosive(30), LIQUID);
        fluid(MINSOL, 0xFADF6A, 3, 0, 3, Symbol.ACID).addTraits(new FT_Corrosive(10), LIQUID);
        fluid(NEON, 0xF1F600, 0, 0, 0, Symbol.CRYOGENIC).addTraits(GASEOUS);
        fluid(ARGON, 0xFD70D0, 0, 0, 0, Symbol.CRYOGENIC).addTraits(GASEOUS);
        fluid(KRYPTON, 0x9AC6E6, 0, 0, 0, Symbol.CRYOGENIC).addTraits(GASEOUS);
        fluid(COFFEE, 0x57493D, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID);
        fluid(TEA, 0x76523C, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID);
        fluid(HONEY, 0xD99A02, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID).shares("c:honey");
        fluid(MILK, 0xCFCFCF, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID).shares("c:milk");
        fluid(SMILK, 0xF5DEE4, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID);
        fluid(CCL, 0x0C3B2F, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, new FT_Corrosive(10));
        fluid(EVEAIR, 0xDCABF8, 4, 0, 0, Symbol.OXIDIZER).addTraits(GASEOUS, new FT_Corrosive(25), new FT_Poison(true, 1));
        fluid(KMnO4, 0x560046, 4, 0, 0, Symbol.ACID).addTraits(LIQUID, new FT_Corrosive(15), new FT_Poison(true, 1));
        fluid(CHLOROMETHANE, 0xD3CF9E, 2, 4, 0, Symbol.NONE).addTraits(GASEOUS, new FT_Corrosive(15)).addTraits(new FT_Flammable(50_000));
        // ethanol but more etha per nol
        fluid(METHANOL, 0x88739F, 3, 4, 0, Symbol.NONE).addTraits(GASEOUS).addTraits(new FT_Flammable(400_000)).addTraits(new FT_Combustible(FuelGrade.HIGH, 600_000), LIQUID);
        fluid(BROMINE, 0xAF2214, 2, 0, 1, Symbol.NONE).addTraits(LIQUID, VISCOUS, new FT_Corrosive(10));
        fluid(CHLOROETHANE, 0xBBA9A0, 2, 0, 0, Symbol.NONE).addTraits(GASEOUS);
        fluid(POLYTHYLENE, 0x35302E, 1, 2, 0, Symbol.NONE).addTraits(LIQUID).addTraits(new FT_Flammable(50_000));
        fluid(FLUORINE, 0xC5C539, 4, 4, 4, Symbol.OXIDIZER).addTraits(GASEOUS, new FT_Corrosive(40), new FT_Poison(true, 1)).addTraits(new FT_Flammable(10_000));
        fluid(TEKTOAIR, 0x245F46, 4, 2, 0, Symbol.OXIDIZER).addTraits(GASEOUS, new FT_Poison(true, 1)).addTraits(new FT_Flammable(30_000));
        fluid(ELBOWGREASE, 0xCBC433, 1, 3, 0, Symbol.NONE).addTraits(new FT_Flammable(600_000), LIQUID);
        fluid(NMASSTETRANOL, 0xF1DB0F, 1, 3, 0, Symbol.NONE).addTraits(new FT_Flammable(1_000_000), LIQUID, new FT_Corrosive(70), new FT_Poison(true, 0), new FT_VentRadiation(0.01F));
        fluid(NMASS, 0x53A9F4, 1, 2, 0, Symbol.NONE).addTraits(LIQUID, new FT_Corrosive(10), new FT_Poison(true, 0), new FT_VentRadiation(0.04F));
        fluid(SCUTTERBLOOD, 0x6C166C, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, DELICIOUS);
        fluid(HTCO4, 0x675454, 1, 3, 0, Symbol.RADIATION).addTraits(LIQUID, new FT_Corrosive(10), new FT_VentRadiation(0.5F));
        fluid(JOOLGAS, 0x96B596, 0, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS);
        fluid(SARNUSGAS, 0xE47D5C, 0, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS);
        fluid(UGAS, 0x718C9A, 0, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS);
        fluid(NGAS, 0xA37BA3, 0, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS);
        fluid(EMILK, 0xCFCFCF, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID);
        fluid(CMILK, 0xCFCFCF, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID);
        fluid(CREAM, 0xCFCFCF, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID);
        fluid(MAGMA, 0xFF3300, 3, 3, 3, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(DICYANOACETYLENE, 0x675A9F, 1, 2, 1, Symbol.NONE).addTraits(new FT_Flammable(4_000_000), GASEOUS);
        fluid(BRINE, 0xD1A73E, 3, 3, 3, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(CONGLOMERA, 0x364D47, 0, 0, 2, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(DUNAAIR, 0xD4704E, 3, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS, new FT_Polluting().release(PollutionType.POISON, POISON_MINOR));
        fluid(SUPERHEATED_HYDROGEN, 0xE39393, 0, 0, 0, Symbol.NONE).setTemp(2200).addTraits(GASEOUS, NOCON, NOID, new FT_Rocket(900, 700_000));
        fluid(GAS_WATZ, 0x86653E, 4, 0, 3, Symbol.ACID).setTemp(2500).addTraits(GASEOUS, NOCON, NOID, new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME), new FT_Rocket(1200, 700_000));
        fluid(URANIUM_BROMIDE, 0xD1CEBE, 0, 0, 0, Symbol.NONE).setTemp(200).addTraits(LIQUID, VISCOUS, new FT_Corrosive(65), new FT_VentRadiation(0.1F));
        fluid(AQUEOUS_COPPER, 0x4CC2A2, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(COPPERSULFATE, 0x55E5CF, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(THORIUM_BROMIDE, 0x7A5542, 0, 0, 0, Symbol.NONE).setTemp(200).addTraits(LIQUID, VISCOUS, new FT_Corrosive(65), new FT_VentRadiation(0.1F));
        fluid(GASEOUS_URANIUM_BROMIDE, 0xD1CEBE, 0, 0, 0, Symbol.NONE).setTemp(2500).addTraits(GASEOUS, NOCON, NOID, new FT_Rocket(1500, 700_000));
        fluid(GASEOUS_PLUTONIUM_BROMIDE, 0x4C4C4C, 0, 0, 0, Symbol.NONE).setTemp(2600).addTraits(GASEOUS, NOCON, NOID, new FT_Rocket(2000, 700_000));
        fluid(GASEOUS_SCHRABIDIUM_BROMIDE, 0x006B6B, 0, 0, 0, Symbol.NONE).setTemp(3000).addTraits(GASEOUS, NOCON, NOID, new FT_Rocket(3000, 700_000));
        fluid(GASEOUS_THORIUM_BROMIDE, 0x7A5542, 0, 0, 0, Symbol.NONE).setTemp(2300).addTraits(GASEOUS, NOCON, NOID, new FT_Rocket(1300, 700_000));
        fluid(HGAS, 0x999368, 0, 0, 0, Symbol.ACID).addTraits(GASEOUS, new FT_Corrosive(120));
        fluid(AQUEOUS_NICKEL, 0xDACEBA, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(VINYL, 0xA2A2A2, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(TCRUDE, 0x051914, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(CBENZ, 0x91C6BB, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(HALOLIGHT, 0xB6F9CF, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(LITHYDRO, 0xD1CEBE, 0, 0, 0, Symbol.NONE).addTraits(GASEOUS);
        fluid(LITHCARBONATE, 0xD1CEBE, 0, 0, 0, Symbol.NONE).addTraits(GASEOUS);

        // after metaOrder
        at(SUPERHEATED_HYDROGEN).addTraits(new FT_Coolable(Fluids.HYDROGEN, 1, 1, 300));

        at(GAS_WATZ).addTraits(new FT_Coolable(Fluids.WATZ, 1, 1, 300));

        at(URANIUM_BROMIDE).addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, GASEOUS_URANIUM_BROMIDE, 1), new FT_PWRModerator(1.75D));
        at(GASEOUS_URANIUM_BROMIDE).addTraits(new FT_Coolable(URANIUM_BROMIDE, 1, 1, 300));

        at(THORIUM_BROMIDE).addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, GASEOUS_THORIUM_BROMIDE, 1), new FT_PWRModerator(1.50D));
        at(GASEOUS_THORIUM_BROMIDE).addTraits(new FT_Coolable(THORIUM_BROMIDE, 1, 1, 300));

        /// FINAL ///

        long baseline = 100_000L; //we do not know
        double demandHigh = 2.0D; //kerosene and jet fuels
        double complexityRefinery = 1.1D;
        double complexityFraction = 1.05D;
        double complexityCracking = 1.25D;
        double flammabilityNormal = 1.0D; //refined oils
        double flammabilityHigh = 2.0D; //satan's asshole

        // hbm-space reads these HBM fluids' spreadsheet values (KEROSENE, BALEFIRE, UNSATURATEDS.getTrait(FT_Flammable))
        // at this point of init; they are its own registerCalculatedFuel lines for them, stated here because the
        // generator runs without HBM's fluid property files: 2,560,000, 256,000,000 and 3,660,000 TU.
        long kerosene = round((long) (baseline / 0.09 * flammabilityNormal * demandHigh * complexityRefinery * complexityFraction));
        long balefire = round((long) (kerosene * 100L));
        long unsaturateds = round((long) (baseline / 0.15 * flammabilityHigh * demandHigh * complexityRefinery * complexityCracking));

        /// the almighty excel spreadsheet has spoken! ///
        registerCalculatedFuel(at(BLOODGAS), kerosene * 0.8, 2.5, FuelGrade.AERO); //0.8
        registerCalculatedFuel(at(NMASSTETRANOL), balefire * 1000, 10.5, FuelGrade.HIGH); //0.8
        registerCalculatedFuel(at(DICYANOACETYLENE), (baseline / 0.15 * flammabilityHigh * demandHigh * complexityRefinery * complexityCracking) + unsaturateds, 0, null);

        registerCalculatedFuel(at(METHANOL), 375_000D, 2.5D, FuelGrade.HIGH); // diesel / 2
    }
}
