# Changelog

## 0.1.0 — unreleased

First build (2026-10-24, overnight).

- Guns: pistol, SMG, shotgun, rifle, sniper rifle, laser rifle. Hit-scan rays, magazines (shown as the item bar),
  the Reload key (R), shell-by-shell shotgun loading, recoil, sneak to steady (and to scope the sniper), headshots,
  crossbow-style hold. Bullet and laser damage types in `#minecraft:is_projectile`.
- Ammunition: pistol rounds, shotgun shells, rifle rounds, sniper rounds, laser cells.
- Throwables: grenade (bounces, 3 s fuse), molotov (fire, respecting mobGriefing).
- Medicine: bandage, medkit, adrenaline and antidote syringes (vanilla consumables).
- Sentry turret: owned, four target modes, pistol or rifle rounds, wear and iron repairs, a screen.
- Improvised gear: crowbar (fast on containers), lead pipe, shiv (LegendQuest finesse), hunting bow.
- Sprung doors, trapdoors and fence gates (1 s close).
- Loot tables: ammo cache, armoury, medical cabinet.
- Art from scratch: Blockbench-editable cuboid gun and turret models with per-gun palettes, flat icons, sprites;
  synthesised sounds. Tools in `tools/`, guide in `docs/ART.md`.
- Pipe bomb (lures monsters, then a big blast), smoke grenade (blinds, breaks targeting), chemical grenade (Wither,
  Poison, Slowness gas).
- C4 charges (stick to any face, owned) and the detonator (96 blocks, rippling; blasts chain).
- Barbed wire (drags and cuts) and spikes (hurt unless you sneak; double fall damage), with their own death messages.
- Guns reload from backpacks and other container items (NeoForge's item capability), and from Curios slots.
- Cooking: fused throwables start their fuse when you press use and fly on release with what's left; too long and
  they go off at your feet.
- From Threadwork: the crowbar's sprite, lore and stone-pickaxe-and-axe mining; the Plaster (instant 2-heart heal).
- The crowbar, lead pipe, shiv and hunting bow are enchantable.
- Self-test: `./gradlew runServer -Pselftest`, 55 checks (put Sophisticated Backpacks in run/mods to test it too).
