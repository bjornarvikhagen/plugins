package org.bjornar.survivalGames;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.List;

public class Commands implements CommandExecutor {
    private final SurvivalGames plugin;
    private final GameManager gameManager;
    private final ChestManager chestManager;
    private final LeaderboardManager leaderboardManager;

    public Commands(SurvivalGames plugin, GameManager gameManager, ChestManager chestManager, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.chestManager = chestManager;
        this.leaderboardManager = leaderboardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("start")) {
            if (gameManager.getGameState() != GameState.WAITING) {
                sender.sendMessage(ChatColor.RED + "A game is already in progress or ending.");
                return true;
            }

            if (gameManager.getSpawnPoints().isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No spawn points set. Set spawn points first.");
                return true;
            }

            if (plugin.getServer().getOnlinePlayers().size() > gameManager.getSpawnPoints().size()) {
                sender.sendMessage(ChatColor.RED + "Not enough spawn points for all players.");
                return true;
            }

            chestManager.clearAndPopulateChests();
            gameManager.startGame();
            return true;
        }

        if (command.getName().equalsIgnoreCase("leaderboard")) {
            List<String> topPlayers = leaderboardManager.getTopPlayers(10);
            sender.sendMessage(ChatColor.GOLD + "=== Survival Games Leaderboard ===");
            for (String playerInfo : topPlayers) {
                sender.sendMessage(playerInfo);
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("setborder")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            Player player = (Player) sender;
            gameManager.giveBorderSetterStick(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("setlobby")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            Player player = (Player) sender;
            gameManager.setLobbyLocation(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Lobby location set to your current position.");
            return true;
        }

        return false;
    }
}