package com.sablednah.apocalypsetoolkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.sablednah.apocalypsetoolkit.gun.GunItem;
import com.sablednah.apocalypsetoolkit.sprung.Sprung;
import com.sablednah.apocalypsetoolkit.throwable.GrenadeEntity;
import com.sablednah.apocalypsetoolkit.turret.SentryBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A self-test of the mechanics a dedicated server can see, run with {@code ./gradlew runServer -Pselftest}: a fake
 * player fires, reloads and loads shells; a sentry shoots; a sprung door closes itself; a grenade goes off; plus the
 * data (projectile tag, recipes, the crowbar's speed, the medical components). Built on a stone platform high above
 * spawn so nothing in the world gets in the way. Prints PASS/FAIL per check, a total, then stops the server.
 * Rendering, sound and keys need a real client and are not covered.
 */
public final class SelfTest {
    private SelfTest() {}

    private static final List<String> results = new ArrayList<>();
    private static int passed, failed;
    private static long started = -1;
    /** Checks that need time: run that many ticks after the start, then the test ends after the last. */
    private static final java.util.TreeMap<Integer, List<Runnable>> later = new java.util.TreeMap<>();

    private static void at(int tick, Runnable r) {
        later.computeIfAbsent(tick, k -> new ArrayList<>()).add(r);
    }

    public static void register() {
        if (!Boolean.getBoolean("apocalypse_toolkit.selftest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> run(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (started < 0 || later.isEmpty()) return;
            long t = e.getServer().getTickCount() - started;
            while (!later.isEmpty() && later.firstKey() <= t) later.pollFirstEntry().getValue().forEach(Runnable::run);
            if (later.isEmpty()) finish(e.getServer());
        });
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) passed++; else failed++;
        results.add((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : " (" + detail + ")"));
    }

    private static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = new BlockPos(0, 220, 0);
        for (int cx = -1; cx <= 6; cx++)                       // no player is online to keep the test ground loaded
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced(cx, cz, true);
        for (int x = -4; x <= 96; x++)
            for (int z = -4; z <= 8; z++) level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(base).inflate(110, 40, 40),
                e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());   // a previous run's leftovers
        FakePlayer fake = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes("ToolkitTest".getBytes()), "ToolkitTest"));

        try {
            // ---- data ----
            var bullet = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GunItem.BULLET);
            check("bullet damage is projectile damage", bullet.is(DamageTypeTags.IS_PROJECTILE), "");
            for (String r : new String[] { "pistol", "shotgun", "sniper_rifle", "pistol_rounds", "grenade", "medkit", "sentry", "sprung", "crowbar",
                    "hunting_bow", "flamethrower", "fuel_canister", "pipe_bomb", "smoke_grenade", "chemical_grenade", "c4", "detonator", "barbed_wire", "spikes", "plaster" })
                check("recipe " + r, server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, r))).isPresent(), "");
            ItemStack crowbar = new ItemStack(ModRegistry.CROWBAR.get());
            float speed = crowbar.getDestroySpeed(Blocks.CHEST.defaultBlockState());
            check("crowbar opens chests fast", speed >= 8F, "speed " + speed);
            for (String t : new String[] { "ammo_cache", "armoury", "medical_cabinet" }) {
                var key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "chests/" + t));
                var lt = server.reloadableRegistries().getLootTable(key);
                int items = 0;
                for (int i = 0; i < 20; i++)
                    items += lt.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(base))
                            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST)).size();
                check("loot table " + t + " rolls items", lt != net.minecraft.world.level.storage.loot.LootTable.EMPTY && items > 0, items + " stacks in 20 rolls");
            }
            check("crowbar mines stone like a stone pickaxe", crowbar.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 4F
                    && crowbar.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()) && !crowbar.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),
                    "speed " + crowbar.getDestroySpeed(Blocks.STONE.defaultBlockState()));
            fake.setHealth(10F);
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.PLASTER.get(), 2));
            ModRegistry.PLASTER.get().use(level, fake, InteractionHand.MAIN_HAND);
            check("plaster heals two hearts", fake.getHealth() == 14F && fake.getMainHandItem().getCount() == 1, "health " + fake.getHealth());
            fake.setHealth(fake.getMaxHealth());
            check("medkit is consumable", new ItemStack(ModRegistry.MEDKIT.get()).has(DataComponents.CONSUMABLE), "");

            // ---- enchanting ----
            var ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ItemStack shiv = new ItemStack(ModRegistry.SHIV.get());
            shiv.enchant(ench.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 5);
            int sharp = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    ench.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), shiv);
            check("shiv: Sharpness V counts as VI", sharp == 6, "level " + sharp);
            java.util.function.BiPredicate<net.minecraft.world.item.Item, String> in = (it, tag) -> new ItemStack(it).is(
                    net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("enchantable/" + tag)));
            check("enchanting tags", in.test(ModRegistry.CROWBAR.get(), "mining") && in.test(ModRegistry.CROWBAR.get(), "sharp_weapon")
                    && in.test(ModRegistry.SHIV.get(), "sweeping") && in.test(ModRegistry.LEAD_PIPE.get(), "mace")
                    && !in.test(ModRegistry.LEAD_PIPE.get(), "sharp_weapon") && in.test(ModRegistry.RIFLE.get(), "crossbow")
                    && in.test(ModRegistry.HUNTING_BOW.get(), "bow"), "");

            // ---- sprung: the recipe takes the material tag ----
            var sprungRecipe = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "sprung")));
            boolean crafts = sprungRecipe.isPresent() && sprungRecipe.get().value() instanceof net.minecraft.world.item.crafting.CraftingRecipe cr
                    && cr instanceof com.sablednah.apocalypsetoolkit.sprung.SprungRecipe sr
                    && sr.matches(net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                            new ItemStack(net.minecraft.world.item.Items.OAK_FENCE_GATE), new ItemStack(net.minecraft.world.item.Items.IRON_NUGGET))), level);
            check("sprung recipe: a gate and the material", crafts, "");
            check("sprung material is a tag", new ItemStack(net.minecraft.world.item.Items.IRON_NUGGET).is(com.sablednah.apocalypsetoolkit.sprung.Sprung.MATERIAL), "");

            // ---- firing ----
            Monster target = husk(level, base.offset(6, 0, 0));
            fake.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, -90F, 0F);   // facing east, at the husk
            ItemStack pistol = new ItemStack(ModRegistry.PISTOL.get());
            pistol.set(ModRegistry.LOADED.get(), 12);
            fake.setItemInHand(InteractionHand.MAIN_HAND, pistol);
            float before = target.getHealth();
            ModRegistry.PISTOL.get().use(level, fake, InteractionHand.MAIN_HAND);
            check("pistol shot hits", target.getHealth() < before, before + " -> " + target.getHealth());
            check("pistol spends a round", GunItem.loaded(fake.getMainHandItem()) == 11, "loaded " + GunItem.loaded(fake.getMainHandItem()));

            // Piercing: one shot through two husks in a row
            Monster near = husk(level, base.offset(3, 0, 0)), far = husk(level, base.offset(5, 0, 0));
            target.discard();
            float n0 = near.getHealth(), f0 = far.getHealth();
            ItemStack piercer = new ItemStack(ModRegistry.PISTOL.get());
            piercer.set(ModRegistry.LOADED.get(), 12);
            piercer.enchant(ench.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PIERCING), 1);
            fake.setItemInHand(InteractionHand.MAIN_HAND, piercer);
            fake.getCooldowns().removeCooldown(Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "pistol"));
            ModRegistry.PISTOL.get().use(level, fake, InteractionHand.MAIN_HAND);
            check("Piercing goes through to the second", near.getHealth() < n0 && far.getHealth() < f0, near.getHealth() + ", " + far.getHealth());
            near.discard(); far.discard();
            // Quick Charge III: a reload takes 40% of the time
            ItemStack quick = new ItemStack(ModRegistry.PISTOL.get());
            quick.set(ModRegistry.LOADED.get(), 0);
            quick.enchant(ench.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.QUICK_CHARGE), 3);
            fake.setItemInHand(InteractionHand.MAIN_HAND, quick);
            fake.getInventory().setItem(9, new ItemStack(ModRegistry.PISTOL_ROUNDS.get(), 20));
            ModRegistry.PISTOL.get().startReload(level, fake, quick);
            long took = quick.getOrDefault(ModRegistry.RELOAD_UNTIL.get(), 0L) - level.getGameTime();
            check("Quick Charge shortens a reload", took == Math.round(ModRegistry.PISTOL.get().spec().reload() * 0.4F), took + " ticks");
            fake.getInventory().clearContent();
            fake.setItemInHand(InteractionHand.MAIN_HAND, pistol);

            // ---- the flamethrower: reach, fire, fuel, and flame skill ----
            fake.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, -90F, 0F);
            Monster close = husk(level, base.offset(4, 0, 0)), far8 = husk(level, base.offset(8, 0, 1));
            far8.snapTo(base.getX() + 8.5, base.getY(), base.getZ() + 0.5, 90F, 0F);
            ItemStack flamer = new ItemStack(ModRegistry.FLAMETHROWER.get());
            flamer.set(ModRegistry.LOADED.get(), 100);
            fake.setItemInHand(InteractionHand.MAIN_HAND, flamer);
            float fc0 = close.getHealth(), ff0 = far8.getHealth();
            for (int i = 0; i < 20; i++) com.sablednah.apocalypsetoolkit.gun.Flames.burstForTest(level, fake, flamer, ModRegistry.FLAMETHROWER.get());
            check("flamethrower burns what's in front", close.getHealth() < fc0 && close.isOnFire(), fc0 + " -> " + close.getHealth() + ", on fire " + close.isOnFire());
            check("flamethrower's reach is short", far8.getHealth() == ff0, "far " + far8.getHealth());
            int plainUse = 100 - GunItem.loaded(flamer);
            check("flamethrower burns fuel", plainUse == 20, "used " + plainUse);
            var attr = fake.getAttribute(ModRegistry.FLAME_SKILL);
            attr.setBaseValue(1.0);
            flamer.set(ModRegistry.LOADED.get(), 100);
            for (int i = 0; i < 200; i++) com.sablednah.apocalypsetoolkit.gun.Flames.burstForTest(level, fake, flamer, ModRegistry.FLAMETHROWER.get());
            int skilledUse = 100 - GunItem.loaded(flamer);
            check("flame skill: fuel lasts about twice as long", skilledUse > 70 && skilledUse < 130, "used " + skilledUse + " in 200 bursts");
            check("flame skill: reach goes further", far8.getHealth() < ff0, "far " + ff0 + " -> " + far8.getHealth());
            attr.setBaseValue(0.0);
            close.discard(); far8.discard();
            flamer.set(ModRegistry.LOADED.get(), 7);
            fake.getInventory().setItem(9, new ItemStack(ModRegistry.FUEL_CANISTER.get(), 3));
            ModRegistry.FLAMETHROWER.get().startReload(level, fake, flamer);
            flamer.set(ModRegistry.RELOAD_UNTIL.get(), 0L);
            ModRegistry.FLAMETHROWER.get().inventoryTick(flamer, level, fake, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            check("one canister fills the tank", GunItem.loaded(flamer) == 100 && fake.getInventory().getItem(9).getCount() == 2,
                    "fuel " + GunItem.loaded(flamer) + ", canisters " + fake.getInventory().getItem(9).getCount());
            fake.getInventory().clearContent();
            fake.setItemInHand(InteractionHand.MAIN_HAND, pistol);

            // ---- reloading ----
            pistol.set(ModRegistry.LOADED.get(), 0);
            fake.getInventory().setItem(9, new ItemStack(ModRegistry.PISTOL_ROUNDS.get(), 20));
            ModRegistry.PISTOL.get().startReload(level, fake, pistol);
            check("reload starts", GunItem.reloading(pistol), "");
            pistol.set(ModRegistry.RELOAD_UNTIL.get(), 0L);                // time's up
            ModRegistry.PISTOL.get().inventoryTick(pistol, level, fake, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            check("reload fills the magazine", GunItem.loaded(pistol) == 12, "loaded " + GunItem.loaded(pistol));
            check("reload takes the rounds", fake.getInventory().getItem(9).getCount() == 8, "left " + fake.getInventory().getItem(9).getCount());

            // ---- reloading from a container item (a backpack, if one is installed; a shulker box if it shares) ----
            for (String id : new String[] { "sophisticatedbackpacks:backpack", "minecraft:shulker_box" }) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.parse(id));
                if (item.isEmpty()) { results.add("SKIP reload from " + id + " (not installed)"); continue; }
                fake.getInventory().clearContent();
                ItemStack box = new ItemStack(item.get());
                if (id.startsWith("sophisticated")) {           // a backpack makes its storage when first opened; do that
                    try {
                        Object w = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper")
                                .getMethod("fromStack", ItemStack.class).invoke(null, box);
                        w.getClass().getMethod("getInventoryHandler").invoke(w);
                    } catch (ReflectiveOperationException e) { results.add("note: couldn't open the backpack (" + e + ")"); }
                }
                fake.getInventory().setItem(20, box);
                var h = net.neoforged.neoforge.transfer.access.ItemAccess.forPlayerSlot(fake, 20)
                        .getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.ITEM);
                if (h == null) { results.add("SKIP reload from " + id + " (shares no contents)"); continue; }
                int put;
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    put = h.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(ModRegistry.PISTOL_ROUNDS.get()), 20, tx);
                    tx.commit();
                }
                ItemStack gun = new ItemStack(ModRegistry.PISTOL.get());
                gun.set(ModRegistry.LOADED.get(), 0);
                fake.setItemInHand(InteractionHand.MAIN_HAND, gun);
                ModRegistry.PISTOL.get().startReload(level, fake, gun);
                gun.set(ModRegistry.RELOAD_UNTIL.get(), 0L);
                ModRegistry.PISTOL.get().inventoryTick(gun, level, fake, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                int left = com.sablednah.apocalypsetoolkit.gun.Ammo.count(fake, ModRegistry.PISTOL_ROUNDS.get());
                check("reload from " + id, put == 20 && GunItem.loaded(gun) == 12 && left == 8, "put " + put + ", loaded " + GunItem.loaded(gun) + ", left " + left);
            }
            fake.getInventory().clearContent();
            var slots = com.sablednah.apocalypsetoolkit.gun.Ammo.Curios.slots(fake);
            var bp = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.parse("sophisticatedbackpacks:backpack"));
            if (slots.isEmpty() || bp.isEmpty()) results.add("SKIP reload from a backpack worn in Curios (" + slots.size() + " Curios slots)");
            else {
                ItemStack pack = new ItemStack(bp.get());
                try {
                    Object w = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper")
                            .getMethod("fromStack", ItemStack.class).invoke(null, pack);
                    Object inv = w.getClass().getMethod("getInventoryHandler").invoke(w);
                } catch (ReflectiveOperationException e) { results.add("note: couldn't open the backpack (" + e + ")"); }
                var h = net.neoforged.neoforge.transfer.access.ItemAccess.forStack(pack).getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.ITEM);
                int put = 0;
                if (h != null) try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    put = h.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(ModRegistry.PISTOL_ROUNDS.get()), 20, tx);
                    tx.commit();
                }
                slots.get(0).set(pack);
                ItemStack gun = new ItemStack(ModRegistry.PISTOL.get());
                gun.set(ModRegistry.LOADED.get(), 0);
                fake.setItemInHand(InteractionHand.MAIN_HAND, gun);
                ModRegistry.PISTOL.get().startReload(level, fake, gun);
                gun.set(ModRegistry.RELOAD_UNTIL.get(), 0L);
                ModRegistry.PISTOL.get().inventoryTick(gun, level, fake, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                int left = com.sablednah.apocalypsetoolkit.gun.Ammo.count(fake, ModRegistry.PISTOL_ROUNDS.get());
                check("reload from a backpack worn in Curios", put == 20 && GunItem.loaded(gun) == 12 && left == 8,
                        "put " + put + ", loaded " + GunItem.loaded(gun) + ", left " + left + ", " + slots.size() + " slots");
                slots.get(0).set(ItemStack.EMPTY);
            }

            ItemStack shotgun = new ItemStack(ModRegistry.SHOTGUN.get());
            fake.setItemInHand(InteractionHand.MAIN_HAND, shotgun);
            fake.getInventory().setItem(10, new ItemStack(ModRegistry.SHOTGUN_SHELL.get(), 3));
            ModRegistry.SHOTGUN.get().startReload(level, fake, shotgun);
            shotgun.set(ModRegistry.RELOAD_UNTIL.get(), 0L);
            ModRegistry.SHOTGUN.get().inventoryTick(shotgun, level, fake, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            check("shotgun loads one shell per step", GunItem.loaded(shotgun) == 1 && GunItem.reloading(shotgun), "loaded " + GunItem.loaded(shotgun));
            Monster second = husk(level, base.offset(4, 0, 0));
            float b2 = second.getHealth();
            shotgun.remove(ModRegistry.RELOAD_UNTIL.get());
            ModRegistry.SHOTGUN.get().use(level, fake, InteractionHand.MAIN_HAND);
            check("shotgun blast hits with pellets", b2 - second.getHealth() > ModRegistry.SHOTGUN.get().spec().damage(), b2 + " -> " + second.getHealth());

            // ---- the sentry ----
            BlockPos turret = base.offset(12, 0, 4);
            level.setBlockAndUpdate(turret, ModRegistry.SENTRY.get().defaultBlockState());
            Monster prey = husk(level, turret.offset(5, 0, 0));
            float b3 = prey.getHealth();
            if (level.getBlockEntity(turret) instanceof SentryBlockEntity be) {
                be.setItem(SentryBlockEntity.AMMO, new ItemStack(ModRegistry.RIFLE_ROUNDS.get(), 10));
                for (int i = 0; i < SentryBlockEntity.RATE * 3; i++)
                    SentryBlockEntity.serverTick(level, turret, level.getBlockState(turret), be);
                check("sentry shoots a hostile", prey.getHealth() < b3, b3 + " -> " + prey.getHealth());
                check("sentry spends ammo", be.getItem(SentryBlockEntity.AMMO).getCount() < 10, "left " + be.getItem(SentryBlockEntity.AMMO).getCount());
                check("sentry turns to face it", Math.abs(net.minecraft.util.Mth.wrapDegrees(be.yaw() - (-90F))) <= SentryBlockEntity.ON_TARGET,
                        "yaw " + be.yaw() + ", aim " + be.aim());
                // pulls ammo from a chest underneath
                be.setItem(SentryBlockEntity.AMMO, ItemStack.EMPTY);
                level.setBlockAndUpdate(turret.below(), Blocks.CHEST.defaultBlockState());
                if (level.getBlockEntity(turret.below()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)
                    chest.setItem(3, new ItemStack(ModRegistry.PISTOL_ROUNDS.get(), 40));
                for (int i = 0; i < 10; i++) SentryBlockEntity.serverTick(level, turret, level.getBlockState(turret), be);
                check("sentry pulls ammo from a chest below", be.getItem(SentryBlockEntity.AMMO).is(ModRegistry.PISTOL_ROUNDS.get()),
                        be.getItem(SentryBlockEntity.AMMO).toString());
            } else {
                check("sentry block entity", false, "missing");
            }

            // ---- a sprung door, and a grenade (both need time) ----
            BlockPos door = base.offset(20, 0, 2);
            var lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(BlockStateProperties.OPEN, true);
            level.setBlock(door, lower, 3);
            level.setBlock(door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
            Sprung.mark(level, door, true);
            Sprung.scheduleClose(level, door);
            GrenadeEntity grenade = new GrenadeEntity(level, fake, new ItemStack(ModRegistry.GRENADE.get()));
            grenade.setPos(base.getX() + 8.5, base.getY() + 0.2, base.getZ() + 6.5);
            grenade.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(grenade);
            started = server.getTickCount();
            final long t0 = level.getGameTime(), due0 = Sprung.pendingAt(level, door);
            at(90, () -> {
                var st = level.getBlockState(door);
                check("sprung door shuts itself", st.hasProperty(BlockStateProperties.OPEN) && !st.getValue(BlockStateProperties.OPEN),
                        st + ", sprung " + Sprung.isSprung(level, door) + ", time " + t0 + "->" + level.getGameTime()
                                + ", due " + due0 + "->" + Sprung.pendingAt(level, door));
                check("grenade goes off", !grenade.isAlive(), "");
            });

            // ---- cooking a grenade: thrown after 2 s of its 3, and held too long ----
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.GRENADE.get(), 2));
            fake.snapTo(base.getX() + 40.5, base.getY(), base.getZ() + 6.5, 0F, 0F);
            int dur = ModRegistry.GRENADE.get().getUseDuration(fake.getMainHandItem(), fake);
            ModRegistry.GRENADE.get().releaseUsing(fake.getMainHandItem(), level, fake, dur - 40);
            var cooked = level.getEntitiesOfClass(GrenadeEntity.class, new net.minecraft.world.phys.AABB(fake.blockPosition()).inflate(4));
            check("a cooked grenade flies with the fuse left", cooked.size() == 1 && cooked.get(0).tickCount == 40,
                    cooked.isEmpty() ? "none" : "fuse at " + cooked.get(0).tickCount);
            Monster beside = husk(level, base.offset(44, 0, 8));
            ModRegistry.GRENADE.get().onUseTick(level, fake, fake.getMainHandItem(), dur - 60);
            var inHand = level.getEntitiesOfClass(GrenadeEntity.class, new net.minecraft.world.phys.AABB(fake.blockPosition()).inflate(4),
                    g -> g.tickCount == 60);
            check("held too long, it drops at your feet spent", inHand.size() == 1 && inHand.get(0).getDeltaMovement().lengthSqr() < 0.01, inHand.size() + " found");
            check("grenades used up", fake.getMainHandItem().isEmpty(), fake.getMainHandItem().toString());
            fake.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, -90F, 0F);

            // ---- smoke and gas ----
            Monster blinded = husk(level, base.offset(27, 0, 6));
            blinded.setTarget(fake);
            throwAt(level, new com.sablednah.apocalypsetoolkit.throwable.SmokeGrenadeEntity(level, fake, new ItemStack(ModRegistry.SMOKE_GRENADE.get())), base.offset(26, 0, 6));
            Monster gassed = husk(level, base.offset(26, 0, -2));
            float g0 = gassed.getHealth();
            throwAt(level, new com.sablednah.apocalypsetoolkit.throwable.ChemicalGrenadeEntity(level, fake, new ItemStack(ModRegistry.CHEMICAL_GRENADE.get())), base.offset(26, 0, -2));
            at(90, () -> {
                check("smoke blinds", blinded.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "");
                check("smoke loses a mob its target", blinded.getTarget() == null, "");
                check("gas cloud hangs there", !level.getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class,
                        new net.minecraft.world.phys.AABB(base.offset(26, 0, -2)).inflate(3)).isEmpty(), "");
                check("gas hurts the dead (wither)", gassed.getHealth() < g0, g0 + " -> " + gassed.getHealth());
            });

            // ---- barbed wire and spikes ----
            BlockPos wire = base.offset(32, 0, 2);
            level.setBlockAndUpdate(wire, ModRegistry.BARBED_WIRE.get().defaultBlockState());
            Monster tangled = husk(level, wire);
            float w0 = tangled.getHealth();
            BlockPos spike = base.offset(35, -1, 2);
            level.setBlockAndUpdate(spike, ModRegistry.SPIKES.get().defaultBlockState());
            Monster impaled = husk(level, spike.above());
            impaled.setNoAi(false);                              // stepOn wants a mob that moves, as a live one does
            float s0 = impaled.getHealth();
            at(60, () -> {
                check("barbed wire cuts", tangled.getHealth() < w0, w0 + " -> " + tangled.getHealth());
                check("spikes hurt", impaled.getHealth() < s0, s0 + " -> " + impaled.getHealth());
            });

            // ---- C4: two of ours, one of someone else's next to them (it should go too, in the chain) ----
            BlockPos c1 = base.offset(52, 0, 0), c2 = base.offset(54, 0, 0), c3 = base.offset(56, 0, 0);
            for (BlockPos c : new BlockPos[] { c1, c2, c3 }) level.setBlockAndUpdate(c, ModRegistry.C4.get().defaultBlockState());
            ((com.sablednah.apocalypsetoolkit.explosive.C4BlockEntity) level.getBlockEntity(c1)).setOwner(fake.getUUID());
            ((com.sablednah.apocalypsetoolkit.explosive.C4BlockEntity) level.getBlockEntity(c2)).setOwner(fake.getUUID());
            ((com.sablednah.apocalypsetoolkit.explosive.C4BlockEntity) level.getBlockEntity(c3)).setOwner(UUID.randomUUID());
            var mine = com.sablednah.apocalypsetoolkit.explosive.DetonatorItem.charges(level, fake);
            check("detonator finds only your charges", mine.size() == 2 && mine.contains(c1) && mine.contains(c2), mine.toString());
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.DETONATOR.get()));
            ModRegistry.DETONATOR.get().use(level, fake, InteractionHand.MAIN_HAND);
            check("detonator arms them", com.sablednah.apocalypsetoolkit.explosive.Blasts.queued(level, c1)
                    && com.sablednah.apocalypsetoolkit.explosive.Blasts.queued(level, c2), "");
            at(40, () -> {
                check("C4 goes off", !level.getBlockState(c1).is(ModRegistry.C4.get()) && !level.getBlockState(c2).is(ModRegistry.C4.get()), "");
                check("a blast sets off the charge beside it", !level.getBlockState(c3).is(ModRegistry.C4.get()), "");
            });

            // ---- the pipe bomb, with a husk free to come to it ----
            Monster lured = husk(level, base.offset(88, 0, 2));
            lured.setNoAi(false);
            var bomb = new com.sablednah.apocalypsetoolkit.throwable.PipeBombEntity(level, fake, new ItemStack(ModRegistry.PIPE_BOMB.get()));
            throwAt(level, bomb, base.offset(72, 0, 2));
            at(95, () -> check("pipe bomb draws the dead in", lured.distanceTo(bomb) < 12, "now " + lured.distanceTo(bomb)));
            at(115, () -> check("pipe bomb goes off", !bomb.isAlive(), ""));
        } catch (Throwable t) {
            check("no exception", false, t.toString());
            finish(server);
        }
    }

    private static void throwAt(ServerLevel level, net.minecraft.world.entity.Entity thrown, BlockPos at) {
        thrown.setPos(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5);
        thrown.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(thrown);
    }

    private static Monster husk(ServerLevel level, BlockPos at) {
        Monster m = net.minecraft.world.entity.EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90F, 0F);
        m.setNoAi(true);
        level.addFreshEntity(m);
        return m;
    }

    private static void finish(MinecraftServer server) {
        results.forEach(r -> ApocalypseToolkit.LOGGER.info("[selftest] {}", r));
        ApocalypseToolkit.LOGGER.info("[selftest] {} passed, {} failed", passed, failed);
        server.halt(false);
    }
}
