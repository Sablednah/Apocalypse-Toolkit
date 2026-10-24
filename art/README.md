# Art for tweaking

## Icons: `icons.png`

Every item icon on one sheet, at real size (16x16 cells, 2px clear gutters). `icons-preview.png` is the same at 8x
with names, to find things. To tweak:

1. Paint on `icons.png` in any pixel editor (Aseprite, Blockbench's 2D paint, GIMP...). Stay inside the cells; keep
   the layout.
2. `python3 tools/art/iconsheet.py import` writes each changed cell back to its texture and lists it in
   `hand_drawn.txt`, so the generator scripts in `tools/art/` never redraw it.
3. Rebuild. After adding items, `python3 tools/art/iconsheet.py export` makes a fresh sheet (it reads the textures, so
   your edits carry over).

## 3D models: Blockbench

These are plain Java models; open them in Blockbench with **File > Open Model** straight from the repo, and **Save**
writes them back in place (textures are found through the `assets/` folders).

| Model | File (under `src/main/resources/assets/apocalypse_toolkit/models/`) | Texture |
|---|---|---|
| Pistol, SMG, Shotgun, Rifle, Sniper Rifle, Laser Rifle (in hand) | `item/<gun>_3d.json` | `textures/item/gun/<gun>.png`, a 16x16 palette |
| Sentry: base (stays put) | `block/sentry_base.json` | `textures/block/sentry.png` |
| Sentry: head (turns; barrel to north, -Z; turns about the block's centre) | `block/sentry_head.json` | same |
| C4 charge (lying on the floor) | `block/c4.json` | `textures/block/c4.png` |
| Spikes | `block/spikes.json` | `textures/block/spikes_*.png` |

- **Display tab**: the guns' first and third person transforms are first guesses; set them there. The inventory shows
  the flat icon, not the 3D model.
- The palettes are 4x4 swatches; every face is mapped to one. For proper painting, re-UV (Box UV is fine) and save a
  bigger texture; the model keeps working.
- The sentry item in your hand draws `sentry_base` and `sentry_head` together, so edit those two.
- Re-running `tools/art/guns.py`, `sentry.py` or `defences.py` regenerates those models from scratch: once you've
  edited one in Blockbench, don't.
