package com.shanebeestudios.stress.api.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main logger for logging messages
 */
@SuppressWarnings("unused")
public class Logger {

    private static final String PREFIX = "&7[&bStress&3Test&bBots&7]";
    private static final String PREFIX_ERROR = "&7[&bStress&3Test&bBots &cERROR&7]&c";
    private static final String PREFIX_WARN = "&7[&bStress&3Test&bBots &eWARN&7]&e";
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f\\d]){6}>");

    @SuppressWarnings("deprecation") // Paper deprecation
    private static String getColString(String string) {
        Matcher matcher = HEX_PATTERN.matcher(string);
        while (matcher.find()) {
            final ChatColor hexColor = ChatColor.of(matcher.group().substring(1, matcher.group().length() - 1));
            final String before = string.substring(0, matcher.start());
            final String after = string.substring(matcher.end());
            string = before + hexColor + after;
            matcher = HEX_PATTERN.matcher(string);
        }
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Log a message to a command sender
     *
     * @param sender  Sender message will go to
     * @param format  Format of message to send
     * @param objects Objects to format into message
     */
    public static void logToSender(CommandSender sender, String format, Object... objects) {
        sender.sendMessage(getColString(PREFIX + " " + String.format(format, objects)));
    }

    private static void log(String prefix, String message) {
        String string = getColString(prefix + " " + message);
        Bukkit.getConsoleSender().sendMessage(string);
    }

    /**
     * Log an error message to console
     *
     * @param format  Format of error message to log
     * @param objects Objects to format into message
     */
    public static void error(String format, Object... objects) {
        log(PREFIX_ERROR, String.format(format, objects));
    }

    /**
     * Print a stacktrace to console
     *
     * @param e Exception to print
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void error(Exception e) {
        e.printStackTrace();
    }

    /**
     * Log a message to console
     *
     * @param format  Format of message to log
     * @param objects Objects to format into message
     */
    public static void info(String format, Object... objects) {
        log(PREFIX, String.format(format, objects));
    }

    /**
     * Log a warning message to console
     *
     * @param format  Format of warning message to log
     * @param objects Objects to format into message
     */
    public static void warn(String format, Object... objects) {
        log(PREFIX_WARN, String.format(format, objects));
    }

}
