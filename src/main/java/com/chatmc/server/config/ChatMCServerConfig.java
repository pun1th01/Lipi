package com.chatmc.server.config;

import com.chatmc.ChatMC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server-side configuration for ChatMC.
 * Stored in config/chatmc-server.toml.
 */
public class ChatMCServerConfig {

    private static final Path CONFIG_PATH = Path.of("config", "chatmc-server.toml");

    /** Number of days to retain log files. 0 = disable logging entirely. Default 30. */
    private int logRetentionDays = 30;

    /** Whether ChatMC is enabled on the server. Default true. */
    private boolean enabled = true;

    public int getLogRetentionDays() {
        return logRetentionDays;
    }

    public void setLogRetentionDays(int days) {
        this.logRetentionDays = Math.max(0, days);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

                switch (key) {
                    case "log-retention-days" -> {
                        try {
                            setLogRetentionDays(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            ChatMC.LOGGER.warn("Invalid log-retention-days value: {}", value);
                        }
                    }
                    case "enabled" -> setEnabled(Boolean.parseBoolean(value));
                }
            }

            ChatMC.LOGGER.info("ChatMC server config loaded. Retention: {} days, Enabled: {}",
                    logRetentionDays, enabled);
        } catch (IOException e) {
            ChatMC.LOGGER.error("Failed to load ChatMC server config", e);
        }
    }

    /**
     * Save current config to disk.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            String content = """
                    # ChatMC Server Configuration
                    
                    # Number of days to retain chat log files. Set to 0 to disable logging entirely.
                    log-retention-days = %d
                    
                    # Whether ChatMC is enabled on this server.
                    enabled = %s
                    """.formatted(logRetentionDays, enabled);

            Files.writeString(CONFIG_PATH, content);
        } catch (IOException e) {
            ChatMC.LOGGER.error("Failed to save ChatMC server config", e);
        }
    }
}
