"""Writes the mod's data and lang: damage types (+ #minecraft:is_projectile), tags (its own, LegendQuest's thrown),
vanilla-style recipes (packs override these; ZARP's Threadwork does), en_us.json and sounds.json.
Run: python3 tools/gen_data.py"""
import json, os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "apocalypse_toolkit"
DATA = os.path.join(ROOT, "data")
ASSETS = os.path.join(ROOT, "assets", NS)

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write("\n")

GUNS = ["pistol", "smg", "shotgun", "rifle", "sniper_rifle", "laser_rifle"]
AMMO = ["pistol_rounds", "shotgun_shell", "rifle_rounds", "sniper_round", "laser_cell"]
THROWN = ["grenade", "molotov", "pipe_bomb", "smoke_grenade", "chemical_grenade"]
MEDICAL = ["plaster", "bandage", "medkit", "adrenaline_syringe", "antidote_syringe"]
ns = lambda n: f"{NS}:{n}"

# ---- damage types ---------------------------------------------------------------------------
for t in ("bullet", "laser"):
    w(f"{DATA}/{NS}/damage_type/{t}.json", {"exhaustion": 0.1, "message_id": f"{NS}.{t}",
                                            "scaling": "when_caused_by_living_non_player"})
for t in ("barbed_wire", "spikes"):                 # like a cactus: no attacker, armour helps
    w(f"{DATA}/{NS}/damage_type/{t}.json", {"exhaustion": 0.1, "message_id": f"{NS}.{t}",
                                            "scaling": "when_caused_by_living_non_player"})
w(f"{DATA}/minecraft/tags/damage_type/is_projectile.json", {"replace": False, "values": [ns("bullet"), ns("laser")]})

# ---- tags -----------------------------------------------------------------------------------
for name, items in (("guns", GUNS), ("ammo", AMMO), ("throwables", THROWN), ("medical", MEDICAL)):
    w(f"{DATA}/{NS}/tags/item/{name}.json", {"values": [ns(i) for i in items]})
# LegendQuest: thrown projectiles take the better of STR and DEX (an entity type tag; optional, so no LQ is fine)
w(f"{DATA}/legendquest/tags/entity_type/thrown.json",
  {"replace": False, "values": [{"id": ns(e), "required": False} for e in THROWN]})

# ---- recipes (vanilla-style; a pack overrides them) ------------------------------------------
def shaped(name, pattern, key, count=1, category="equipment"):
    w(f"{DATA}/{NS}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category,
                                          "pattern": pattern, "key": key, "result": {"id": ns(name), "count": count}})

def shapeless(name, ingredients, count=1, category="equipment"):
    w(f"{DATA}/{NS}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
                                          "ingredients": ingredients, "result": {"id": ns(name), "count": count}})

