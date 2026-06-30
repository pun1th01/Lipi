package com.chatmc.server.moderation;

import com.chatmc.ChatMC;
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
 * Manages the in-memory mute list for ChatMC.
 * Persisted to config/chatmc/muted-players.json as a JSON array of UUID strings.
 */
public class MuteManager {

    private static final Path MUTE_FILE = Path.of("config", "chatmc", "muted-players.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Thread-safe set of muted player UUIDs. */
    private final Set<UUID> mutedPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Mutes a player by UUID.
     */
    public void mute(UUID uuid) {
        mutedPlayers.add(uuid);
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
     * Checks if a player is muted.
     */
    public boolean isMuted(UUID uuid) {
        return mutedPlayers.contains(uuid);
    }

    /**
     * Returns an unmodifiable view of all muted player UUIDs.
     */
    public Set<UUID> getMutedPlayers() {
        return Collections.unmodifiableSet(mutedPlayers);
    }

    /**
     * Loads the mute list from disk.
     */
    public void load() {
        try {
            if (!Files.exists(MUTE_FILE)) {
                return;
            }

            String json = Files.readString(MUTE_FILE);
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> uuidStrings = GSON.fromJson(json, listType);

            if (uuidStrings != null) {
                mutedPlayers.clear();
                for (String uuidStr : uuidStrings) {
                    try {
                        mutedPlayers.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException e) {
                        ChatMC.LOGGER.warn("Invalid UUID in mute list: {}", uuidStr);
                    }
                }
            }

            ChatMC.LOGGER.info("Loaded {} muted players.", mutedPlayers.size());
        } catch (IOException e) {
            ChatMC.LOGGER.error("Failed to load muted players list", e);
        }
    }

    /**
     * Saves the mute list to disk.
     */
    public void save() {
        try {
            Files.createDirectories(MUTE_FILE.getParent());

            List<String> uuidStrings = new ArrayList<>();
            for (UUID uuid : mutedPlayers) {
                uuidStrings.add(uuid.toString());
            }

            String json = GSON.toJson(uuidStrings);
            Files.writeString(MUTE_FILE, json);
        } catch (IOException e) {
            ChatMC.LOGGER.error("Failed to save muted players list", e);
        }
    }
}
