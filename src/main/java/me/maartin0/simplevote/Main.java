package me.maartin0.simplevote;

import me.maartin0.simplevote.util.Command;
import me.maartin0.simplevote.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Main extends JavaPlugin {

    public static JavaPlugin plugin;
    public static final String permission = "simplevote.manage";
    @Override
    public void onEnable() {
        plugin = this;
        new VoteCommand().register("vote");
        new VoteCommand().register("unvote");
        new VoteManageCommand().register("votes");
        Logger.logInfo("Ready!");
    }

    @Override
    public void onDisable() {
        Logger.logInfo("Disabled");
    }
}

class VoteCommand extends Command {
    @Override
    public boolean onCommand(@NotNull Player player, @NotNull String commandName, @NotNull String[] args) {
        if (args.length == 0) return false;

        OfflinePlayer target = Arrays.stream(Bukkit.getOfflinePlayers()).filter(p -> {
            String name = p.getName();
            if (name == null) return false;
            else return name.equalsIgnoreCase(args[0]);
        }).findFirst().orElse(null);
        if (target == null) {
            Logger.sendPlayerMessage(player, "Player not found!");
            return true;
        }


        VoteBoard board = args.length > 1
                ? VoteBoard.getBoard(args[1])
                : VoteBoard.getBoard();
        if (board == null) {
            Logger.sendPlayerMessage(player, "Voting board not found!");
            return true;
        }

        boolean adding = commandName.equalsIgnoreCase("vote");
        try {
            if (adding) {
                if (player.getUniqueId().equals(target.getUniqueId())) {
                    Logger.sendPlayerMessage(player, "You can't vote for yourself!");
                    return true;
                } else if (board.canVote(player)) {
                    board.addVote(player, target);
                } else {
                    Logger.sendPlayerMessage(player, "You have no remaining votes!");
                    return true;
                }
            } else board.removeVote(player, target);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            Logger.sendPlayerGenericErrorMessage(player);
            return true;
        }

        Logger.sendPlayerMessage(
                player,
                "Successfully %s vote %s %s!\n Your current (%s) votes are: %s\nYou have %s/%s votes remaining".formatted(
                        adding ? "added" : "removed",
                        adding ? "to" : "from",
                        board.getName(),
                        board.getVoterVotes(player).size(),
                        board.getVoterVotes(player)
                                .stream()
                                .map(OfflinePlayer::getName)
                                .collect(Collectors.joining(", ")),
                        board.getRemainingVotes(player),
                        board.getMaxVotes()));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull Player player, @NotNull String commandName, @NotNull String[] args) {
        return args.length < 2
                ? Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).toList()
                : args.length == 2
                ? VoteBoard.getBoardNames().stream().toList()
                : null;
    }
}

class VoteManageCommand extends Command {
    @Override
    public boolean onCommand(@NotNull Player player, @NotNull String commandName, @NotNull String[] args) {
        VoteBoard board = args.length == 0
                ? VoteBoard.getBoard()
                : VoteBoard.getBoard(args[0]);
        if (board == null) {
            if (args.length > 0
                && player.hasPermission(Main.permission)) {
                try {
                    VoteBoard.createBoard(args[0]);
                    Logger.sendPlayerMessage(player, "Successfully created new voting board!");
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                    Logger.sendPlayerGenericErrorMessage(player);
                }
            } else {
                Logger.sendPlayerMessage(player, "Voting board not found!");
            }
            return true;
        }

        if (args.length < 2 || !player.hasPermission(Main.permission)) {
            if (board.enabled()) Logger.sendPlayerMessage(player, "%s votes for %s:\n%s".formatted(
                    board.getAllTotals().values().stream().reduce(Long::sum).orElse(0L),
                    board.getName(),
                    board));
            else Logger.sendPlayerMessage(player, "This voting board is disabled");
            return true;
        }

        try {
            if (args[1].equals("enable")) board.setEnabled(true);
            else if (args[1].equals("disable")) board.setEnabled(false);
            else if (args[1].equals("remove")) board.delete();
            else if (args.length > 3
                    && args[1].equals("config")
                    && args[2].equals("max")) {
                try {
                    board.setMaxVotes(Integer.parseInt(args[3]));
                } catch (NumberFormatException e) {
                    Logger.sendPlayerGenericErrorMessage(player);
                    return true;
                }
            }
            else return false;
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            Logger.sendPlayerGenericErrorMessage(player);
        }

        Logger.sendPlayerMessage(player, "Success!");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull Player player, @NotNull String commandName, @NotNull String[] args) {
        return args.length < 2
                ? VoteBoard.getBoardNames().stream().toList()
                : !player.hasPermission(Main.permission)
                ? null
                : args.length == 2
                ? List.of("<option>", "enable", "disable", "remove", "config")
                : args.length == 3
                ? List.of("<key>", "max")
                : args.length == 4
                ? List.of("<value>")
                : null;
    }
}
