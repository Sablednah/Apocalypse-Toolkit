package com.sablednah.apocalypsetoolkit.feats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;
import com.sablednah.apocalypsetoolkit.ModRegistry;
import com.sablednah.apocalypsetoolkit.throwable.FusedThrowable;
import com.sablednah.apocalypsetoolkit.throwable.GrenadeEntity;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The Toolkit's feats: advancements in its own tab ({@code data/apocalypse_toolkit/advancement/}), the ones a vanilla
 * trigger can't see awarded from here. The gun code reports each shot's kills ({@link #shot}); the rest come off
 * deaths and item use below. Long Shot and Barbecue are plain data (player_killed_entity).
 */
public final class Feats {
    private Feats() {}

    /** Award every criterion of {@code apocalypse_toolkit:feats/<name>}. */
    public static void award(Player player, String name) {
        if (!(player instanceof ServerPlayer sp)) return;
        AdvancementHolder a = sp.level().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "feats/" + name));
        if (a == null) return;
        for (String c : a.value().criteria().keySet()) sp.getAdvancements().award(a, c);
    }

    /** One trigger pull's tally, filled in by the rays it fired. */
    public static final class Shot {
        public int kills, headshotKills, oneShotKills;
    }

    /** After a shot: what it killed, and with what. */
    public static void shot(Player player, com.sablednah.apocalypsetoolkit.gun.GunItem gun, Shot s) {
        if (s.headshotKills > 0) award(player, "headshot");
        if (gun == ModRegistry.SHOTGUN.get() && s.kills >= 3) award(player, "three_for_one");
        if (gun == ModRegistry.SNIPER_RIFLE.get() && s.oneShotKills > 0) award(player, "one_shot_one_kill");
        if (gun == ModRegistry.LASER_RIFLE.get() && s.kills >= 2) award(player, "laser_show");
    }

    /** Kills by one grenade or pipe bomb, counted as its blast lands (all in one tick). */
    private static final Map<UUID, int[]> BLASTS = new HashMap<>();
    private static final Map<UUID, Boolean> LOW_ON_MEDKIT = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            LivingEntity dead = e.getEntity();
            if (dead.level().isClientSide()) return;
            var src = e.getSource();
            if ((src.getDirectEntity() instanceof GrenadeEntity || src.getDirectEntity() instanceof FusedThrowable)
                    && src.getEntity() instanceof Player thrower) {
                int[] n = BLASTS.computeIfAbsent(src.getDirectEntity().getUUID(), k -> new int[] { 0 });
                if (++n[0] >= 3) award(thrower, "fragged");
            }
            if (src.is(ModRegistry.BARBED_WIRE_DAMAGE) || src.is(ModRegistry.SPIKES_DAMAGE))
                for (Player p : dead.level().getEntitiesOfClass(Player.class, new AABB(dead.blockPosition()).inflate(24)))
                    award(p, "hold_the_line");
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> BLASTS.clear());
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Start e) -> {
            if (e.getEntity() instanceof Player p && e.getItem().is(ModRegistry.MEDKIT.get()))
                LOW_ON_MEDKIT.put(p.getUUID(), p.getHealth() <= 4F);
        });
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Finish e) -> {
            if (e.getEntity() instanceof Player p && e.getItem().is(ModRegistry.MEDKIT.get())
                    && Boolean.TRUE.equals(LOW_ON_MEDKIT.remove(p.getUUID())))
                award(p, "patched_up");
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> { BLASTS.clear(); LOW_ON_MEDKIT.clear(); });
    }
}
