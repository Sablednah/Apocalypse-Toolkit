"""The guns' models and textures, made from scratch to be tweaked in Blockbench (open the *_3d.json as a Java item
model). Each gun is a handful of cuboids along +X (barrel at the high end, y up, 1 unit = 1/16 block), every face
painted from a swatch of that gun's own palette texture (textures/item/gun/<name>.png, 4x4 swatches of 4x4 pixels),
so a repaint in Blockbench is one gun at a time. The inventory icon (textures/item/<name>.png) is the same cuboids
seen from the side, drawn at 16x16, and items/<name>.json shows the icon in the GUI and the 3D model everywhere else.
Display transforms are a first guess: set them in Blockbench's Display tab.
Run: python3 tools/art/guns.py"""
import json, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "apocalypse_toolkit")
NS = "apocalypse_toolkit"

# swatch index -> colour (4 per row of the 16x16 palette)
PALETTE = [
    (52, 54, 58), (88, 92, 98), (150, 154, 160), (28, 28, 30),       # gunmetal dark, gunmetal, steel, black
    (126, 82, 48), (90, 56, 32), (190, 150, 60), (60, 46, 38),       # wood, dark wood, brass, grip rubber
    (214, 218, 222), (220, 44, 44), (70, 210, 230), (82, 92, 58),    # laser white, laser red, cyan cell, olive
    (120, 124, 128), (40, 70, 110), (230, 230, 210), (170, 40, 30),  # mid grey, scope glass, pale, dark red
]
DARK, METAL, STEEL, BLACK, WOOD, DWOOD, BRASS, GRIP, WHITE, RED, CYAN, OLIVE, GREY, GLASS, PALE, DRED = range(16)

