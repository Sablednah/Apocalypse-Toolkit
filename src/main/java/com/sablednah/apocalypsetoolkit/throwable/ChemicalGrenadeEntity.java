package com.sablednah.apocalypsetoolkit.throwable;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A chemical grenade: after {@link #FUSE} ticks it bursts into a lingering gas cloud ({@link #RADIUS} blocks,
 * {@link #CLOUD} ticks) of Wither, Poison and Slowness. Wither because the dead don't care about poison.
 */
public class ChemicalGrenadeEntity extends FusedThrowable {
    static final int FUSE = 30, CLOUD = 200;
    static final float RADIUS = 3.5F;

    public ChemicalGrenadeEntity(EntityType<? extends ChemicalGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public ChemicalGrenadeEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(ModRegistry.CHEMICAL_GRENADE_ENTITY.get(), level, owner, stack);
    }

    @Override protected Item getDefaultItem() { return ModRegistry.CHEMICAL_GRENADE.get(); }
    @Override protected int fuse() { return FUSE; }

    @Override
    protected void detonate(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        AreaEffectCloud cloud = new AreaEffectCloud(level, getX(), getY(), getZ());
        if (getOwner() instanceof LivingEntity owner) cloud.setOwner(owner);
        cloud.setRadius(RADIUS);
        cloud.setRadiusOnUse(-0.1F);
        cloud.setWaitTime(5);
        cloud.setDuration(CLOUD);
        cloud.setCustomParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF7FB238));
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
        level.addFreshEntity(cloud);
    }
}
