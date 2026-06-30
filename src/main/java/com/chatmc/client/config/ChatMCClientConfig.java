package com.chatmc.client.config;

import com.chatmc.ChatMC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Client-side configuration for ChatMC.
 * Stored in config/chatmc-client.toml.
 */
public class ChatMCClientConfig {

    private static final Path CONFIG_PATH = Path.of("config", "chatmc-client.toml");

    /** Alpha of the chat background rectangle (0.0 = fully transparent, 1.0 = opaque). Default 0.5. */
    private float chatBackgroundOpacity = 0.5f;

    public float getChatBackgroundOpacity() {
        return chatBackgroundOpacity;
    }

    public void setChatBackgroundOpacity(float value) {
        this.chatBackgroundOpacity = Math.max(0.0f, Math.min(1.0f, value));
    }

    /**
     * Load config from disk. Creates default file if it doesn't exist.
     */
    public void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }

            List<String> lines = Files.readAllLines(CONFIG_PATH);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                if ("chat-background-opacity".equals(key)) {
                    try {
                        setChatBackgroundOpacity(Float.parseFloat(value));
                    } catch (NumberFormatException e) {
                        ChatMC.LOGGER.warn("Invalid chat-background-opacity value: {}", value);
                    }
                }
            }

            ChatMC.LOGGER.info("ChatMC client config loaded. Opacity: {}", chatBackgroundOpacity);
        } catch (IOException e) {
            ChatMC.LOGGER.error("Failed to load ChatMC client config", e);
        }
    }

    /**
     * Save current config to disk.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            String content = """
                    # ChatMC Client Configuration
                    
                    # Controls the transparency of the chat background (0.0 = transparent, 1.0 = opaque)
                    chat-background-opacity = %s
                    """.formatted(chatBackgroundOpacity);

            Files.writeString(CONFIG_PATH, content);
        } catch (IOException e) {
            ChatMC.LOGGER.error("Failed to save ChatMC client config", e);
        }
    }
}
