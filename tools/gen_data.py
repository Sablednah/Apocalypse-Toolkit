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
THROWN = ["grenade", "molotov"]
MEDICAL = ["bandage", "medkit", "adrenaline_syringe", "antidote_syringe"]
ns = lambda n: f"{NS}:{n}"

# ---- damage types ---------------------------------------------------------------------------
for t in ("bullet", "laser"):
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
shapeless("bandage", ["#minecraft:wool", "minecraft:string"], 3, "misc")
shapeless("medkit", [ns("bandage"), ns("bandage"), "minecraft:glistering_melon_slice", "minecraft:leather"], 1, "misc")
shapeless("adrenaline_syringe", ["minecraft:glass_bottle", "minecraft:sugar", "minecraft:sugar", "minecraft:iron_nugget"], 1, "misc")
shapeless("antidote_syringe", ["minecraft:glass_bottle", "minecraft:honeycomb", "minecraft:iron_nugget"], 1, "misc")

# ---- lang -----------------------------------------------------------------------------------
names = {
    "pistol": "Pistol", "smg": "SMG", "shotgun": "Shotgun", "rifle": "Rifle", "sniper_rifle": "Sniper Rifle",
    "laser_rifle": "Laser Rifle", "pistol_rounds": "Pistol Rounds", "shotgun_shell": "Shotgun Shell",
    "rifle_rounds": "Rifle Rounds", "sniper_round": "Sniper Round", "laser_cell": "Laser Cell", "grenade": "Grenade",
    "molotov": "Molotov", "bandage": "Bandage", "medkit": "Medkit", "adrenaline_syringe": "Adrenaline Syringe",
    "antidote_syringe": "Antidote Syringe",
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
                "gun.shell_in": "Shell loaded"}.items():
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
}
w(f"{ASSETS}/sounds.json", {e: {"subtitle": f"subtitles.{NS}.{e}",
                                "sounds": [{"name": f"{NS}:{f}", "attenuation_distance": d} for f in files]}
                            for e, (files, d) in SOUNDS.items()})
print("data written")
