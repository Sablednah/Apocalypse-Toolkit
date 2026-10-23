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
    private static Runnable later;

    public static void register() {
        if (!Boolean.getBoolean("apocalypse_toolkit.selftest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> run(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (started < 0 || later == null) return;
            if (e.getServer().getTickCount() - started >= 90) {
                Runnable r = later;
                later = null;
                r.run();
                finish(e.getServer());
            }
        });
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) passed++; else failed++;
        results.add((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : " (" + detail + ")"));
    }

    private static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = new BlockPos(0, 220, 0);
        for (int cx = -1; cx <= 2; cx++)                       // no player is online to keep the test ground loaded
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced(cx, cz, true);
        for (int x = -4; x <= 24; x++)
            for (int z = -4; z <= 8; z++) level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(base).inflate(40),
                e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());   // a previous run's leftovers
        FakePlayer fake = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes("ToolkitTest".getBytes()), "ToolkitTest"));

        try {
            // ---- data ----
            var bullet = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GunItem.BULLET);
            check("bullet damage is projectile damage", bullet.is(DamageTypeTags.IS_PROJECTILE), "");
            for (String r : new String[] { "pistol", "shotgun", "sniper_rifle", "pistol_rounds", "grenade", "medkit", "sentry", "sprung", "crowbar", "hunting_bow" })
                check("recipe " + r, server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, r))).isPresent(), "");
            ItemStack crowbar = new ItemStack(ModRegistry.CROWBAR.get());
            float speed = crowbar.getDestroySpeed(Blocks.CHEST.defaultBlockState());
            check("crowbar opens chests fast", speed >= 8F, "speed " + speed);
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
            later = () -> {
                var st = level.getBlockState(door);
                check("sprung door shuts itself", st.hasProperty(BlockStateProperties.OPEN) && !st.getValue(BlockStateProperties.OPEN),
                        st + ", sprung " + Sprung.isSprung(level, door) + ", time " + t0 + "->" + level.getGameTime()
                                + ", due " + due0 + "->" + Sprung.pendingAt(level, door));
                check("grenade goes off", !grenade.isAlive(), "");
            };
        } catch (Throwable t) {
            check("no exception", false, t.toString());
            finish(server);
        }
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
