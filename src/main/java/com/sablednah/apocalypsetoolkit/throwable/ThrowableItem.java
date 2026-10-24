package com.sablednah.apocalypsetoolkit.throwable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A grenade, molotov, pipe bomb, smoke or chemical grenade: right-click throws it (a short cooldown between throws). */
public class ThrowableItem extends Item {
    public enum Kind { GRENADE, MOLOTOV, PIPE_BOMB, SMOKE, CHEMICAL }

    private final Kind kind;

    public ThrowableItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
                0.6F, 0.5F + level.getRandom().nextFloat() * 0.2F);
        if (level instanceof ServerLevel server) {
            ThrowableItemProjectile thrown = switch (kind) {
                case GRENADE -> new GrenadeEntity(server, player, stack);
                case MOLOTOV -> new MolotovEntity(server, player, stack);
                case PIPE_BOMB -> new PipeBombEntity(server, player, stack);
                case SMOKE -> new SmokeGrenadeEntity(server, player, stack);
                case CHEMICAL -> new ChemicalGrenadeEntity(server, player, stack);
            };
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 1.1F, 1.0F);
            server.addFreshEntity(thrown);
        }
        player.getCooldowns().addCooldown(stack, 20);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
