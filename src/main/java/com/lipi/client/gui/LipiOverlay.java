package com.lipi.client.gui;

import com.lipi.client.LipiClient;
import com.lipi.client.LipiMessageStore;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * Persistent HUD overlay for Lipi.
 * Shows an "L" indicator on the middle-right of the screen during normal gameplay.
 * Displays an unread message badge when there are new messages.
 * Hidden when LipiChatScreen is open (the screen handles its own display).
 */
public class LipiOverlay {

    private static final int TEAL = 0x55FFFF;
    private static final int BADGE_BG = 0xFFCC3333;  // Red badge background
    private static final int BADGE_TEXT = 0xFFFFFFFF; // White badge text
    private static final int INDICATOR_BG = 0xC0101018; // Dark semi-transparent

    private LipiOverlay() {
        // Utility class
    }

    /**
     * Registers the HUD render callback.
     */
    public static void register() {
        HudLayerRegistrationCallback.EVENT.register((drawer) -> {
            drawer.attachLayerAfter(
                    IdentifiedLayer.CHAT,
                    IdentifiedLayer.of(Identifier.of("lipi", "overlay"), LipiOverlay::renderOverlay)
            );
        });
    }

    private static void renderOverlay(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Don't show when chat screen is open — it has its own display
        if (client.currentScreen instanceof LipiChatScreen) return;

        // Don't show on servers without Lipi
        if (!LipiClient.serverSupported) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position: middle-right of screen
        String indicator = "L";
        int textWidth = textRenderer.getWidth(indicator);
        int x = screenWidth - 20;
        int y = screenHeight / 2;

        // Draw indicator background (rounded-ish pill)
        drawContext.fill(x - 4, y - 3, x + textWidth + 4, y + 11, INDICATOR_BG);

        // Draw "L" text
        drawContext.drawText(textRenderer, indicator, x, y, TEAL, true);

        // Draw unread badge if there are unread messages
        int unread = LipiMessageStore.getUnreadCount();
        if (unread > 0) {
            String badgeText = unread > 9 ? "9+" : String.valueOf(unread);
            int badgeTextWidth = textRenderer.getWidth(badgeText);

            // Position badge above and to the right of the indicator
            int badgeX = x + textWidth - 1;
            int badgeY = y - 8;
            int badgePadding = 2;

            // Badge background (red pill)
            drawContext.fill(
                    badgeX - badgePadding,
                    badgeY - badgePadding,
                    badgeX + badgeTextWidth + badgePadding,
                    badgeY + 8 + badgePadding,
                    BADGE_BG
            );

            // Badge text
            drawContext.drawText(textRenderer, badgeText, badgeX, badgeY, BADGE_TEXT, true);
        }
    }
}
