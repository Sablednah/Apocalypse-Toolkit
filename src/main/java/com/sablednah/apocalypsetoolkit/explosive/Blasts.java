package com.sablednah.apocalypsetoolkit.explosive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * C4 charges waiting to go off: a detonator queues its owner's charges a few ticks apart (they ripple rather than
 * all going at once), and a blast that catches a charge queues it too (a chain). Not saved: a charge queued at the
 * moment the server stops just stays where it is.
 */
public final class Blasts {
    private Blasts() {}

    /** How hard a charge goes off (TNT is 4): it breaks blocks only where mobs may. */
    public static final float POWER = 4.0F;

    private record Pending(BlockPos pos, long due, @Nullable UUID owner) {}

    private static final Map<ResourceKey<Level>, List<Pending>> PENDING = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            for (ServerLevel level : e.getServer().getAllLevels()) {
                List<Pending> list = PENDING.get(level.dimension());
                if (list == null || list.isEmpty()) continue;
                long now = level.getGameTime();
                List<Pending> due = new ArrayList<>();
                for (Iterator<Pending> it = list.iterator(); it.hasNext();) {
                    Pending p = it.next();
                    if (p.due() <= now) { due.add(p); it.remove(); }
                }
                due.forEach(p -> fire(level, p));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> PENDING.clear());
    }

    /**
     * Queues the charge at {@code pos} to go off in {@code delay} ticks. With an owner, it goes off only if a charge of
     * that owner's is still there then; without one (a chain), the block is already gone and it goes off regardless.
     */
    public static void queue(ServerLevel level, BlockPos pos, int delay, @Nullable UUID owner) {
        PENDING.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(new Pending(pos.immutable(), level.getGameTime() + delay, owner));
    }

    private static void fire(ServerLevel level, Pending p) {
        if (p.owner() != null) {
            if (!(level.getBlockEntity(p.pos()) instanceof C4BlockEntity be) || !p.owner().equals(be.owner())) return;
            level.setBlock(p.pos(), Blocks.AIR.defaultBlockState(), 3);
        }
        level.explode(null, p.pos().getX() + 0.5, p.pos().getY() + 0.5, p.pos().getZ() + 0.5, POWER, Level.ExplosionInteraction.MOB);
    }

    /** Is a charge at {@code pos} already queued (the self-test)? */
    public static boolean queued(ServerLevel level, BlockPos pos) {
        List<Pending> list = PENDING.get(level.dimension());
        return list != null && list.stream().anyMatch(p -> p.pos().equals(pos));
    }

    static boolean isCharge(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModRegistry.C4.get());
    }
}
