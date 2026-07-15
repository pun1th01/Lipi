package com.lipi.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client → Server packet.
 * Sent when a player sends a message while Lipi mode is active.
 * Only carries the message content and channel — the server derives
 * sender identity and timestamp from the authenticated connection.
 */
public record ChatMessagePayload(
        String message,
        String channel
) implements CustomPayload {

    public static final CustomPayload.Id<ChatMessagePayload> ID =
            new CustomPayload.Id<>(Identifier.of("lipi", "chat_message"));

    public static final PacketCodec<RegistryByteBuf, ChatMessagePayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.message());
                buf.writeString(value.channel());
            },
            buf -> new ChatMessagePayload(
                    buf.readString(),
                    buf.readString()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
