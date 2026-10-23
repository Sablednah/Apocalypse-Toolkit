package com.sablednah.apocalypsetoolkit.sprung;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Sprung doors, trapdoors and fence gates: any of them (any wood, any mod's, anything in the vanilla tags with an
 * {@code open} property), crafted with an iron nugget ({@link SprungRecipe}), carries the {@link #SPRUNG} component.
 * Placed, its position goes in the chunk's {@link #POSITIONS} attachment (a door by its lower half). Opened by hand,
 * it swings shut {@link #CLOSE_DELAY} ticks later, waiting while anyone stands in it and leaving a redstone-powered
 * one alone. Broken, it drops still sprung. Pending closes are not saved: one left open across a restart stays open
 * until used again.
 *
 * <p>Ported from Threadwork's sprung gates (2 s there; Sable wanted 1 s, and doors and trapdoors too).
 */
public final class Sprung {
    private Sprung() {}

    public static final int CLOSE_DELAY = 20, RETRY = 10;

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ApocalypseToolkit.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ApocalypseToolkit.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ApocalypseToolkit.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SPRUNG =
            COMPONENTS.registerComponentType("sprung", b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    /** Sprung positions in one chunk, as {@link BlockPos#asLong()}. Immutable; replaced on change. */
    public record Positions(List<Long> list) {
        static final MapCodec<Positions> CODEC = Codec.LONG.listOf().fieldOf("positions").xmap(Positions::new, Positions::list);
    }

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Positions>> POSITIONS = ATTACHMENTS.register("sprung",
            () -> AttachmentType.builder(() -> new Positions(List.of())).serialize(Positions.CODEC, p -> !p.list().isEmpty()).build());

    static {
        SERIALIZERS.register("sprung", () -> SprungRecipe.SERIALIZER);
    }

    /** Level -> position -> game time to close at. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> PENDING = new HashMap<>();

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        SERIALIZERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Sprung::onPlace);
        NeoForge.EVENT_BUS.addListener(Sprung::onUse);
        NeoForge.EVENT_BUS.addListener(Sprung::onDrops);
        NeoForge.EVENT_BUS.addListener(Sprung::onLevelTick);
        NeoForge.EVENT_BUS.addListener(Sprung::onTooltip);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> PENDING.clear());
    }

    public static boolean sprungable(BlockState state) {
        return (state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS))
                && state.hasProperty(BlockStateProperties.OPEN);
    }

    /** A door is tracked by its lower half. */
    private static BlockPos anchor(BlockState state, BlockPos pos) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                ? pos.below() : pos;
    }

    public static boolean isSprung(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getChunkAt(pos).getData(POSITIONS).list().contains(pos.asLong());
    }

    public static void mark(Level level, BlockPos pos, boolean sprung) {
        var chunk = level.getChunkAt(pos);
        List<Long> list = new ArrayList<>(chunk.getData(POSITIONS).list());
        boolean had = list.remove((Long) pos.asLong());
        if (sprung) list.add(pos.asLong());
        if (had != sprung || sprung) chunk.setData(POSITIONS, new Positions(List.copyOf(list)));
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        BlockState placed = event.getPlacedBlock();
        if (!sprungable(placed)) return;
        ItemStack used = ItemStack.EMPTY;
        if (event.getEntity() instanceof Player player)
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack s = player.getItemInHand(hand);
                if (s.getItem() instanceof BlockItem bi && bi.getBlock() == placed.getBlock()) { used = s; break; }
            }
        mark(level, anchor(placed, event.getPos()), used.has(SPRUNG.get()));
    }

    private static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = level.getBlockState(event.getPos());
        BlockPos pos = anchor(state, event.getPos());
        if (!isSprung(level, pos)) return;
        if (!sprungable(level.getBlockState(pos))) { mark(level, pos, false); return; }
        schedule(level, pos, CLOSE_DELAY);
    }

    /** The game time a close is due at {@code pos}, or -1 (the self-test). */
    public static long pendingAt(ServerLevel level, BlockPos pos) {
        var m = PENDING.get(level.dimension());
        return m == null ? -1 : m.getOrDefault(pos, -1L);
    }

    /** Closes a sprung block at {@code pos} after the usual delay, as if it had just been opened (the self-test). */
    public static void scheduleClose(ServerLevel level, BlockPos pos) {
        schedule(level, pos, CLOSE_DELAY);
    }

    private static void schedule(ServerLevel level, BlockPos pos, int ticks) {
        PENDING.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), level.getGameTime() + ticks);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Map<BlockPos, Long> pending = PENDING.get(level.dimension());
        if (pending == null || pending.isEmpty()) return;
        long now = level.getGameTime();
        List<BlockPos> due = new ArrayList<>();
        pending.forEach((pos, at) -> { if (at <= now) due.add(pos); });
        for (BlockPos pos : due) {
            pending.remove(pos);
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!sprungable(state)) { mark(level, pos, false); continue; }
            if (!state.getValue(BlockStateProperties.OPEN)) continue;
            if (state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED)) continue;
            AABB space = new AABB(pos);
            if (state.getBlock() instanceof DoorBlock) space = space.expandTowards(0, 1, 0);
            if (!level.getEntitiesOfClass(LivingEntity.class, space).isEmpty()) { schedule(level, pos, RETRY); continue; }
            close(level, pos, state);
        }
    }

    private static void close(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(null, level, state, pos, false);            // both halves, with the door's own sound
            return;
        }
        level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, false), 10);
        var sound = state.getBlock() instanceof TrapDoorBlock ? SoundEvents.WOODEN_TRAPDOOR_CLOSE : SoundEvents.FENCE_GATE_CLOSE;
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(null, GameEvent.BLOCK_CLOSE, pos);
    }

    private static void onDrops(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        BlockPos pos = anchor(event.getState(), event.getPos());
        if (!isSprung(level, pos)) return;
        for (var drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() == event.getState().getBlock()) {
                stack.set(SPRUNG.get(), Unit.INSTANCE);
                drop.setItem(stack);
            }
        }
        mark(level, pos, false);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().has(SPRUNG.get()))
            event.getToolTip().add(1, Component.translatable("tooltip.apocalypse_toolkit.sprung").withStyle(ChatFormatting.GRAY));
    }
}
