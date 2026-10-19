"""Writes the HBM hazard value-map rows of hbm-space's non-space fork content (the "isotope chain" and the other
HazardRegistry / OreDictManager additions) into data/ntmspace/value_maps/patches/hbm/item/hazards.json.

hbm-space gives its new actinides their hazards through 1.7 DictFrames (OreDictManager: CM242.rad(cm242)...), so every
item carrying the ore-dictionary name gets level * shape multiplier (nugget 0.1, billet 0.5, ingot 1, dust 3, plate 1).
HBM spells those as tag rows ("#c:ingots/cm242", "mutex": 1, like its own). Constants are hbm-space HazardRegistry's;
products are Java float math (numpy float32), as 1.7's HazardEntry.clone(mult) did.

Not written: the NEUTRON entries (.neutron(...)) - they are code rows of com.ntmspace.fork.neutron.NeutronActivation
(a hazard transformer: several of their items are HBM's, whose rows this file would override); and the AUTISM / GLITCH types
(sat_gerald, bobbleheads).

The file is merged: rows this script owns are replaced, every other row (other families) is kept. Run from the
repository root:  python -I new-hbm-space/tools/gen_fork_hazards.py
"""
import json
import os

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PATH = os.path.join(ROOT, "new-hbm-space", "src", "main", "resources", "data", "ntmspace", "value_maps", "patches", "hbm", "item", "hazards.json")

f32 = np.float32

# hbm-space HazardRegistry shape multipliers
NUGGET, BILLET, INGOT, POWDER, PLATE = f32(0.1), f32(0.5), f32(1.0), f32(3.0), f32(1.0)
SHAPES = {"nuggets": NUGGET, "billets": BILLET, "ingots": INGOT, "dusts": POWDER, "plates": PLATE}

# hbm-space HazardRegistry constants
CM242, CM243, CM244, CM245, CM246, CM247 = 9.3, 5.6, 2.0, 0.8, 2.5, 0.2
CMRG, CMF, BK247, CF251, CF252, ES253, ES255, CN989 = 6.0, 2.2, 10.5, 14.3, 15.3, 18.3, 19.3, 89.0

# OreDictManager (hbm-space lines 406-434): material -> (rad, hot, shapes the frame registers)
NBI = ("nuggets", "billets", "ingots")
FRAMES = {
    "cm242": (CM242, None, NBI),
    "cm243": (CM243, None, NBI),
    "cm244": (CM244, None, NBI),
    "cm245": (CM245, None, NBI),
    "cm246": (CM246, None, NBI),
    "cm247": (CM247, None, NBI),
    "cm_mix": (CMRG, None, NBI),      # CMRG frame: nugget_cm_mix / billet_cm_mix / ingot_cm_mix
    "cm_fuel": (CMF, None, NBI),      # CMF frame
    "bk247": (BK247, None, NBI),
    "cf251": (CF251, 2.0, NBI),
    "cf252": (CF252, 3.0, NBI),
    "es253": (ES253, 4.0, NBI),
    "es255": (ES255, None, ("ingots",)),
    "cn989": (CN989, 4.0, NBI + ("dusts", "plates")),
}


def fl(x):
    """A Java float as JSON reads it back (shortest repr of the float32)."""
    return float(repr(float(f32(x))))


def rows():
    out = {}
    for mat, (rad, hot, shapes) in FRAMES.items():
        for shape in shapes:
            mult = SHAPES[shape]
            entries = [{"type": "hbm:radiation", "level": fl(f32(rad) * mult)}]
            if hot is not None:
                entries.append({"type": "hbm:hot", "level": fl(f32(hot) * mult)})
            out["#c:%s/%s" % (shape, mat)] = {"mutex": 1, "entries": entries}
    # HazardRegistry.registerItem additions (hbm-space lines 459-470)
    out["ntmspace:ammonium_nitrate"] = {"entries": [{"type": "hbm:explosive", "level": 4.0}]}
    out["ntmspace:powder_wd2004"] = {"entries": [{"type": "hbm:digamma", "level": 1.0}]}
    out["ntmspace:powder_wd2004_tiny"] = {"entries": [{"type": "hbm:digamma", "level": fl(0.05)}]}
    # hbm-space lines 187-199: bobblehead metadata 22 ASBESTOS 1 ("//this works okay"), metadata 25 GLITCH 1, sat_gerald
    # AUTISM 1. hbm-space reordered BobbleType, so 22 is its JUICE and 25 its MICROWAVE (ntmspace:bobblehead_juice is
    # NTM: Space's figure, hbm:bobblehead_microwave HBM's); GLITCH/AUTISM are NTM: Space's hazard types (fork/bobble/ForkHazards).
    out["ntmspace:bobblehead_juice"] = {"entries": [{"type": "hbm:asbestos", "level": 1.0}]}
    out["hbm:bobblehead_microwave"] = {"entries": [{"type": "ntmspace:glitch", "level": 1.0}]}
    out["hbm:sat_gerald"] = {"entries": [{"type": "ntmspace:autism", "level": 1.0}]}
    return out


def main():
    with open(PATH, encoding="utf-8") as f:
        data = json.load(f)
    values = data.setdefault("values", {})
    mine = rows()
    for k, v in mine.items():
        values[k] = v
    data["replace"] = False
    with open(PATH, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("gen_fork_hazards: wrote %d row(s), file has %d" % (len(mine), len(values)))


if __name__ == "__main__":
    main()
