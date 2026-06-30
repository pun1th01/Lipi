package com.chatmc.server;

import com.chatmc.ChatMC;
import com.chatmc.network.*;
import com.chatmc.server.command.ChatMCCommand;
import com.chatmc.server.config.ChatMCServerConfig;
import com.chatmc.server.log.ChatLogger;
import com.chatmc.server.moderation.MuteManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Server-side initializer for ChatMC.
 * Handles incoming chat packets, broadcasting, logging, and admin commands.
 */
public class ChatMCServer implements DedicatedServerModInitializer {

    public static ChatMCServerConfig config;
    public static ChatLogger chatLogger;
    public static MuteManager muteManager;
    public static MinecraftServer serverInstance;

    @Override
    public void onInitializeServer() {
        ChatMC.LOGGER.info("ChatMC server initializing...");

        // Load server config
        config = new ChatMCServerConfig();
        config.load();

        // Initialize logger
        chatLogger = new ChatLogger();

        // Initialize mute manager
        muteManager = new MuteManager();
        muteManager.load();

        // Server started event — clean old logs
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;

            if (config.getLogRetentionDays() > 0) {
                chatLogger.cleanOldLogs(config.getLogRetentionDays());
            }
        });

        // Player join — send server status and chat history
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Check if the client has ChatMC installed (can receive our packets)
            if (ServerPlayNetworking.canSend(player, ServerStatusPayload.ID)) {
                // Send server status
                ServerPlayNetworking.send(player, new ServerStatusPayload(config.isEnabled()));

                // Log the join
                if (config.isEnabled() && config.getLogRetentionDays() > 0) {
                    chatLogger.logJoin(player.getUuid(), player.getName().getString());
                }

                // Send chat history (last 20 messages from today)
                if (config.isEnabled()) {
                    List<String> history = chatLogger.getLastMessages(20);
                    if (!history.isEmpty()) {
                        ServerPlayNetworking.send(player, new ChatHistoryPayload(history));
                    }
                }

                ChatMC.LOGGER.info("Player {} has ChatMC installed. Sent status and history.", player.getName().getString());
            }
        });

        // Player disconnect — log leave
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (config.isEnabled() && config.getLogRetentionDays() > 0) {
                chatLogger.logLeave(player.getUuid(), player.getName().getString());
            }
        });

        // Handle incoming ChatMC messages (C2S)
        ServerPlayNetworking.registerGlobalReceiver(ChatMessagePayload.ID, (payload, context) -> {
            ServerPlayerEntity sender = context.player();
            MinecraftServer server = sender.getServer();

            if (server == null) return;

            server.execute(() -> {
                // Check if ChatMC is enabled
                if (!config.isEnabled()) {
                    return;
                }

                // Check if the player is muted
                if (muteManager.isMuted(sender.getUuid())) {
                    // Silently drop the message
                    ChatMC.LOGGER.info("Dropped message from muted player: {}", sender.getName().getString());
                    return;
                }

                // Log the message
                if (config.getLogRetentionDays() > 0) {
                    chatLogger.logMessage(
                            payload.senderUuid(),
                            payload.playerName(),
                            payload.message(),
                            payload.channel()
                    );
                }

                // Create broadcast payload
                ChatBroadcastPayload broadcast = new ChatBroadcastPayload(
                        payload.senderUuid(),
                        payload.playerName(),
                        payload.message(),
                        payload.timestamp(),
                        payload.channel()
                );

                // Broadcast to all players with ChatMC installed
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (ServerPlayNetworking.canSend(player, ChatBroadcastPayload.ID)) {
                        ServerPlayNetworking.send(player, broadcast);
                    }
                }
            });
        });

        // Register admin commands
        ChatMCCommand.register();

        ChatMC.LOGGER.info("ChatMC server initialized.");
    }
}
