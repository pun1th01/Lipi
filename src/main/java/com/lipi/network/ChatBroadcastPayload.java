package com.lipi.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Server → All Clients packet.
 * Broadcast when the server relays a Lipi message to all connected Lipi players.
 */
public record ChatBroadcastPayload(
        UUID senderUuid,
        String playerName,
        String message,
        long timestamp,
        String channel
) implements CustomPayload {

    public static final CustomPayload.Id<ChatBroadcastPayload> ID =
            new CustomPayload.Id<>(Identifier.of("lipi", "chat_broadcast"));

    public static final PacketCodec<RegistryByteBuf, ChatBroadcastPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.senderUuid());
                buf.writeString(value.playerName());
                buf.writeString(value.message());
                buf.writeVarLong(value.timestamp());
                buf.writeString(value.channel());
            },
            buf -> new ChatBroadcastPayload(
                    buf.readUuid(),
                    buf.readString(),
                    buf.readString(),
                    buf.readVarLong(),
                    buf.readString()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
