package com.sablednah.apocalypsetoolkit.throwable;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown grenade. It bounces off what it hits, losing most of its speed, and goes off {@link #FUSE} ticks after the
 * throw: an explosion of {@link #POWER} that breaks blocks only where mobs may (the mobGriefing rule, and anything
 * that vetoes explosions, a claim mod).
 */
public class GrenadeEntity extends ThrowableItemProjectile {
    static final int FUSE = 60;
    static final float POWER = 2.5F;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(ModRegistry.GRENADE_ENTITY.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModRegistry.GRENADE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            return;
        }
        if (tickCount >= FUSE) {
            level().explode(this, getX(), getY(), getZ(), POWER, Level.ExplosionInteraction.MOB);
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        Vec3 v = getDeltaMovement();
        Vec3 bounced = switch (hit.getDirection().getAxis()) {
            case X -> new Vec3(-v.x, v.y, v.z);
            case Y -> new Vec3(v.x, -v.y, v.z);
            case Z -> new Vec3(v.x, v.y, -v.z);
        };
        setDeltaMovement(bounced.scale(0.35));
        if (bounced.lengthSqr() > 0.01 && level() instanceof ServerLevel server)
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.4F, 1.6F);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        setDeltaMovement(getDeltaMovement().scale(-0.2));
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult hit) {
        // not discarded on impact: it rests until the fuse burns down
        if (hit instanceof BlockHitResult b) onHitBlock(b);
        else if (hit instanceof EntityHitResult e) onHitEntity(e);
    }
}
