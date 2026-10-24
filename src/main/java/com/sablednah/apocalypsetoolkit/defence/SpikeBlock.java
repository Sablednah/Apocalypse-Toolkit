package com.sablednah.apocalypsetoolkit.defence;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A spike block: walking on it hurts ({@link #DAMAGE} a time) unless you sneak across, as magma does but worse, and
 * landing on it from a height doubles the fall.
 */
public class SpikeBlock extends Block {
    static final float DAMAGE = 3.0F;

    public SpikeBlock(Properties properties) {
        super(properties);
    }

    private static DamageSource spikes(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ModRegistry.SPIKES_DAMAGE));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living && !entity.isSteppingCarefully())
            living.hurtServer(server, spikes(server), DAMAGE);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (level instanceof ServerLevel server) entity.causeFallDamage(fallDistance, 2.0F, spikes(server));
        else super.fallOn(level, state, pos, entity, fallDistance);
    }
}
