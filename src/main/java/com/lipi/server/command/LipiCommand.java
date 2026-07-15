package com.lipi.server.command;

import com.lipi.Lipi;
import com.lipi.server.LipiServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mojang.authlib.GameProfile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin commands for Lipi.
 *
 * /lipi mute <player>              — permanently mutes a player
 * /lipi mute <player> <duration>   — temp mutes (10s, 5m, 1h, 1d)
 * /lipi unmute <player>            — unmutes a player
 * /lipi mutelist                   — lists all muted players with expiry
 * /lipi toggle                     — enables/disables Lipi on the server
 * /lipi log <player>               — shows last 10 messages from a player
 */
public class LipiCommand {

    private static final int OP_PERMISSION_LEVEL = 3;

    private LipiCommand() {
        // Utility class
    }

    /**
     * Registers all /lipi subcommands.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("lipi")
                            .requires(source -> source.hasPermissionLevel(OP_PERMISSION_LEVEL))
                            // /lipi mute <player> — permanent mute
                            .then(CommandManager.literal("mute")
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(LipiCommand::executeMute)
                                            // /lipi mute <player> <duration> — temp mute
                                            .then(CommandManager.argument("duration", StringArgumentType.word())
                                                    .executes(LipiCommand::executeTempMute))))
                            // /lipi unmute <player>
                            .then(CommandManager.literal("unmute")
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(LipiCommand::executeUnmute)))
                            // /lipi mutelist
                            .then(CommandManager.literal("mutelist")
                                    .executes(LipiCommand::executeMuteList))
                            // /lipi toggle
                            .then(CommandManager.literal("toggle")
                                    .executes(LipiCommand::executeToggle))
                            // /lipi log <player>
                            .then(CommandManager.literal("log")
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .executes(LipiCommand::executeLog)))
            );
        });
    }

    private static int executeMute(CommandContext<ServerCommandSource> context) {
        try {
            Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");

            for (GameProfile profile : profiles) {
                LipiServer.muteManager.mute(profile.getId());
                context.getSource().sendFeedback(
                        () -> Text.literal("[Lipi] ")
                                .formatted(Formatting.AQUA)
                                .append(Text.literal("Permanently muted player: " + profile.getName())
                                        .formatted(Formatting.YELLOW)),
                        true
                );
                Lipi.LOGGER.info("Permanently muted player: {} ({})", profile.getName(), profile.getId());
            }

            return profiles.size();
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("[Lipi] Failed to mute player: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeTempMute(CommandContext<ServerCommandSource> context) {
        try {
            Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
            String durationStr = StringArgumentType.getString(context, "duration");

            long durationMs = parseDuration(durationStr);
            if (durationMs <= 0) {
                context.getSource().sendError(
                        Text.literal("[Lipi] Invalid duration: " + durationStr + ". Use format: 10s, 5m, 1h, 1d")
                );
                return 0;
            }

            for (GameProfile profile : profiles) {
                LipiServer.muteManager.mute(profile.getId(), durationMs);
                String humanDuration = formatDuration(durationMs);
                context.getSource().sendFeedback(
                        () -> Text.literal("[Lipi] ")
                                .formatted(Formatting.AQUA)
                                .append(Text.literal("Muted player: " + profile.getName() + " for " + humanDuration)
                                        .formatted(Formatting.YELLOW)),
                        true
                );
                Lipi.LOGGER.info("Temp muted player: {} ({}) for {}", profile.getName(), profile.getId(), humanDuration);
            }

            return profiles.size();
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("[Lipi] Failed to mute player: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeUnmute(CommandContext<ServerCommandSource> context) {
        try {
            Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");

            for (GameProfile profile : profiles) {
                LipiServer.muteManager.unmute(profile.getId());
                context.getSource().sendFeedback(
                        () -> Text.literal("[Lipi] ")
                                .formatted(Formatting.AQUA)
                                .append(Text.literal("Unmuted player: " + profile.getName())
                                        .formatted(Formatting.GREEN)),
                        true
                );
                Lipi.LOGGER.info("Unmuted player: {} ({})", profile.getName(), profile.getId());
            }

            return profiles.size();
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("[Lipi] Failed to unmute player: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeMuteList(CommandContext<ServerCommandSource> context) {
        Map<UUID, Long> muted = LipiServer.muteManager.getMutedPlayers();

        if (muted.isEmpty()) {
            context.getSource().sendFeedback(
                    () -> Text.literal("[Lipi] ")
                            .formatted(Formatting.AQUA)
                            .append(Text.literal("No players are currently muted.")
                                    .formatted(Formatting.GRAY)),
                    false
            );
            return 0;
        }

        context.getSource().sendFeedback(
                () -> Text.literal("[Lipi] ")
                        .formatted(Formatting.AQUA)
                        .append(Text.literal("Muted players (" + muted.size() + "):")
                                .formatted(Formatting.YELLOW)),
                false
        );

        for (Map.Entry<UUID, Long> entry : muted.entrySet()) {
            UUID uuid = entry.getKey();
            long expiry = entry.getValue();

            // Try to get player name from server
            String name = getPlayerName(uuid);
            String expiryText;

            if (expiry == Long.MAX_VALUE) {
                expiryText = "(permanent)";
            } else {
                long remaining = expiry - System.currentTimeMillis();
                if (remaining <= 0) {
                    expiryText = "(expired)";
                } else {
                    expiryText = "(expires in " + formatDuration(remaining) + ")";
                }
            }

            String finalText = "  " + name + " " + expiryText;
            context.getSource().sendFeedback(
                    () -> Text.literal(finalText).formatted(Formatting.GRAY),
                    false
            );
        }

        return muted.size();
    }

    private static int executeToggle(CommandContext<ServerCommandSource> context) {
        boolean newState = !LipiServer.config.isEnabled();
        LipiServer.config.setEnabled(newState);
        LipiServer.config.save();

        String statusText = newState ? "enabled" : "disabled";
        Formatting color = newState ? Formatting.GREEN : Formatting.RED;

        context.getSource().sendFeedback(
                () -> Text.literal("[Lipi] ")
                        .formatted(Formatting.AQUA)
                        .append(Text.literal("Lipi has been " + statusText + ".")
                                .formatted(color)),
                true
        );

        Lipi.LOGGER.info("Lipi toggled: {}", statusText);
        return 1;
    }

    private static int executeLog(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");

        List<String> messages = LipiServer.chatLogger.getPlayerMessages(playerName, 10);

        if (messages.isEmpty()) {
            context.getSource().sendFeedback(
                    () -> Text.literal("[Lipi] ")
                            .formatted(Formatting.AQUA)
                            .append(Text.literal("No messages found for " + playerName + " in today's log.")
                                    .formatted(Formatting.GRAY)),
                    false
            );
        } else {
            context.getSource().sendFeedback(
                    () -> Text.literal("[Lipi] ")
                            .formatted(Formatting.AQUA)
                            .append(Text.literal("Last " + messages.size() + " messages from " + playerName + ":")
                                    .formatted(Formatting.YELLOW)),
                    false
            );

            for (String message : messages) {
                context.getSource().sendFeedback(
                        () -> Text.literal("  " + message).formatted(Formatting.GRAY),
                        false
                );
            }
        }

        return messages.size();
    }

    // --- Utility methods ---

    /**
     * Parses a duration string like "10s", "5m", "1h", "1d" into milliseconds.
     * Returns -1 if the format is invalid.
     */
    private static long parseDuration(String input) {
        if (input == null || input.length() < 2) return -1;

        String numPart = input.substring(0, input.length() - 1);
        char unit = input.charAt(input.length() - 1);

        try {
            long value = Long.parseLong(numPart);
            if (value <= 0) return -1;

            return switch (unit) {
                case 's' -> value * 1000L;
                case 'm' -> value * 60_000L;
                case 'h' -> value * 3_600_000L;
                case 'd' -> value * 86_400_000L;
                default -> -1;
            };
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Formats a duration in milliseconds into a human-readable string.
     */
    private static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";

        long seconds = ms / 1000;
        if (seconds < 60) return seconds + "s";

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";

        long hours = minutes / 60;
        if (hours < 24) {
            long remainingMinutes = minutes % 60;
            if (remainingMinutes > 0) return hours + "h " + remainingMinutes + "m";
            return hours + "h";
        }

        long days = hours / 24;
        long remainingHours = hours % 24;
        if (remainingHours > 0) return days + "d " + remainingHours + "h";
        return days + "d";
    }

    /**
     * Attempts to get a player's name from the server, falls back to UUID string.
     */
    private static String getPlayerName(UUID uuid) {
        if (LipiServer.serverInstance != null) {
            var player = LipiServer.serverInstance.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return uuid.toString();
    }
}