# (from xyz, to xyz, swatch): each gun along +X
GUNS = {
    "pistol": [
        ((4, 8, 7.25), (13, 10.5, 8.75), DARK),      # slide
        ((13, 8.5, 7.6), (14, 9.6, 8.4), BLACK),     # muzzle
        ((4.5, 7, 7.4), (12, 8, 8.6), METAL),        # frame
        ((4.5, 3, 7.3), (7, 7, 8.7), GRIP),          # grip
        ((7, 6, 7.7), (9, 7, 8.3), BLACK),           # trigger guard
        ((11.5, 10.5, 7.8), (12.3, 11, 8.2), BLACK),  # front sight
    ],
    "smg": [
        ((2, 7, 7.2), (12, 10, 8.8), DARK),          # receiver
        ((12, 8, 7.6), (16, 9, 8.4), BLACK),         # barrel
        ((7, 2.5, 7.4), (9, 7, 8.6), BLACK),         # magazine
        ((3, 3.5, 7.3), (5, 7, 8.7), GRIP),          # grip
        ((-1, 8, 7.6), (2, 9, 8.4), METAL),          # wire stock
        ((-2, 6.5, 7.4), (-1, 9.5, 8.6), METAL),     # stock butt
        ((5, 10, 7.7), (8, 10.6, 8.3), BLACK),       # top rail
    ],
    "shotgun": [
        ((2, 9, 7.4), (17, 10.2, 8.6), DARK),        # barrel
        ((5, 7.8, 7.5), (15, 9, 8.5), METAL),        # tube
        ((10, 7.4, 7.2), (13.5, 9.4, 8.8), WOOD),    # pump
        ((0, 7.4, 7.2), (5, 10.4, 8.8), DARK),       # receiver
        ((-7, 6, 7.3), (0, 9.6, 8.7), WOOD),         # stock
        ((-1, 4, 7.4), (1.5, 7.4, 8.6), DWOOD),      # grip
        ((16, 10.2, 7.8), (16.7, 10.7, 8.2), BRASS),  # bead
    ],
    "rifle": [
        ((0, 7, 7.2), (10, 10, 8.8), DARK),          # receiver
        ((10, 8, 7.6), (19, 9, 8.4), BLACK),         # barrel
        ((8, 7.4, 7.2), (13, 9.6, 8.8), WOOD),       # handguard
        ((6, 2, 7.4), (8.5, 7, 8.6), METAL),         # magazine
        ((-6, 6, 7.3), (0, 9.4, 8.7), WOOD),         # stock
        ((1, 3.5, 7.3), (3, 7, 8.7), GRIP),          # grip
        ((17.5, 9, 7.8), (18.2, 10, 8.2), BLACK),    # front sight
    ],
    "sniper_rifle": [
        ((0, 7.5, 7.3), (9, 10, 8.7), OLIVE),        # receiver
        ((9, 8.4, 7.65), (23, 9.4, 8.35), BLACK),    # barrel
        ((1.5, 10.6, 7.3), (9, 12, 8.7), BLACK),     # scope tube
        ((1, 10.3, 7.15), (2, 12.3, 8.85), DARK),     # scope eyepiece
        ((8.5, 10.3, 7.15), (9.5, 12.3, 8.85), DARK),  # scope objective
        ((9.4, 10.8, 7.5), (9.6, 11.8, 8.5), GLASS),  # lens
        ((3, 10, 7.7), (4, 10.6, 8.3), DARK),        # rings
        ((7, 10, 7.7), (8, 10.6, 8.3), DARK),
        ((-7, 6.2, 7.3), (0, 9.6, 8.7), OLIVE),      # stock
        ((0.5, 4, 7.3), (2.5, 7.5, 8.7), GRIP),      # grip
        ((5, 6, 7.5), (6.5, 7.5, 8.5), DARK),        # magazine
    ],
    "laser_rifle": [
        ((0, 7, 7.0), (12, 10.6, 9.0), WHITE),       # body
        ((12, 8, 7.4), (16, 9.6, 8.6), GREY),        # emitter
        ((16, 8.3, 7.6), (17, 9.3, 8.4), RED),       # lens
        ((3, 10.6, 7.4), (8, 11.6, 8.6), CYAN),      # cell
        ((2, 3.5, 7.3), (4, 7, 8.7), GREY),          # grip
        ((-5, 6.5, 7.2), (0, 9.8, 8.8), WHITE),      # stock
        ((5, 6, 7.6), (11, 7, 8.4), RED),            # under-glow strip
    ],
}
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 0], "translation": [0, 2, 1], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 2, 1], "scale": [0.6, 0.6, 0.6]},
    "firstperson_righthand": {"rotation": [0, -90, 0], "translation": [2, 1, 0], "scale": [0.7, 0.7, 0.7]},
    "firstperson_lefthand": {"rotation": [0, 90, 0], "translation": [2, 1, 0], "scale": [0.7, 0.7, 0.7]},
    "ground": {"translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
    "fixed": {"scale": [0.7, 0.7, 0.7]},
    "gui": {"rotation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
}

def swatch_uv(i):
    x, y = (i % 4) * 4, (i // 4) * 4
    return [x, y, x + 4, y + 4]

def palette(seed):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16)); px = im.load()
    for i, c in enumerate(PALETTE):
        x0, y0 = (i % 4) * 4, (i // 4) * 4
        for y in range(4):
            for x in range(4):
                g = rnd.randint(-7, 7) - (4 if y == 3 else 0) + (4 if y == 0 else 0)  # a lit top edge, a shaded foot
                px[x0 + x, y0 + y] = tuple(max(0, min(255, v + g)) for v in c) + (255,)
    return im

def model(name, boxes):
    elements = []
    for (a, b, s) in boxes:
        uv = swatch_uv(s)
        elements.append({"from": list(a), "to": list(b),
                         "faces": {f: {"uv": uv, "texture": "#gun"} for f in ("north", "east", "south", "west", "up", "down")}})
    return {"credit": "Apocalypse Toolkit, tools/art/guns.py; tweak in Blockbench",
            "texture_size": [16, 16], "textures": {"gun": f"{NS}:item/gun/{name}", "particle": f"{NS}:item/gun/{name}"},
            "elements": elements, "display": DISPLAY}

def icon(boxes, angle_deg=0.0):
    """The side view at 16x16: x along, y up, tilted up by angle_deg (long guns go corner to corner, as vanilla's
    tools do), painted far-to-near (by z), with a dark outline."""
    import math
    a = math.radians(angle_deg); ca, sa = math.cos(a), math.sin(a)
    rot = lambda x, y: (x * ca - y * sa, x * sa + y * ca)
    corners = [rot(x, y) for p, q, _ in boxes for x in (p[0], q[0]) for y in (p[1], q[1])]
    x0, x1 = min(c[0] for c in corners), max(c[0] for c in corners)
    y0, y1 = min(c[1] for c in corners), max(c[1] for c in corners)
    scale = 15.0 / max(x1 - x0, y1 - y0)
    ox = (16 - (x1 - x0) * scale) / 2; oy = (16 - (y1 - y0) * scale) / 2
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = im.load()
    for p, q, s in sorted(boxes, key=lambda e: -(e[0][2] + e[1][2])):
        c = PALETTE[s]
        for py in range(16):
            for pxl in range(16):
                rx = x0 + (pxl + 0.5 - ox) / scale
                ry = y1 - (py + 0.5 - oy) / scale
                mx, my = rx * ca + ry * sa, -rx * sa + ry * ca          # back into the gun's own frame
                if p[0] <= mx <= q[0] and p[1] <= my <= q[1]:
                    lit = 18 if my > (p[1] + q[1]) / 2 else -10
                    px[pxl, py] = tuple(max(0, min(255, v + lit)) for v in c) + (255,)
    out = im.copy(); o = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3]
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[x, y] = (18, 18, 20, 255)
    return out

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2); f.write("\n")

def main():
    for n, (name, boxes) in enumerate(GUNS.items()):
        os.makedirs(os.path.join(RES, "textures", "item", "gun"), exist_ok=True)
        palette(n + 1).save(os.path.join(RES, "textures", "item", "gun", name + ".png"))
        icon(boxes, 0.0 if name == "pistol" else 35.0).save(os.path.join(RES, "textures", "item", name + ".png"))
        write(os.path.join(RES, "models", "item", name + "_3d.json"), model(name, boxes))
        write(os.path.join(RES, "models", "item", name + ".json"),
              {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/{name}"}})
        write(os.path.join(RES, "items", name + ".json"), {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui"], "model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}}],
            "fallback": {"type": "minecraft:model", "model": f"{NS}:item/{name}_3d"}}})
    print("guns written")

if __name__ == "__main__":
    main()
