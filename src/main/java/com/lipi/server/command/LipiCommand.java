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

/**
 * Admin commands for Lipi.
 *
 * /lipi mute <player>   — mutes a player (silently drops their packets)
 * /lipi unmute <player>  — unmutes a player
 * /lipi toggle           — enables/disables Lipi on the server
 * /lipi log <player>     — shows last 10 messages from a player in today's log
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
                            .then(CommandManager.literal("mute")
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(LipiCommand::executeMute)))
                            .then(CommandManager.literal("unmute")
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(LipiCommand::executeUnmute)))
                            .then(CommandManager.literal("toggle")
                                    .executes(LipiCommand::executeToggle))
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
                                .append(Text.literal("Muted player: " + profile.getName())
                                        .formatted(Formatting.YELLOW)),
                        true
                );
                Lipi.LOGGER.info("Muted player: {} ({})", profile.getName(), profile.getId());
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
}
