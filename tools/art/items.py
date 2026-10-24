"""Flat 16x16 sprites for the ammunition, throwables and medical kit, drawn from scratch in vanilla's style (dark
outline, light from the top left). Writes the textures and their item models/definitions.
Run: python3 tools/art/items.py"""
import json, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "apocalypse_toolkit")
NS = "apocalypse_toolkit"
OUTLINE = (24, 22, 22, 255)

def canvas():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return im, im.load()

def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): px[x, y] = c + (255,) if len(c) == 3 else c

def outline(im):
    px = im.load(); out = im.copy(); o = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3]
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                o[x, y] = OUTLINE
    return out

def cartridge(px, x, y, h, case, tip, w=2):
    """One upright round: a case of height h and a tip, top-left at (x, y)."""
    rect(px, x, y + 2, x + w - 1, y + h, case)
    rect(px, x, y + 2, x, y + h, tuple(min(255, v + 40) for v in case))        # lit edge
    rect(px, x, y, x + w - 1, y + 1, tip)
    rect(px, x, y + h, x + w - 1, y + h, tuple(max(0, v - 50) for v in case))  # rim

BRASS, COPPER, LEAD, STEEL = (206, 164, 66), (196, 112, 64), (130, 132, 140), (170, 174, 180)

def pistol_rounds():
    im, px = canvas()
    for i, (x, y) in enumerate([(3, 6), (6, 5), (9, 6), (12, 7)]): cartridge(px, x, y, 6, BRASS, LEAD)
    return outline(im)

def rifle_rounds():
    im, px = canvas()
    for x, y in [(3, 3), (7, 2), (11, 3)]:
        cartridge(px, x, y, 10, BRASS, COPPER)
        px[x, y] = px[x + 1, y] = (0, 0, 0, 0); px[x, y + 1] = COPPER + (255,)      # pointed
    return outline(im)

def sniper_round():
    im, px = canvas()
    for i in range(13):                                    # one long round, corner to corner
        x, y = 2 + i, 13 - i
        c = COPPER if i > 9 else BRASS
        px[x, y] = c + (255,); px[x, y - 1] = tuple(min(255, v + 35) for v in c) + (255,)
        if i < 10: px[x + 1, y] = tuple(max(0, v - 40) for v in c) + (255,)
    return outline(im)

def shotgun_shell():
    im, px = canvas()
    for x, y in [(4, 4), (9, 5)]:
        rect(px, x, y + 3, x + 2, y + 10, (186, 40, 36)); rect(px, x, y + 3, x, y + 10, (226, 80, 70))
        rect(px, x, y, x + 2, y + 2, (196, 40, 36)); rect(px, x, y + 8, x + 2, y + 10, BRASS)
        rect(px, x, y + 10, x + 2, y + 10, (150, 110, 40))
    return outline(im)

def laser_cell():
    im, px = canvas()
    rect(px, 5, 3, 10, 13, (200, 204, 210)); rect(px, 5, 3, 5, 13, (236, 238, 242))
    rect(px, 6, 5, 9, 11, (70, 210, 230)); rect(px, 6, 5, 6, 11, (150, 240, 250))   # the glowing core
    rect(px, 7, 1, 8, 2, (120, 124, 128)); rect(px, 5, 13, 10, 13, (140, 144, 150))
    return outline(im)

def grenade():
    im, px = canvas()
    for y in range(5, 15):
        for x in range(3, 13):
            if (x - 7.5) ** 2 / 25 + (y - 9.8) ** 2 / 27 <= 1:
                band = (y - 5) % 3 == 0 or (x - 3) % 3 == 0
                base = (78, 92, 56) if not band else (58, 70, 40)
                lit = 20 if x + y < 15 else 0
                px[x, y] = tuple(min(255, v + lit) for v in base) + (255,)
    rect(px, 6, 3, 9, 4, (120, 124, 128)); rect(px, 9, 2, 12, 2, (150, 154, 160))  # cap and lever
    px[12, 3] = (150, 154, 160, 255); px[4, 3] = px[5, 2] = (196, 164, 80, 255)   # the pin's ring
    return outline(im)

def molotov():
    im, px = canvas()
    for y in range(6, 15):
        for x in range(4, 12):
            if (x - 7.5) ** 2 / 14 + (y - 10.5) ** 2 / 22 <= 1:
                px[x, y] = ((96, 130, 70) if y > 8 else (130, 160, 96)) + (255,)      # green glass, fuel line
    rect(px, 4, 9, 11, 9, (170, 120, 50))
    rect(px, 6, 3, 9, 6, (80, 110, 60))                                              # neck
    rect(px, 7, 1, 8, 2, (228, 220, 200)); px[8, 0] = (240, 150, 40, 255); px[9, 1] = (250, 200, 60, 255)  # rag, flame
    return outline(im)

def bandage():
    im, px = canvas()
    for y in range(4, 13):
        for x in range(3, 13):
            if (x - 8) ** 2 + (y - 8.5) ** 2 <= 19:
                px[x, y] = ((236, 232, 220) if (x + y) % 3 else (214, 208, 194)) + (255,)
    rect(px, 7, 7, 9, 10, (190, 184, 170))                                           # the roll's hole
    rect(px, 11, 9, 14, 11, (236, 232, 220))                                         # a loose tail
    return outline(im)

