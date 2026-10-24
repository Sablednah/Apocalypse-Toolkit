package com.sablednah.apocalypsetoolkit.throwable;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A smoke grenade: after {@link #FUSE} ticks it pours out a cloud {@link #RADIUS} blocks across for {@link #CLOUD}
 * ticks. Mobs in it lose whoever they were after, and anything in it is blinded, so it covers a retreat.
 */
public class SmokeGrenadeEntity extends FusedThrowable {
    static final int FUSE = 30, CLOUD = 300;
    static final double RADIUS = 4.0;

    public SmokeGrenadeEntity(EntityType<? extends SmokeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public SmokeGrenadeEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(ModRegistry.SMOKE_GRENADE_ENTITY.get(), level, owner, stack);
    }

    @Override protected Item getDefaultItem() { return ModRegistry.SMOKE_GRENADE.get(); }
    @Override protected int fuse() { return FUSE; }
    @Override protected int lingers() { return CLOUD; }

    @Override
    protected void detonate(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    @Override
    protected void linger(ServerLevel level, int tick) {
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.5, getZ(), 6, RADIUS / 2, 0.8, RADIUS / 2, 0.01);
        if (tick % 10 != 0) return;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(RADIUS))) {
            if (e.distanceToSqr(this) > RADIUS * RADIUS) continue;
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            if (e instanceof Mob mob) mob.setTarget(null);
        }
    }
}
