# Changelog

## 0.1.0 — unreleased

First build (2026-10-24, overnight).

- Guns: pistol, SMG, shotgun, rifle, sniper rifle, laser rifle. Hit-scan rays, magazines (shown as the item bar),
  the Reload key (B), shell-by-shell shotgun loading, recoil, sneak to steady (and to scope the sniper), headshots,
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
- Self-test: `./gradlew runServer -Pselftest`, 28 checks.
