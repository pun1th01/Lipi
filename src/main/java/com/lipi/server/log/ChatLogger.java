package com.lipi.server.log;

import com.lipi.Lipi;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

/**
 * Flat-file chat logger for Lipi.
 * Logs are stored in config/lipi/logs/YYYY-MM-DD.log.
 *
 * Format per line:
 *   [2026-06-27 16:45:12] [GLOBAL] [uuid] playerName: message
 *   [2026-06-27 16:45:12] [JOIN] [uuid] playerName joined Lipi
 *   [2026-06-27 16:45:12] [LEAVE] [uuid] playerName left Lipi
 */
public class ChatLogger {

    private static final Path LOG_DIR = Path.of("config", "lipi", "logs");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ChatLogger() {
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            Lipi.LOGGER.error("Failed to create Lipi log directory", e);
        }
    }

    /**
     * Gets the path to today's log file.
     */
    private Path getTodayLogPath() {
        return LOG_DIR.resolve(LocalDate.now().format(DATE_FORMAT) + ".log");
    }

    /**
     * Appends a line to today's log file.
     */
    private void appendLine(String line) {
        try {
            Path logFile = getTodayLogPath();
            Files.writeString(logFile, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            Lipi.LOGGER.error("Failed to write to Lipi log", e);
        }
    }

    /**
     * Returns the current timestamp formatted for logging.
     */
    private String timestamp() {
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }

    /**
     * Logs a chat message.
     */
    public void logMessage(UUID uuid, String playerName, String message, String channel) {
        String line = String.format("[%s] [%s] [%s] %s: %s",
                timestamp(), channel, uuid, playerName, message);
        appendLine(line);
    }

    /**
     * Logs a player joining Lipi.
     */
    public void logJoin(UUID uuid, String playerName) {
        String line = String.format("[%s] [JOIN] [%s] %s joined Lipi",
                timestamp(), uuid, playerName);
        appendLine(line);
    }

    /**
     * Logs a player leaving Lipi.
     */
    public void logLeave(UUID uuid, String playerName) {
        String line = String.format("[%s] [LEAVE] [%s] %s left Lipi",
                timestamp(), uuid, playerName);
        appendLine(line);
    }

    /**
     * Gets the last N lines from today's log file.
     *
     * @param count Maximum number of lines to return
     * @return List of log lines, most recent last
     */
    public List<String> getLastMessages(int count) {
        Path logFile = getTodayLogPath();
        if (!Files.exists(logFile)) {
            return Collections.emptyList();
        }

        try {
            List<String> allLines = Files.readAllLines(logFile);
            if (allLines.isEmpty()) {
                return Collections.emptyList();
            }

            int start = Math.max(0, allLines.size() - count);
            return new ArrayList<>(allLines.subList(start, allLines.size()));
        } catch (IOException e) {
            Lipi.LOGGER.error("Failed to read Lipi log", e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets the last N messages from a specific player in today's log.
     *
     * @param playerName The player name to filter by
     * @param count      Maximum number of messages to return
     * @return List of matching log lines, most recent last
     */
    public List<String> getPlayerMessages(String playerName, int count) {
        Path logFile = getTodayLogPath();
        if (!Files.exists(logFile)) {
            return Collections.emptyList();
        }

        try {
            List<String> allLines = Files.readAllLines(logFile);
            List<String> filtered = new ArrayList<>();

            // Filter lines that contain the player name in a message context
            // Pattern: ... playerName: message  (for GLOBAL messages)
            String messagePattern = playerName + ": ";

            for (String line : allLines) {
                if (line.contains(messagePattern) && line.contains("[GLOBAL]")) {
                    filtered.add(line);
                }
            }

            // Return last 'count' entries
            int start = Math.max(0, filtered.size() - count);
            return new ArrayList<>(filtered.subList(start, filtered.size()));
        } catch (IOException e) {
            Lipi.LOGGER.error("Failed to read Lipi log for player {}", playerName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Deletes log files older than the specified retention period.
     *
     * @param retentionDays Number of days to retain logs
     */
    public void cleanOldLogs(int retentionDays) {
        if (retentionDays <= 0) return;

        LocalDate cutoffDate = LocalDate.now().minus(retentionDays, ChronoUnit.DAYS);

        try (Stream<Path> files = Files.list(LOG_DIR)) {
            files.filter(path -> {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(".log")) return false;

                String datePart = fileName.replace(".log", "");
                try {
                    LocalDate fileDate = LocalDate.parse(datePart, DATE_FORMAT);
                    return fileDate.isBefore(cutoffDate);
                } catch (Exception e) {
                    return false; // Skip files that don't match the date pattern
                }
            }).forEach(path -> {
                try {
                    Files.delete(path);
                    Lipi.LOGGER.info("Deleted old Lipi log: {}", path.getFileName());
                } catch (IOException e) {
                    Lipi.LOGGER.error("Failed to delete old Lipi log: {}", path.getFileName(), e);
                }
            });
        } catch (IOException e) {
            Lipi.LOGGER.error("Failed to clean old Lipi logs", e);
        }

        Lipi.LOGGER.info("Log retention cleanup complete. Cutoff: {} days ({}).", retentionDays, cutoffDate);
    }
}
