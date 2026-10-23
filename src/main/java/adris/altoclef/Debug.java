package adris.altoclef;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// TODO: Debug library or use Minecraft's built in debugger
public class Debug {

    private static final int DEBUG_LOG_LEVEL = 0;
    private static final int WARN_LOG_LEVEL = 1;
    private static final int ERROR_LOG_LEVEL = 2;
    /**
     * The level for an answer to something the PLAYER asked for — the result of a command they
     * typed. It sits above the "Important only" threshold on purpose: that setting is about routine
     * narration, and hiding it is the point. Hiding the ANSWER to a command is not.
     *
     * <p>Measured reason this exists: with the default verbosity ("Important only") every
     * {@code logMessage} is console-only, so {@code @get bed 1} with a bed already in the pack ran,
     * found the goal already met, finished on the first tick and printed NOTHING. From the player's
     * side that is identical to the bot ignoring the command and standing still.
     */
    private static final int USER_LOG_LEVEL = 3;

    public static void logInternal(String message) {
        if (canLog(DEBUG_LOG_LEVEL)) {
            System.out.println("ALTO CLEF: " + message);
        }
    }

    public static void logInternal(String format, Object... args) {
        logInternal(String.format(format, args));
    }

    private static String getLogPrefix() {
        AltoClef altoClef = AltoClef.getInstance();
        if (altoClef != null) {
            return altoClef.getModSettings().getChatLogPrefix();
        }
        return "[Alto Clef] ";
    }

    // sendMessage bakes font glyphs, which MUST happen on the render thread. Commands invoked
    // over py4j run on Worker-Main threads: a direct player.sendMessage there throws
    // "Rendersystem called from wrong thread" and ABORTS the caller mid-command (@connect
    // died on its own log line before reaching connectToServer, pon4ikyt ticket 1783254983).
    // client.execute runs inline when already on the render thread, else queues for next tick.
    private static void sendChatOnClientThread(String msg) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.nullToEmpty(msg));
            }
        });
    }

    public static void logMessage(String message, boolean prefix) {
        boolean hasPlayer = Minecraft.getInstance() != null && Minecraft.getInstance().player != null;
        if (hasPlayer && chatEnabled(DEBUG_LOG_LEVEL)) {
            if (prefix) {
                message = "\u00A72\u00A7l\u00A7o" + getLogPrefix() + "\u00A7r" + message;
            }
            sendChatOnClientThread(message);

        } else {
            logInternal(message);
        }
    }

    public static void logMessage(String message) {
        logMessage(message, true);
    }

    public static void logMessage(String format, Object... args) {
        logMessage(String.format(format, args));
    }

    /**
     * A message the player asked for: the result of a command they typed.
     *
     * <p>Sent to chat under "All" and "Important only" — only "Off" silences it — and always written
     * to the log as well, so a headless run keeps the record. See {@link #USER_LOG_LEVEL}.
     */
    public static void logUserMessage(String message) {
        boolean hasPlayer = Minecraft.getInstance() != null && Minecraft.getInstance().player != null;
        if (hasPlayer && chatEnabled(USER_LOG_LEVEL)) {
            sendChatOnClientThread("\u00A72\u00A7l\u00A7o" + getLogPrefix() + "\u00A7r" + message);
        }
        logInternal(message);
    }

    public static void logUserMessage(String format, Object... args) {
        logUserMessage(String.format(format, args));
    }

    public static void logWarning(String message) {
        if (canLog(WARN_LOG_LEVEL)) {
            System.out.println("ALTO CLEF: WARNING: " + message);
        }

        AltoClef altoClef = AltoClef.getInstance();
        if (altoClef != null && !altoClef.getModSettings().shouldHideAllWarningLogs()
                && chatEnabled(WARN_LOG_LEVEL)) {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().player != null) {
                String msg = "\u00A72\u00A7l\u00A7o" + getLogPrefix() + "\u00A7c" + message + "\u00A7r";
                sendChatOnClientThread(msg);

            }
        }
    }

    public static void logWarning(String format, Object... args) {
        logWarning(String.format(format, args));
    }

    public static void logError(String message) {
        String stacktrace = getStack(2);

        if (canLog(ERROR_LOG_LEVEL)) {
            System.err.println(message);
            System.err.println("at:");
            System.err.println(stacktrace);
        }

        if (chatEnabled(ERROR_LOG_LEVEL)
                && Minecraft.getInstance() != null && Minecraft.getInstance().player != null) {
            String msg = "\u00A72\u00A7l\u00A7c" + getLogPrefix() + "[ERROR] " + message + "\nat:\n" + stacktrace + "\u00A7r";
            sendChatOnClientThread(msg);
        }
    }

    public static void logError(String format, Object... args) {
        logError(String.format(format, args));
    }

    public static void logStack() {
        logInternal("STACKTRACE: \n" + getStack(2));
    }

    private static String getStack(int toSkip) {
        StringBuilder stacktrace = new StringBuilder();
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if (toSkip-- <= 0) {
                stacktrace.append(ste.toString()).append("\n");
            }
        }
        return stacktrace.toString();
    }

    /** In-game chat verbosity: "All", "Important only" (warn+error), or "Off". Console log is unaffected. */
    private static boolean chatEnabled(int level) {
        AltoClef altoClef = AltoClef.getInstance();
        if (altoClef == null || altoClef.getModSettings() == null) return true;
        return switch (altoClef.getModSettings().getChatVerbosity()) {
            case "Off" -> false;
            case "Important only" -> level >= WARN_LOG_LEVEL;
            default -> true; // "All"
        };
    }

    private static boolean canLog(int level) {
        if (AltoClef.getInstance() == null || AltoClef.getInstance().getModSettings() == null) return true;

        String enabledLogLevel = AltoClef.getInstance().getModSettings().getLogLevel();

        return switch (enabledLogLevel) {
            case "NONE" -> false;
            case "ALL" -> true;
            case "NORMAL" -> level == WARN_LOG_LEVEL || level == ERROR_LOG_LEVEL;
            case "WARN" -> level == WARN_LOG_LEVEL;
            case "ERROR" -> level == ERROR_LOG_LEVEL;
            default ->
                    // invalid log level, switch to default (NORMAL)
                    level != DEBUG_LOG_LEVEL;
        };

    }

}
