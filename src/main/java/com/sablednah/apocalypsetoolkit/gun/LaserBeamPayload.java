package com.sablednah.apocalypsetoolkit.gun;

import com.sablednah.apocalypsetoolkit.ApocalypseToolkit;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Server to client: a laser beam from {@code from} to {@code to}. The client draws the red dots along it itself; it
 * used to be one particle packet per dot, ~270 for a long shot, to everyone near.
 */
public record LaserBeamPayload(Vec3 from, Vec3 to) implements CustomPacketPayload {
    public static final Type<LaserBeamPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ApocalypseToolkit.MOD_ID, "laser_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserBeamPayload> CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC, LaserBeamPayload::from, Vec3.STREAM_CODEC, LaserBeamPayload::to, LaserBeamPayload::new);

    @Override
    public Type<LaserBeamPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1")
                .playToClient(TYPE, CODEC, (payload, context) -> context.enqueueWork(
                        () -> com.sablednah.apocalypsetoolkit.client.LaserBeams.draw(payload.from(), payload.to()))));
    }

    /** Show the beam to everyone near enough to see it (the shooter included). */
    public static void send(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 mid = from.add(to).scale(0.5);
        PacketDistributor.sendToPlayersNear(level, null, mid.x, mid.y, mid.z, 48 + from.distanceTo(to) / 2, new LaserBeamPayload(from, to));
    }
}
