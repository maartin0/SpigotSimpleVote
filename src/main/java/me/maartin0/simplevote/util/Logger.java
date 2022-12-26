package me.maartin0.simplevote.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Logger {
    public static final String prefix = ChatColor.GREEN + "";
    public static void sendPlayerMessage(Player player, String message) {
        player.sendMessage(prefix + message);
    }
    public static void sendPlayerGenericErrorMessage(Player player) {
        sendPlayerMessage(player, "An unknown error occurred.");
    }
    public static void broadcastDebugMessage(String message) {
        Bukkit.broadcastMessage(prefix + "DEBUG - " + message);
    }
    public static void logWarning(String message) {
        Bukkit.getLogger().warning(message);
    }
    public static void logInfo(String message) {
        Bukkit.getLogger().info(message);
    }
}
