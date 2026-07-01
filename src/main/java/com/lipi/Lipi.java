package com.lipi;

import com.lipi.network.LipiPackets;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared mod entrypoint for Lipi.
 * Registers packet types that are needed on both client and server sides.
 */
public class Lipi implements ModInitializer {

    public static final String MOD_ID = "lipi";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Lipi initializing...");

        // Register all custom network payload types
        LipiPackets.registerAll();

        LOGGER.info("Lipi initialized successfully.");
    }
}
