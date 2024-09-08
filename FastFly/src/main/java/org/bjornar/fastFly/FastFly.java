package org.bjornar.fastFly;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastFly extends JavaPlugin {

    private final Map<UUID, Float> originalFlySpeeds = new HashMap<>();
    private final float FAST_FLY_SPEED = 1.0f; // Maximum fly speed

    @Override
    public void onEnable() {
        getLogger().info("FastFly plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Reset fly speeds when the plugin is disabled
        for (UUID playerId : originalFlySpeeds.keySet()) {
            Player player = getServer().getPlayer(playerId);
            if (player != null) {
                player.setFlySpeed(originalFlySpeeds.get(playerId));
            }
        }
        getLogger().info("FastFly plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("fastfly")) {
            if (!player.hasPermission("fastfly.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                player.sendMessage(ChatColor.GREEN + "Flight enabled.");
            }

            if (originalFlySpeeds.containsKey(player.getUniqueId())) {
                // Disable fast fly
                float originalSpeed = originalFlySpeeds.get(player.getUniqueId());
                player.setFlySpeed(originalSpeed);
                originalFlySpeeds.remove(player.getUniqueId());
                player.sendMessage(ChatColor.YELLOW + "Fast fly mode disabled.");
            } else {
                // Enable fast fly
                originalFlySpeeds.put(player.getUniqueId(), player.getFlySpeed());
                player.setFlySpeed(FAST_FLY_SPEED);
                player.sendMessage(ChatColor.GREEN + "Fast fly mode enabled!");
            }
            return true;
        }
        return false;
    }
}