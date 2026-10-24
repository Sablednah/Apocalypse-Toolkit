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
                    "hunting_bow", "pipe_bomb", "smoke_grenade", "chemical_grenade", "c4", "detonator", "barbed_wire", "spikes" })
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
            check("medkit is consumable", new ItemStack(ModRegistry.MEDKIT.get()).has(DataComponents.CONSUMABLE), "");

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

            // ---- reloading ----
            pistol.set(ModRegistry.LOADED.get(), 0);
            fake.getInventory().setItem(9, new ItemStack(ModRegistry.PISTOL_ROUNDS.get(), 20));
            ModRegistry.PISTOL.get().startReload(level, fake, pistol);
            check("reload starts", GunItem.reloading(pistol), "");
            pistol.set(ModRegistry.RELOAD_UNTIL.get(), 0L);                // time's up
            ModRegistry.PISTOL.get().inventoryTick(pistol, level, fake, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            check("reload fills the magazine", GunItem.loaded(pistol) == 12, "loaded " + GunItem.loaded(pistol));
            check("reload takes the rounds", fake.getInventory().getItem(9).getCount() == 8, "left " + fake.getInventory().getItem(9).getCount());

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
                for (int i = 0; i < SentryBlockEntity.RATE * 2 + 2; i++)
                    SentryBlockEntity.serverTick(level, turret, level.getBlockState(turret), be);
                check("sentry shoots a hostile", prey.getHealth() < b3, b3 + " -> " + prey.getHealth());
                check("sentry spends ammo", be.getItem(SentryBlockEntity.AMMO).getCount() < 10, "left " + be.getItem(SentryBlockEntity.AMMO).getCount());
                check("sentry turns to face it", level.getBlockState(turret).getValue(com.sablednah.apocalypsetoolkit.turret.SentryBlock.FACING)
                        == net.minecraft.core.Direction.EAST, "");
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
