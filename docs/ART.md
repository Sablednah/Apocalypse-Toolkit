# Art, sound and Blockbench

Everything here was made from scratch by the scripts in `tools/` (nothing reused from Gamingbarn's Guns). They are
starting points: tweak in Blockbench, then keep the edited files (don't re-run a script over a file you've
hand-edited unless you mean to start again).

## The guns (3D models)

- `assets/apocalypse_toolkit/models/item/<gun>_3d.json`: open in Blockbench as a **Java Block/Item** model
  (File > Open Model). Each gun is a handful of cuboids along +X, barrel at the high end, y up.
- Textures: `textures/item/gun/<gun>.png`, one palette per gun (a 4x4 grid of 4x4-pixel swatches: gunmetal, steel,
  black, wood, brass, grip, laser white/red, cyan, olive...). Every face is UV-mapped to a swatch, so repainting a
  swatch recolours those parts. To paint a gun properly, re-UV in Blockbench (Box UV or per-face) and save a bigger
  texture; the model keeps working.
- **Display transforms are a first guess.** Set them in Blockbench's Display tab (first person, third person, ground,
  item frame); the GUI shows the flat icon instead (below).
- `items/<gun>.json` picks the flat icon in the GUI and the 3D model everywhere else.
- Regenerate: `python3 tools/art/guns.py` (rewrites models, palettes and icons for every gun).

## Flat icons and sprites

- Gun icons (`textures/item/<gun>.png`) are the 3D model's side view at 16x16, long guns tilted corner to corner.
  Repaint them by hand, or regenerate after changing the cuboids in `tools/art/guns.py`.
- Ammo, throwables and medical: `tools/art/items.py`. Crowbar, lead pipe, shiv, hunting bow (+ 3 pulling frames):
  `tools/art/gear.py`. All 16x16, dark outline, light from the top left.

## The sentry turret

- `models/block/sentry.json` (cuboids, facing north = barrel to -Z) on `textures/block/sentry.png` (a palette, as the
  guns). The blockstate turns it in quarter turns as it tracks a target. Its screen: `textures/gui/container/sentry.png`.
- Regenerate: `python3 tools/art/sentry.py`.

## C4, barbed wire, spikes

- `tools/art/defences.py`. The C4 charge is cuboids on a palette (`models/block/c4.json`, lying on the floor, turned
  like a button by its blockstate). Barbed wire is a cross (like a cobweb) of `textures/block/barbed_wire.png`. Spikes
  are a 9-pixel riveted plate with crossed planes of points standing on it (`models/block/spikes.json`): edit the
  points in `spikes_side.png`'s top 7 rows.
- Pipe bomb, smoke and chemical grenades and the detonator are sprites in `tools/art/items.py`.

## Sounds

- `tools/audio/synth.py` (numpy + soundfile; `../Threadwork/tools/audio/.venv/bin/python` has both) synthesises every
  sound to `sounds/gun/*.ogg`: two takes of each gunshot (pistol, smg, shotgun, rifle, sniper), the laser, dry fire,
  magazine out/in, shell in. Each gun's character is one line of numbers in `main()` (crack, thump, tail).
- `sounds.json` (from `tools/gen_data.py`) sets how far each is heard: shots 48-96 blocks, mechanical 12.
- Replacing one with a recorded or edited sound: drop an .ogg over it (mono, 44.1 kHz).

## Data

`tools/gen_data.py` writes recipes, tags, loot tables, damage types, lang and sounds.json. Edit it and re-run rather
than hand-editing those files.
