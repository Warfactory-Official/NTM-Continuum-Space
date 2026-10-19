"""Writes data/ntmspace/value_maps/patches/hbm/block/fluid_deposit_states.json: hbm-space's per-body fluid ores (BlockOreFluid) as
HBM drill deposits, one entry per ore block keyed by its `body` state.

hbm-space's derrick takes the per-deposit amounts straight from the ore (BlockOreFluid.getPrimaryFluidAmount /
getSecondaryFluidAmount / getDrainChance, WorldConfig defaults below, by body metadata). HBM's drills keep 1.7's
derrick amounts (500 oil, 100..500 gas, drain 0.05; pumpjack 750, 50..250, 0.025) and scale them per deposit, so a body
is yield = primary / 500, secondary_yield = gas min / 100 (= gas max / 500), drain = chance / 0.05. That reproduces the
fork's derrick exactly and its pumpjack exactly (its 1.5 / 0.5 / 0.5 multipliers are HBM's 750 / 50..250 / 0.025).
The fork's fracking tower took the ore's amounts unscaled (500 + 100..500, drain 0.05 on Earth); HBM's keeps 1.7's
1000 + 100..500, 0.02 - see the fork-content triage (P2).

Fluids per BlockOreFluid.getPrimaryFluid / getSecondaryFluid: OIL -> oil + gas (Laythe: oil_ds; Tekto: tcrude + hgas),
GAS -> gas + petroleum, BRINE -> brine. The empty block keeps the body (HBM DepositDataMaps.emptyState).

Run from the repository root:  python -I new-hbm-space/tools/gen_fluid_deposits.py
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PATH = os.path.join(ROOT, "new-hbm-space", "src", "main", "resources", "data", "ntmspace", "value_maps", "patches", "hbm", "block",
                    "fluid_deposit_states.json")

BODIES = ["kerbin", "mun", "minmus", "duna", "moho", "dres", "eve", "ike", "laythe", "tekto"]

# hbm-space WorldConfig defaults: body -> (primary per deposit, gas min, gas max, drain chance); others = Earth
EARTH = (500, 100, 500, 0.05)
PER_BODY = {
    "duna": (200, 100, 500, 0.1),
    "laythe": (500, 100, 500, 0.05),
    "eve": (500, 20, 100, 0.05),
    "mun": (300, 100, 500, 0.05),
    "minmus": (300, 100, 500, 0.05),
    "ike": (300, 100, 500, 0.05),
    "tekto": (500, 100, 500, 0.05),
}
# the HBM derrick's 1.7 amounts the yields scale
WELL = (500, 100, 500, 0.05)

# block -> (reserve type, empty block)
ORES = {
    "ntmspace:ore_oil": ("OIL", "ntmspace:ore_oil_empty"),
    "ntmspace:ore_gas": ("GAS", "ntmspace:ore_gas_empty"),
    "ntmspace:ore_brine": ("BRINE", "ntmspace:ore_brine_empty"),
    "ntmspace:ore_tekto": ("OIL", "ntmspace:ore_tekto_empty"),
}


def fluids(kind, body):
    if kind == "OIL":
        primary = {"laythe": "hbm:oil_ds", "tekto": "ntmspace:tcrude"}.get(body, "hbm:oil")
        secondary = "ntmspace:hgas" if body == "tekto" else "hbm:gas"
        return primary, secondary
    if kind == "GAS":
        return "hbm:gas", "hbm:petroleum"
    return "ntmspace:brine", "minecraft:empty"


def num(x):
    return int(x) if float(x).is_integer() else x


def deposit(kind, empty, body):
    primary, gas_min, gas_max, drain = PER_BODY.get(body, EARTH)
    assert gas_min * WELL[2] == gas_max * WELL[1], body  # one secondary_yield fits both ends of the range
    p, s = fluids(kind, body)
    out = {"primary": p}
    if s != "minecraft:empty":
        out["secondary"] = s
    out["empty"] = empty
    out["yield"] = num(round(primary / WELL[0], 6))
    secondary_yield = round(gas_min / WELL[1], 6)
    if s != "minecraft:empty" and secondary_yield != out["yield"]:
        out["secondary_yield"] = num(secondary_yield)
    out["drain"] = num(round(drain / WELL[3], 6))
    return out


# hbm-space's ore_bedrock_oil (BlockOreFluid with no empty block, OIL) off Kerbin: ntmspace:ore_bedrock_oil[body=...]
# (fork content part 2; Tekto's MapGenBedrockOil places it). WorldConfig bedrockOilPerDeposit 100, gas 10..50 (Tekto:
# tektoBedrockOilPerDeposit 100, tektoBedrockGas 10..50) over the fracking tower's 500 + 100..500 (NTM: Space sets 500),
# fracking only, never drains - the same yields as the addon's hbm:ore_bedrock_oil entry in fluid_deposit.json.
FRACKING = (500, 100, 500)
BEDROCK = {"tekto": (100, 10, 50)}
BEDROCK_EARTH = (100, 10, 50)


def bedrock(body):
    primary, gas_min, gas_max = BEDROCK.get(body, BEDROCK_EARTH)
    assert gas_min * FRACKING[2] == gas_max * FRACKING[1], body
    p, s = fluids("OIL", body)
    out = {"primary": p, "secondary": s, "yield": num(round(primary / FRACKING[0], 6))}
    secondary_yield = num(round(gas_min / FRACKING[1], 6))
    if secondary_yield != out["yield"]:
        out["secondary_yield"] = secondary_yield
    out["fracking_only"] = True
    return out


def main():
    values = {}
    for block, (kind, empty) in ORES.items():
        values[block] = {"property": "body", "values": {body: deposit(kind, empty, body) for body in BODIES}}
    values["ntmspace:ore_bedrock_oil"] = {"property": "body", "values": {body: bedrock(body) for body in BODIES}}
    os.makedirs(os.path.dirname(PATH), exist_ok=True)
    with open(PATH, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"replace": False, "values": values}, f, indent=2)
        f.write("\n")
    print("gen_fluid_deposits: %d block(s) x %d bodies" % (len(values), len(BODIES)))


if __name__ == "__main__":
    main()
