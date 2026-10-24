package com.sablednah.apocalypsetoolkit.medical;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A Plaster: right-click heals {@link #HEAL} health (two hearts) at once and uses the plaster up; the small instant
 * option beside the bandage, which heals over time. A short cooldown stops a whole stack going in one click-burst; at
 * full health it is refused and not used up. (Moved from Threadwork, 2026-10-24.)
 */
public class PlasterItem extends Item {
    public static final float HEAL = 4.0F;
    public static final int COOLDOWN_TICKS = 20;

    public PlasterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getHealth() >= player.getMaxHealth()) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.apocalypse_toolkit.not_hurt"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            player.heal(HEAL);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.PLAYERS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
