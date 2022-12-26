package me.maartin0.simplevote;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains data file loading and saving logic
 */
class Data extends YamlConfiguration {
    File file = new File(Main.plugin.getDataFolder(), "data.yml");
    public Data() throws IOException, InvalidConfigurationException {
        super();
        if (file.exists()) load();
        else save();
    }
    public synchronized void reload() throws IOException, InvalidConfigurationException {
        save();
        load();
    }
    public synchronized void load() throws IOException, InvalidConfigurationException {
        this.load(file);
    }
    public synchronized void save() throws IOException {
        save(file);
    }
}


/**
 * A set of votes
 */
public class VoteBoard {
    private static final Data data;

    static {
        try {
            data = new Data();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private ConfigurationSection section;

    /**
     * Creates the {@link VoteBoard} from the supplied {@link ConfigurationSection}
     * @param section The configuration section to load from
     * @return The constructed {@link VoteBoard}
     */
    private static VoteBoard fromConfigurationSection(ConfigurationSection section) {
        VoteBoard board = new VoteBoard();
        board.section = section;
        return board;
    }

    /**
     * @return a {@link Set} of {@link VoteBoard} names
     */
    public static Set<String> getBoardNames() {
        return data.getKeys(false);
    }

    /**
     * Gets the (first) {@link VoteBoard}
     * @return The retrieved {@link VoteBoard} or {@literal null} if no {@link VoteBoard} was found
     */
    @Nullable
    public static VoteBoard getBoard() {
        return (getBoardNames().size() < 1)
                ? null
                : getBoard(getBoardNames().stream().findFirst().orElse(null));
    }

    /**
     * @param name The name of the {@link VoteBoard} (case-sensitive)
     * @return The retrieved {@link VoteBoard} or {@literal null} if no {@link VoteBoard} was found
     */
    @Nullable
    public static VoteBoard getBoard(String name) {
        ConfigurationSection section = data.getConfigurationSection(name);
        return (section == null)
                ? null
                : fromConfigurationSection(section);
    }

    /**
     * Creates a {@link VoteBoard} with the specified name
     * @param name The name of the {@link VoteBoard}
     */
    public static synchronized void createBoard(String name) throws IOException, InvalidConfigurationException {
        if (exists(name)) return;
        ConfigurationSection section = data.createSection(name);
        section.set("enabled", true);
        section.set("max-votes", 1);
        section.createSection("votes");
        data.reload();
    }

    /**
     * Checks if the specified {@link VoteBoard} exists
     * @param name The name of the {@link VoteBoard}
     * @return {@literal true} if the {@link VoteBoard} exists
     */
    public static boolean exists(String name) {
        return getBoard(name) != null;
    }

    @NotNull
    private ConfigurationSection getVotesSection() {
        return Objects.requireNonNull(section.getConfigurationSection("votes"));
    }

    private Map<OfflinePlayer, Set<OfflinePlayer>> getAllVotes() {
        return getVotesSection()
                .getKeys(false)
                .stream()
                .map(UUID::fromString)
                .map(Bukkit::getOfflinePlayer)
                .collect(Collectors.toMap(voter -> voter, this::getVoterVotes));
    }

    /**
     * Retrieves all vote totals
     * @return A {@link Map} of all totals per player
     */
    public Map<OfflinePlayer, Long> getAllTotals() {
        List<OfflinePlayer> votes = getVotes();
        return votes.stream()
                .distinct()
                .collect(Collectors.toMap(vote -> vote,
                        vote -> votes.stream()
                                .filter(player -> playersEqual(vote, player))
                                .count()));
    }

    /**
     * Gets the name of the {@link VoteBoard}
     * @return The {@link VoteBoard}'s name
     */
    public String getName() {
        return section.getName();
    }

    /**
     * Checks if the {@link VoteBoard} is enabled
     * @return {@literal true} if the {@link VoteBoard} is enabled
     */
    public boolean enabled() {
        return section.getBoolean("enabled");
    }

    /**
     * Deletes the {@link VoteBoard}
     */
    public synchronized void delete() throws IOException, InvalidConfigurationException {
        data.set(getName(), null);
        data.reload();
    }

    /**
     * Modifies the enabled state of the {@link VoteBoard}
     * @param enabled Whether the {@link VoteBoard} should be enabled or not
     * @throws IOException I/O Error
     * @throws InvalidConfigurationException File parse error
     */
    public synchronized void setEnabled(boolean enabled) throws IOException, InvalidConfigurationException {
        section.set("enabled", enabled);
        data.reload();
    }

    /**
     * Gets the maximum number of votes per player
     * @return The maximum number of votes per player
     */
    public int getMaxVotes() {
        return section.getInt("max-votes");
    }

    /**
     * Modifies the maximum number of votes for this {@link VoteBoard}
     * @param maxVotes The new number of max votes
     * @throws IOException I/O Error
     * @throws InvalidConfigurationException File parse error
     */
    public synchronized void setMaxVotes(int maxVotes) throws IOException, InvalidConfigurationException {
        section.set("max-votes", maxVotes);
        data.reload();
    }

    /**
     * Gets a player's votes
     * @param voter The voter
     * @return A list of {@link OfflinePlayer}s that the voter has voted for
     */
    public Set<OfflinePlayer> getVoterVotes(OfflinePlayer voter) {
        return getVotesSection()
                .getStringList(voter.getUniqueId().toString())
                .stream()
                .map(UUID::fromString)
                .map(Bukkit::getOfflinePlayer)
                .collect(Collectors.toSet());
    }

    private List<OfflinePlayer> getVotes() {
        return getAllVotes()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
    }

    private static boolean playersEqual(OfflinePlayer p1, OfflinePlayer p2) {
        return p1.getUniqueId().equals(p2.getUniqueId());
    }

    private static boolean containsPlayer(Collection<OfflinePlayer> players, OfflinePlayer player) {
        return players.stream().anyMatch(p -> playersEqual(p, player));
    }

    /**
     * Gets the number of votes towards the specified player
     * @param target The target player
     * @return The number of votes for that player
     */
    public long getVotes(OfflinePlayer target) {
        return getVotes()
                .stream()
                .filter(t -> playersEqual(t, target))
                .count();
    }

    /**
     * Gets the total remaining votes for the specified player
     * @param player The player
     * @return The total votes cast by that player
     */
    public int getTotalVotes(OfflinePlayer player) {
        return getVoterVotes(player).size();
    }

    /**
     * Gets the remaining votes for the specified player
     * @param player The player
     * @return The total remaining votes for that player
     */
    public int getRemainingVotes(OfflinePlayer player) {
        return getMaxVotes() - getTotalVotes(player);
    }

    /**
     * Checks if the specified player can add any additional votes
     * @param player The player
     * @return Whether the player can add any additional votes
     */
    public boolean canVote(OfflinePlayer player) {
        return getRemainingVotes(player) > 0;
    }

    /**
     * Adds a vote for the specified voter and target, if a vote already exists then the vote is ignored
     * @param voter The voting player
     * @param target The player that the voter is voting for
     * @throws IOException I/O Error
     * @throws InvalidConfigurationException File parse error
     */
    public synchronized void addVote(OfflinePlayer voter, OfflinePlayer target) throws IOException, InvalidConfigurationException {
        if (containsPlayer(getVoterVotes(voter), target)) return;
        List<String> votes = getVotesSection().getStringList(voter.getUniqueId().toString());
        votes.add(target.getUniqueId().toString());
        getVotesSection().set(voter.getUniqueId().toString(), votes);
        data.reload();
    }

    /**
     * Removes a vote for the specified voter and target, if no vote exists then the vote is ignored
     * @param voter The voting player
     * @param target The player that the voter is voting for
     * @throws IOException I/O Error
     * @throws InvalidConfigurationException File parse error
     */
    public synchronized void removeVote(OfflinePlayer voter, OfflinePlayer target) throws IOException, InvalidConfigurationException {
        if (!containsPlayer(getVoterVotes(voter), target)) return;
        List<String> votes = getVotesSection().getStringList(voter.getUniqueId().toString());
        votes.remove(target.getUniqueId().toString());
        getVotesSection().set(voter.getUniqueId().toString(), votes);
        data.reload();
    }

    /**
     * @return A string a representation of the current {@link VoteBoard}'s votes
     */
    @Override
    public String toString() {
        return getAllTotals()
                .entrySet()
                .stream()
                .map(e -> "%s: %s".formatted(e.getKey().getName(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }
}
