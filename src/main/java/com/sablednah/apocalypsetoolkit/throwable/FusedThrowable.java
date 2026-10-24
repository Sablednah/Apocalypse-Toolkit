package com.sablednah.apocalypsetoolkit.throwable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown thing on a fuse: it bounces off what it hits, losing most of its speed, and {@link #fuse()} ticks after the
 * throw calls {@link #detonate(ServerLevel)}. If {@link #lingers()} is above zero it stays (invisible work, a cloud)
 * that many more ticks, calling {@link #linger(ServerLevel, int)} each tick, then goes.
 */
public abstract class FusedThrowable extends ThrowableItemProjectile {
    private boolean popped;
    private int lingered;

    protected FusedThrowable(EntityType<? extends FusedThrowable> type, Level level) {
        super(type, level);
    }

    protected FusedThrowable(EntityType<? extends FusedThrowable> type, Level level, LivingEntity owner, ItemStack stack) {
        super(type, owner, level, stack);
    }

    protected abstract int fuse();

    protected abstract void detonate(ServerLevel level);

    protected int lingers() {
        return 0;
    }

    protected void linger(ServerLevel level, int tick) {
    }

    /** Each tick before it goes off (sounds, a lure). */
    protected void burning(ServerLevel level, int tick) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (!popped && tickCount % 2 == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (!popped) {
            burning(level, tickCount);
            if (tickCount < fuse()) return;
            popped = true;
            detonate(level);
            if (lingers() <= 0) { discard(); return; }
            setDeltaMovement(Vec3.ZERO);
        }
        linger(level, lingered++);
        if (lingered >= lingers()) discard();
    }

    @Override
    protected void onHit(HitResult hit) {
        if (popped) return;                                  // resting where it went off
        if (hit instanceof BlockHitResult b) {
            Vec3 v = getDeltaMovement();
            Vec3 bounced = switch (b.getDirection().getAxis()) {
                case X -> new Vec3(-v.x, v.y, v.z);
                case Y -> new Vec3(v.x, -v.y, v.z);
                case Z -> new Vec3(v.x, v.y, -v.z);
            };
            setDeltaMovement(bounced.scale(0.35));
            if (bounced.lengthSqr() > 0.01 && level() instanceof ServerLevel server)
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.4F, 1.6F);
        } else if (hit instanceof EntityHitResult) {
            setDeltaMovement(getDeltaMovement().scale(-0.2));
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return popped ? 0.0 : super.getDefaultGravity();
    }
}
