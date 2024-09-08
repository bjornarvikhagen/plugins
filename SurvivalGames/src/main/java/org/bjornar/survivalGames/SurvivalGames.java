package org.bjornar.survivalGames;

import org.bukkit.plugin.java.JavaPlugin;

public final class SurvivalGames extends JavaPlugin {
    private GameManager gameManager;
    private ChestManager chestManager;
    private EventListeners eventListeners;
    private Commands commands;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        leaderboardManager = new LeaderboardManager(this);
        gameManager = new GameManager(this, leaderboardManager);
        chestManager = new ChestManager();
        eventListeners = new EventListeners(this, gameManager);
        commands = new Commands(this, gameManager, chestManager, leaderboardManager);

        getServer().getPluginManager().registerEvents(eventListeners, this);
        getCommand("start").setExecutor(commands);
        getCommand("stats").setExecutor(commands);
        getCommand("leaderboard").setExecutor(commands);
        getCommand("setborder").setExecutor(commands);
        getCommand("setlobby").setExecutor(commands);
    }

    @Override
    public void onDisable() {
        // Reset the border when the plugin is disabled
        if (gameManager != null) {
            gameManager.resetBorder();
        }
        
        // Additional cleanup if needed
        getLogger().info("SurvivalGames plugin has been disabled.");
    }
}
