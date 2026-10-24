"""Blocks for holding ground: the C4 charge (a cuboid model on a palette, for Blockbench, turned like a button),
barbed wire (a cross of coiled wire, like a cobweb) and the spike block (a plate of spikes, its sides and underside a
riveted iron box). Textures, models, blockstates, item definitions and loot tables.
Run: python3 tools/art/defences.py"""
import json, math, os, random, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import guns  # noqa: E402
from PIL import Image

RES = os.path.join(HERE, "..", "..", "src", "main", "resources")
NS = "apocalypse_toolkit"
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data")

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2); f.write("\n")

def loot(name, survive=True):
    pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}]}
    if survive: pool["conditions"] = [{"condition": "minecraft:survives_explosion"}]
    write(os.path.join(D, NS, "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [pool]})

# ---- C4: on the floor, the long side along z -------------------------------------------------------------------

C4_BOXES = [
    ((4, 0, 3), (12, 2.5, 13), guns.PALE),        # the putty, in its wrap
    ((4, 0.4, 5), (12, 2.6, 6), guns.BLACK),       # straps
    ((4, 0.4, 10), (12, 2.6, 11), guns.BLACK),
    ((5.5, 2.5, 6.5), (10.5, 3.5, 9.5), guns.DARK),  # the receiver
    ((6, 3.5, 7), (8, 3.6, 8), guns.RED),          # its light
    ((9, 3.5, 7), (9.5, 6.5, 7.5), guns.BLACK),    # aerial
]

def c4():
    tex = os.path.join(A, "textures", "block"); os.makedirs(tex, exist_ok=True)
    guns.palette(44).save(os.path.join(tex, "c4.png"))
    elements = [{"from": list(a), "to": list(b),
                 "faces": {f: {"uv": guns.swatch_uv(s), "texture": "#c4"} for f in ("north", "east", "south", "west", "up", "down")}}
                for a, b, s in C4_BOXES]
    write(os.path.join(A, "models", "block", "c4.json"),
          {"credit": "Apocalypse Toolkit, tools/art/defences.py; tweak in Blockbench", "texture_size": [16, 16],
           "textures": {"c4": f"{NS}:block/c4", "particle": f"{NS}:block/c4"}, "elements": elements,
           "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [0.9, 0.9, 0.9]},
                       "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                       "fixed": {"rotation": [90, 0, 0], "scale": [0.8, 0.8, 0.8]},
                       "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 2], "scale": [0.5, 0.5, 0.5]},
                       "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
                       "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]}}})
    # turned as vanilla turns a button
    turns = {("floor", "north"): {}, ("floor", "east"): {"y": 90}, ("floor", "south"): {"y": 180}, ("floor", "west"): {"y": 270},
             ("wall", "north"): {"x": 90}, ("wall", "east"): {"x": 90, "y": 90}, ("wall", "south"): {"x": 90, "y": 180},
             ("wall", "west"): {"x": 90, "y": 270},
             ("ceiling", "north"): {"x": 180, "y": 180}, ("ceiling", "east"): {"x": 180, "y": 270},
             ("ceiling", "south"): {"x": 180}, ("ceiling", "west"): {"x": 180, "y": 90}}
    write(os.path.join(A, "blockstates", "c4.json"), {"variants": {
        f"face={f},facing={d}": {"model": f"{NS}:block/c4", **r} for (f, d), r in turns.items()}})
    write(os.path.join(A, "items", "c4.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:block/c4"}})
    loot("c4", survive=False)

# ---- barbed wire --------------------------------------------------------------------------------------------------

def barbed_wire_texture():
    """A coil of wire seen side on: overlapping loops (lit along their tops, dark underneath) with barbs twisted on."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = im.load()
    lit, wire, dark, barb = (196, 200, 206, 255), (140, 144, 150, 255), (78, 80, 86, 255), (60, 62, 66, 255)
    for cx in (3.0, 8.0, 13.0):
        for t in range(0, 120):
            ang = t / 120 * math.tau
            x, y = cx + math.cos(ang) * 3.4, 9 + math.sin(ang) * 5.0
            xi, yi = int(round(x)), int(round(y))
            if 0 <= xi < 16 and 0 <= yi < 16:
                px[xi, yi] = lit if math.sin(ang) < -0.4 else dark if math.sin(ang) > 0.5 else wire
    for bx, by in ((5, 4), (11, 4), (1, 9), (8, 14), (14, 11), (3, 13), (10, 7)):   # barbs: little x marks
        for dx, dy in ((-1, -1), (1, 1), (1, -1), (-1, 1)):
            if 0 <= bx + dx < 16 and 0 <= by + dy < 16: px[bx + dx, by + dy] = barb
        px[bx, by] = lit
    return im

def barbed_wire():
    barbed_wire_texture().save(os.path.join(A, "textures", "block", "barbed_wire.png"))
    write(os.path.join(A, "models", "block", "barbed_wire.json"),
          {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": f"{NS}:block/barbed_wire"}})
    write(os.path.join(A, "blockstates", "barbed_wire.json"), {"variants": {"": {"model": f"{NS}:block/barbed_wire"}}})
    write(os.path.join(A, "models", "item", "barbed_wire.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/barbed_wire"}})
    write(os.path.join(A, "items", "barbed_wire.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/barbed_wire"}})
    loot("barbed_wire")

# ---- spikes -------------------------------------------------------------------------------------------------------

def riveted(seed):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            g = rnd.randint(-6, 6) + (10 if y == 0 or x == 0 else -12 if y == 15 or x == 15 else 0)
            rust = 1 if rnd.random() < 0.06 else 0
            base = (112, 92, 78) if rust else (92, 96, 102)
            px[x, y] = tuple(max(0, min(255, v + g)) for v in base) + (255,)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = (150, 154, 160, 255); px[x + 1, y + 1] = (50, 52, 56, 255)
    return im

def spikes_top():
    im = riveted(7); px = im.load()
    for cx, cy in ((3, 3), (8, 3), (13, 3), (5, 8), (10, 8), (3, 13), (8, 13), (13, 13), (0, 8), (15, 8)):
        for r, c in ((2, (60, 62, 68)), (1, (150, 154, 162)), (0, (226, 228, 232))):   # a cone seen from above
            for dy in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if abs(dx) + abs(dy) <= r and 0 <= cx + dx < 16 and 0 <= cy + dy < 16:
                        px[cx + dx, cy + dy] = c + (255,)
    return im

def spikes_side():
    """The iron box below, the spikes' points rising over its top edge (three to a side)."""
    im = riveted(9); px = im.load()
    for y in range(0, 7):
        for x in range(16): px[x, y] = (0, 0, 0, 0)
    for cx in (2.5, 7.5, 12.5):
        for y in range(0, 7):
            half = (y + 1) * 0.36
            for x in range(16):
                if abs(x + 0.5 - cx) <= half:
                    px[x, y] = ((236, 238, 242) if x + 0.5 < cx else (150, 154, 162) if abs(x + 0.5 - cx) < half - 0.6 else (96, 98, 104)) + (255,)
    for x in range(16): px[x, 7] = (50, 52, 56, 255)
    return im

def spikes():
    tex = os.path.join(A, "textures", "block")
    spikes_top().save(os.path.join(tex, "spikes_top.png"))
    spikes_side().save(os.path.join(tex, "spikes_side.png"))
    riveted(11).save(os.path.join(tex, "spikes_bottom.png"))
    T = lambda n: f"{NS}:block/{n}"
    side_low = {"uv": [0, 7, 16, 16], "texture": "#side"}
    elements = [{"from": [0, 0, 0], "to": [16, 9, 16], "faces": {
        "north": side_low, "south": side_low, "east": side_low, "west": side_low,
        "up": {"uv": [0, 0, 16, 16], "texture": "#top"}, "down": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"}}}]
    points = {"uv": [0, 0, 16, 7], "texture": "#side"}
    for c in (2.5, 7.5, 12.5):                     # rows of points both ways, crossing over the plate
        elements.append({"from": [0, 9, c], "to": [16, 16, c], "shade": False, "faces": {"north": points, "south": points}})
        elements.append({"from": [c, 9, 0], "to": [c, 16, 16], "shade": False, "faces": {"east": points, "west": points}})
    write(os.path.join(A, "models", "block", "spikes.json"),
          {"credit": "Apocalypse Toolkit, tools/art/defences.py; tweak in Blockbench", "parent": "minecraft:block/block",
           "render_type": "minecraft:cutout",
           "textures": {"top": T("spikes_top"), "side": T("spikes_side"), "bottom": T("spikes_bottom"), "particle": T("spikes_bottom")},
           "elements": elements})
    write(os.path.join(A, "blockstates", "spikes.json"), {"variants": {"": {"model": f"{NS}:block/spikes"}}})
    write(os.path.join(A, "items", "spikes.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:block/spikes"}})
    loot("spikes")

def main():
    c4(); barbed_wire(); spikes()
    print("defences written")

if __name__ == "__main__":
    main()
