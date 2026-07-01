package com.lipi.client;

import com.lipi.Lipi;
import com.lipi.client.config.LipiClientConfig;
import com.lipi.client.gui.LipiOverlay;
import com.lipi.network.ChatBroadcastPayload;
import com.lipi.network.ChatHistoryPayload;
import com.lipi.network.ChatMessagePayload;
import com.lipi.network.ServerStatusPayload;
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
 * Client-side initializer for Lipi.
 * Handles keybind registration, chat interception, and network receivers.
 */
public class LipiClient implements ClientModInitializer {

    /** Whether Lipi mode is currently active (player is typing in Lipi mode). */
    public static boolean active = false;

    /** Whether the current server supports Lipi (has the mod installed). */
    public static boolean serverSupported = false;

    /** Whether Lipi is enabled on the server. */
    public static boolean serverEnabled = true;

    /** The keybinding to toggle Lipi mode. */
    private static KeyBinding toggleKey;

    /** Client config instance. */
    public static LipiClientConfig config;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void onInitializeClient() {
        Lipi.LOGGER.info("Lipi client initializing...");

        // Load client config
        config = new LipiClientConfig();
        config.load();

        // Register keybind: Right Shift
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lipi.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.lipi"
        ));

        // Register overlay renderer
        LipiOverlay.register();

        // Handle keybind press each tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (!serverSupported) {
                    // Server doesn't have Lipi
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.literal("This server does not support Lipi")
                                        .formatted(Formatting.RED),
                                false
                        );
                    }
                    return;
                }

                if (!serverEnabled) {
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.literal("Lipi is currently disabled on this server")
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
                                Text.literal("[Lipi] ")
                                        .formatted(Formatting.AQUA)
                                        .append(Text.literal("Chat mode activated. Messages will be sent via Lipi.")
                                                .formatted(Formatting.GRAY)),
                                false
                        );
                    } else {
                        client.player.sendMessage(
                                Text.literal("[Lipi] ")
                                        .formatted(Formatting.AQUA)
                                        .append(Text.literal("Chat mode deactivated. Using vanilla chat.")
                                                .formatted(Formatting.GRAY)),
                                false
                        );
                    }
                }
            }
        });

        // Intercept chat messages when Lipi is active
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!active || !serverSupported || !serverEnabled) {
                return true; // Allow vanilla chat
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return true;

            // Send via Lipi packet instead
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

        // Server status - confirms Lipi is available
        ClientPlayNetworking.registerGlobalReceiver(ServerStatusPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                serverSupported = true;
                serverEnabled = payload.active();
                Lipi.LOGGER.info("Server supports Lipi. Enabled: {}", serverEnabled);

                MinecraftClient client = context.client();
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("[Lipi] ")
                                    .formatted(Formatting.AQUA)
                                    .append(Text.literal("Connected! Press Right Shift to toggle Lipi mode.")
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

                // Format: [Lipi] [HH:mm:ss] playerName: message
                MutableText chatText = Text.literal("")
                        .append(Text.literal("[Lipi] ").styled(s -> s.withColor(0x55FFFF)))
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
                            Text.literal("--- Lipi History ---")
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

        Lipi.LOGGER.info("Lipi client initialized.");
    }
}
