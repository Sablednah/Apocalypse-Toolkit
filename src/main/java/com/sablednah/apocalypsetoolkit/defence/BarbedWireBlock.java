package com.sablednah.apocalypsetoolkit.defence;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Barbed wire: there's no wall to it, so anything can push in, but it drags like a cobweb and cuts
 * ({@link #DAMAGE} a time, at the usual hurt-and-recover pace) whatever living thing is caught in it.
 */
public class BarbedWireBlock extends Block {
    static final float DAMAGE = 1.5F;
    private static final Vec3 DRAG = new Vec3(0.35, 0.6, 0.35);

    public BarbedWireBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (!(entity instanceof LivingEntity living)) return;
        entity.makeStuckInBlock(state, DRAG);
        if (level instanceof ServerLevel server)
            living.hurtServer(server, new DamageSource(server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getOrThrow(ModRegistry.BARBED_WIRE_DAMAGE)), DAMAGE);
    }
}
