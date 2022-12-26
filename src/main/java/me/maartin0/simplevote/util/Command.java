package me.maartin0.simplevote.util;

import me.maartin0.simplevote.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class Command implements CommandExecutor, TabCompleter {
    protected static final String notPlayerError = "You can only use this command as a player!";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(notPlayerError);
            return true;
        }
        return this.onCommand((Player) sender, command.getName(), args);
    }

    abstract public boolean onCommand(@NotNull Player player, @NotNull String commandName, @NotNull String[] args);

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return null;
        return onTabComplete((Player) sender, command.getName(), args);
    }

    @Nullable
    abstract public List<String> onTabComplete(@NotNull Player player, @NotNull String commandName, @NotNull String[] args);

    public void register(String commandName) {
        PluginCommand command = Main.plugin.getCommand(commandName);
        if (command == null) {
            Bukkit.getLogger().warning("Unable to register command with name '" + commandName + "': null");
        } else {
            command.setExecutor(this);
        }
    }
}
