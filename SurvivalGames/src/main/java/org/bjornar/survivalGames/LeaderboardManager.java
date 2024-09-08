package org.bjornar.survivalGames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LeaderboardManager {
    private final SurvivalGames plugin;
    private final File leaderboardFile;
    private FileConfiguration leaderboardConfig;

    public LeaderboardManager(SurvivalGames plugin) {
        this.plugin = plugin;
        this.leaderboardFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        if (!leaderboardFile.exists()) {
            try {
                leaderboardFile.getParentFile().mkdirs();
                leaderboardFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create leaderboard.yml");
                e.printStackTrace();
            }
        }
        leaderboardConfig = YamlConfiguration.loadConfiguration(leaderboardFile);
    }

    public void updatePlayerStats(Player player, boolean won) {
        String uuid = player.getUniqueId().toString();
        int gamesPlayed = leaderboardConfig.getInt(uuid + ".gamesPlayed", 0) + 1;
        int gamesWon = leaderboardConfig.getInt(uuid + ".gamesWon", 0) + (won ? 1 : 0);

        leaderboardConfig.set(uuid + ".name", player.getName());
        leaderboardConfig.set(uuid + ".gamesPlayed", gamesPlayed);
        leaderboardConfig.set(uuid + ".gamesWon", gamesWon);

        try {
            leaderboardConfig.save(leaderboardFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTopPlayers(int limit) {
        Map<String, Integer> playerWins = new HashMap<>();
        for (String key : leaderboardConfig.getKeys(false)) {
            String playerName = leaderboardConfig.getString(key + ".name");
            int wins = leaderboardConfig.getInt(key + ".gamesWon");
            playerWins.put(playerName, wins);
        }

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(playerWins.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        List<String> topPlayers = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sortedEntries.size()); i++) {
            Map.Entry<String, Integer> entry = sortedEntries.get(i);
            topPlayers.add(ChatColor.GOLD + "" + (i + 1) + ". " + ChatColor.GREEN + entry.getKey() + ": " + ChatColor.YELLOW + entry.getValue() + " wins");
        }

        return topPlayers;
    }
}