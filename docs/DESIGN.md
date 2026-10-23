# Apocalypse Toolkit — design

Survival gear for the end of the world, as a standalone NeoForge mod (Minecraft 26.2 first). Started 2026-10-23 to
replace Gamingbarn's Guns (GBG) in LegendQuest: ZARP: GBG's results were good, its implementation is a datapack that
dresses crossbows up as guns with command functions (in creative, off-hand ammo places command blocks). This mod
does the same jobs with real items, entities and menus.

Sable's decisions (2026-10-23):

- **Its own mod, shipped on its own.** Not inside Threadwork. A pack configures it; ZARP's Threadwork overrides its
  recipes and wires it into ZARP's loot, quests and LegendQuest gating.
- **Art: simple and Minecraft-sympathetic**, flat 16x item sprites and blocky models, not high-detail gun models.
  Our own art; nothing of GBG's (models, textures, sounds) is reused.
- **Sounds: synthesised** (our own), one set per gun plus reload, empty click, throwables and turrets.
- **Ammo: craftable and findable.** The mod ships plain vanilla-style recipes; packs override them (ZARP does it in
  Threadwork, as it does for GBG now).
- **Client-side parts are fine** (a client mod): sentry turrets get a GUI.
- **Order: reimplement GBG's useful set first, then our own additions.**

## Phase 1 — reimplement

### Guns

| Gun | Notes |
|---|---|
| Pistol | sidearm; magazine |
| Shotgun | spread of pellets, short range; shells, loaded one at a time |
| Rifle | GBG's assault rifle, semi or burst |
| SMG | new: fast, weak, short range |
| Sniper rifle | long range, zoom, slow |
| One laser gun | the "fancy" one, replacing GBG's four lasers and five ray guns; battery ammo, rare |

GBG also had a bazooka, flamethrower, grenade launcher and minigun. **Not in phase 1**; ask Sable before adding any.

Mechanics to settle in the mod session: hit-scan (a ray, like GBG) vs projectile per gun; headshot multiplier;
armour penetration; magazine size; reload time; a reload key (GBG used swap-offhand; a dedicated, rebindable key is
better); recoil (client camera kick); zoom (sniper); durability or not. Ammo is real items; a gun holds its loaded
rounds in a data component.

### Ammo

Pistol rounds, shotgun shells, rifle rounds, SMG rounds (or shared with pistol), sniper rounds, laser battery.
Vanilla-style recipes (gunpowder, iron/copper nuggets, paper). Loot tables of its own so packs can drop them in.

### Throwables

Grenade and molotov (Sable's list); GBG's pipe bomb, smoke grenade and chemical grenade are candidates. Thrown
entities; molotov leaves fire; explosions respect the mob-griefing rule and NeoForge's explosion events (so claim
mods can protect bases).

### Medical

Keep, reimplemented: GBG had simple and advanced bandages, a medkit, a syringe and effect syringes (absorption,
antidote, health boost, regeneration, resistance, swiftness), and a minor-infection cure. Use-over-time items
(bandaging takes a moment). ZARP's Threadwork Plaster may fold in or stay in Threadwork; decide in the mod session.

### Sentry turrets

Reworked from GBG's three turrets (auto, sentry, perdition). Good for engineering classes. A placed block entity:
ammo in, targets out. **Right-click opens a GUI**: repair (with scrap or iron), ammo slot, and target settings
(hostiles, players not in your faction, animals, nobody). Durability that wears with shots and hits.

## Phase 2 — our own additions

- **Crowbar** moves here from Threadwork (`threadwork:crowbar`; keep a migration so existing ZARP crowbars convert),
  and it **mines barrels, crates and chests faster** (a block tag, so packs can add their crates).
- **Sprung gates, doors and trapdoors.** Sprung gates move here from Threadwork (`gate/SprungGates`: any fence gate
  crafted with scrap carries a `sprung` component and swings shut after use, waiting while someone is in the gateway,
  leaving redstone-powered ones alone; positions kept in a chunk attachment). Add **sprung doors and sprung
  trapdoors**. Close delay **1 second** (Threadwork's is 2 s, 40 ticks; Sable finds that too long). ZARP's camp gates
  are placed sprung by Threadwork's CampBuilder, which would then call this mod.
- **Hunting bow**: an improved bow, a survival weapon.
- **Melee**: lead pipe, shiv (The Last of Us style), more improvised weapons later.

## LegendQuest attack stats (LegendQuest 654201e, 2026-10-23)

LegendQuest rolls attacks on STR in melee and DEX with projectiles, and the better of the two for
`#legendquest:finesse` items (melee) and `#legendquest:thrown` entity types (projectiles). It decides by the damage's
direct entity: the player is melee, a Projectile is ranged. So:

- **Shiv (and any knife-like weapon): in `#legendquest:finesse`** (an optional tag entry, so the mod runs without LQ).
  Crowbar and lead pipe stay STR.
- **Grenade, molotov and other thrown entities: in `#legendquest:thrown`.**
- **Hunting bow**: arrows are projectiles, DEX already.
- **Guns must read as ranged.** A hit-scan shot has no projectile entity, so its direct entity would be the shooter
  and LQ would roll STR. Either fire real projectile entities, or (preferred, asked of the LegendQuest session on
  2026-10-23) use a damage type in `#minecraft:is_projectile` and have LQ treat that tag as ranged too.

## What ZARP has to rewire at the switch (Threadwork, ZARP repo)

Found 2026-10-23; all of this names GBG today:

- `Threadwork/src/main/resources/datapacks/guns-tuning/`: recipe overrides for GBG's standard guns, throwables and
  turrets, the disabled laser and ray guns, the vault ammo and armoury loot (`cityworld:chests/vault_*_extra`), and a
  Threadwork recipe. Becomes overrides of this mod's recipes.
- `Threadwork/.../neoforge/guns/` (`GunPermissions`, `ItemGate`): LegendQuest class, race and feat gating of every GBG
  item. Gate this mod's items instead, ideally by item tag (this mod should ship tags like `#apocalypse_toolkit:guns`,
  `#.../sentries`, `#.../medical`).
- Loot: `zarp-world` CityWorld hooks (5 tables), `data/threadwork/loot_table/chests/consignment` (2),
  the CrawlSpace dungeon ammo sub-table (`zarp-crawlspace/.../mods/ammo.json`).
- The Winchester's loaded shotgun frame (`zarp-world/.../schematic_fitout/winchester.json`, a GBG crossbow SNBT).
- `ZARP/docs/MODS.md`: GBG listed as installed; the gating notes.
- Thenard sells no guns today; ammo would suit him.

## Conventions (as Sable's other mods)

- Repo `../Apocalypse-Toolkit`, its own Claude session. Build: `./gradlew build` with Java 25
  (`JAVA_HOME=/home/sable/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2`); check the jar has `META-INF/neoforge.mods.toml`.
- `src/main/templates/META-INF/neoforge.mods.toml` is the template (expanded from `gradle.properties`).
- The LegendQuest ZARP instance has `.sablecraft-no-deploy`: never copy a jar there without Sable's say-so.
