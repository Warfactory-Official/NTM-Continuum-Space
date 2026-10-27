"""Writes the HBM value-map entries of NTM: Space's special items (registry/SpaceSpecialItems) from hbm-space's values.

  data/ntmspace/value_maps/patches/hbm/item/watz_pellet.json  the nine Watz pellets hbm-space added to EnumWatzType, and
                                                              HBM's twelve at hbm-space's doubled yield
  data/ntmspace/value_maps/patches/hbm/item/rtg_pellet.json   pellet_rtg_berkelium / cf251 / cf252
  data/ntmspace/value_maps/patches/hbm/item/hazards.json      HazardRegistry rows of these items

Each file is merged: entries this script owns are replaced, any other entry (written by another family) is kept, and
"replace": false is set so HBM's own entries stay. Float arithmetic follows 1.7's Java float math (numpy float32). Run
from the repository root:  python -I new-hbm-space/tools/gen_special_item_data.py
"""
import json
import os

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MAPS = os.path.join(ROOT, "new-hbm-space", "src", "main", "resources", "data", "ntmspace", "value_maps", "patches", "hbm", "item")
NS = "ntmspace:"

f32 = np.float32


def fl(x):
    """A Java float as JSON reads it back (shortest repr of the float32)."""
    return float(repr(float(f32(x))))


# ---------------------------------------------------------------- Watz (hbm-space ItemWatzPellet.EnumWatzType)
# name: (light, dark, passive, heat, mud (halved by the constructor), burn level, heat_div sqrt level, heat_div off)
WATZ = {
    "pu241": (0x78817E, 394240, 1950, 25, 0.0025, 1.30, 2.66 / 18, 24.0 * 24.0),
    "amrg": (0x93767B, 0x66474D, 2888, 48, 0.0035, 1.33, 4.33 / 25.5, 28.0 * 28.0),
    "amf": (0x93767B, 0x66474D, 2333, 44, 0.003, 1.33, 4.11 / 22.2, 27.0 * 27.0),
    "cmrg": (0xD8C2C4, 0xAD9799, 2999, 50, 0.005, 1.5, 5.5 / 25.5, 30.0 * 28.0),
    "cmf": (0xD8C2C4, 0xAD9799, 2444, 48, 0.0045, 1.8, 5.0 / 20, 26.0 * 24.0),
    "bk247": (0xC2C9C7, 0x8D9592, 3000, 55, 0.012, 1.5, 6.0 / 23.5, 10.0 * 10.0),
    "cf251": (0x7879B4, 0x4D4E89, 1250, 60, 0.001, 1.7, 6.65 / 23.5, 10.0 * 10.0),
    "cf252": (0x7879B4, 0x4D4E89, 1050, 120, 0.0015, 1.8, 8.85 / 28.8, 10.0 * 10.0),
    "es253": (0xB9BFB2, 0x594E44, 3750, 70, 0.0001, 1.3, 7.0 / 27.7, 10.0 * 10.0),
}
# hbm-space raised every pellet's yield to 1e9 (original-hbm: 5e8, HBM's default). User rule 2026-10-11: with the addon
# installed HBM's own twelve pellets get hbm-space's yield too, so they are restated here (a value-map patch replaces a
# whole entry) from HBM's own map, whose other values equal hbm-space's (EnumWatzType differs only in the yield line).
WATZ_YIELD = 1_000_000_000.0
HBM_WATZ = os.path.join(ROOT, "NTM-Continuum", "src", "main", "resources", "data", "hbm", "value_maps", "item", "watz_pellet.json")


def watz():
    out = {}
    for name, (light, dark, passive, heat, mud, burn, div, off) in WATZ.items():
        out[NS + "watz_pellet_" + name] = {
            "color": "#%06X" % light,
            "color_dark": "#%06X" % dark,
            "passive": float(passive),
            "heat": float(heat),
            "mud": mud / 2.0,
            "burn": {"type": "linear", "level": burn},
            "heat_div": {"type": "sqrt", "level": div, "off": off},
            "yield": WATZ_YIELD,
            "depleted": NS + "watz_pellet_depleted_" + name,
        }
    with open(HBM_WATZ, encoding="utf-8") as fh:
        hbm = json.load(fh)["values"]
    for key, entry in hbm.items():
        patched = {k: v for k, v in entry.items() if k != "depleted"}
        patched["yield"] = WATZ_YIELD
        patched["depleted"] = entry["depleted"]
        out[key] = patched
    return out


