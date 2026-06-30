package com.chatmc.server.command;

import com.chatmc.ChatMC;
import com.chatmc.server.ChatMCServer;
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
 * Admin commands for ChatMC.
 *
 * /chatmc mute <player>   — mutes a player (silently drops their packets)
 * /chatmc unmute <player>  — unmutes a player
 * /chatmc toggle           — enables/disables ChatMC on the server
 * /chatmc log <player>     — shows last 10 messages from a player in today's log
 */
public class ChatMCCommand {

    private static final int OP_PERMISSION_LEVEL = 3;

    private ChatMCCommand() {
        // Utility class
    }

    /**
     * Registers all /chatmc subcommands.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("chatmc")
                            .requires(source -> source.hasPermissionLevel(OP_PERMISSION_LEVEL))
                            .then(CommandManager.literal("mute")
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(ChatMCCommand::executeMute)))
                            .then(CommandManager.literal("unmute")
                                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(ChatMCCommand::executeUnmute)))
                            .then(CommandManager.literal("toggle")
                                    .executes(ChatMCCommand::executeToggle))
                            .then(CommandManager.literal("log")
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .executes(ChatMCCommand::executeLog)))
            );
        });
    }

    private static int executeMute(CommandContext<ServerCommandSource> context) {
        try {
            Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");

            for (GameProfile profile : profiles) {
                ChatMCServer.muteManager.mute(profile.getId());
                context.getSource().sendFeedback(
                        () -> Text.literal("[ChatMC] ")
                                .formatted(Formatting.AQUA)
                                .append(Text.literal("Muted player: " + profile.getName())
                                        .formatted(Formatting.YELLOW)),
                        true
                );
                ChatMC.LOGGER.info("Muted player: {} ({})", profile.getName(), profile.getId());
            }

            return profiles.size();
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("[ChatMC] Failed to mute player: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeUnmute(CommandContext<ServerCommandSource> context) {
        try {
            Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");

            for (GameProfile profile : profiles) {
                ChatMCServer.muteManager.unmute(profile.getId());
                context.getSource().sendFeedback(
                        () -> Text.literal("[ChatMC] ")
                                .formatted(Formatting.AQUA)
                                .append(Text.literal("Unmuted player: " + profile.getName())
                                        .formatted(Formatting.GREEN)),
                        true
                );
                ChatMC.LOGGER.info("Unmuted player: {} ({})", profile.getName(), profile.getId());
            }

            return profiles.size();
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("[ChatMC] Failed to unmute player: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeToggle(CommandContext<ServerCommandSource> context) {
        boolean newState = !ChatMCServer.config.isEnabled();
        ChatMCServer.config.setEnabled(newState);
        ChatMCServer.config.save();

        String statusText = newState ? "enabled" : "disabled";
        Formatting color = newState ? Formatting.GREEN : Formatting.RED;

        context.getSource().sendFeedback(
                () -> Text.literal("[ChatMC] ")
                        .formatted(Formatting.AQUA)
                        .append(Text.literal("ChatMC has been " + statusText + ".")
                                .formatted(color)),
                true
        );

        ChatMC.LOGGER.info("ChatMC toggled: {}", statusText);
        return 1;
    }

    private static int executeLog(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");

        List<String> messages = ChatMCServer.chatLogger.getPlayerMessages(playerName, 10);

        if (messages.isEmpty()) {
            context.getSource().sendFeedback(
                    () -> Text.literal("[ChatMC] ")
                            .formatted(Formatting.AQUA)
                            .append(Text.literal("No messages found for " + playerName + " in today's log.")
                                    .formatted(Formatting.GRAY)),
                    false
            );
        } else {
            context.getSource().sendFeedback(
                    () -> Text.literal("[ChatMC] ")
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
