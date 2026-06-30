package com.chatmc;

import com.chatmc.network.ChatMCPackets;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared mod entrypoint for ChatMC.
 * Registers packet types that are needed on both client and server sides.
 */
public class ChatMC implements ModInitializer {

    public static final String MOD_ID = "chatmc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ChatMC initializing...");

        // Register all custom network payload types
        ChatMCPackets.registerAll();

        LOGGER.info("ChatMC initialized successfully.");
    }
}