# ---------------------------------------------------------------- RTG (hbm-space ModItems)
def lifespan(half_life, kind, mult):
    """(long) (RTGUtil.getLifespan(halfLife, type, false) * mult); getLifespan multiplies in float."""
    base = {"LONG": 48000 * 100 * 100, "SHORT": 48000}[kind]
    return int(float(f32(f32(base) * f32(half_life))) * mult)


def rtg():
    # hbm-space's RTG model is original-hbm's (identical ItemRTGPellet / RTGUtil): linear over one lifespan in ticks
    return {
        NS + "pellet_rtg_berkelium": {"heat": 20, "lifespan": lifespan(13.8, "LONG", 1.5),
                                      "decays_to": NS + "pellet_rtg_depleted_americium"},
        # VersatileConfig.rtgDecay() ? 600 : 200
        NS + "pellet_rtg_cf251": {"heat": 200, "heat_decaying": 600, "lifespan": lifespan(1.0, "SHORT", 2.5),
                                  "decays_to": "hbm:pellet_rtg_depleted_lead"},
        NS + "pellet_rtg_cf252": {"heat": 200, "heat_decaying": 600, "lifespan": lifespan(1.0, "SHORT", 2.5),
                                  "decays_to": "hbm:pellet_rtg_depleted_lead"},
    }


# ---------------------------------------------------------------- hazards (hbm-space HazardRegistry)
WST, BILLET, INGOT, ROD_RBMK = f32(15.0), f32(0.5), f32(1.0), f32(0.5) * f32(8)
RTG_MULT = BILLET * f32(3)
BK247, CF252, CMRG, CMF, AMRG, PU241, ES253 = f32(10.5), f32(15.3), f32(6.0), f32(2.2), f32(9.0), f32(25.0), f32(18.3)


def rad(level):
    return {"entries": [{"type": "hbm:radiation", "level": fl(level)}]}


def rbmk_rod(base, dep):
    return {"entries": [
        {"type": "hbm:radiation", "level": fl(base), "modifiers": [{"target": fl(dep), "type": "hbm:rbmk_radiation"}]},
        {"type": "hbm:hot", "level": 0.0, "modifiers": [{"type": "hbm:rbmk_hot"}]}]}


def rbmk_pellet(base, dep):
    return {"entries": [
        {"type": "hbm:radiation", "level": fl(base), "modifiers": [{"target": fl(dep), "type": "hbm:rbmk_radiation"}]}]}


def hazards():
    h = {}
    h[NS + "rbmk_fuel_bk247"] = rbmk_rod(BK247 * ROD_RBMK, WST * ROD_RBMK * f32(38))
    h[NS + "rbmk_fuel_lecm"] = rbmk_rod(CMRG * f32(2.2) * ROD_RBMK / f32(5), WST * ROD_RBMK)
    h[NS + "rbmk_fuel_mecm"] = rbmk_rod(CMRG * f32(2.2) * ROD_RBMK / f32(3), WST * ROD_RBMK * f32(3))
    h[NS + "rbmk_fuel_hecm"] = rbmk_rod(CMRG * f32(2.2) * ROD_RBMK / f32(1.5), WST * ROD_RBMK * f32(5))
    h[NS + "rbmk_pellet_bk247"] = rbmk_pellet(BK247 * BILLET, WST * BILLET * f32(37))
    h[NS + "rbmk_pellet_lecm"] = rbmk_pellet(CMRG * BILLET, WST * CF252 * f32(2.5))
    h[NS + "rbmk_pellet_mecm"] = rbmk_pellet(CMRG * BILLET, WST * CF252 * f32(2.6))
    h[NS + "rbmk_pellet_hecm"] = rbmk_pellet(CMRG * BILLET, WST * CF252 * f32(2.7))
    h[NS + "pellet_rtg_depleted_americium"] = rad(AMRG * RTG_MULT)
    for name, base in (("pu241", PU241), ("amrg", AMRG), ("cmrg", CMRG), ("cmf", CMF), ("bk247", BK247), ("cf252", CF252),
                       ("es253", ES253)):
        h[NS + "watz_pellet_" + name] = rad(base * INGOT * f32(4))
    return h


def merge(file, entries):
    path = os.path.join(MAPS, file)
    data = {"replace": False, "values": {}}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as fh:
            data = json.load(fh)
        data["replace"] = False
    values = data.setdefault("values", {})
    for key, value in entries.items():
        values[key] = value
    os.makedirs(MAPS, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump({"replace": False, "values": values}, fh, indent=2)
        fh.write("\n")
    print(file, len(entries), "entries")


if __name__ == "__main__":
    merge("watz_pellet.json", watz())
    merge("rtg_pellet.json", rtg())
    merge("hazards.json", hazards())
