package com.lipi.client.gui;

import com.lipi.client.LipiClient;
import com.lipi.client.LipiMessageStore;
import com.lipi.network.ChatMessagePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dedicated Lipi chat screen — popup with sidebar, message display, and input.
 * Opens via Right Shift keybind, closes with ESC.
 * Does NOT extend or depend on vanilla ChatScreen.
 *
 * Input routing:
 *   - "/" prefix → server command via networkHandler.sendCommand()
 *   - anything else → global Lipi message via ChatMessagePayload packet
 */
public class LipiChatScreen extends Screen {

    // --- Colors ---
    private static final int TEAL = 0x55FFFF;
    private static final int WHITE = 0xFFFFFF;
    private static final int LIGHT_GRAY = 0xBBBBBB;
    private static final int DARK_GRAY = 0x888888;
    private static final int SIDEBAR_BG = 0xE0101018;
    private static final int SIDEBAR_SELECTED = 0xE0252530;
    private static final int INPUT_BG = 0xE0181820;
    private static final int DIVIDER = 0xFF333340;

    // --- Layout constants ---
    private static final int WINDOW_WIDTH = 320;
    private static final int WINDOW_HEIGHT = 240;
    private static final int SIDEBAR_WIDTH = 60;
    private static final int INPUT_HEIGHT = 14;
    private static final int INPUT_PADDING = 4;
    private static final int MESSAGE_PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int MESSAGE_GAP = 4;

    // --- Time formatting ---
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    // --- Widgets ---
    private TextFieldWidget inputField;
    private ChatInputSuggestor commandSuggestor;

    // --- Focus fallback ---
    private boolean initialFocusSet = false;

    // --- Scroll state ---
    private int scrollOffset = 0; // pixels from bottom, 0 = at bottom
    private int lastMessageCount = 0;

    // --- Window bounds (computed in init) ---
    private int winX, winY;

    public LipiChatScreen() {
        super(Text.literal("Lipi Chat"));
    }

    @Override
    protected void init() {
        // Center the window
        this.winX = (this.width - WINDOW_WIDTH) / 2;
        this.winY = (this.height - WINDOW_HEIGHT) / 2;

        // Input field — bottom of main area (right of sidebar)
        int mainX = winX + SIDEBAR_WIDTH + 1; // +1 for divider
        int mainWidth = WINDOW_WIDTH - SIDEBAR_WIDTH - 1;
        int fieldX = mainX + INPUT_PADDING;
        int fieldY = winY + WINDOW_HEIGHT - INPUT_HEIGHT - INPUT_PADDING;
        int fieldWidth = mainWidth - (INPUT_PADDING * 2);

        this.inputField = new TextFieldWidget(
                this.textRenderer,
                fieldX, fieldY,
                fieldWidth, INPUT_HEIGHT,
                Text.literal("Message #general...")
        );
        this.inputField.setMaxLength(256);
        this.inputField.setFocused(true);
        this.inputField.setEditableColor(WHITE);
        this.inputField.setChangedListener(text -> {
            if (commandSuggestor != null) {
                commandSuggestor.refresh();
            }
        });

        this.addSelectableChild(this.inputField);
        this.setInitialFocus(this.inputField);

        // Initialize command suggestor for / commands
        this.commandSuggestor = new ChatInputSuggestor(
                this.client,
                this,
                this.inputField,
                this.textRenderer,
                false,
                false,
                0,
                7,
                true,
                -805306368
        );
        this.commandSuggestor.setWindowActive(true);
        this.commandSuggestor.refresh();

        // Reset focus fallback flag for re-init
        this.initialFocusSet = false;

        // Mark screen as open and messages read
        LipiMessageStore.setScreenOpen(true);
        LipiMessageStore.markAllRead();

        // Track message count for auto-scroll
        this.lastMessageCount = LipiMessageStore.getMessages().size();
    }