def medkit():
    im, px = canvas()
    rect(px, 2, 5, 13, 13, (196, 40, 36)); rect(px, 2, 5, 13, 5, (226, 80, 70)); rect(px, 2, 13, 13, 13, (150, 28, 26))
    rect(px, 6, 3, 9, 4, (60, 60, 64))                                               # handle
    rect(px, 7, 7, 8, 11, (240, 240, 236)); rect(px, 5, 8, 10, 10, (240, 240, 236))  # white cross
    return outline(im)

def syringe(fluid):
    im, px = canvas()
    for i in range(10):                                   # barrel, corner to corner
        x, y = 3 + i, 12 - i
        px[x, y] = (220, 226, 230, 255); px[x + 1, y] = (190, 196, 202, 255)
        if 2 <= i <= 7: px[x, y] = fluid + (255,)
    px[2, 13] = px[1, 14] = (120, 124, 128, 255)          # plunger
    px[13, 2] = px[14, 1] = (170, 174, 180, 255)          # needle
    return outline(im)

def pipe_bomb():
    im, px = canvas()
    for i in range(9):                                    # a capped length of pipe, corner to corner
        x, y = 3 + i, 12 - i
        for d in (0, 1, 2):
            c = (110, 114, 122) if d == 1 else (150, 154, 162) if d == 0 else (76, 78, 84)
            if 0 <= y - 1 + d < 16: px[x, y - 1 + d] = c + (255,)
    for x, y in [(2, 12), (3, 13), (2, 13), (11, 2), (12, 3), (12, 2)]: px[x, y] = (60, 62, 68, 255)   # end caps
    for i in (3, 5): px[3 + i, 11 - i] = px[4 + i, 12 - i] = (60, 90, 170, 255)                         # duct tape
    rect(px, 6, 8, 8, 9, (40, 40, 44)); px[7, 8] = (220, 40, 40, 255)                                   # the timer, lit
    px[13, 1] = (200, 60, 50, 255); px[14, 1] = (200, 60, 50, 255); px[14, 0] = (60, 120, 200, 255)     # wires
    return outline(im)

def canister(body, band, top=(120, 124, 128)):
    im, px = canvas()
    rect(px, 5, 5, 10, 14, body); rect(px, 5, 5, 5, 14, tuple(min(255, v + 40) for v in body))
    rect(px, 10, 5, 10, 14, tuple(max(0, v - 30) for v in body))
    rect(px, 5, 8, 10, 9, band)
    rect(px, 6, 3, 9, 4, top); rect(px, 9, 2, 12, 2, (150, 154, 160)); px[12, 3] = (150, 154, 160, 255)  # cap and lever
    px[4, 3] = px[5, 2] = (196, 164, 80, 255)                                                            # the pin's ring
    return im, px

def smoke_grenade():
    im, px = canister((92, 96, 102), (220, 220, 214))
    for x, y in [(6, 11), (8, 12), (7, 13)]: px[x, y] = (200, 200, 196, 255)
    return outline(im)

def chemical_grenade():
    im, px = canister((150, 160, 50), (30, 30, 30))
    rect(px, 7, 11, 8, 12, (30, 30, 30)); px[6, 13] = px[9, 13] = (30, 30, 30, 255)                    # a hazard mark
    return outline(im)

def detonator():
    im, px = canvas()
    rect(px, 4, 6, 11, 14, (64, 68, 60)); rect(px, 4, 6, 11, 6, (96, 100, 90)); rect(px, 4, 14, 11, 14, (40, 42, 38))
    rect(px, 6, 8, 9, 9, (30, 32, 30)); px[7, 8] = (220, 40, 40, 255)                                   # the readout
    rect(px, 6, 11, 9, 12, (200, 40, 36)); rect(px, 6, 11, 9, 11, (236, 80, 70))                        # the button
    rect(px, 10, 1, 10, 5, (40, 40, 44)); px[10, 0] = (200, 40, 36, 255)                                # aerial
    return outline(im)

SPRITES = {
    "pistol_rounds": pistol_rounds, "rifle_rounds": rifle_rounds, "sniper_round": sniper_round,
    "shotgun_shell": shotgun_shell, "laser_cell": laser_cell, "grenade": grenade, "molotov": molotov,
    "bandage": bandage, "medkit": medkit,
    "adrenaline_syringe": lambda: syringe((230, 200, 60)), "antidote_syringe": lambda: syringe((90, 200, 120)),
    "pipe_bomb": pipe_bomb, "smoke_grenade": smoke_grenade, "chemical_grenade": chemical_grenade, "detonator": detonator,
}

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2); f.write("\n")

HAND_DRAWN = {"plaster"}   # Threadwork's plaster sprite, moved over 2026-10-24: models only

def main():
    os.makedirs(os.path.join(RES, "textures", "item"), exist_ok=True)
    for name in HAND_DRAWN:
        write(os.path.join(RES, "models", "item", name + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        write(os.path.join(RES, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
    for name, draw in SPRITES.items():
        draw().save(os.path.join(RES, "textures", "item", name + ".png"))
        write(os.path.join(RES, "models", "item", name + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        write(os.path.join(RES, "items", name + ".json"),
              {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
    print(len(SPRITES), "sprites written")

if __name__ == "__main__":
    main()
