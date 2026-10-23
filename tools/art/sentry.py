"""The sentry turret's block model (cuboids on a palette texture, for Blockbench), its blockstate (the head in quarter
turns), item definition, loot table, recipe and mining tag, and its screen (176x166, drawn in vanilla's greys).
Run: python3 tools/art/sentry.py"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import guns  # noqa: E402  (palette and swatch helpers)
from PIL import Image

RES = os.path.join(HERE, "..", "..", "src", "main", "resources")
NS = "apocalypse_toolkit"
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data")

# facing north: the barrel points to -Z
BOXES = [
    ((3, 0, 3), (13, 1, 13), guns.DARK),         # base plate
    ((7, 1, 7), (9, 8, 9), guns.METAL),          # post
    ((4, 1, 4), (5, 3, 5), guns.DARK),           # feet
    ((11, 1, 4), (12, 3, 5), guns.DARK),
    ((4, 1, 11), (5, 3, 12), guns.DARK),
    ((11, 1, 11), (12, 3, 12), guns.DARK),
    ((4.5, 8, 5), (11.5, 13, 12), guns.OLIVE),   # body
    ((11.5, 8.5, 7), (13.5, 11.5, 11), guns.DARK),  # ammo box
    ((7.2, 9.6, 0.5), (8.8, 11.2, 5), guns.BLACK),  # barrel
    ((6.8, 9.2, 4), (9.2, 11.6, 5.2), guns.DARK),   # barrel shroud
    ((5.5, 13, 7), (10.5, 13.6, 10), guns.METAL),   # top plate
    ((6, 10.4, 4.98), (10, 12.2, 5.02), guns.RED),  # sensor strip
]

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2); f.write("\n")

def gui():
    W, H = 176, 166
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0)); px = im.load()
    PANEL, LIGHT, SHADE, OUT = (198, 198, 198, 255), (255, 255, 255, 255), (85, 85, 85, 255), (0, 0, 0, 255)
    for y in range(H):
        for x in range(W):
            px[x, y] = PANEL
    for x in range(W): px[x, 0] = px[x, H - 1] = OUT
    for y in range(H): px[0, y] = px[W - 1, y] = OUT
    for i in range(1, W - 1): px[i, 1] = px[i, 2] = LIGHT; px[i, H - 2] = px[i, H - 3] = SHADE
    for i in range(1, H - 1): px[1, i] = px[2, i] = LIGHT; px[W - 2, i] = px[W - 3, i] = SHADE
    def slot(x, y):
        for j in range(18):
            for i in range(18):
                c = (139, 139, 139, 255)
                if i == 0 or j == 0: c = (55, 55, 55, 255)
                if i == 17 or j == 17: c = (255, 255, 255, 255)
                px[x + i, y + j] = c
    slot(43, 34); slot(115, 34)
    for r in range(3):
        for c in range(9): slot(7 + c * 18, 83 + r * 18)
    for c in range(9): slot(7 + c * 18, 141)
    return im

def main():
    guns.palette(42).save(os.path.join(A, "textures", "block", "sentry.png")) if os.path.isdir(os.path.join(A, "textures", "block")) \
        else (os.makedirs(os.path.join(A, "textures", "block")), guns.palette(42).save(os.path.join(A, "textures", "block", "sentry.png")))
    elements = [{"from": list(a), "to": list(b),
                 "faces": {f: {"uv": guns.swatch_uv(s), "texture": "#sentry"} for f in ("north", "east", "south", "west", "up", "down")}}
                for a, b, s in BOXES]
    write(os.path.join(A, "models", "block", "sentry.json"),
          {"credit": "Apocalypse Toolkit, tools/art/sentry.py; tweak in Blockbench", "texture_size": [16, 16],
           "textures": {"sentry": f"{NS}:block/sentry", "particle": f"{NS}:block/sentry"}, "elements": elements,
           "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]},
                       "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                       "fixed": {"scale": [0.5, 0.5, 0.5]},
                       "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                       "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
                       "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]}}})
    write(os.path.join(A, "blockstates", "sentry.json"), {"variants": {
        f"facing={f}": {"model": f"{NS}:block/sentry", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(os.path.join(A, "items", "sentry.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:block/sentry"}})
    write(os.path.join(D, NS, "loot_table", "blocks", "sentry.json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:sentry"}],
         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(os.path.join(D, "minecraft", "tags", "block", "mineable", "pickaxe.json"), {"values": [f"{NS}:sentry"]})
    write(os.path.join(D, NS, "recipe", "sentry.json"), {"type": "minecraft:crafting_shaped", "category": "redstone",
        "pattern": ["IDI", "RCR", "I I"], "key": {"I": "minecraft:iron_ingot", "D": "minecraft:dispenser",
                                                  "R": "minecraft:redstone", "C": "minecraft:observer"},
        "result": {"id": f"{NS}:sentry", "count": 1}})
    os.makedirs(os.path.join(A, "textures", "gui", "container"), exist_ok=True)
    gui().save(os.path.join(A, "textures", "gui", "container", "sentry.png"))
    print("sentry written")

if __name__ == "__main__":
    main()
