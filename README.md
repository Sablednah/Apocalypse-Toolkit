# Apocalypse Toolkit

Survival gear for the end of the world, for NeoForge (Minecraft 26.2): guns and ammunition, throwables, field
medicine, a sentry turret, and the improvised kit in between. Simple, Minecraft-styled, made to stand alone or slot
into a pack.

## Guns

| Gun | Damage | Range | Rate | Magazine | Ammo | Notes |
|---|---|---|---|---|---|---|
| Pistol | 6 | 48 | 3.3/s | 12 | Pistol Rounds | |
| SMG | 3.5 | 32 | 10/s | 30 | Pistol Rounds | automatic |
| Shotgun | 8 x 3.5 | 20 | 1.1/s | 6 | Shotgun Shells | loads shell by shell; firing interrupts |
| Rifle | 8 | 64 | 5/s | 20 | Rifle Rounds | automatic |
| Sniper Rifle | 22 (x2 headshot) | 128 | 0.7/s | 5 | Sniper Rounds | sneak to look through the scope |
| Laser Rifle | 12 | 96 | 2.5/s | 16 | Laser Cells | a beam, rare |

- **Right-click** fires (hold for an automatic). An empty gun clicks and starts reloading if you carry its ammo.
- **Reload**: the Reload key (B by default, rebindable). Ammo comes from your inventory, then Curios slots, then any
  backpack or container item that shares its contents (Sophisticated Backpacks, shulker boxes), carried or worn.
- **Sneak** to steady your aim (half the spread).
- A hit at head height does 1.5x damage (2x for the sniper). The bar under the gun shows rounds left.
- Guns hit instantly (no bullet entity). Damage is `apocalypse_toolkit:bullet` / `:laser`, both in
  `#minecraft:is_projectile`, so Projectile Protection and stat mods treat it as a shot.

## Throwables

- **Grenade**: bounces, goes off after 3 seconds. Breaks blocks only where mobs may (the mobGriefing rule, and claim
  mods that veto explosions).
- **Molotov**: smashes and sets everything within 2.5 blocks alight; lays fire on the ground only where mobs may grief.
- **Pipe Bomb**: beeps faster and faster for 5 seconds while every monster within 24 blocks makes for it, then a
  bigger blast than a grenade. Throw it, then go the other way.
- **Smoke Grenade**: a cloud 4 blocks across for 15 seconds. Mobs in it lose whoever they were after; anything in it
  is blinded. Covers a retreat.
- **Chemical Grenade**: a lingering gas cloud of Wither, Poison and Slowness (Wither because the dead shrug off poison).

## C4

Stick a **C4 Charge** to any floor, wall or ceiling; it's yours. Your **Detonator** sets off every charge you placed
within 96 blocks, nearest first, in a quick ripple. A blast that reaches a charge (anyone's) sets it off too. Broken
by hand, a charge drops safely.

## Defences

- **Barbed Wire**: no wall to it, so anything can push in, but it drags like a cobweb and cuts whatever is caught in
  it (1.5 a time). Zombies walk straight into it.
- **Spikes**: hurt whatever walks on them (3 a time) unless it sneaks across; landing on them doubles the fall.

## Medicine

Bandage (Regeneration II, 5 s), Medkit (Instant Health II, Regeneration, cures poison, wither, hunger), Adrenaline
Syringe (Speed II and Strength for 30 s), Antidote Syringe (cures poison, wither, nausea, hunger, weakness).

## Sentry Turret

Place it and it is yours. It guards 16 blocks, shooting the nearest target it can see, one round every 3/4 second:
pistol rounds (4 damage) or rifle rounds (6). Right-click for its screen: ammo, iron ingots for repairs (each shot
wears it), and what it shoots at (off, hostiles, hostiles and other players, everything but you and your pets).

## Improvised gear

- **Crowbar**: a weapon that opens chests, barrels and crates fast (`#apocalypse_toolkit:crowbar_mineable`, plus
  anything in `#c:chests` / `#c:barrels`).
- **Lead Pipe** (slow, heavy) and **Shiv** (fast, light).
- **Hunting Bow**: arrows fly 25% faster, twice as true, and hit 40% harder.
- **Sprung doors, trapdoors and gates**: craft any of them with an iron nugget; it swings shut a second after you
  go through, waits while someone stands in it, and leaves redstone-powered ones alone.

## For packs

- Recipes are plain vanilla-style JSON; override them.
- Recipes for barbed wire and spikes use iron nuggets, so a pack can swap in its scrap.
- Tags: `#apocalypse_toolkit:guns`, `:ammo`, `:throwables`, `:medical`, `:melee`, `:sprungable`.
- Loot tables: `apocalypse_toolkit:chests/ammo_cache`, `:chests/armoury`, `:chests/medical_cabinet` (rarer things
  carry quality, so Luck finds them more often).
- LegendQuest: the shiv is `#legendquest:finesse`, every throwable `#legendquest:thrown` (optional entries).

## Building and testing

`./gradlew build` (Java 25). `./gradlew runServer -Pselftest` boots a dev server, runs 49 checks of the mechanics
(firing, reloading, shotgun loading, the sentry, sprung doors, every throwable, C4, wire and spikes, recipes, loot) and stops. Art and sound are
from `tools/`; see `docs/ART.md`.

MIT licence.
