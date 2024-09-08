package org.bjornar.survivalGames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;

public class EventListeners implements Listener {
    private final SurvivalGames plugin;
    private final GameManager gameManager;

    public EventListeners(SurvivalGames plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = ChatColor.GREEN + "Welcome, " + 
                         ChatColor.YELLOW + event.getPlayer().getName() + 
                         ChatColor.GREEN + " to the Survival Games!";
        Bukkit.broadcastMessage(message);
        
        Player player = event.getPlayer();
        PlayerStats stats = gameManager.getPlayerStats(player);
        player.sendMessage(ChatColor.GOLD + "Your stats: Games played: " + stats.gamesPlayed + ", Games won: " + stats.gamesWon);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        EntityType entityType = event.getEntityType();
        
        switch (entityType) {
            case COW:
            case SHEEP:
            case PIG:
            case CHICKEN:
            case HORSE:
            case DONKEY:
            case MULE:
            case RABBIT:
            case LLAMA:
            case PARROT:
            case TURTLE:
            case FOX:
            case BEE:
                // Allow these animals to spawn
                break;
            default:
                // Cancel spawning for all other entities
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.STICK && item.hasItemMeta() &&
            "Border Setter".equals(ChatColor.stripColor(item.getItemMeta().getDisplayName()))) {
            
            event.setCancelled(true);
            Location clickedLocation = event.getClickedBlock().getLocation();
            
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                gameManager.setBorderEnd(clickedLocation, true);
                player.sendMessage(ChatColor.GREEN + "North end of border set at " + gameManager.formatLocation(clickedLocation));
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                gameManager.setBorderEnd(clickedLocation, false);
                player.sendMessage(ChatColor.GREEN + "South end of border set at " + gameManager.formatLocation(clickedLocation));
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && 
                   event.getItem() != null && 
                   event.getItem().getType() == Material.GOLDEN_HOE) {
            
            Location spawnPoint = event.getClickedBlock().getLocation().add(0, 1, 0);
            gameManager.addSpawnPoint(spawnPoint);
            
            event.getPlayer().sendMessage(ChatColor.GREEN + "Spawn point set at " + 
                                          gameManager.formatLocation(spawnPoint));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (gameManager.getGameState() == GameState.ACTIVE) {
            Player player = event.getEntity();
            if (gameManager.isPlayerAlive(player)) {
                gameManager.eliminatePlayer(player);
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(ChatColor.RED + "You have been eliminated! You are now in spectator mode.");
                player.sendMessage(ChatColor.GRAY + "Sneak to cycle through players to spectate.");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        gameManager.handlePlayerMove(event);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (gameManager.getGameState() == GameState.ACTIVE && gameManager.isGracePeriod()) {
            if (event.getEntity() instanceof Player) {
                Player damaged = (Player) event.getEntity();
                Player damager = null;

                if (event.getDamager() instanceof Player) {
                    damager = (Player) event.getDamager();
                } else if (event.getDamager() instanceof Projectile) {
                    Projectile projectile = (Projectile) event.getDamager();
                    if (projectile.getShooter() instanceof Player) {
                        damager = (Player) projectile.getShooter();
                    }
                }

                if (damager != null) {
                    event.setCancelled(true);
                    damager.sendMessage(ChatColor.RED + "PvP is disabled during the grace period!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR && event.isSneaking()) {
            Player nearestPlayer = getNearestPlayer(player);
            if (nearestPlayer != null) {
                player.setSpectatorTarget(nearestPlayer);
                player.sendMessage(ChatColor.GRAY + "Now spectating: " + nearestPlayer.getName());
            }
        }
    }

    private Player getNearestPlayer(Player spectator) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player player : gameManager.getAlivePlayers()) {
            double distance = player.getLocation().distance(spectator.getLocation());
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}