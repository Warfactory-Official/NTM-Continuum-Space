package com.ntmspace.recipes.data;

import com.hbm.api.data.DepositDataMaps;
import com.hbm.api.data.DepositDataMaps.Deposit;
import com.hbm.api.data.DepositDataMaps.StateDeposits;
import dev.golem.core.data.DataMapSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * hbm-space {@code BlockOreFluid}: the fluid deposits of its per-body oil, gas, brine and bedrock oil ores, as patches of
 * HBM's deposit maps. A deposit scales the drill's own 1.7 amounts, so each body's {@code WorldConfig} defaults are stated
 * relative to Earth's ({@code earthOilPerDeposit} 500, gas 100..500, {@code earthOilDrainChance} 0.05): {@code yield} is the
 * body's {@code *PerDeposit} over 500, {@code secondary_yield} its gas range over Earth's, {@code drain} its drain chance over
 * 0.05. 1.7 picked the amounts by metadata only, so all four reserve blocks share one per-body table.
 */
public final class SpaceDeposits {

    private SpaceDeposits() {}

    /** hbm-space {@code SolarSystem.Body}, without {@code ORBIT}: the {@code body} state of the ores. */
    static final String[] BODIES = {"kerbin", "mun", "minmus", "duna", "moho", "dres", "eve", "ike", "laythe", "tekto"};

    private static final double EARTH_OIL = 500;
    private static final double EARTH_GAS_MIN = 100;
    private static final double EARTH_DRAIN = 0.05D;

    /** {@code getPrimaryFluidAmount(meta)} of a draining ore (a deposit with an empty block). */
    private static int primaryAmount(String body) {
        return switch (body) {
            case "duna" -> 200;             // dunaOilPerDeposit
            case "laythe" -> 500;           // laytheOilPerDeposit
            case "eve" -> 500;              // eveGasPerDeposit
            case "mun", "minmus", "ike" -> 300; // munBrinePerDeposit, minmusBrinePerDeposit, ikeBrinePerDeposit
            case "tekto" -> 500;            // tektoOilPerDeposit
            default -> 500;                 // earthOilPerDeposit
        };
    }

    /** The minimum of {@code getSecondaryFluidAmount(meta)}'s range (each range scales Earth's 100..500 evenly). */
    private static int secondaryMin(String body) {
        return switch (body) {
            case "duna" -> 100;             // dunaGasPerDepositMin
            case "laythe" -> 100;           // laytheGasPerDepositMin
            case "eve" -> 20;               // evePetPerDepositMin
            case "tekto" -> 100;            // tektoGasPerDepositMin
            default -> 100;                 // earthGasPerDepositMin
        };
    }

    /** {@code getDrainChance(meta)}. */
    private static double drainChance(String body) {
        return switch (body) {
            case "duna" -> 0.1D;            // dunaOilDrainChance
            default -> 0.05D;               // laythe, eve, mun, minmus, ike, tekto and earth: 0.05
        };
    }

    /** {@code getPrimaryFluid(meta)} / {@code getSecondaryFluid(meta)} of an OIL reserve. */
    private static String oilPrimary(String body) {
        return switch (body) {
            case "laythe" -> "hbm:oil_ds";
            case "tekto" -> "ntmspace:tcrude";
            default -> "hbm:oil";
        };
    }

    private static String oilSecondary(String body) {
        return body.equals("tekto") ? "ntmspace:hgas" : "hbm:gas";
    }

    /** One draining reserve: {@code secondary} null for BRINE (Fluids.NONE). */
    private static Deposit draining(String body, String primary, String secondary, String empty) {
        double yield = primaryAmount(body) / EARTH_OIL;
        double secondaryYield = secondaryMin(body) / EARTH_GAS_MIN;
        return new Deposit(primary, secondary == null ? "minecraft:empty" : secondary, Optional.of(empty), yield,
                secondary == null || secondaryYield == yield ? Optional.empty() : Optional.of(secondaryYield),
                drainChance(body) / EARTH_DRAIN, false);
    }

    /** {@code hbm:fluid_deposit_states}: the per-body ores (one block, a {@code body} state). */
    public static final class States extends DataMapSource<StateDeposits> {

        public States() {
            super(DepositDataMaps.FLUID_DEPOSIT_STATES, true);
        }

        @Override
        protected void values() {
            Map<String, Deposit> oil = new LinkedHashMap<>();
            Map<String, Deposit> gas = new LinkedHashMap<>();
            Map<String, Deposit> brine = new LinkedHashMap<>();
            Map<String, Deposit> tekto = new LinkedHashMap<>();
            Map<String, Deposit> bedrock = new LinkedHashMap<>();
            for (String body : BODIES) {
                oil.put(body, draining(body, oilPrimary(body), oilSecondary(body), "ntmspace:ore_oil_empty"));
                gas.put(body, draining(body, "hbm:gas", "hbm:petroleum", "ntmspace:ore_gas_empty"));
                brine.put(body, draining(body, "ntmspace:brine", null, "ntmspace:ore_brine_empty"));
                tekto.put(body, draining(body, oilPrimary(body), oilSecondary(body), "ntmspace:ore_tekto_empty"));
                // empty == null: requiresFracking, never drains; bedrockOilPerDeposit 100 (Tekto's the same), gas 10..50
                bedrock.put(body, new Deposit(oilPrimary(body), oilSecondary(body), Optional.empty(), 100 / EARTH_OIL,
                        Optional.of(10 / EARTH_GAS_MIN), 1D, true));
            }
            put("ntmspace:ore_oil", new StateDeposits("body", oil));
            put("ntmspace:ore_gas", new StateDeposits("body", gas));
            put("ntmspace:ore_brine", new StateDeposits("body", brine));
            put("ntmspace:ore_tekto", new StateDeposits("body", tekto));
            put("ntmspace:ore_bedrock_oil", new StateDeposits("body", bedrock));
        }
    }

    /** {@code hbm:fluid_deposit}: HBM's bedrock oil at hbm-space's {@code bedrockOilPerDeposit} (Kerbin's). */
    public static final class Blocks extends DataMapSource<Deposit> {

        public Blocks() {
            super(DepositDataMaps.FLUID_DEPOSIT, true);
        }

        @Override
        protected void values() {
            put("hbm:ore_bedrock_oil", new Deposit("hbm:oil", "hbm:gas", Optional.empty(), 100 / EARTH_OIL,
                    Optional.of(10 / EARTH_GAS_MIN), 1D, true));
        }
    }
}
