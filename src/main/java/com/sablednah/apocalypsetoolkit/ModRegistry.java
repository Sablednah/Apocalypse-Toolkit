package com.sablednah.apocalypsetoolkit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.sablednah.apocalypsetoolkit.gun.GunItem;
import com.sablednah.apocalypsetoolkit.gun.GunSpec;
import com.sablednah.apocalypsetoolkit.medical.Medical;
import com.sablednah.apocalypsetoolkit.throwable.GrenadeEntity;
import com.sablednah.apocalypsetoolkit.throwable.MolotovEntity;
import com.sablednah.apocalypsetoolkit.throwable.ThrowableItem;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Everything the mod registers, in one place. */
public final class ModRegistry {
    private ModRegistry() {}

    private static final String ID = ApocalypseToolkit.MOD_ID;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);

    /** Every item in tab order. */
    private static final List<DeferredItem<? extends Item>> ORDER = new ArrayList<>();

    // ---- data components ----------------------------------------------------------------------

    /** Rounds in a gun's magazine. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LOADED =
            COMPONENTS.registerComponentType("loaded", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** While a gun reloads: the game time the current reload step finishes (absent when not reloading). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RELOAD_UNTIL =
            COMPONENTS.registerComponentType("reload_until", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    // ---- sounds -------------------------------------------------------------------------------

    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_SHOT = sound("gun.pistol");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMG_SHOT = sound("gun.smg");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_SHOT = sound("gun.shotgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_SHOT = sound("gun.rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNIPER_SHOT = sound("gun.sniper");
    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_SHOT = sound("gun.laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRY_FIRE = sound("gun.empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_START = sound("gun.reload_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_END = sound("gun.reload_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_IN = sound("gun.shell_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEEP = sound("pipe_bomb.beep");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ID, name)));
    }

    // ---- ammunition ---------------------------------------------------------------------------

    public static final DeferredItem<Item> PISTOL_ROUNDS = item("pistol_rounds", p -> p);
    public static final DeferredItem<Item> SHOTGUN_SHELL = item("shotgun_shell", p -> p);
    public static final DeferredItem<Item> RIFLE_ROUNDS = item("rifle_rounds", p -> p);
    public static final DeferredItem<Item> SNIPER_ROUND = item("sniper_round", p -> p);
    public static final DeferredItem<Item> LASER_CELL = item("laser_cell", p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));

    // ---- guns ---------------------------------------------------------------------------------

    public static final DeferredItem<GunItem> PISTOL = gun("pistol", GunSpec.builder()
            .damage(6F).range(48).cooldown(6).magazine(12).reload(30).ammo(PISTOL_ROUNDS).spread(1.5F).recoil(2.5F)
            .sound(PISTOL_SHOT).build(), Rarity.COMMON);
    public static final DeferredItem<GunItem> SMG = gun("smg", GunSpec.builder()
            .damage(3.5F).range(32).cooldown(2).magazine(30).reload(40).ammo(PISTOL_ROUNDS).spread(4F).recoil(1.2F)
            .automatic().sound(SMG_SHOT).build(), Rarity.UNCOMMON);
    public static final DeferredItem<GunItem> SHOTGUN = gun("shotgun", GunSpec.builder()
            .damage(3.5F).pellets(8).range(16).cooldown(18).magazine(6).reload(12).perRoundReload().ammo(SHOTGUN_SHELL)
            .spread(9F).recoil(7F).sound(SHOTGUN_SHOT).build(), Rarity.COMMON);
    public static final DeferredItem<GunItem> RIFLE = gun("rifle", GunSpec.builder()
            .damage(8F).range(64).cooldown(4).magazine(20).reload(45).ammo(RIFLE_ROUNDS).spread(1F).recoil(2F)
            .automatic().sound(RIFLE_SHOT).build(), Rarity.UNCOMMON);
    public static final DeferredItem<GunItem> SNIPER_RIFLE = gun("sniper_rifle", GunSpec.builder()
            .damage(22F).headshot(2F).range(128).cooldown(30).magazine(5).reload(60).ammo(SNIPER_ROUND).spread(0.3F)
            .recoil(9F).zoom(0.25F).sound(SNIPER_SHOT).build(), Rarity.RARE);
    public static final DeferredItem<GunItem> LASER_RIFLE = gun("laser_rifle", GunSpec.builder()
            .damage(12F).range(96).cooldown(8).magazine(16).reload(50).ammo(LASER_CELL).spread(0F).recoil(0.5F)
            .laser().sound(LASER_SHOT).build(), Rarity.EPIC);

    private static DeferredItem<GunItem> gun(String name, GunSpec spec, Rarity rarity) {
        DeferredItem<GunItem> it = ITEMS.registerItem(name, p -> new GunItem(p, spec), p -> p.stacksTo(1).rarity(rarity).enchantable(10));
        ORDER.add(it);
        return it;
    }

    // ---- throwables ---------------------------------------------------------------------------

    public static final DeferredHolder<EntityType<?>, EntityType<GrenadeEntity>> GRENADE_ENTITY =
            ENTITIES.register("grenade", () -> EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(key("grenade")));
    public static final DeferredHolder<EntityType<?>, EntityType<MolotovEntity>> MOLOTOV_ENTITY =
            ENTITIES.register("molotov", () -> EntityType.Builder.<MolotovEntity>of(MolotovEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(key("molotov")));

    public static final DeferredItem<ThrowableItem> GRENADE = ITEMS.registerItem("grenade",
            p -> new ThrowableItem(p, ThrowableItem.Kind.GRENADE), p -> p.stacksTo(8));
    public static final DeferredItem<ThrowableItem> MOLOTOV = ITEMS.registerItem("molotov",
            p -> new ThrowableItem(p, ThrowableItem.Kind.MOLOTOV), p -> p.stacksTo(8));
    static { ORDER.add(GRENADE); ORDER.add(MOLOTOV); }

    public static final DeferredHolder<EntityType<?>, EntityType<com.sablednah.apocalypsetoolkit.throwable.PipeBombEntity>> PIPE_BOMB_ENTITY =
            thrown("pipe_bomb", com.sablednah.apocalypsetoolkit.throwable.PipeBombEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.sablednah.apocalypsetoolkit.throwable.SmokeGrenadeEntity>> SMOKE_GRENADE_ENTITY =
            thrown("smoke_grenade", com.sablednah.apocalypsetoolkit.throwable.SmokeGrenadeEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.sablednah.apocalypsetoolkit.throwable.ChemicalGrenadeEntity>> CHEMICAL_GRENADE_ENTITY =
            thrown("chemical_grenade", com.sablednah.apocalypsetoolkit.throwable.ChemicalGrenadeEntity::new);

    public static final DeferredItem<ThrowableItem> PIPE_BOMB = throwable("pipe_bomb", ThrowableItem.Kind.PIPE_BOMB);
    public static final DeferredItem<ThrowableItem> SMOKE_GRENADE = throwable("smoke_grenade", ThrowableItem.Kind.SMOKE);
    public static final DeferredItem<ThrowableItem> CHEMICAL_GRENADE = throwable("chemical_grenade", ThrowableItem.Kind.CHEMICAL);

    private static <T extends net.minecraft.world.entity.Entity> DeferredHolder<EntityType<?>, EntityType<T>> thrown(String name, EntityType.EntityFactory<T> factory) {
        return ENTITIES.register(name, () -> EntityType.Builder.<T>of(factory, MobCategory.MISC)
                .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(key(name)));
    }

    private static DeferredItem<ThrowableItem> throwable(String name, ThrowableItem.Kind kind) {
        DeferredItem<ThrowableItem> it = ITEMS.registerItem(name, p -> new ThrowableItem(p, kind), p -> p.stacksTo(8));
        ORDER.add(it);
        return it;
    }

    // ---- C4 -----------------------------------------------------------------------------------

    public static final net.neoforged.neoforge.registries.DeferredBlock<com.sablednah.apocalypsetoolkit.explosive.C4Block> C4 =
            BLOCKS.registerBlock("c4", com.sablednah.apocalypsetoolkit.explosive.C4Block::new,
                    p -> p.mapColor(net.minecraft.world.level.material.MapColor.SAND).strength(0.5F).noCollision()
                            .sound(net.minecraft.world.level.block.SoundType.WOOL).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> C4_ITEM = ITEMS.registerSimpleBlockItem("c4", C4,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final Supplier<net.minecraft.world.level.block.entity.BlockEntityType<com.sablednah.apocalypsetoolkit.explosive.C4BlockEntity>> C4_BE =
            BLOCK_ENTITIES.register("c4", () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                    com.sablednah.apocalypsetoolkit.explosive.C4BlockEntity::new, C4.get()));
    public static final DeferredItem<com.sablednah.apocalypsetoolkit.explosive.DetonatorItem> DETONATOR = ITEMS.registerItem("detonator",
            com.sablednah.apocalypsetoolkit.explosive.DetonatorItem::new, p -> p.stacksTo(1));
    static { ORDER.add(C4_ITEM); ORDER.add(DETONATOR); }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, name));
    }

    // ---- medical ------------------------------------------------------------------------------

    public static final DeferredItem<com.sablednah.apocalypsetoolkit.medical.PlasterItem> PLASTER = ITEMS.registerItem("plaster",
            com.sablednah.apocalypsetoolkit.medical.PlasterItem::new, p -> p.stacksTo(16).component(net.minecraft.core.component.DataComponents.LORE,
                    lore("item.apocalypse_toolkit.plaster.lore")));
    static { ORDER.add(PLASTER); }
    public static final DeferredItem<Item> BANDAGE = item("bandage", Medical::bandage);
    public static final DeferredItem<Item> MEDKIT = item("medkit", Medical::medkit);
    public static final DeferredItem<Item> ADRENALINE = item("adrenaline_syringe", Medical::adrenaline);
    public static final DeferredItem<Item> ANTIDOTE = item("antidote_syringe", Medical::antidote);

    private static net.minecraft.world.item.component.ItemLore lore(String key) {
        return new net.minecraft.world.item.component.ItemLore(java.util.List.of(Component.translatable(key)
                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)));
    }

    private static DeferredItem<Item> item(String name, java.util.function.UnaryOperator<Item.Properties> props) {
        DeferredItem<Item> it = ITEMS.registerSimpleItem(name, props);
        ORDER.add(it);
        return it;
    }

    // ---- improvised gear ----------------------------------------------------------------------

    /** What the crowbar opens fast: chests, barrels and anything tagged as one (a pack adds its crates). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> CROWBAR_MINEABLE =
            net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ID, "crowbar_mineable"));
    private static final net.minecraft.tags.TagKey<Item> IRON_REPAIR =
            net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/iron"));
    public static final net.minecraft.world.item.ToolMaterial SCRAP_IRON = new net.minecraft.world.item.ToolMaterial(
            net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 9.0F, 2.0F, 10, IRON_REPAIR);

    /**
     * The crowbar: hits like an iron sword (6, 1.6 a second), opens containers fast, and mines like a stone pickaxe and
     * axe together (Threadwork's crowbar, moved over). Not in the sword/axe/pickaxe tags, so LegendQuest lets every
     * class swing it; enchantable through the enchantable tags instead.
     */
    public static final DeferredItem<Item> CROWBAR = item("crowbar", p -> {
        var blocks = net.minecraft.core.registries.BuiltInRegistries.acquireBootstrapRegistrationLookup(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        return p.sword(SCRAP_IRON, 3.0F, -2.4F)
                .component(net.minecraft.core.component.DataComponents.TOOL, new net.minecraft.world.item.component.Tool(java.util.List.of(
                        net.minecraft.world.item.component.Tool.Rule.deniesDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.INCORRECT_FOR_STONE_TOOL)),
                        net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(CROWBAR_MINEABLE), 9.0F),
                        net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), 4.0F),
                        net.minecraft.world.item.component.Tool.Rule.minesAndDrops(blocks.getOrThrow(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE), 4.0F)),
                        1.0F, 1, false))
                .component(net.minecraft.core.component.DataComponents.LORE, lore("item.apocalypse_toolkit.crowbar.lore"));
    });
    public static final DeferredItem<Item> LEAD_PIPE = item("lead_pipe", p -> p.sword(SCRAP_IRON, 2.5F, -2.9F).durability(300));
    public static final DeferredItem<Item> SHIV = item("shiv", p -> p.sword(SCRAP_IRON, -0.5F, -1.6F).durability(120)
            .component(net.minecraft.core.component.DataComponents.LORE, lore("tooltip.apocalypse_toolkit.shiv_sharp")));
    public static final DeferredItem<com.sablednah.apocalypsetoolkit.gear.HuntingBowItem> HUNTING_BOW = ITEMS.registerItem("hunting_bow",
            com.sablednah.apocalypsetoolkit.gear.HuntingBowItem::new, p -> p.durability(500).enchantable(1));
    static { ORDER.add(HUNTING_BOW); }

    // ---- defences ------------------------------------------------------------------------------

    public static final ResourceKey<net.minecraft.world.damagesource.DamageType> BARBED_WIRE_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(ID, "barbed_wire"));
    public static final ResourceKey<net.minecraft.world.damagesource.DamageType> SPIKES_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(ID, "spikes"));

    public static final net.neoforged.neoforge.registries.DeferredBlock<com.sablednah.apocalypsetoolkit.defence.BarbedWireBlock> BARBED_WIRE =
            BLOCKS.registerBlock("barbed_wire", com.sablednah.apocalypsetoolkit.defence.BarbedWireBlock::new,
                    p -> p.mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(2.0F, 3F).requiresCorrectToolForDrops()
                            .noCollision().noOcclusion().sound(net.minecraft.world.level.block.SoundType.CHAIN));
    public static final net.neoforged.neoforge.registries.DeferredBlock<com.sablednah.apocalypsetoolkit.defence.SpikeBlock> SPIKES =
            BLOCKS.registerBlock("spikes", com.sablednah.apocalypsetoolkit.defence.SpikeBlock::new,
                    p -> p.mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(3.0F, 6F).requiresCorrectToolForDrops()
                            .noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BARBED_WIRE_ITEM = ITEMS.registerSimpleBlockItem("barbed_wire", BARBED_WIRE);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SPIKES_ITEM = ITEMS.registerSimpleBlockItem("spikes", SPIKES);
    static { ORDER.add(BARBED_WIRE_ITEM); ORDER.add(SPIKES_ITEM); }

    // ---- the sentry turret --------------------------------------------------------------------

    public static final net.neoforged.neoforge.registries.DeferredBlock<com.sablednah.apocalypsetoolkit.turret.SentryBlock> SENTRY =
            BLOCKS.registerBlock("sentry", com.sablednah.apocalypsetoolkit.turret.SentryBlock::new,
                    p -> p.mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(3.5F, 6F)
                            .requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SENTRY_ITEM = ITEMS.registerSimpleBlockItem("sentry", SENTRY,
            p -> p.rarity(Rarity.UNCOMMON));
    static { ORDER.add(SENTRY_ITEM); }
    public static final Supplier<net.minecraft.world.level.block.entity.BlockEntityType<com.sablednah.apocalypsetoolkit.turret.SentryBlockEntity>> SENTRY_BE =
            BLOCK_ENTITIES.register("sentry", () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                    com.sablednah.apocalypsetoolkit.turret.SentryBlockEntity::new, SENTRY.get()));
    public static final Supplier<net.minecraft.world.inventory.MenuType<com.sablednah.apocalypsetoolkit.turret.SentryMenu>> SENTRY_MENU =
            MENUS.register("sentry", () -> new net.minecraft.world.inventory.MenuType<>(
                    com.sablednah.apocalypsetoolkit.turret.SentryMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // ---- the tab ------------------------------------------------------------------------------

    static {
        TABS.register("toolkit", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.apocalypse_toolkit"))
                .icon(() -> new ItemStack(PISTOL.get()))
                .displayItems((params, out) -> {
                    for (DeferredItem<? extends Item> it : ORDER) {
                        ItemStack stack = new ItemStack(it.get());
                        if (it.get() instanceof GunItem gun) stack.set(LOADED.get(), gun.spec().magazine());
                        out.accept(stack);
                    }
                })
                .build());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
    }

    /** For callers that only need a supplier. */
    public static Supplier<Item> ammo(DeferredItem<Item> it) {
        return it;
    }
}
