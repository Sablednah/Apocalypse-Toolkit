package com.sablednah.apocalypsetoolkit.gun;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client to server: the Reload key was pressed; reload the gun in the main hand. */
public record ReloadPayload() implements CustomPacketPayload {
    public static final Type<ReloadPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadPayload> CODEC = StreamCodec.unit(new ReloadPayload());

    @Override
    public Type<ReloadPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1")
                .playToServer(TYPE, CODEC, (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) return;
                    var stack = sp.getMainHandItem();
                    if (stack.getItem() instanceof GunItem gun) gun.startReload(level, sp, stack);
                })));
    }
}
