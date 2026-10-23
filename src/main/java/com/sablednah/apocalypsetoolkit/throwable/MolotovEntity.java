package com.sablednah.apocalypsetoolkit.throwable;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown molotov: it smashes on whatever it hits and sets everything within {@link #RADIUS} blocks alight for
 * {@link #BURN} seconds. It lays fire on the ground around the smash only where mobs may grief (the mobGriefing rule),
 * so on a protected server it burns creatures, not buildings.
 */
public class MolotovEntity extends ThrowableItemProjectile {
    static final double RADIUS = 2.5;
    static final float BURN = 6F;

    public MolotovEntity(EntityType<? extends MolotovEntity> type, Level level) {
        super(type, level);
    }

    public MolotovEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(ModRegistry.MOLOTOV_ENTITY.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModRegistry.MOLOTOV.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel level)) return;
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 40, RADIUS / 2, 0.3, RADIUS / 2, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 10, RADIUS / 2, 0.4, RADIUS / 2, 0.02);
        for (Entity e : level.getEntities(this, new AABB(position(), position()).inflate(RADIUS), e -> e instanceof LivingEntity))
            e.igniteForSeconds(BURN);
        if (net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, this)) {
            BlockPos centre = blockPosition();
            int r = (int) Math.ceil(RADIUS);
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -1, -r), centre.offset(r, 1, r))) {
                if (p.distSqr(centre) > RADIUS * RADIUS || level.getRandom().nextFloat() < 0.35F) continue;
                if (level.getBlockState(p).isAir() && BaseFireBlock.canBePlacedAt(level, p, net.minecraft.core.Direction.UP))
                    level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p));
            }
        }
        discard();
    }
}
