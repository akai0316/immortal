package com.bird.immortal.message;

import com.bird.immortal.config.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class MessageManager {
    private static FileConfiguration config;
    
    public static void initialize() {
        config = ConfigManager.getConfig();
    }
    
    public static String getMessage(String path) {
        String message = config.getString("messages." + path, "Message not found: " + path);
        return translateColorCodes(message);
    }
    
    public static String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return translateColorCodes(message);
    }
    
    public static Map<String, String> createPlaceholders() {
        return new HashMap<>();
    }
    
    public static String getFormattedMessage(String path, Map<String, Object> values) {
        Map<String, String> placeholders = createPlaceholders();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() instanceof Number) {
                placeholders.put(entry.getKey(), formatNumber((Number) entry.getValue()));
            } else {
                placeholders.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return getMessage(path, placeholders);
    }
    
    private static String formatNumber(Number number) {
        double value = number.doubleValue();
        if (value < 1000) {
            return String.valueOf(value);
        } else if (value < 1000000) {
            return String.format("%.1f", value / 1000.0) + "K";
        } else if (value < 1000000000) {
            return String.format("%.1f", value / 1000000.0) + "M";
        } else {
            return String.format("%.1f", value / 1000000000.0) + "B";
        }
    }
    
    private static String translateColorCodes(String message) {
        if (message == null) return "";
        message = message.replaceAll("^\\s+", "").replaceAll("\\n\\s+", "\n");
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static List<String> getMultilineMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        return Arrays.asList(message.split("(&r|\\n)"));
    }
} 