I, T, W = "minecraft:iron_ingot", "minecraft:tripwire_hook", "#minecraft:planks"
shaped("pistol", ["II", "TW"], {"I": I, "T": T, "W": W})
shaped("smg", ["III", "ITW"], {"I": I, "T": T, "W": W})
shaped("shotgun", ["III", "TWW"], {"I": I, "T": T, "W": W})
shaped("rifle", ["III", "BTW"], {"I": I, "B": "minecraft:iron_block", "T": T, "W": W})
shaped("sniper_rifle", ["SII", "BTW"], {"S": "minecraft:spyglass", "I": I, "B": "minecraft:iron_block", "T": T, "W": W})
shaped("laser_rifle", ["DIE", "RTR"], {"D": "minecraft:diamond", "I": I, "E": "minecraft:ender_eye", "R": "minecraft:redstone_block", "T": T})
shapeless("pistol_rounds", ["minecraft:copper_ingot", "minecraft:gunpowder"], 8)
shapeless("rifle_rounds", ["minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:gunpowder"], 8)
shapeless("shotgun_shell", ["minecraft:paper", "minecraft:gunpowder", "minecraft:iron_nugget"], 6)
shapeless("sniper_round", [I, "minecraft:copper_ingot", "minecraft:gunpowder", "minecraft:gunpowder"], 4)
shapeless("laser_cell", ["minecraft:redstone", "minecraft:redstone", "minecraft:glowstone_dust", "minecraft:copper_ingot"], 2)
shaped("grenade", [" N ", "IGI", " I "], {"N": "minecraft:iron_nugget", "I": I, "G": "minecraft:gunpowder"}, 2)
shapeless("molotov", ["minecraft:glass_bottle", "minecraft:string", "#minecraft:coals"])
shapeless("pipe_bomb", [I, "minecraft:gunpowder", "minecraft:gunpowder", "minecraft:redstone", "minecraft:string"])
shapeless("smoke_grenade", [I, "minecraft:gunpowder", "#minecraft:coals", "#minecraft:coals"], 2)
shapeless("chemical_grenade", [I, "minecraft:gunpowder", "minecraft:fermented_spider_eye", "minecraft:poisonous_potato"], 2)
shapeless("c4", ["minecraft:tnt", "minecraft:clay_ball", "minecraft:redstone"], 2)
shapeless("detonator", [I, "minecraft:copper_ingot", "minecraft:redstone_torch", "minecraft:stone_button"])
# defences: a use for scrap (a pack can swap the nuggets for its scrap)
shaped("barbed_wire", ["N N", " N ", "N N"], {"N": "minecraft:iron_nugget"}, 3, "building")
shaped("spikes", ["N N", "III"], {"N": "minecraft:iron_nugget", "I": I}, 2, "building")
shapeless("plaster", ["minecraft:paper", "minecraft:string"], 2, "misc")
shapeless("bandage", ["#minecraft:wool", "minecraft:string"], 3, "misc")
shapeless("medkit", [ns("bandage"), ns("bandage"), "minecraft:glistering_melon_slice", "minecraft:leather"], 1, "misc")
shapeless("adrenaline_syringe", ["minecraft:glass_bottle", "minecraft:sugar", "minecraft:sugar", "minecraft:iron_nugget"], 1, "misc")
shapeless("antidote_syringe", ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:iron_nugget"], 1, "misc")

# sprung doors, trapdoors and gates: any of them plus an iron nugget (a pack swaps the material)
w(f"{DATA}/{NS}/tags/item/sprungable.json", {"values": ["#minecraft:doors", "#minecraft:trapdoors", "#minecraft:fence_gates"]})
w(f"{DATA}/{NS}/recipe/sprung.json", {"type": ns("sprung"), "item": f"#{NS}:sprungable", "material": "minecraft:iron_nugget"})

# improvised gear
w(f"{DATA}/{NS}/tags/block/crowbar_mineable.json", {"values": [
    "minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel",
    {"id": "#c:chests", "required": False}, {"id": "#c:barrels", "required": False}]})
w(f"{DATA}/legendquest/tags/item/finesse.json", {"replace": False, "values": [{"id": ns("shiv"), "required": False}]})
w(f"{DATA}/{NS}/tags/item/melee.json", {"values": [ns("crowbar"), ns("lead_pipe"), ns("shiv")]})
# vanilla enchanting is by tag. Crowbar: sword and pickaxe/shovel enchantments. Shiv: a sword's (and Sharpness counts a
# level higher on it, in code). Lead pipe: a mace's. Guns: a crossbow's (Quick Charge, Multishot, Piercing; read in
# GunItem). Hunting bow: a bow's.
MELEE_SWORD = ["crowbar", "shiv"]
ENCHANTABLE = {
    "sharp_weapon": MELEE_SWORD, "melee_weapon": MELEE_SWORD, "sweeping": MELEE_SWORD,
    "weapon": MELEE_SWORD + ["lead_pipe"], "fire_aspect": MELEE_SWORD + ["lead_pipe"], "mace": ["lead_pipe"],
    "mining": ["crowbar"], "mining_loot": ["crowbar"],
    "crossbow": GUNS, "bow": ["hunting_bow"],
    "durability": MELEE_SWORD + ["lead_pipe", "hunting_bow"],
    "vanishing": MELEE_SWORD + ["lead_pipe", "hunting_bow"] + GUNS,
}
for tag, items in ENCHANTABLE.items():
    w(f"{DATA}/minecraft/tags/item/enchantable/{tag}.json", {"replace": False, "values": [ns(i) for i in items]})
shaped("crowbar", ["  I", " I ", "I  "], {"I": I})
shaped("lead_pipe", [" I", "I "], {"I": "minecraft:iron_block"})
shaped("shiv", ["N", "S"], {"N": "minecraft:iron_nugget", "S": "minecraft:string"})
shapeless("hunting_bow", ["minecraft:bow", "minecraft:string", "minecraft:leather", "minecraft:iron_nugget"])

# loot tables a pack can drop in (ZARP's Threadwork points CityWorld's hooks at these); quality: luck finds the rarer
def entry(item, weight, lo=1, hi=1, quality=0):
    e = {"type": "minecraft:item", "name": ns(item), "weight": weight}
    if quality: e["quality"] = quality
    if (lo, hi) != (1, 1):
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e

def table(name, rolls, entries, empty=0):
    pool = {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}
    if empty: pool["entries"] = entries + [{"type": "minecraft:empty", "weight": empty, "quality": -2}]
    w(f"{DATA}/{NS}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [pool]})

table("ammo_cache", (2, 4), [entry("pistol_rounds", 10, 4, 16), entry("shotgun_shell", 8, 3, 10),
      entry("rifle_rounds", 6, 4, 12, 1), entry("sniper_round", 2, 1, 4, 2), entry("laser_cell", 1, 1, 2, 3)], 4)
table("armoury", (2, 4), [entry("pistol", 6, quality=1), entry("shotgun", 4, quality=1), entry("smg", 3, quality=2),
      entry("rifle", 2, quality=2), entry("sniper_rifle", 1, quality=3), entry("grenade", 5, 1, 3), entry("molotov", 4, 1, 3),
      entry("pipe_bomb", 3, 1, 2, 1), entry("smoke_grenade", 3, 1, 2), entry("chemical_grenade", 2, 1, 2, 1), entry("c4", 1, 1, 2, 3),
      entry("pistol_rounds", 8, 4, 16), entry("rifle_rounds", 5, 4, 12), entry("sentry", 1, quality=3)], 6)
table("medical_cabinet", (1, 3), [entry("plaster", 10, 1, 4), entry("bandage", 8, 1, 4), entry("medkit", 3, quality=2),
      entry("adrenaline_syringe", 3, quality=1), entry("antidote_syringe", 4)], 5)

# ---- lang -----------------------------------------------------------------------------------
names = {
    "pistol": "Pistol", "smg": "SMG", "shotgun": "Shotgun", "rifle": "Rifle", "sniper_rifle": "Sniper Rifle",
    "laser_rifle": "Laser Rifle", "pistol_rounds": "Pistol Rounds", "shotgun_shell": "Shotgun Shell",
    "rifle_rounds": "Rifle Rounds", "sniper_round": "Sniper Round", "laser_cell": "Laser Cell", "grenade": "Grenade",
    "molotov": "Molotov", "bandage": "Bandage", "medkit": "Medkit", "adrenaline_syringe": "Adrenaline Syringe",
    "antidote_syringe": "Antidote Syringe", "crowbar": "Crowbar", "lead_pipe": "Lead Pipe", "shiv": "Shiv",
    "hunting_bow": "Hunting Bow", "plaster": "Plaster", "pipe_bomb": "Pipe Bomb", "smoke_grenade": "Smoke Grenade",
    "chemical_grenade": "Chemical Grenade", "detonator": "Detonator",
}
lang = {f"item.{NS}.{k}": v for k, v in names.items()}
lang.update({f"entity.{NS}.{e}": names[e] for e in THROWN})
lang.update({
    "itemGroup.apocalypse_toolkit": "Apocalypse Toolkit",
    "key.categories.apocalypse_toolkit.toolkit": "Apocalypse Toolkit",
    "key.category.apocalypse_toolkit.toolkit": "Apocalypse Toolkit",
    "key.apocalypse_toolkit.reload": "Reload",
    "message.apocalypse_toolkit.no_ammo": "No %s to load.",
    "tooltip.apocalypse_toolkit.ammo": "Ammo: %s/%s (%s)",
    "tooltip.apocalypse_toolkit.stats": "Damage %s, range %s, %s shots a second",
    "tooltip.apocalypse_toolkit.zoom": "Sneak to aim through the scope",
    "tooltip.apocalypse_toolkit.shiv_sharp": "Sharpness counts one level higher",
    "tooltip.apocalypse_toolkit.sprung": "Sprung: swings shut behind you",
    "block.apocalypse_toolkit.c4": "C4 Charge", "block.apocalypse_toolkit.barbed_wire": "Barbed Wire",
    "block.apocalypse_toolkit.spikes": "Spikes",
    "item.apocalypse_toolkit.plaster.lore": "Sticks to most things. Heals 2 hearts.",
    "message.apocalypse_toolkit.not_hurt": "You're not hurt.",
    "item.apocalypse_toolkit.crowbar.lore": "Theoretical physicist not included.",
    "message.apocalypse_toolkit.fuse": "Fuse: %s s (let go to throw)",
    "message.apocalypse_toolkit.fuse_hand": "Held it too long!",
    "message.apocalypse_toolkit.detonator.none": "No charges of yours within %s blocks.",
    "message.apocalypse_toolkit.detonator.armed": "%s charge(s) going off!",
    f"death.attack.{NS}.barbed_wire": "%1$s got tangled in barbed wire",
    f"death.attack.{NS}.barbed_wire.player": "%1$s got tangled in barbed wire whilst trying to escape %2$s",
    f"death.attack.{NS}.spikes": "%1$s was impaled on spikes",
    f"death.attack.{NS}.spikes.player": "%1$s was impaled on spikes whilst trying to escape %2$s",
    "block.apocalypse_toolkit.sentry": "Sentry Turret", "sentry.apocalypse_toolkit.title": "%s's Sentry",
    "sentry.apocalypse_toolkit.wear": "Condition %s/%s", "sentry.apocalypse_toolkit.ammo": "Ammo",
    "sentry.apocalypse_toolkit.condition": "Condition", "sentry.apocalypse_toolkit.repair": "Iron", "sentry.apocalypse_toolkit.mode.off": "Off",
    "sentry.apocalypse_toolkit.mode.hostiles": "Hostiles", "sentry.apocalypse_toolkit.mode.hostiles_and_players": "+ Players",
    "sentry.apocalypse_toolkit.mode.everything": "Everything",
    f"death.attack.{NS}.bullet": "%1$s was shot by %2$s",
    f"death.attack.{NS}.bullet.item": "%1$s was shot by %2$s using %3$s",
    f"death.attack.{NS}.bullet.player": "%1$s was shot whilst fighting %2$s",
    f"death.attack.{NS}.laser": "%1$s was burned through by %2$s",
    f"death.attack.{NS}.laser.item": "%1$s was burned through by %2$s using %3$s",
    f"death.attack.{NS}.laser.player": "%1$s was burned through whilst fighting %2$s",
})
for s, text in {"gun.pistol": "Pistol fires", "gun.smg": "SMG fires", "gun.shotgun": "Shotgun fires",
                "gun.rifle": "Rifle fires", "gun.sniper": "Sniper rifle fires", "gun.laser": "Laser fires",
                "gun.empty": "Gun clicks empty", "gun.reload_start": "Magazine out", "gun.reload_end": "Magazine in",
                "gun.shell_in": "Shell loaded", "pipe_bomb.beep": "Pipe bomb beeps"}.items():
    lang[f"subtitles.{NS}.{s}"] = text
w(f"{ASSETS}/lang/en_us.json", dict(sorted(lang.items())))

# ---- sounds.json ----------------------------------------------------------------------------
# event -> (files under sounds/, attenuation distance): gunshots carry; two takes of each shot
SOUNDS = {
    "gun.pistol": (["gun/pistol1", "gun/pistol2"], 48), "gun.smg": (["gun/smg1", "gun/smg2"], 48),
    "gun.shotgun": (["gun/shotgun1", "gun/shotgun2"], 64), "gun.rifle": (["gun/rifle1", "gun/rifle2"], 64),
    "gun.sniper": (["gun/sniper1", "gun/sniper2"], 96), "gun.laser": (["gun/laser1", "gun/laser2"], 32),
    "gun.empty": (["gun/empty"], 12), "gun.reload_start": (["gun/reload_start"], 12),
    "gun.reload_end": (["gun/reload_end"], 12), "gun.shell_in": (["gun/shell_in"], 12),
    "pipe_bomb.beep": (["gun/beep"], 24),
}
w(f"{ASSETS}/sounds.json", {e: {"subtitle": f"subtitles.{NS}.{e}",
                                "sounds": [{"name": f"{NS}:{f}", "attenuation_distance": d} for f in files]}
                            for e, (files, d) in SOUNDS.items()})
print("data written")
