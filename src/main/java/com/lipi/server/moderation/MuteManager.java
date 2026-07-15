package com.lipi.server.moderation;

import com.lipi.Lipi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the in-memory mute list for Lipi with expiry support.
 * Persisted to config/lipi/muted-players.json as a JSON map of UUID → expiry timestamp.
 * Expiry of Long.MAX_VALUE means permanent mute.
 */
public class MuteManager {

    private static final Path MUTE_FILE = Path.of("config", "lipi", "muted-players.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Thread-safe map of muted player UUIDs to their mute expiry timestamp (epoch millis).
     * Long.MAX_VALUE = permanent mute.
     */
    private final ConcurrentHashMap<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();

    /**
     * Permanently mutes a player by UUID (no expiry).
     */
    public void mute(UUID uuid) {
        mutedPlayers.put(uuid, Long.MAX_VALUE);
        save();
    }

    /**
     * Mutes a player by UUID with a duration in milliseconds.
     *
     * @param uuid       The player's UUID
     * @param durationMs Duration of the mute in milliseconds
     */
    public void mute(UUID uuid, long durationMs) {
        long expiry = System.currentTimeMillis() + durationMs;
        mutedPlayers.put(uuid, expiry);
        save();
    }

    /**
     * Unmutes a player by UUID.
     */
    public void unmute(UUID uuid) {
        mutedPlayers.remove(uuid);
        save();
    }

    /**
     * Checks if a player is currently muted.
     * Automatically unmutes expired entries.
     */
    public boolean isMuted(UUID uuid) {
        Long expiry = mutedPlayers.get(uuid);
        if (expiry == null) return false;

        if (expiry != Long.MAX_VALUE && System.currentTimeMillis() > expiry) {
            // Mute has expired — auto-unmute
            mutedPlayers.remove(uuid);
            save();
            Lipi.LOGGER.info("Auto-unmuted player {} (mute expired)", uuid);
            return false;
        }

        return true;
    }

    /**
     * Returns an unmodifiable view of all muted player UUIDs and their expiry timestamps.
     * Does NOT auto-clean expired entries (use isMuted for that).
     */
    public Map<UUID, Long> getMutedPlayers() {
        return Collections.unmodifiableMap(mutedPlayers);
    }

    /**
     * Loads the mute list from disk.
     * Supports both legacy format (List of UUID strings → permanent mutes)
     * and new format (Map of UUID string → expiry timestamp).
     */
    public void load() {
        try {
            if (!Files.exists(MUTE_FILE)) {
                return;
            }

            String json = Files.readString(MUTE_FILE);
            mutedPlayers.clear();

            // Try new format first: Map<String, Long>
            try {
                Type mapType = new TypeToken<Map<String, Long>>() {}.getType();
                Map<String, Long> entries = GSON.fromJson(json, mapType);
                if (entries != null) {
                    for (Map.Entry<String, Long> entry : entries.entrySet()) {
                        try {
                            mutedPlayers.put(UUID.fromString(entry.getKey()), entry.getValue());
                        } catch (IllegalArgumentException e) {
                            Lipi.LOGGER.warn("Invalid UUID in mute list: {}", entry.getKey());
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback: try legacy format (List<String> → all permanent)
                try {
                    Type listType = new TypeToken<List<String>>() {}.getType();
                    List<String> uuidStrings = GSON.fromJson(json, listType);
                    if (uuidStrings != null) {
                        for (String uuidStr : uuidStrings) {
                            try {
                                mutedPlayers.put(UUID.fromString(uuidStr), Long.MAX_VALUE);
                            } catch (IllegalArgumentException ex) {
                                Lipi.LOGGER.warn("Invalid UUID in mute list: {}", uuidStr);
                            }
                        }
                    }
                } catch (Exception ex) {
                    Lipi.LOGGER.error("Failed to parse muted players file", ex);
                }
            }

            Lipi.LOGGER.info("Loaded {} muted players.", mutedPlayers.size());
        } catch (IOException e) {
            Lipi.LOGGER.error("Failed to load muted players list", e);
        }
    }

    /**
     * Saves the mute list to disk in the new Map format.
     */
    public void save() {
        try {
            Files.createDirectories(MUTE_FILE.getParent());

            Map<String, Long> entries = new LinkedHashMap<>();
            for (Map.Entry<UUID, Long> entry : mutedPlayers.entrySet()) {
                entries.put(entry.getKey().toString(), entry.getValue());
            }

            String json = GSON.toJson(entries);
            Files.writeString(MUTE_FILE, json);
        } catch (IOException e) {
            Lipi.LOGGER.error("Failed to save muted players list", e);
        }
    }
}
