package com.lipi.client;

import com.lipi.Lipi;
import com.lipi.client.config.LipiClientConfig;
import com.lipi.client.gui.LipiChatScreen;
import com.lipi.client.gui.LipiOverlay;
import com.lipi.network.ChatBroadcastPayload;
import com.lipi.network.ChatHistoryPayload;
import com.lipi.network.ServerStatusPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Client-side initializer for Lipi.
 * Handles keybind registration, screen opening, and network receivers.
 */
public class LipiClient implements ClientModInitializer {

    /** Whether the current server supports Lipi (has the mod installed). */
    public static boolean serverSupported = false;

    /** Whether Lipi is enabled on the server. */
    public static boolean serverEnabled = true;

    /** The keybinding to open the Lipi chat screen. */
    private static KeyBinding openChatKey;

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

        // Register keybind: Right Shift to open Lipi chat screen
        openChatKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lipi.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.lipi"
        ));

        // Register overlay renderer
        LipiOverlay.register();

        // Handle keybind press each tick — open LipiChatScreen
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openChatKey.wasPressed()) {
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

                // Open the Lipi chat screen
                client.setScreen(new LipiChatScreen());
            }
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
                                    .append(Text.literal("Connected! Press Right Shift to open Lipi chat.")
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
            serverSupported = false;
            serverEnabled = true;
        });

        Lipi.LOGGER.info("Lipi client initialized.");
    }
}
