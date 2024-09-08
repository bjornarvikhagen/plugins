package org.bjornar.survivalGames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Random;
import org.bukkit.entity.EntityType;

public class GameManager {
    private final SurvivalGames plugin;
    private boolean countdownActive = false;
    private List<Location> spawnPoints = new ArrayList<>();
    private GameState gameState = GameState.WAITING;
    private List<Player> alivePlayers = new ArrayList<>();
    private WorldBorder worldBorder;
    private Scoreboard scoreboard;
    private Objective objective;
    private boolean gracePeriod = true;
    private int gracePeriodDuration;
    private int supplyDropInterval;
    private Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private FileConfiguration config;
    private double borderRadius;
    private Location borderCenter;
    private Map<String, Kit> kits = new HashMap<>();
    private Random random = new Random();
    private Location borderNorth;
    private Location borderSouth;
    private final LeaderboardManager leaderboardManager;
    private Location lobbyLocation;

    public GameManager(SurvivalGames plugin, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.leaderboardManager = leaderboardManager;
        this.config = plugin.getConfig();
        loadConfig();
        setupScoreboard();
        this.borderRadius = config.getDouble("initial-border-radius", 300);
        this.borderCenter = Bukkit.getWorld("world").getSpawnLocation();
        loadKits();
    }

    private void loadConfig() {
        gracePeriodDuration = config.getInt("grace-period-duration", 30);
        supplyDropInterval = config.getInt("supply-drop-interval", 30);
    }

    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("gameInfo", "dummy", ChatColor.GOLD + "Survival Games");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void addSpawnPoint(Location location) {
        spawnPoints.add(location);
    }

    public List<Location> getSpawnPoints() {
        return spawnPoints;
    }

    public boolean isCountdownActive() {
        return countdownActive;
    }

