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
            .damage(3.5F).pellets(8).range(20).cooldown(18).magazine(6).reload(12).perRoundReload().ammo(SHOTGUN_SHELL)
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
        DeferredItem<GunItem> it = ITEMS.registerItem(name, p -> new GunItem(p, spec), p -> p.stacksTo(1).rarity(rarity));
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

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, name));
    }

    // ---- medical ------------------------------------------------------------------------------

    public static final DeferredItem<Item> BANDAGE = item("bandage", Medical::bandage);
    public static final DeferredItem<Item> MEDKIT = item("medkit", Medical::medkit);
    public static final DeferredItem<Item> ADRENALINE = item("adrenaline_syringe", Medical::adrenaline);
    public static final DeferredItem<Item> ANTIDOTE = item("antidote_syringe", Medical::antidote);

    private static DeferredItem<Item> item(String name, java.util.function.UnaryOperator<Item.Properties> props) {
        DeferredItem<Item> it = ITEMS.registerSimpleItem(name, props);
        ORDER.add(it);
        return it;
    }

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
