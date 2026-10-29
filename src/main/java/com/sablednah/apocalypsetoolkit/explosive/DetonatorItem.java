package com.sablednah.apocalypsetoolkit.explosive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The detonator: right-click sets off every C4 charge you placed within {@link #RANGE} blocks (in loaded ground),
 * nearest first, a couple of ticks apart.
 */
public class DetonatorItem extends Item {
    public static final int RANGE = 96;

    public DetonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.8F, 1.6F);
        if (level instanceof ServerLevel server) {
            List<BlockPos> charges = charges(server, player);
            for (int i = 0; i < charges.size(); i++) Blasts.queue(server, charges.get(i), 6 + i * 2, player.getUUID());
            if (charges.size() >= 3) com.sablednah.apocalypsetoolkit.feats.Feats.award(player, "controlled_demolition");
            player.sendOverlayMessage(charges.isEmpty() ? Component.translatable("message.apocalypse_toolkit.detonator.none", RANGE)
                    : Component.translatable("message.apocalypse_toolkit.detonator.armed", charges.size()));
        }
        player.getCooldowns().addCooldown(stack, 20);
        return InteractionResult.SUCCESS;
    }

    /** {@code player}'s charges within range, nearest first. */
    public static List<BlockPos> charges(ServerLevel level, Player player) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos at = player.blockPosition();
        int r = (RANGE >> 4) + 1, cx = at.getX() >> 4, cz = at.getZ() >> 4;
        for (int x = cx - r; x <= cx + r; x++)
            for (int z = cz - r; z <= cz + r; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values())
                    if (be instanceof C4BlockEntity c4 && player.getUUID().equals(c4.owner()) && be.getBlockPos().closerThan(at, RANGE))
                        found.add(be.getBlockPos());
            }
        found.sort(Comparator.comparingDouble(p -> p.distSqr(at)));
        return found;
    }
}
