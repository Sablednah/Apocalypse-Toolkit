package com.sablednah.apocalypsetoolkit.gun;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;
import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The flamethrower's stream: each burst (every {@code cooldown} ticks while use is held) scorches everything it can see
 * in a cone out to its range, sets it alight, and now and then lights the ground where the stream ends (only where
 * mobs may grief). Fuel is the gun's magazine.
 *
 * <p>The holder's {@code apocalypse_toolkit:flame_skill} attribute (0 for most; a pack gives its fire specialists
 * more) makes them better at it, per point: half as much range again, half as much damage again, burns twice as long,
 * and fuel lasting twice as long.</p>
 */
public final class Flames {
    private Flames() {}

    public static final ResourceKey<DamageType> FLAME =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "flamethrower"));
    /** Half-angle of the cone, degrees; seconds a target burns, before skill. */
    static final double CONE = 16.0, BURN = 4.0;

    public static float skill(Player player) {
        return (float) player.getAttributeValue(ModRegistry.FLAME_SKILL);
    }

    /** The self-test: one burst, as if use were held. */
    public static void burstForTest(ServerLevel level, Player player, ItemStack stack, GunItem gun) {
        burst(level, player, stack, gun);
    }

    static void burst(ServerLevel level, Player player, ItemStack stack, GunItem gun) {
        GunSpec spec = gun.spec();
        if (player.isUnderWater()) {
            level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getEyeY(), player.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
            return;
        }
        float skill = skill(player);
        // fuel: one unit a burst, or less often with skill (twice as long a point)
        if (!player.isCreative() && level.getRandom().nextFloat() < 1F / (1F + skill))
            stack.set(ModRegistry.LOADED.get(), GunItem.loaded(stack) - 1);
        double range = spec.range() * (1.0 + 0.5 * skill);
        float damage = spec.damage() * (1F + 0.5F * skill);
        Vec3 eye = player.getEyePosition(), dir = Vec3.directionFromRotation(player.getXRot(), player.getYRot());   // as the guns aim
        if (level.getGameTime() % 8 < spec.cooldown())
            level.playSound(null, player.getX(), player.getY(), player.getZ(), spec.sound().get(), SoundSource.PLAYERS, 1.0F,
                    0.9F + level.getRandom().nextFloat() * 0.2F);
        // where the stream stops: the first solid block, or its full length
        BlockHitResult wall = level.clip(new ClipContext(eye, eye.add(dir.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        double reach = wall.getType() == HitResult.Type.MISS ? range : wall.getLocation().distanceTo(eye);
        for (double d = 0.8; d < reach; d += 0.5) {
            Vec3 p = eye.add(dir.scale(d)).add(0, -0.2, 0);
            double w = 0.04 + d * Math.tan(Math.toRadians(CONE)) * 0.4;
            level.sendParticles(d < reach * 0.6 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 2, w, w, w, 0.01);
        }
        Vec3 end = eye.add(dir.scale(reach));
        level.sendParticles(ParticleTypes.LARGE_SMOKE, end.x, end.y, end.z, 1, 0.2, 0.2, 0.2, 0.01);
        // everything in the cone it can see
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(FLAME), player);
        double cos = Math.cos(Math.toRadians(CONE));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range), e -> e != player && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.01 || to.normalize().dot(dir) < cos) continue;
            if (level.clip(new ClipContext(eye, e.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != HitResult.Type.MISS) continue;
            e.hurtServer(level, source, damage);
            if (!e.fireImmune()) e.igniteForSeconds((float) (BURN * (1.0 + skill)));
        }
        // the ground it lands on catches, now and then, where mobs may grief
        if (wall.getType() == HitResult.Type.BLOCK && level.getRandom().nextFloat() < 0.15F
                && net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, player)) {
            BlockPos at = wall.getBlockPos().relative(wall.getDirection());
            if (level.getBlockState(at).isAir() && BaseFireBlock.canBePlacedAt(level, at, Direction.UP))
                level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
        }
    }
}
