package com.chatmc.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Client → Server packet.
 * Sent when a player sends a message while ChatMC mode is active.
 */
public record ChatMessagePayload(
        UUID senderUuid,
        String playerName,
        String message,
        long timestamp,
        String channel
) implements CustomPayload {

    public static final CustomPayload.Id<ChatMessagePayload> ID =
            new CustomPayload.Id<>(Identifier.of("chatmc", "chat_message"));

    public static final PacketCodec<RegistryByteBuf, ChatMessagePayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.senderUuid());
                buf.writeString(value.playerName());
                buf.writeString(value.message());
                buf.writeVarLong(value.timestamp());
                buf.writeString(value.channel());
            },
            buf -> new ChatMessagePayload(
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
