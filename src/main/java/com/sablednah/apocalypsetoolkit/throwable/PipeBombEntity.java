package com.sablednah.apocalypsetoolkit.throwable;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A pipe bomb: it beeps, faster as it burns down, and every monster within {@link #LURE} blocks makes for it; after
 * {@link #FUSE} ticks it goes off, bigger than a grenade ({@link #POWER}), breaking blocks only where mobs may.
 */
public class PipeBombEntity extends FusedThrowable {
    static final int FUSE = 100, LURE = 24;
    static final float POWER = 3.2F;

    public PipeBombEntity(EntityType<? extends PipeBombEntity> type, Level level) {
        super(type, level);
    }

    public PipeBombEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(ModRegistry.PIPE_BOMB_ENTITY.get(), level, owner, stack);
    }

    @Override protected Item getDefaultItem() { return ModRegistry.PIPE_BOMB.get(); }
    @Override protected int fuse() { return FUSE; }

    @Override
    protected void burning(ServerLevel level, int tick) {
        int every = tick < 50 ? 12 : tick < 80 ? 6 : 3;
        if (tick % every == 0)
            level.playSound(null, getX(), getY(), getZ(), com.sablednah.apocalypsetoolkit.ModRegistry.BEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        // Once a second: who's in range (the nearest MAX_LURED). Each tick: a twentieth of them repath, so a crowd's
        // pathfinding is spread over the second instead of all in one tick; one already on its way is left alone.
        if (tick % 20 == 0) {
            lured = level.getEntitiesOfClass(Monster.class, new AABB(blockPosition()).inflate(LURE), Monster::isAlive);
            if (lured.size() > MAX_LURED) {
                lured = new java.util.ArrayList<>(lured);
                lured.sort(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(this)));
                lured = lured.subList(0, MAX_LURED);
            }
        }
        for (int i = tick % 20; i < lured.size(); i += 20) {
            Monster m = lured.get(i);
            if (!m.isAlive()) continue;
            m.setTarget(null);
            var going = m.getNavigation().getTargetPos();
            if (going != null && !m.getNavigation().isDone() && going.closerToCenterThan(position(), 2.0)) continue;
            m.getNavigation().moveTo(getX(), getY(), getZ(), 1.1);
        }
    }

    /** At most this many monsters are drawn to one bomb (the nearest). */
    static final int MAX_LURED = 48;
    private java.util.List<Monster> lured = java.util.List.of();

    @Override
    protected void detonate(ServerLevel level) {
        level.explode(this, getX(), getY(), getZ(), POWER, Level.ExplosionInteraction.MOB);
    }
}
