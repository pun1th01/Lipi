package com.lipi.client.gui;

import com.lipi.client.LipiClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Renders the [Lipi] indicator on the HUD when Lipi mode is active
 * and the chat screen is open.
 */
public class LipiOverlay {

    private static final int TEAL_COLOR = 0x55FFFF;
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black

    private LipiOverlay() {
        // Utility class
    }

    /**
     * Registers the HUD render callback.
     */
    public static void register() {
        HudRenderCallback.EVENT.register((DrawContext drawContext, RenderTickCounter tickCounter) -> {
            if (!LipiClient.active) return;

            MinecraftClient client = MinecraftClient.getInstance();

            // Only show indicator when chat screen is NOT open (as a persistent reminder)
            // The ChatScreenMixin handles the indicator when chat IS open
            if (client.currentScreen instanceof ChatScreen) return;

            TextRenderer textRenderer = client.textRenderer;
            String indicator = "[Lipi Active]";
            int textWidth = textRenderer.getWidth(indicator);

            int screenWidth = client.getWindow().getScaledWidth();

            // Position: top-center of screen
            int x = (screenWidth - textWidth) / 2;
            int y = 4;

            // Draw background
            drawContext.fill(x - 3, y - 2, x + textWidth + 3, y + 10, BACKGROUND_COLOR);

            // Draw text
            drawContext.drawText(textRenderer, indicator, x, y, TEAL_COLOR, true);
        });
    }
}
