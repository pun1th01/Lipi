package com.chatmc.client;

import com.chatmc.ChatMC;
import com.chatmc.client.config.ChatMCClientConfig;
import com.chatmc.client.gui.ChatMCOverlay;
import com.chatmc.network.ChatBroadcastPayload;
import com.chatmc.network.ChatHistoryPayload;
import com.chatmc.network.ChatMessagePayload;
import com.chatmc.network.ServerStatusPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Client-side initializer for ChatMC.
 * Handles keybind registration, chat interception, and network receivers.
 */
public class ChatMCClient implements ClientModInitializer {

    /** Whether ChatMC mode is currently active (player is typing in ChatMC mode). */
    public static boolean active = false;

    /** Whether the current server supports ChatMC (has the mod installed). */
    public static boolean serverSupported = false;

    /** Whether ChatMC is enabled on the server. */
    public static boolean serverEnabled = true;

    /** The keybinding to toggle ChatMC mode. */
    private static KeyBinding toggleKey;

    /** Client config instance. */
    public static ChatMCClientConfig config;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void onInitializeClient() {
        ChatMC.LOGGER.info("ChatMC client initializing...");

        // Load client config
        config = new ChatMCClientConfig();
        config.load();

        // Register keybind: Right Shift
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatmc.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.chatmc"
        ));

        // Register overlay renderer
        ChatMCOverlay.register();

        // Handle keybind press each tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (!serverSupported) {
                    // Server doesn't have ChatMC
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.literal("This server does not support ChatMC")
                                        .formatted(Formatting.RED),
                                false
                        );
                    }
                    return;
                }

                if (!serverEnabled) {
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.literal("ChatMC is currently disabled on this server")
                                        .formatted(Formatting.YELLOW),
                                false
                        );
                    }
                    return;
                }

                // Toggle active state
                active = !active;

                if (client.player != null) {
                    if (active) {
                        client.player.sendMessage(
                                Text.literal("[ChatMC] ")
                                        .formatted(Formatting.AQUA)
                                        .append(Text.literal("Chat mode activated. Messages will be sent via ChatMC.")
                                                .formatted(Formatting.GRAY)),
                                false
                        );
                    } else {
                        client.player.sendMessage(
                                Text.literal("[ChatMC] ")
                                        .formatted(Formatting.AQUA)
                                        .append(Text.literal("Chat mode deactivated. Using vanilla chat.")
                                                .formatted(Formatting.GRAY)),
                                false
                        );
                    }
                }
            }
        });

        // Intercept chat messages when ChatMC is active
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!active || !serverSupported || !serverEnabled) {
                return true; // Allow vanilla chat
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return true;

            // Send via ChatMC packet instead
            UUID playerUuid = client.player.getUuid();
            String playerName = client.player.getName().getString();
            long timestamp = System.currentTimeMillis();

            ChatMessagePayload payload = new ChatMessagePayload(
                    playerUuid,
                    playerName,
                    message,
                    timestamp,
                    "GLOBAL"
            );

            ClientPlayNetworking.send(payload);

            // Cancel vanilla chat send
            return false;
        });

        // Register network receivers for S2C packets

        // Server status - confirms ChatMC is available
        ClientPlayNetworking.registerGlobalReceiver(ServerStatusPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                serverSupported = true;
                serverEnabled = payload.active();
                ChatMC.LOGGER.info("Server supports ChatMC. Enabled: {}", serverEnabled);

                MinecraftClient client = context.client();
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("[ChatMC] ")
                                    .formatted(Formatting.AQUA)
                                    .append(Text.literal("Connected! Press Right Shift to toggle ChatMC mode.")
                                            .formatted(Formatting.GRAY)),
                            false
                    );
                }
            });
        });

        // Chat broadcast - incoming messages from other players
        ClientPlayNetworking.registerGlobalReceiver(ChatBroadcastPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.inGameHud == null) return;

                String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(payload.timestamp()));

                // Format: [ChatMC] [HH:mm:ss] playerName: message
                MutableText chatText = Text.literal("")
                        .append(Text.literal("[ChatMC] ").styled(s -> s.withColor(0x55FFFF)))
                        .append(Text.literal("[" + timeStr + "] ").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(payload.playerName()).formatted(Formatting.WHITE))
                        .append(Text.literal(": ").formatted(Formatting.GRAY))
                        .append(Text.literal(payload.message()).formatted(Formatting.WHITE));

                client.inGameHud.getChatHud().addMessage(chatText);
            });
        });

        // Chat history - past messages rendered in italic grey
        ClientPlayNetworking.registerGlobalReceiver(ChatHistoryPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.inGameHud == null) return;

                if (!payload.lines().isEmpty()) {
                    // Header
                    client.inGameHud.getChatHud().addMessage(
                            Text.literal("--- ChatMC History ---")
                                    .styled(s -> s.withColor(0x55FFFF).withItalic(true))
                    );

                    for (String line : payload.lines()) {
                        MutableText historyText = Text.literal(line)
                                .styled(s -> s.withColor(0xAAAAAA).withItalic(true));
                        client.inGameHud.getChatHud().addMessage(historyText);
                    }

                    client.inGameHud.getChatHud().addMessage(
                            Text.literal("--- End History ---")
                                    .styled(s -> s.withColor(0x55FFFF).withItalic(true))
                    );
                }
            });
        });

        // Reset state when disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            active = false;
            serverSupported = false;
            serverEnabled = true;
        });

        ChatMC.LOGGER.info("ChatMC client initialized.");
    }
}
