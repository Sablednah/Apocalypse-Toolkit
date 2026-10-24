package com.sablednah.apocalypsetoolkit.throwable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * A grenade, molotov, pipe bomb, smoke or chemical grenade. Anything on a fuse is cooked: pressing use pulls the pin
 * and the fuse starts; letting go throws it with whatever is left (the thrown entity starts that far into its fuse);
 * holding on past the end and it goes off in your hand. The time left shows on the action bar. A molotov has no fuse
 * and is thrown on the click. A short cooldown follows each throw.
 */
public class ThrowableItem extends Item {
    public enum Kind {
        GRENADE(GrenadeEntity.FUSE), MOLOTOV(0), PIPE_BOMB(PipeBombEntity.FUSE),
        SMOKE(SmokeGrenadeEntity.FUSE), CHEMICAL(ChemicalGrenadeEntity.FUSE);

        /** Ticks from pulling the pin to going off; 0 for none (thrown at once). */
        final int fuse;

        Kind(int fuse) {
            this.fuse = fuse;
        }
    }

    private final Kind kind;

    public ThrowableItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind.fuse <= 0) {
            release(level, player, stack, 0, true);
            return InteractionResult.SUCCESS;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 0.8F, 1.6F);
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return kind.fuse > 0 ? ItemUseAnimation.TRIDENT : ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return;
        int held = getUseDuration(stack, entity) - remaining;
        int left = kind.fuse - held;
        if (left <= 0) {                                    // too long: it goes off where you stand
            player.stopUsingItem();
            release(level, player, stack, kind.fuse, false);
            return;
        }
        if (kind == Kind.PIPE_BOMB) {
            int every = held < 50 ? 12 : held < 80 ? 6 : 3;
            if (held % every == 0)
                server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.2F, 1.8F);
        } else if (held % 20 == 0) {
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.PLAYERS, 0.5F, 2.0F);
        }
        if (held % 2 == 0)
            player.sendOverlayMessage(Component.translatable("message.apocalypse_toolkit.fuse", String.format("%.1f", left / 20.0)));
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof Player player)) return false;
        int held = getUseDuration(stack, entity) - remaining;
        release(level, player, stack, Math.min(held, kind.fuse), held < kind.fuse);
        return true;
    }

    /** Throws it ({@code thrown}), or drops it at the player's feet already spent (cooked too long), {@code burnt} ticks in. */
    private void release(Level level, Player player, ItemStack stack, int burnt, boolean thrown) {
        if (thrown)
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
                    0.6F, 0.5F + level.getRandom().nextFloat() * 0.2F);
        if (level instanceof ServerLevel server) {
            ThrowableItemProjectile p = switch (kind) {
                case GRENADE -> new GrenadeEntity(server, player, stack);
                case MOLOTOV -> new MolotovEntity(server, player, stack);
                case PIPE_BOMB -> new PipeBombEntity(server, player, stack);
                case SMOKE -> new SmokeGrenadeEntity(server, player, stack);
                case CHEMICAL -> new ChemicalGrenadeEntity(server, player, stack);
            };
            p.tickCount = burnt;                            // the fuse already burnt in the hand
            if (thrown) p.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 1.1F, 1.0F);
            else {
                p.setPos(player.getX(), player.getY() + 1.0, player.getZ());
                p.setDeltaMovement(0, 0, 0);
            }
            server.addFreshEntity(p);
            if (!thrown) player.sendOverlayMessage(Component.translatable("message.apocalypse_toolkit.fuse_hand"));
        }
        player.getCooldowns().addCooldown(stack, 20);
        stack.consume(1, player);
    }
}
