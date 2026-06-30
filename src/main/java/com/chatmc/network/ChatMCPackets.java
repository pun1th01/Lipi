package com.chatmc.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers all ChatMC custom payload types with the Fabric networking API.
 * Must be called from both client and server initializers (the shared ModInitializer).
 */
public final class ChatMCPackets {

    private ChatMCPackets() {
        // Utility class
    }

    /**
     * Registers all packet payload types in the PayloadTypeRegistry.
     * C2S = Client to Server, S2C = Server to Client.
     */
    public static void registerAll() {
        // Client → Server
        PayloadTypeRegistry.playC2S().register(ChatMessagePayload.ID, ChatMessagePayload.CODEC);

        // Server → Client
        PayloadTypeRegistry.playS2C().register(ChatBroadcastPayload.ID, ChatBroadcastPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatHistoryPayload.ID, ChatHistoryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerStatusPayload.ID, ServerStatusPayload.CODEC);
    }
}
