"""Data of hbm-space's four extra short-lived waste classes (ItemWasteShort.WasteClass AMERICIUM241 = meta 7,
BERKELIUM247 = 9, CURIUM244 = 10, CURIUM245 = 11), the items registered by registry/SpaceWasteItems:

- crafting: MineralRecipes add1To9PairSameMeta for nuclear_waste_short <-> _tiny and _depleted <-> _depleted_tiny,
  named as HBM names its own pairs (<one>_to_<nine>_<meta>, <nine>_to_<one>_<meta>, the 1.7 metadata)
- storage drum (hbm:storage_drum rows): TileEntityStorageDrum.updateEntity turns short waste into its depleted form,
  tiny waste with a tenth of the chance (divisor 10) and a tenth of the byproducts. The byproducts are 1.7's values as
  1.7 computed them: `ItemWasteShort.WasteClass.values()[ItemWasteLong.rectify(meta)]`, i.e. the class at index
  meta % 5 (the long family's size), NOT the class's own liquid/gas - a 1.7 slip that is ported as it is (AGENTS.md
  section 3). hbm-space order: 0 U235 (0, 100), 1 U233 (50, 100), 2 NEPTUNIUM (150, 500), 3 PU239, 4 PU240 (350, 1000).
  So Am-241 (7 -> 2) 150/500, Bk-247 (9 -> 4) 350/1000, Cm-244 (10 -> 0) 0/100, Cm-245 (11 -> 1) 50/100.
- RTG/radgen fuel (hbm:radgen_fuel value map, the addon's copy merges with HBM's): TileEntityMachineRadGen.fuels for
  every WasteClass: short 1500 HE/t for 30 min -> short_depleted, short_tiny 150 for 3 min -> short_depleted_tiny
- hazards (hbm:hazards value map rows, merged into the addon's hazards.json): HazardRegistry short RADIATION 30 + HOT 5,
  short_tiny 3 + HOT 5, short_depleted 3, short_depleted_tiny 0.3 (per item, as 1.7 registered per item)

SILEX rows for these classes are in data/ntmspace/recipe/silex (m3 recipe-change family); the tiny forms run the full
form's row through HBM's SILEXRecipes.addTinyWaste (SpaceWasteItems.init).

Idempotent; owns the files it writes (listed in tools/gen_space_waste.manifest) and only its rows of the two value maps.
Run: python -I new-hbm-space/tools/gen_space_waste.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
RECIPE = os.path.join(DATA, "ntmspace", "recipe")
VALUE_MAPS = os.path.join(DATA, "ntmspace", "value_maps", "patches", "hbm", "item")
MANIFEST = os.path.join(ROOT, "tools", "gen_space_waste.manifest")
NS = "ntmspace:"

# class id suffix -> 1.7 metadata
CLASSES = {"americium241": 7, "berkelium247": 9, "curium244": 10, "curium245": 11}
# hbm-space ItemWasteShort.WasteClass in its order: (liquid, gas)
SHORT_17 = [(0, 100), (50, 100), (150, 500), (250, 1000), (350, 1000), (500, 1000), (750, 1000), (750, 1000),
            (1000, 1000), (1000, 1000), (1000, 1000), (1000, 1000)]
LONG_SIZE = 5  # ItemWasteLong.WasteClass.values().length

files = {}
radgen = {}
hazards = {}

for c, meta in CLASSES.items():
    full, tiny = "nuclear_waste_short_" + c, "nuclear_waste_short_tiny_" + c
    dfull, dtiny = "nuclear_waste_short_depleted_" + c, "nuclear_waste_short_depleted_tiny_" + c
    for one, nine, fam_one, fam_nine in [(full, tiny, "nuclear_waste_short", "nuclear_waste_short_tiny"),
                                         (dfull, dtiny, "nuclear_waste_short_depleted", "nuclear_waste_short_depleted_tiny")]:
        files["crafting/%s_to_%s_%d" % (fam_one, fam_nine, meta)] = {
            "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [NS + one],
            "result": {"id": NS + nine, "count": 9}}
        files["crafting/%s_to_%s_%d" % (fam_nine, fam_one, meta)] = {
            "type": "minecraft:crafting_shaped", "category": "misc", "key": {"#": NS + nine},
            "pattern": ["###", "###", "###"], "result": {"id": NS + one}}
    liquid, gas = SHORT_17[meta % LONG_SIZE]
    files["storage_drum/" + full] = {"type": "hbm:storage_drum", "input": NS + full, "output": NS + dfull,
                                     "decay": "short", "liquid": liquid, "gas": gas}
    files["storage_drum/" + tiny] = {"type": "hbm:storage_drum", "input": NS + tiny, "output": NS + dtiny,
                                     "decay": "short", "divisor": 10, "liquid": liquid // 10, "gas": gas // 10}
    radgen[NS + full] = {"production": 1500, "duration": 30 * 60 * 20, "output": NS + dfull}
    radgen[NS + tiny] = {"production": 150, "duration": 3 * 60 * 20, "output": NS + dtiny}
    hazards[NS + full] = {"entries": [{"type": "hbm:radiation", "level": 30.0}, {"type": "hbm:hot", "level": 5.0}]}
    hazards[NS + tiny] = {"entries": [{"type": "hbm:radiation", "level": 3.0}, {"type": "hbm:hot", "level": 5.0}]}
    hazards[NS + dfull] = {"entries": [{"type": "hbm:radiation", "level": 3.0}]}
    hazards[NS + dtiny] = {"entries": [{"type": "hbm:radiation", "level": 0.3}]}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(data, indent=2) + "\n")


def merge_map(name, rows):
    path = os.path.join(VALUE_MAPS, name + ".json")
    data = {"replace": False, "values": {}}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    data["values"].update(rows)
    write(path, data)


def main():
    old = []
    if os.path.exists(MANIFEST):
        old = [line.strip() for line in open(MANIFEST, encoding="utf-8") if line.strip()]
    for rel in old:
        if rel not in files and os.path.exists(os.path.join(RECIPE, rel + ".json")):
            os.remove(os.path.join(RECIPE, rel + ".json"))
    for rel, data in files.items():
        write(os.path.join(RECIPE, rel + ".json"), data)
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(sorted(files)) + "\n")
    merge_map("radgen_fuel", radgen)
    merge_map("hazards", hazards)
    print("wrote %d recipe files, %d radgen_fuel rows, %d hazard rows" % (len(files), len(radgen), len(hazards)))


if __name__ == "__main__":
    main()
