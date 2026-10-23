package com.sablednah.apocalypsetoolkit.gear;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A hunting bow: arrows leave {@link #SPEED} times faster, half as wild, and hit {@link #DAMAGE} times as hard. */
public class HuntingBowItem extends BowItem {
    static final float SPEED = 1.25F, DAMAGE = 1.4F;

    public HuntingBowItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
        Projectile p = super.createProjectile(level, shooter, weapon, ammo, crit);
        if (p instanceof AbstractArrow arrow) arrow.setBaseDamage(2.0 * DAMAGE);   // vanilla arrows start at 2; Power and the like apply on top
        return p;
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy,
            float angle, LivingEntity target) {
        super.shootProjectile(shooter, projectile, index, velocity * SPEED, inaccuracy * 0.5F, angle, target);
    }
}
