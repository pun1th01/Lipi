package com.chatmc.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server → Client packet.
 * Sent on player join to confirm that the server has ChatMC installed and whether it is active.
 */
public record ServerStatusPayload(
        boolean active
) implements CustomPayload {

    public static final CustomPayload.Id<ServerStatusPayload> ID =
            new CustomPayload.Id<>(Identifier.of("chatmc", "server_status"));

    public static final PacketCodec<RegistryByteBuf, ServerStatusPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, ServerStatusPayload::active,
            ServerStatusPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
