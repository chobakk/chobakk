#!/usr/bin/env python3
"""Packs the three.js renders (out-prod/) into the game's sprite assets (palette PNGs + sprites.json).
Usage: python3 pack.py <render-out-dir> <app/src/main/assets/sprites>"""
from PIL import Image
import json, os, glob, sys
S, A = sys.argv[1], sys.argv[2]
os.makedirs(A, exist_ok=True)
def q(im):
    # palette quantize with alpha to shrink files ~3x
    return im.quantize(colors=255, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
man = {"chars": {}, "guns": {}}
for n in [f"z{i}" for i in range(11)] + [f"s{i}" for i in range(4)] + ["b0", "b1", "b2"]:
    im = Image.open(f"{S}/{n}.png").convert("RGBA")
    W, H = im.size; cw, ch = W // 8, H // 9
    q(im).save(f"{A}/{n}.png", optimize=True)
    man["chars"][n] = {"cw": cw, "ch": ch, "frames": 8, "rows": 9}
guns = sorted(set(os.path.basename(p).rsplit('_', 1)[0] for p in glob.glob(f"{S}/g_*_side.png")))
atlas = Image.new("RGBA", (512, 128 * len(guns)))
for i, g in enumerate(guns):
    side = Image.open(f"{S}/{g}_side.png").convert("RGBA"); top = Image.open(f"{S}/{g}_top.png").convert("RGBA")
    atlas.alpha_composite(side, (0, i * 128)); atlas.alpha_composite(top, (256, i * 128))
    fp = Image.open(f"{S}/{g}_fp.png").convert("RGBA").resize((384, 384), Image.LANCZOS)
    px = fp.load(); best = None
    for y in range(384):
        for x in range(384):
            if px[x, y][3] > 40 and (best is None or x + y < best[0]): best = (x + y, x, y)
    q(fp).save(f"{A}/fp_{g[2:]}.png", optimize=True)
    sb = side.getbbox(); sp = side.load(); mx = sb[2] - 1
    ys = [y for y in range(128) if sp[mx, y][3] > 40]; my = sum(ys) / len(ys) if ys else 64
    man["guns"][g[2:]] = {"row": i, "sideBox": sb, "muzzle": [mx, my], "fpMuzzle": [best[1], best[2]], "fpBox": fp.getbbox()}
q(atlas).save(f"{A}/guns.png", optimize=True)
json.dump(man, open(f"{A}/sprites.json", "w"), indent=1)
print("packed", len(man["chars"]), "characters,", len(guns), "guns")
