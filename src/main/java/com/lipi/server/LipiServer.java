package com.lipi.server;

import com.lipi.Lipi;
import com.lipi.network.*;
import com.lipi.server.command.LipiCommand;
import com.lipi.server.config.LipiServerConfig;
import com.lipi.server.log.ChatLogger;
import com.lipi.server.moderation.MuteManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side initializer for Lipi.
 * Handles incoming chat packets, broadcasting, logging, and admin commands.
 */
public class LipiServer implements DedicatedServerModInitializer {

    public static LipiServerConfig config;
    public static ChatLogger chatLogger;
    public static MuteManager muteManager;
    public static MinecraftServer serverInstance;

    /** Per-player cooldown tracking: UUID → last message timestamp (epoch millis). */
    private static final ConcurrentHashMap<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();

    /** Global slow mode tracking: last message timestamp from any player. */
    private static long lastGlobalMessage = 0;

    @Override
    public void onInitializeServer() {
        Lipi.LOGGER.info("Lipi server initializing...");

        // Load server config
        config = new LipiServerConfig();
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

            // Check if the client has Lipi installed (can receive our packets)
            if (ServerPlayNetworking.canSend(player, ServerStatusPayload.ID)) {
                // Send server status
                ServerPlayNetworking.send(player, new ServerStatusPayload(config.isEnabled()));

                // Log the join
                if (config.isEnabled() && config.getLogRetentionDays() > 0) {
                    chatLogger.logJoin(player.getUuid(), player.getName().getString());
                }

                // Send chat history (last 20 chat messages from today, excluding JOIN/LEAVE)
                if (config.isEnabled()) {
                    List<String> history = chatLogger.getLastMessages(20).stream()
                            .filter(line -> !line.contains("[JOIN]") && !line.contains("[LEAVE]"))
                            .toList();
                    if (!history.isEmpty()) {
                        ServerPlayNetworking.send(player, new ChatHistoryPayload(history));
                    }
                }

                Lipi.LOGGER.info("Player {} has Lipi installed. Sent status and history.", player.getName().getString());
            }
        });

        // Player disconnect — log leave
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (config.isEnabled() && config.getLogRetentionDays() > 0) {
                chatLogger.logLeave(player.getUuid(), player.getName().getString());
            }
        });

        // Handle incoming Lipi messages (C2S)
        ServerPlayNetworking.registerGlobalReceiver(ChatMessagePayload.ID, (payload, context) -> {
            ServerPlayerEntity sender = context.player();
            MinecraftServer server = sender.getServer();

            if (server == null) return;

            server.execute(() -> {
                // Check if Lipi is enabled
                if (!config.isEnabled()) {
                    return;
                }

                // Check if the player is muted
                if (muteManager.isMuted(sender.getUuid())) {
                    // Silently drop the message
                    Lipi.LOGGER.info("Dropped message from muted player: {}", sender.getName().getString());
                    return;
                }

                // Derive identity server-side — never trust client-supplied values
                UUID senderUuid = sender.getUuid();
                String senderName = sender.getName().getString();
                long now = System.currentTimeMillis();
                String message = payload.message();

                // Validate message length
                if (message.length() > config.getMaxMessageLength()) {
                    sender.sendMessage(
                            net.minecraft.text.Text.literal("[Lipi] Message too long (max " + config.getMaxMessageLength() + " characters)")
                                    .formatted(net.minecraft.util.Formatting.RED),
                            false
                    );
                    return;
                }

                // Per-player cooldown check
                int cooldownSec = config.getMessageCooldownSeconds();
                if (cooldownSec > 0) {
                    Long lastTime = playerCooldowns.get(senderUuid);
                    if (lastTime != null && (now - lastTime) < cooldownSec * 1000L) {
                        long remaining = (cooldownSec * 1000L - (now - lastTime)) / 1000 + 1;
                        sender.sendMessage(
                                net.minecraft.text.Text.literal("[Lipi] Please wait " + remaining + "s before sending another message")
                                        .formatted(net.minecraft.util.Formatting.YELLOW),
                                false
                        );
                        return;
                    }
                }

                // Global slow mode check
                int slowModeSec = config.getSlowModeSeconds();
                if (slowModeSec > 0 && (now - lastGlobalMessage) < slowModeSec * 1000L) {
                    long remaining = (slowModeSec * 1000L - (now - lastGlobalMessage)) / 1000 + 1;
                    sender.sendMessage(
                            net.minecraft.text.Text.literal("[Lipi] Slow mode active. Please wait " + remaining + "s")
                                    .formatted(net.minecraft.util.Formatting.YELLOW),
                            false
                    );
                    return;
                }

                // Update cooldown trackers
                playerCooldowns.put(senderUuid, now);
                lastGlobalMessage = now;

                // Log the message
                if (config.getLogRetentionDays() > 0) {
                    chatLogger.logMessage(
                            senderUuid,
                            senderName,
                            payload.message(),
                            payload.channel()
                    );
                }

                // Create broadcast payload with server-derived identity
                ChatBroadcastPayload broadcast = new ChatBroadcastPayload(
                        senderUuid,
                        senderName,
                        payload.message(),
                        now,
                        payload.channel()
                );

                // Broadcast to all players with Lipi installed
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (ServerPlayNetworking.canSend(player, ChatBroadcastPayload.ID)) {
                        ServerPlayNetworking.send(player, broadcast);
                    }
                }
            });
        });

        // Register admin commands
        LipiCommand.register();

        Lipi.LOGGER.info("Lipi server initialized.");
    }
}
