package com.lipi.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-side in-memory message store for Lipi.
 * All received Lipi messages (broadcast + history) are stored here.
 * LipiChatScreen reads from this, LipiOverlay checks unread count.
 */
public class LipiMessageStore {

    private static final int MAX_MESSAGES = 200;
    private static final List<LipiMessage> messages = new ArrayList<>();
    private static int unreadCount = 0;
    private static boolean screenOpen = false;

    /**
     * A single Lipi message with sender, content, timestamp, and channel.
     */
    public record LipiMessage(
            String senderName,
            String message,
            long timestamp,
            String channel
    ) {}

    /**
     * Adds a message to the store. Increments unread count if the screen is closed.
     */
    public static void addMessage(LipiMessage msg) {
        messages.add(msg);
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
        if (!screenOpen) {
            unreadCount++;
        }
    }

    /**
     * Returns an unmodifiable view of all stored messages.
     */
    public static List<LipiMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Returns the current unread message count.
     */
    public static int getUnreadCount() {
        return unreadCount;
    }

    /**
     * Marks all messages as read, resetting the unread counter.
     */
    public static void markAllRead() {
        unreadCount = 0;
    }

    /**
     * Sets whether the Lipi chat screen is currently open.
     * When open, new messages do not increment the unread counter.
     */
    public static void setScreenOpen(boolean open) {
        screenOpen = open;
    }

    /**
     * Clears all messages and resets the unread counter.
     * Called on disconnect.
     */
    public static void clear() {
        messages.clear();
        unreadCount = 0;
    }
}
