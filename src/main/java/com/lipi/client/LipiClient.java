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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** Pattern to parse history log lines: [timestamp] [CHANNEL] [uuid] playerName: message */
    private static final Pattern HISTORY_PATTERN =
            Pattern.compile("\\[(.+?)\\] \\[(.+?)\\] \\[(.+?)\\] (.+?): (.+)");

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
                                    .append(Text.literal("Press Right Shift to open Lipi chat.")
                                        .formatted(Formatting.GRAY)),
                            false
                    );
                }
            });
        });

        // Chat broadcast — route to LipiMessageStore (NOT vanilla chat)
        ClientPlayNetworking.registerGlobalReceiver(ChatBroadcastPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                LipiMessageStore.addMessage(new LipiMessageStore.LipiMessage(
                        payload.playerName(),
                        payload.message(),
                        payload.timestamp(),
                        payload.channel()
                ));
            });
        });

        // Chat history — parse log lines and route to LipiMessageStore
        ClientPlayNetworking.registerGlobalReceiver(ChatHistoryPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                for (String line : payload.lines()) {
                    Matcher m = HISTORY_PATTERN.matcher(line);
                    if (m.matches()) {
                        String senderName = m.group(4);
                        String message = m.group(5);
                        String channel = m.group(2);
                        // History messages use timestamp 0 — they pre-date the session
                        LipiMessageStore.addMessage(new LipiMessageStore.LipiMessage(
                                senderName, message, 0L, channel
                        ));
                    } else {
                        // Fallback: store raw line with "Server" as sender
                        LipiMessageStore.addMessage(new LipiMessageStore.LipiMessage(
                                "Server", line, 0L, "GLOBAL"
                        ));
                    }
                }
                // History messages shouldn't count as unread
                LipiMessageStore.markAllRead();
            });
        });

        // Reset state when disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverSupported = false;
            serverEnabled = true;
            LipiMessageStore.clear();
        });

        Lipi.LOGGER.info("Lipi client initialized.");
    }
}
