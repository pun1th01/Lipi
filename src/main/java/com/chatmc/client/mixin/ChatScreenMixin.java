package com.chatmc.client.mixin;

import com.chatmc.client.ChatMCClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ChatScreen to render the [ChatMC] indicator on the chat input bar
 * when ChatMC mode is active.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    private static final int TEAL_COLOR = 0x55FFFF;
    private static final int INDICATOR_BG_COLOR = 0xCC000000; // Semi-transparent black

    /**
     * Injects at the end of render() to draw the [ChatMC] label
     * on the chat input bar when ChatMC mode is active.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void renderChatMCIndicator(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!ChatMCClient.active) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        String indicator = "[ChatMC]";
        int textWidth = textRenderer.getWidth(indicator);

        // Position the indicator to the left of the chat input field
        int x;
        int y;

        if (chatField != null) {
            // Place it just to the right inside the chat field area
            x = chatField.getX() + chatField.getWidth() - textWidth - 4;
            y = chatField.getY() + (chatField.getHeight() - 8) / 2; // Center vertically
        } else {
            // Fallback position
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            x = screenWidth - textWidth - 6;
            y = screenHeight - 14;
        }

        // Draw background behind the indicator
        context.fill(x - 2, y - 2, x + textWidth + 2, y + 10, INDICATOR_BG_COLOR);

        // Draw the [ChatMC] text in teal
        context.drawText(textRenderer, indicator, x, y, TEAL_COLOR, true);
    }
}
