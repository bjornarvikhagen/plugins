package org.bjornar.survivalGames;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ChestManager {
    private final List<ItemStack> possibleItems = Arrays.asList(
        // Weapons and Tools
        new ItemStack(Material.WOODEN_SWORD),
        new ItemStack(Material.STONE_SWORD),
        new ItemStack(Material.IRON_SWORD),
        new ItemStack(Material.BOW),
        new ItemStack(Material.ARROW, 16),
        new ItemStack(Material.FISHING_ROD),
        new ItemStack(Material.FLINT_AND_STEEL),
        new ItemStack(Material.SHIELD),

        // Armor
        new ItemStack(Material.LEATHER_HELMET),
        new ItemStack(Material.LEATHER_CHESTPLATE),
        new ItemStack(Material.LEATHER_LEGGINGS),
        new ItemStack(Material.LEATHER_BOOTS),
        new ItemStack(Material.CHAINMAIL_HELMET),
        new ItemStack(Material.CHAINMAIL_CHESTPLATE),
        new ItemStack(Material.CHAINMAIL_LEGGINGS),
        new ItemStack(Material.CHAINMAIL_BOOTS),
        new ItemStack(Material.IRON_HELMET),
        new ItemStack(Material.IRON_CHESTPLATE),
        new ItemStack(Material.IRON_LEGGINGS),
        new ItemStack(Material.IRON_BOOTS),

        // Food
        new ItemStack(Material.BREAD, 3),
        new ItemStack(Material.APPLE, 3),
        new ItemStack(Material.COOKED_BEEF, 2),
        new ItemStack(Material.COOKED_CHICKEN, 2),
        new ItemStack(Material.COOKED_PORKCHOP, 2),
        new ItemStack(Material.BAKED_POTATO, 3),
        new ItemStack(Material.CARROT, 3),
        new ItemStack(Material.GOLDEN_APPLE),

        // Craftable and Useful Items
        new ItemStack(Material.STICK, 4),
        new ItemStack(Material.COBBLESTONE, 16),
        new ItemStack(Material.IRON_INGOT, 3),
        new ItemStack(Material.GOLD_INGOT, 2),
        new ItemStack(Material.STRING, 3),
        new ItemStack(Material.LEATHER, 3),
        new ItemStack(Material.FEATHER, 3),
        new ItemStack(Material.FLINT, 2),
        new ItemStack(Material.COAL, 4),
        new ItemStack(Material.TORCH, 8),
        new ItemStack(Material.CRAFTING_TABLE),
        new ItemStack(Material.FURNACE)
    );

    public void clearAndPopulateChests() {
        Random random = new Random();

        for (Chunk chunk : Bukkit.getWorld("world").getLoadedChunks()) {
            for (BlockState blockState : chunk.getTileEntities()) {
                if (blockState instanceof Chest) {
                    Chest chest = (Chest) blockState;
                    chest.getInventory().clear();
                    
                    // Add 4-8 random items to each chest
                    int itemCount = random.nextInt(5) + 4;
                    for (int i = 0; i < itemCount; i++) {
                        ItemStack item = possibleItems.get(random.nextInt(possibleItems.size()));
                        int randomSlot = random.nextInt(27); // Chest has 27 slots
                        chest.getInventory().setItem(randomSlot, item.clone());
                    }
                }
            }
        }
    }
}