"""Icon sheet: every 16x16 item sprite on one image, to paint on in any pixel editor, then split back.

  python3 tools/art/iconsheet.py export   -> art/icons.png (real size, a 16x16 cell per icon, 2px clear gutters),
                                              art/icons.json (which cell is which file), art/icons-preview.png (8x, labelled)
  python3 tools/art/iconsheet.py import   -> writes every cell that changed back to its texture, and adds its name to
                                              art/hand_drawn.txt so the generator scripts leave it alone from then on

Keep the sheet's layout: draw inside the cells only (the gutters are ignored). Re-export after adding items."""
import json, os, sys
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
NS = os.environ.get("ICON_NS") or next(l.split("=", 1)[1].strip() for l in open(os.path.join(ROOT, "gradle.properties"))
                                       if l.startswith("mod_id"))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", NS, "textures", "item")
ART = os.path.join(ROOT, "art")
SKIP_DIRS = {"gun"}          # the guns' UV palettes are model textures, not icons
CELL, GUT, COLS = 16, 2, 12

def icons():
    out = []
    for dirpath, dirs, files in os.walk(TEX):
        dirs[:] = sorted(d for d in dirs if d not in SKIP_DIRS)
        for f in sorted(files):
            if f.endswith(".png"):
                rel = os.path.relpath(os.path.join(dirpath, f), TEX).replace(os.sep, "/")
                if Image.open(os.path.join(TEX, rel)).size == (CELL, CELL): out.append(rel)
    return out

def at(i):
    return GUT + (i % COLS) * (CELL + GUT), GUT + (i // COLS) * (CELL + GUT)

def export():
    names = icons(); rows = (len(names) + COLS - 1) // COLS
    sheet = Image.new("RGBA", (GUT + COLS * (CELL + GUT), GUT + rows * (CELL + GUT)), (0, 0, 0, 0))
    for i, n in enumerate(names): sheet.alpha_composite(Image.open(os.path.join(TEX, n)).convert("RGBA"), at(i))
    os.makedirs(ART, exist_ok=True)
    sheet.save(os.path.join(ART, "icons.png"))
    with open(os.path.join(ART, "icons.json"), "w") as f:
        json.dump({"namespace": NS, "cell": CELL, "gutter": GUT, "columns": COLS,
                   "icons": [{"file": n, "x": at(i)[0], "y": at(i)[1]} for i, n in enumerate(names)]}, f, indent=1)
    S, LBL = 8, 12                                   # the preview: 8x, a checkerboard behind, the name under each
    W, H = CELL * S + 16, CELL * S + LBL + 14
    prev = Image.new("RGB", (COLS * W, rows * H), (40, 40, 46)); d = ImageDraw.Draw(prev)
    for i, n in enumerate(names):
        cx, cy = (i % COLS) * W + 8, (i // COLS) * H + 6
        for y in range(0, CELL * S, S * 2):
            for x in range(0, CELL * S, S * 2):
                for dx, dy, c in ((0, 0, 70), (S, S, 70), (S, 0, 58), (0, S, 58)):
                    d.rectangle((cx + x + dx, cy + y + dy, cx + x + dx + S - 1, cy + y + dy + S - 1), fill=(c, c, c + 4))
        icon = Image.open(os.path.join(TEX, n)).convert("RGBA").resize((CELL * S, CELL * S), Image.NEAREST)
        prev.paste(icon, (cx, cy), icon)
        label = n[:-4][:21]
        d.text((cx + (CELL * S - len(label) * 6) // 2, cy + CELL * S + 3), label, fill=(220, 220, 210))
    prev.save(os.path.join(ART, "icons-preview.png"))
    print(f"{len(names)} icons -> art/icons.png, art/icons-preview.png")

def imp():
    meta = json.load(open(os.path.join(ART, "icons.json"))); sheet = Image.open(os.path.join(ART, "icons.png")).convert("RGBA")
    keep_path = os.path.join(ART, "hand_drawn.txt")
    keep = set(open(keep_path).read().split()) if os.path.exists(keep_path) else set()
    changed = []
    for e in meta["icons"]:
        cell = sheet.crop((e["x"], e["y"], e["x"] + CELL, e["y"] + CELL))
        dst = os.path.join(TEX, e["file"])
        if os.path.exists(dst) and list(Image.open(dst).convert("RGBA").getdata()) == list(cell.getdata()): continue
        cell.save(dst); changed.append(e["file"]); keep.add(e["file"][:-4])
    with open(keep_path, "w") as f: f.write("\n".join(sorted(keep)) + "\n")
    print(f"{len(changed)} icons written back: {', '.join(changed) or '-'}")

def hand_drawn():
    """For the generator scripts: names (without .png) that were painted by hand and must not be redrawn."""
    p = os.path.join(ART, "hand_drawn.txt")
    return set(open(p).read().split()) if os.path.exists(p) else set()

if __name__ == "__main__":
    {"export": export, "import": imp}[sys.argv[1] if len(sys.argv) > 1 else "export"]()