    public void startGame() {
        if (gameState != GameState.WAITING) {
            return;
        }
        gameState = GameState.STARTING;
        countdownActive = true;
        alivePlayers.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            alivePlayers.add(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20);
        }
        teleportPlayersToSpawnPoints();
        setupWorldBorder();
        startCountdown();
        startGracePeriod();
        scheduleSupplyDrops();
        updateScoreboard();
        for (Player player : alivePlayers) {
            assignRandomKit(player);
        }
        startInGameEvents();
    }

    private void startGracePeriod() {
        gracePeriod = true;
        Bukkit.broadcastMessage(ChatColor.GOLD + "Grace period has started! No PvP for " + gracePeriodDuration + " seconds.");
        new BukkitRunnable() {
            @Override
            public void run() {
                gracePeriod = false;
                Bukkit.broadcastMessage(ChatColor.RED + "Grace period has ended! PvP is now enabled!");
            }
        }.runTaskLater(plugin, gracePeriodDuration * 20L);
    }

    private void scheduleSupplyDrops() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState == GameState.ACTIVE) {
                    spawnSupplyDrop();
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, supplyDropInterval * 20L, supplyDropInterval * 20L);
    }

    private void spawnSupplyDrop() {
        World world = Bukkit.getWorld("world");
        double angle = Math.random() * 2 * Math.PI;
        double r = Math.sqrt(Math.random()) * borderRadius;
        double x = borderCenter.getX() + r * Math.cos(angle);
        double z = borderCenter.getZ() + r * Math.sin(angle);
        Location dropLocation = world.getHighestBlockAt((int)x, (int)z).getLocation().add(0, 1, 0);
        
        world.dropItemNaturally(dropLocation, new ItemStack(Material.CHEST));
        Bukkit.broadcastMessage(ChatColor.GOLD + "A supply drop has appeared at " + formatLocation(dropLocation) + "!");
    }

    private void setupWorldBorder() {
        World world = Bukkit.getWorld("world");
        worldBorder = world.getWorldBorder();
        worldBorder.setCenter(borderCenter);
        worldBorder.setSize(borderRadius * 2); // Set to diameter for visualization
    }

    private void teleportPlayersToSpawnPoints() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);
        Collections.shuffle(spawnPoints);

        for (int i = 0; i < Math.min(players.size(), spawnPoints.size()); i++) {
            Player player = players.get(i);
            Location spawnPoint = spawnPoints.get(i);
            player.teleport(spawnPoint);
            player.sendMessage(ChatColor.GREEN + "Prepare for the game to start!");
        }
    }

    private void startCountdown() {
        new BukkitRunnable() {
            int count = 10;

            @Override
            public void run() {
                if (count > 0) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Game starting in " + count + "...");
                    count--;
                } else {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "The Survival Games have begun!");
                    gameState = GameState.ACTIVE;
                    countdownActive = false;
                    startBorderShrinking();
                    this.cancel();
                }
                updateScoreboard();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startBorderShrinking() {
        double finalRadius = config.getDouble("final-border-radius", 25);
        long shrinkTime = config.getLong("border-shrink-time", 900) * 20; // Convert to ticks
        double shrinkAmount = (borderRadius - finalRadius) / shrinkTime;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState == GameState.ACTIVE && borderRadius > finalRadius) {
                    borderRadius -= shrinkAmount;
                    worldBorder.setSize(borderRadius * 2); // Update visual border
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void eliminatePlayer(Player player) {
        alivePlayers.remove(player);
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " has been eliminated!");
        leaderboardManager.updatePlayerStats(player, false);
        
        if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.get(0);
            endGameWithWinner(winner);
        } else if (alivePlayers.isEmpty()) {
            endGameWithNoWinner();
        }
        
        updateScoreboard();
    }

    private void endGameWithWinner(Player winner) {
        Bukkit.broadcastMessage(ChatColor.GOLD + "Congratulations! " + winner.getName() + " has won the Survival Games!");
        leaderboardManager.updatePlayerStats(winner, true);
        gameState = GameState.ENDED;
        // Reset the game after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                resetGame();
            }
        }.runTaskLater(plugin, 200L);
    }

    private void endGameWithNoWinner() {
        Bukkit.broadcastMessage(ChatColor.GOLD + "The game has ended with no winners!");
        gameState = GameState.ENDED;
        // Reset the game after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                resetGame();
            }
        }.runTaskLater(plugin, 200L);
    }

    private void resetGame() {
        gameState = GameState.WAITING;
        alivePlayers.clear();
        resetBorder();
        // Teleport players back to lobby
        Location lobby = getLobbyLocation();
        if (lobby != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(lobby);
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(20);
            }
        } else {
            plugin.getLogger().warning("Lobby location not set. Players not teleported.");
        }
        updateScoreboard();
    }

    private void updateScoreboard() {
        objective.getScore(ChatColor.WHITE + "Game State:").setScore(15);
        objective.getScore(ChatColor.GREEN + gameState.toString()).setScore(14);
        objective.getScore(ChatColor.WHITE + "Players Alive:").setScore(13);
        objective.getScore(ChatColor.GREEN + String.valueOf(alivePlayers.size())).setScore(12);
        objective.getScore(ChatColor.WHITE + "Border Size:").setScore(11);
        objective.getScore(ChatColor.GREEN + String.format("%.0f", worldBorder.getSize())).setScore(10);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    public String formatLocation(Location loc) {
        return String.format("X: %d, Y: %d, Z: %d", 
                             loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isPlayerAlive(Player player) {
        return alivePlayers.contains(player);
    }

    public boolean isGracePeriod() {
        return gracePeriod;
    }

    public PlayerStats getPlayerStats(Player player) {
        return playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());
    }

    public boolean isInsideBorder(Location location) {
        double dx = location.getX() - borderCenter.getX();
        double dz = location.getZ() - borderCenter.getZ();
        return (dx * dx + dz * dz) <= (borderRadius * borderRadius);
    }

    public void handlePlayerMove(PlayerMoveEvent event) {
        if (gameState == GameState.STARTING && countdownActive) {
            // Prevent all movement during countdown
            event.setCancelled(true);
        } else if (gameState == GameState.ACTIVE) {
            Location to = event.getTo();
            if (to != null && !isInsideBorder(to)) {
                Player player = event.getPlayer();
                Vector direction = borderCenter.toVector().subtract(player.getLocation().toVector()).normalize();
                Location safeLocation = player.getLocation().add(direction);
                event.setTo(safeLocation);
                player.damage(1); // Damage player for being outside the border
                player.sendMessage(ChatColor.RED + "You're outside the border! Move back inside!");
            }
        }
    }

    private void loadKits() {
        kits.put("Warrior", new Kit("Warrior", new ItemStack[]{
            new ItemStack(Material.STONE_SWORD),
            new ItemStack(Material.CHAINMAIL_CHESTPLATE)
        }));
        kits.put("Archer", new Kit("Archer", new ItemStack[]{
            new ItemStack(Material.BOW),
            new ItemStack(Material.ARROW, 16),
            new ItemStack(Material.LEATHER_CHESTPLATE)
        }));
        kits.put("Miner", new Kit("Miner", new ItemStack[]{
            new ItemStack(Material.IRON_PICKAXE),
            new ItemStack(Material.TORCH, 16),
            new ItemStack(Material.COOKED_BEEF, 3)
        }));
    }

    public void assignRandomKit(Player player) {
        Kit randomKit = (Kit) kits.values().toArray()[random.nextInt(kits.size())];
        for (ItemStack item : randomKit.getItems()) {
            player.getInventory().addItem(item);
        }
        player.sendMessage(ChatColor.GREEN + "You've been assigned the " + randomKit.getName() + " kit!");
    }

    private void startInGameEvents() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState == GameState.ACTIVE) {
                    triggerRandomEvent();
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 600L, 600L); // Run every 30 seconds
    }

    private void triggerRandomEvent() {
        switch (random.nextInt(5)) {
            case 0:
                healAllPlayers();
                break;
            case 1:
                giveRandomItems();
                break;
            case 2:
                summonLightning();
                break;
            case 3:
                teleportRandomPlayer();
                break;
            case 4:
                spawnHostileMobs();
                break;
        }
    }

    private void healAllPlayers() {
        for (Player player : alivePlayers) {
            player.setHealth(player.getMaxHealth());
            player.sendMessage(ChatColor.GREEN + "All players have been healed!");
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "A wave of healing energy sweeps across the arena!");
    }

    private void giveRandomItems() {
        ItemStack[] possibleItems = {
            new ItemStack(Material.GOLDEN_APPLE),
            new ItemStack(Material.ARROW, 5),
            new ItemStack(Material.COOKED_BEEF, 3),
            new ItemStack(Material.IRON_INGOT, 2)
        };
        for (Player player : alivePlayers) {
            ItemStack randomItem = possibleItems[random.nextInt(possibleItems.length)];
            player.getInventory().addItem(randomItem);
            player.sendMessage(ChatColor.GREEN + "You received a random item: " + randomItem.getType().name());
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "Random items have been distributed to all players!");
    }

    private void summonLightning() {
        for (Player player : alivePlayers) {
            player.getWorld().strikeLightningEffect(player.getLocation());
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "Lightning strikes around the arena! Watch your step!");
    }

    private void teleportRandomPlayer() {
        if (alivePlayers.size() > 1) {
            Player player = alivePlayers.get(random.nextInt(alivePlayers.size()));
            Location newLocation = getRandomLocationInBorder();
            player.teleport(newLocation);
            player.sendMessage(ChatColor.GOLD + "You've been teleported to a new location!");
            Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " has been teleported to a new location!");
        }
    }

    private void spawnHostileMobs() {
        for (Player player : alivePlayers) {
            Location playerLocation = player.getLocation();
            World world = playerLocation.getWorld();
            
            // Spawn hostile mobs around the player
            for (int i = 0; i < 3; i++) {
                Location mobLocation = playerLocation.clone().add(random.nextInt(10) - 5, 0, random.nextInt(10) - 5);
                world.spawnEntity(mobLocation, EntityType.ZOMBIE);
            }
        }
        Bukkit.broadcastMessage(ChatColor.RED + "Hostile mobs have spawned near players!");
    }

    private Location getRandomLocationInBorder() {
        double angle = Math.random() * 2 * Math.PI;
        double r = Math.sqrt(Math.random()) * borderRadius;
        double x = borderCenter.getX() + r * Math.cos(angle);
        double z = borderCenter.getZ() + r * Math.sin(angle);
        return Bukkit.getWorld("world").getHighestBlockAt((int)x, (int)z).getLocation().add(0, 1, 0);
    }

    public List<Player> getAlivePlayers() {
        return new ArrayList<>(alivePlayers);
    }

    public void setBorderEnd(Location location, boolean isNorth) {
        if (isNorth) {
            borderNorth = location;
        } else {
            borderSouth = location;
        }
        
        if (borderNorth != null && borderSouth != null) {
            calculateAndSetBorder();
        }
    }

    private void calculateAndSetBorder() {
        borderCenter = borderNorth.clone().add(borderSouth).multiply(0.5);
        borderRadius = borderNorth.distance(borderSouth) / 2;
        
        setupWorldBorder();
        Bukkit.broadcastMessage(ChatColor.GREEN + "Border has been set! Center: " + 
                                formatLocation(borderCenter) + ", Radius: " + String.format("%.1f", borderRadius));
    }

    public void giveBorderSetterStick(Player player) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Border Setter");
        stick.setItemMeta(meta);
        player.getInventory().addItem(stick);
        player.sendMessage(ChatColor.GREEN + "You've been given the Border Setter stick. Right-click to set north end, left-click to set south end.");
    }

    public void resetBorder() {
        World world = Bukkit.getWorld("world");
        if (world != null) {
            WorldBorder border = world.getWorldBorder();
            border.setSize(6000000); // Reset to default Minecraft world border size
            border.setCenter(0, 0);  // Reset to default center
        }
        // Reset our custom border variables
        this.borderRadius = config.getDouble("initial-border-radius", 300);
        this.borderCenter = Bukkit.getWorld("world").getSpawnLocation();
        this.borderNorth = null;
        this.borderSouth = null;
    }

    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
        // Save the lobby location to config
        config.set("lobby.world", location.getWorld().getName());
        config.set("lobby.x", location.getX());
        config.set("lobby.y", location.getY());
        config.set("lobby.z", location.getZ());
        config.set("lobby.yaw", location.getYaw());
        config.set("lobby.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public Location getLobbyLocation() {
        if (lobbyLocation == null) {
            // Load from config if not set
            String worldName = config.getString("lobby.world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                double x = config.getDouble("lobby.x");
                double y = config.getDouble("lobby.y");
                double z = config.getDouble("lobby.z");
                float yaw = (float) config.getDouble("lobby.yaw");
                float pitch = (float) config.getDouble("lobby.pitch");
                lobbyLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }
        return lobbyLocation;
    }
}

enum GameState {
    WAITING, STARTING, ACTIVE, ENDED
}

class PlayerStats {
    int gamesPlayed = 0;
    int gamesWon = 0;
}

class Kit {
    private String name;
    private ItemStack[] items;

    public Kit(String name, ItemStack[] items) {
        this.name = name;
        this.items = items;
    }

    public String getName() { return name; }
    public ItemStack[] getItems() { return items; }
}