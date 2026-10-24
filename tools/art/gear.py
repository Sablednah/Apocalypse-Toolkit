"""Improvised gear sprites (16x16, held diagonally like vanilla's tools): crowbar, lead pipe, shiv, and the hunting
bow with its three pulling frames (the bow's models inherit vanilla's bow display, so it is held like one).
Run: python3 tools/art/gear.py"""
import json, math, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "apocalypse_toolkit")
NS = "apocalypse_toolkit"
OUTLINE = (24, 22, 22, 255)
# sprites drawn by hand, not by this script: the crowbar is Threadwork's (moved over 2026-10-24)
HAND_DRAWN = {"crowbar"}

def canvas():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return im, im.load()

def outline(im):
    px = im.load(); out = im.copy(); o = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3]
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[x, y] = OUTLINE
    return out

def diag(px, i0, i1, colour, lit=None, shade=None):
    """A two-pixel-wide shaft along the main diagonal, from step i0 to i1 (0 = bottom left)."""
    for i in range(i0, i1 + 1):
        x, y = 2 + i, 13 - i
        px[x, y] = (lit or colour) + (255,)
        if x + 1 < 16: px[x + 1, y] = colour + (255,)
        if y + 1 < 16 and shade: px[x, y + 1] = shade + (255,)

def crowbar():
    im, px = canvas()
    red, lit, dark = (170, 40, 34), (210, 80, 70), (110, 24, 20)
    diag(px, 1, 10, red, lit, dark)
    for x, y in [(12, 2), (13, 2), (13, 3), (14, 3), (14, 4)]: px[x, y] = (150, 154, 160, 255)  # the hook
    px[13, 4] = (110, 114, 120, 255)
    for x, y in [(1, 13), (2, 14), (1, 14)]: px[x, y] = (150, 154, 160, 255)                     # the flat end
    return outline(im)

def lead_pipe():
    im, px = canvas()
    grey, lit, dark = (104, 108, 116), (150, 154, 162), (66, 68, 74)
    diag(px, 1, 11, grey, lit, dark)
    for x, y in [(12, 1), (13, 1), (13, 2), (14, 2)]: px[x, y] = (84, 88, 96, 255)                # the elbow joint
    for x, y in [(2, 12), (3, 12), (2, 13), (3, 13)]: px[x, y] = (62, 64, 70, 255)                # tape grip
    return outline(im)

def shiv():
    im, px = canvas()
    for i in range(6, 12):                                         # the blade, a sharpened scrap
        x, y = 2 + i, 13 - i
        px[x, y] = (200, 204, 210, 255)
        if i < 11: px[x + 1, y] = (150, 154, 160, 255)
    for i in range(1, 6):                                          # a handle of wrapped cloth
        x, y = 2 + i, 13 - i
        c = (176, 150, 110) if i % 2 else (140, 116, 82)
        px[x, y] = c + (255,); px[x + 1, y] = tuple(v - 20 for v in c) + (255,)
    return outline(im)

def bow(pull):
    """pull 0 = standby, 1-3 = drawn further each frame (an arrow nocked). The wood bows toward the top left, the
    string runs tip to tip and is drawn back toward the bottom right."""
    im, px = canvas()
    wood, dark, string = (110, 70, 40), (78, 48, 26), (230, 228, 220)
    depth = 3.2 + pull * 0.5
    (x0, y0), (x1, y1) = (3, 14), (14, 3)                           # the tips: bottom left, top right
    def at(a, k):                                                   # a point a along the tips, k out to the top left
        return (x0 + (x1 - x0) * a - k * 0.7071, y0 + (y1 - y0) * a - k * 0.7071)
    for t in range(0, 41):
        a = t / 40
        x, y = at(a, math.sin(a * math.pi) * depth)
        xi, yi = round(x), round(y)
        if 0 <= xi < 16 and 0 <= yi < 16:
            px[xi, yi] = wood + (255,)
            if 0 <= xi - 1 and t % 3 == 0: px[xi - 1, yi] = dark + (255,)
    back = pull * 1.4
    for t in range(0, 41):
        a = t / 40
        x, y = at(a, -math.sin(a * math.pi) * back)
        xi, yi = round(x), round(y)
        if 0 <= xi < 16 and 0 <= yi < 16 and px[xi, yi][3] == 0: px[xi, yi] = string + (255,)
    if pull:                                                        # the arrow, nocked at the string's centre
        nx, ny = at(0.5, -back)
        for i in range(-2, 9):                                      # along the bow's facing (top left)
            x, y = round(nx - i * 0.7071), round(ny - i * 0.7071)
            if 0 <= x < 16 and 0 <= y < 16: px[x, y] = (150, 120, 80, 255)
        hx, hy = round(nx - 9 * 0.7071), round(ny - 9 * 0.7071)
        for dx, dy in ((0, 0), (-1, 0), (0, -1)):
            if 0 <= hx + dx < 16 and 0 <= hy + dy < 16: px[hx + dx, hy + dy] = (180, 184, 190, 255)
    return outline(im)

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2); f.write("\n")

def main():
    tex = os.path.join(RES, "textures", "item")
    for name, draw in (("crowbar", crowbar), ("lead_pipe", lead_pipe), ("shiv", shiv)):
        if name not in HAND_DRAWN: draw().save(os.path.join(tex, name + ".png"))
        write(os.path.join(RES, "models", "item", name + ".json"),
              {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/{name}"}})
        write(os.path.join(RES, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
    frames = ["hunting_bow", "hunting_bow_pulling_0", "hunting_bow_pulling_1", "hunting_bow_pulling_2"]
    for i, name in enumerate(frames):
        bow(i).save(os.path.join(tex, name + ".png"))
        write(os.path.join(RES, "models", "item", name + ".json"),
              {"parent": "minecraft:item/bow", "textures": {"layer0": f"{NS}:item/{name}"}})
    m = lambda n: {"type": "minecraft:model", "model": f"{NS}:item/{n}"}
    write(os.path.join(RES, "items", "hunting_bow.json"), {"model": {
        "type": "minecraft:condition", "property": "minecraft:using_item", "on_false": m("hunting_bow"),
        "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
                    "entries": [{"threshold": 0.65, "model": m("hunting_bow_pulling_1")},
                                {"threshold": 0.9, "model": m("hunting_bow_pulling_2")}],
                    "fallback": m("hunting_bow_pulling_0")}}})
    print("gear written")

if __name__ == "__main__":
    main()
