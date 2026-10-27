"""Pre-bakes NTM: Space's Watz pellet textures (hbm-space ItemWatzPellet.registerIcons).

1.7 recoloured the grey watz_pellet sprite at atlas-stitch time with
RGBMutatorInterpolatedComponentRemap(0xD2D2D2, 0x333333, light, dark), the depleted pellet with desaturate(light/dark).
Modern items have no runtime atlas mutation, so (like HBM's own pre-baked watz_pellet_<type>.png) the result is written
once: ntmspace:textures/items/watz_pellet_<type>.png and watz_pellet_depleted_<type>.png. Integer arithmetic follows the
Java source exactly (int casts truncate toward zero, & 0xff wraps). Run from the repository root:

    python -I new-hbm-space/tools/gen_watz_pellet_textures.py [--check-hbm]

--check-hbm regenerates HBM's twelve pellets into memory and compares them with HBM's committed PNGs: with the remap
rounded (as HBM's own baker did; 1.7's (int) cast truncates, so HBM's PNGs are up to 1 per channel off 1.7) all 24 match
pixel for pixel, the proof that this is otherwise the same transform. NTM: Space's pellets are written with 1.7's
truncation.
"""
import os
import sys
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
BASE = os.path.join(ROOT, "NTM-Continuum", "src", "main", "resources", "assets", "hbm", "textures", "items", "watz_pellet.png")
OUT = os.path.join(ROOT, "new-hbm-space", "src", "main", "resources", "assets", "ntmspace", "textures", "items")

# hbm-space EnumWatzType additions: (light, dark)
SPACE = {
    "pu241": (0x78817E, 394240),
    "amrg": (0x93767B, 0x66474D),
    "amf": (0x93767B, 0x66474D),
    "cmrg": (0xD8C2C4, 0xAD9799),
    "cmf": (0xD8C2C4, 0xAD9799),
    "bk247": (0xC2C9C7, 0x8D9592),
    "cf251": (0x7879B4, 0x4D4E89),
    "cf252": (0x7879B4, 0x4D4E89),
    "es253": (0xB9BFB2, 0x594E44),
}
HBM = {
    "schrabidium": (0x32FFFF, 0x005C5C), "hes": (0x66DCD6, 0x023933), "mes": (0xCBEADF, 0x28473C),
    "les": (0xABB4A8, 0x0C1105), "hen": (0xA6B2A6, 0x030F03), "meu": (0xC1C7BD, 0x2B3227),
    "mep": (0x9AA3A0, 0x111A17), "lead": (0xA6A6B2, 0x03030F), "boron": (0xBDC8D2, 0x29343E),
    "du": (0xC1C7BD, 0x2B3227), "nqd": (0x4B4B4B, 0x121212), "nqr": (0x2D2D2D, 0x0B0B0B),
}


def jint(x):
    """Java (int) cast of a double: truncation toward zero."""
    return int(x)


def desaturate(color):
    r = (color & 0xff0000) >> 16
    g = (color & 0x00ff00) >> 8
    b = color & 0x0000ff
    avg = (r + g + b) // 3
    approach, mult = 0.9, 0.75
    # Java compound assignment on int: r -= double  ->  r = (int)(r - double)
    r = jint(r - (r - avg) * approach)
    g = jint(g - (g - avg) * approach)
    b = jint(b - (b - avg) * approach)
    r = jint(r * mult)
    g = jint(g * mult)
    b = jint(b * mult)
    return (r << 16) | (g << 8) | b


ROUND_REMAP = False  # True only for --check-hbm: HBM's baker rounded here where 1.7's (int) cast truncates


def shift(bound_light, bound_dark, light, dark, comp_of, value):
    lo, hi = comp_of(bound_light), comp_of(bound_dark)
    pos = (value - lo) / (hi - lo)
    t_lo, t_hi = comp_of(light), comp_of(dark)
    v = t_lo + pos * (t_hi - t_lo)
    return (int(v + 0.5) if v >= 0 else -int(-v + 0.5)) & 0xff if ROUND_REMAP else jint(v) & 0xff


def remap(image, light, dark):
    out = image.copy()
    px = out.load()
    for x in range(out.width):
        for y in range(out.height):
            r, g, b, a = px[x, y]
            nr = shift(0xD2D2D2, 0x333333, light, dark, lambda c: (c >> 16) & 0xff, r)
            ng = shift(0xD2D2D2, 0x333333, light, dark, lambda c: (c >> 8) & 0xff, g)
            nb = shift(0xD2D2D2, 0x333333, light, dark, lambda c: c & 0xff, b)
            px[x, y] = (nr, ng, nb, a)
    return out


def bake(base, light, dark, depleted):
    if depleted:
        light, dark = desaturate(light), desaturate(dark)
    return remap(base, light, dark)


def main():
    base = Image.open(BASE).convert("RGBA")
    if "--check-hbm" in sys.argv:
        global ROUND_REMAP
        ROUND_REMAP = True
        hbm_dir = os.path.dirname(BASE)
        bad = 0
        for name, (light, dark) in HBM.items():
            for depleted in (False, True):
                file = os.path.join(hbm_dir, ("watz_pellet_depleted_" if depleted else "watz_pellet_") + name + ".png")
                ref = Image.open(file).convert("RGBA")
                mine = bake(base, light, dark, depleted)
                # transparent pixels carry no colour: compare RGB only where alpha > 0
                diff = sum(1 for p, q in zip(mine.getdata(), ref.getdata()) if p[3] != q[3] or (p[3] > 0 and p[:3] != q[:3]))
                if diff:
                    bad += 1
                    print("DIFF", os.path.basename(file), diff, "pixels")
        print("checked", len(HBM) * 2, "HBM textures,", bad, "differ")
        return
    os.makedirs(OUT, exist_ok=True)
    for name, (light, dark) in SPACE.items():
        bake(base, light, dark, False).save(os.path.join(OUT, "watz_pellet_" + name + ".png"))
        bake(base, light, dark, True).save(os.path.join(OUT, "watz_pellet_depleted_" + name + ".png"))
    print("wrote", len(SPACE) * 2, "textures to", OUT)


if __name__ == "__main__":
    main()
