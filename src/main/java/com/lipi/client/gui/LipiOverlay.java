package com.lipi.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Renders the [Lipi] branding indicator on the HUD when the Lipi chat
 * screen is open.
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
            MinecraftClient client = MinecraftClient.getInstance();

            // Only show the [Lipi] indicator when the LipiChatScreen is open
            if (!(client.currentScreen instanceof LipiChatScreen)) return;

            TextRenderer textRenderer = client.textRenderer;
            String indicator = "[Lipi]";
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
