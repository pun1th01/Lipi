package com.lipi.client.gui;

import com.lipi.Lipi;
import com.lipi.client.LipiClient;
import com.lipi.network.ChatMessagePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Dedicated Lipi chat input screen.
 * Opens via Right Shift keybind, closes with ESC.
 * Does NOT extend or depend on vanilla ChatScreen.
 *
 * Input routing:
 *   - "/" prefix (except /w and /lipi) → server command via networkHandler.sendCommand()
 *   - "/w " prefix → vanilla whisper via sendCommand("w ...")
 *   - "/lipi " prefix → admin command via sendCommand("lipi ...")
 *   - anything else → global Lipi message via ChatMessagePayload packet
 */
public class LipiChatScreen extends Screen {

    private static final int TEAL_COLOR = 0x55FFFF;
    private static final int INPUT_HEIGHT = 12;
    private static final int INPUT_PADDING = 4;

    private TextFieldWidget inputField;

    public LipiChatScreen() {
        super(Text.literal("Lipi Chat"));
    }

    @Override
    protected void init() {
        // Calculate input field dimensions — full width at the bottom of screen
        int fieldY = this.height - INPUT_HEIGHT - INPUT_PADDING;
        int fieldX = INPUT_PADDING;
        int fieldWidth = this.width - (INPUT_PADDING * 2);

        this.inputField = new TextFieldWidget(
                this.textRenderer,
                fieldX, fieldY,
                fieldWidth, INPUT_HEIGHT,
                Text.literal("Chat...")
        );
        this.inputField.setMaxLength(256);
        this.inputField.setFocused(true);
        this.inputField.setEditableColor(0xFFFFFF);

        this.addSelectableChild(this.inputField);
        this.setInitialFocus(this.inputField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw transparent background using config opacity
        float opacity = LipiClient.config.getChatBackgroundOpacity();
        int alpha = (int) (opacity * 255.0f) & 0xFF;
        int bgColor = (alpha << 24); // Black with configured alpha

        context.fill(0, 0, this.width, this.height, bgColor);

        // Draw [Lipi] label to the left of the input field
        if (this.inputField != null) {
            String label = "[Lipi]";
            int labelWidth = this.textRenderer.getWidth(label);
            int labelX = this.inputField.getX();
            int labelY = this.inputField.getY() - 12;

            // Background behind label
            context.fill(
                    labelX - 2, labelY - 2,
                    labelX + labelWidth + 2, labelY + 10,
                    0xCC000000
            );

            context.drawText(this.textRenderer, label, labelX, labelY, TEAL_COLOR, true);
        }

        // Render the input field
        this.inputField.render(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter key sends the message
        if (keyCode == 257 /* GLFW_KEY_ENTER */ || keyCode == 335 /* GLFW_KEY_KP_ENTER */) {
            String message = this.inputField.getText().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Routes the message based on its prefix.
     */
    private void sendMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (message.startsWith("/")) {
            // Command handling — route through Minecraft's command system
            // This preserves server-side permission checks
            String command = message.substring(1); // Strip the leading "/"

            if (client.player.networkHandler != null) {
                client.player.networkHandler.sendCommand(command);
            }
        } else {
            // Global Lipi message — send via packet
            UUID playerUuid = client.player.getUuid();
            String playerName = client.player.getName().getString();
            long timestamp = System.currentTimeMillis();

            ChatMessagePayload payload = new ChatMessagePayload(
                    playerUuid,
                    playerName,
                    message,
                    timestamp,
                    "GLOBAL"
            );

            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
