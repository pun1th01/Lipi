package com.chatmc.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Server → Joining Client packet.
 * Contains the last 20 log lines from today's chat log, sent on player join.
 */
public record ChatHistoryPayload(
        List<String> lines
) implements CustomPayload {

    public static final CustomPayload.Id<ChatHistoryPayload> ID =
            new CustomPayload.Id<>(Identifier.of("chatmc", "chat_history"));

    public static final PacketCodec<RegistryByteBuf, ChatHistoryPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.collect(PacketCodecs.toList()), ChatHistoryPayload::lines,
            ChatHistoryPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