    @Override
    public void removed() {
        super.removed();
        LipiMessageStore.setScreenOpen(false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fallback focus — ensure input field is focused on first render
        if (!initialFocusSet) {
            setFocused(inputField);
            inputField.setFocused(true);
            initialFocusSet = true;
        }

        // Dim the background behind the popup
        context.fill(0, 0, this.width, this.height, 0x80000000);

        // Get config opacity for window background
        float opacity = LipiClient.config.getChatBackgroundOpacity();
        int alpha = Math.max(0xC0, (int) (opacity * 255.0f) & 0xFF);
        int windowBg = (alpha << 24) | 0x0C0C14;

        // Draw window background
        context.fill(winX, winY, winX + WINDOW_WIDTH, winY + WINDOW_HEIGHT, windowBg);

        // Draw border
        drawBorder(context, winX, winY, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF444455);

        // --- Sidebar ---
        renderSidebar(context);

        // --- Divider between sidebar and main ---
        context.fill(winX + SIDEBAR_WIDTH, winY, winX + SIDEBAR_WIDTH + 1, winY + WINDOW_HEIGHT, DIVIDER);

        // --- Messages ---
        renderMessages(context);

        // --- Input area background ---
        int mainX = winX + SIDEBAR_WIDTH + 1;
        int mainWidth = WINDOW_WIDTH - SIDEBAR_WIDTH - 1;
        int inputAreaY = winY + WINDOW_HEIGHT - INPUT_HEIGHT - (INPUT_PADDING * 2);
        context.fill(mainX, inputAreaY, mainX + mainWidth, winY + WINDOW_HEIGHT, INPUT_BG);

        // Divider above input
        context.fill(mainX, inputAreaY, mainX + mainWidth, inputAreaY + 1, DIVIDER);

        // --- Render input field ---
        this.inputField.render(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);

        // --- Render command suggestions on top ---
        this.commandSuggestor.render(context, mouseX, mouseY);
    }

    private void renderSidebar(DrawContext context) {
        // Sidebar background
        context.fill(winX, winY, winX + SIDEBAR_WIDTH, winY + WINDOW_HEIGHT, SIDEBAR_BG);

        // "Lipi" header
        String header = "Lipi";
        int headerX = winX + (SIDEBAR_WIDTH - this.textRenderer.getWidth(header)) / 2;
        int headerY = winY + 6;
        context.drawText(this.textRenderer, header, headerX, headerY, TEAL, true);

        // Divider under header
        context.fill(winX + 4, winY + 18, winX + SIDEBAR_WIDTH - 4, winY + 19, DIVIDER);

        // Channel: # general (selected)
        int channelY = winY + 24;
        // Selected highlight
        context.fill(winX + 2, channelY, winX + SIDEBAR_WIDTH - 2, channelY + 14, SIDEBAR_SELECTED);
        String channelText = "# general";
        int channelX = winX + 6;
        context.drawText(this.textRenderer, channelText, channelX, channelY + 3, WHITE, false);
    }

    private void renderMessages(DrawContext context) {
        List<LipiMessageStore.LipiMessage> messages = LipiMessageStore.getMessages();

        // Auto-scroll: if we were at the bottom and new messages arrived, stay at bottom
        if (scrollOffset == 0 && messages.size() > lastMessageCount) {
            // Already at bottom, stay there
        }
        lastMessageCount = messages.size();

        // Define the message viewport
        int mainX = winX + SIDEBAR_WIDTH + 1 + MESSAGE_PADDING;
        int mainRight = winX + WINDOW_WIDTH - MESSAGE_PADDING;
        int msgAreaTop = winY + 4;
        int inputAreaY = winY + WINDOW_HEIGHT - INPUT_HEIGHT - (INPUT_PADDING * 2);
        int msgAreaBottom = inputAreaY - 2;
        int msgAreaHeight = msgAreaBottom - msgAreaTop;

        if (messages.isEmpty()) {
            // Show placeholder text
            String placeholder = "No messages yet...";
            int px = mainX + (mainRight - mainX - this.textRenderer.getWidth(placeholder)) / 2;
            int py = msgAreaTop + msgAreaHeight / 2 - 4;
            context.drawText(this.textRenderer, placeholder, px, py, DARK_GRAY, false);
            return;
        }

        // Calculate total content height
        int maxTextWidth = mainRight - mainX;
        int totalHeight = 0;
        for (LipiMessageStore.LipiMessage msg : messages) {
            totalHeight += LINE_HEIGHT; // sender + time line
            // Wrap message text
            List<String> wrappedLines = wrapText(msg.message(), maxTextWidth);
            totalHeight += wrappedLines.size() * LINE_HEIGHT;
            totalHeight += MESSAGE_GAP;
        }

        // Clamp scrollOffset
        int maxScroll = Math.max(0, totalHeight - msgAreaHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        scrollOffset = Math.max(0, scrollOffset);

        // When at bottom, mark all read
        if (scrollOffset == 0) {
            LipiMessageStore.markAllRead();
        }

        // Enable scissor to clip messages to viewport
        context.enableScissor(mainX - 2, msgAreaTop, mainRight + 2, msgAreaBottom);

        // Render from top; offset = content drawn from (totalHeight - msgAreaHeight - scrollOffset) from top
        int yStart = msgAreaBottom - totalHeight + scrollOffset;

        int y = yStart;
        for (LipiMessageStore.LipiMessage msg : messages) {
            // Sender name (bold) + timestamp
            String senderName = msg.senderName();
            String timeStr;
            if (msg.timestamp() > 0) {
                timeStr = Instant.ofEpochMilli(msg.timestamp())
                        .atZone(ZoneId.systemDefault())
                        .format(TIME_FORMATTER);
            } else {
                timeStr = "";
            }

            // Draw sender name
            context.drawText(this.textRenderer, senderName, mainX, y, TEAL, true);

            // Draw timestamp after sender
            if (!timeStr.isEmpty()) {
                int nameWidth = this.textRenderer.getWidth(senderName);
                context.drawText(this.textRenderer, " " + timeStr, mainX + nameWidth, y, DARK_GRAY, false);
            }
            y += LINE_HEIGHT;

            // Draw wrapped message lines
            List<String> wrappedLines = wrapText(msg.message(), maxTextWidth);
            for (String line : wrappedLines) {
                context.drawText(this.textRenderer, line, mainX, y, LIGHT_GRAY, false);
                y += LINE_HEIGHT;
            }

            y += MESSAGE_GAP;
        }

        context.disableScissor();
    }

    /**
     * Simple word-aware text wrapping.
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();
        for (String word : text.split(" ")) {
            if (currentLine.isEmpty()) {
                currentLine.append(word);
            } else {
                String test = currentLine + " " + word;
                if (this.textRenderer.getWidth(test) > maxWidth) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append(" ").append(word);
                }
            }
            // Handle single words wider than maxWidth
            if (this.textRenderer.getWidth(currentLine.toString()) > maxWidth && lines.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Let suggestor handle scroll first (e.g. navigating suggestion list)
        if (commandSuggestor.mouseScrolled(verticalAmount)) {
            return true;
        }
        // Scroll up = increase offset, scroll down = decrease offset
        this.scrollOffset += (int) (-verticalAmount * LINE_HEIGHT * 3);
        // Clamping happens in renderMessages
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (commandSuggestor.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Let suggestor handle key events first (Tab completion, arrow navigation, etc.)
        if (commandSuggestor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        // Enter key sends the message
        if (keyCode == 257 /* GLFW_KEY_ENTER */ || keyCode == 335 /* GLFW_KEY_KP_ENTER */) {
            String message = this.inputField.getText().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
            this.inputField.setText("");
            // Stay on screen after sending — don't close
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Routes the message based on its prefix.
     * "/" prefix → Minecraft command; plain text → Lipi message.
     */
    private void sendMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (message.startsWith("/")) {
            // Command handling — route through Minecraft's command system
            String command = message.substring(1);
            if (client.player.networkHandler != null) {
                client.player.networkHandler.sendCommand(command);
            }
        } else {
            // Global Lipi message — send via packet (only message + channel)
            ChatMessagePayload payload = new ChatMessagePayload(message, "GLOBAL");
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void applyBlur() {
        // No-op: prevent Minecraft from applying background blur
        // so the game world remains visible through the chat window
    }

    /**
     * Draws a 1-pixel border around a rectangle.
     */
    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);          // top
        context.fill(x, y + h - 1, x + w, y + h, color);  // bottom
        context.fill(x, y, x + 1, y + h, color);           // left
        context.fill(x + w - 1, y, x + w, y + h, color);   // right
    }
}
