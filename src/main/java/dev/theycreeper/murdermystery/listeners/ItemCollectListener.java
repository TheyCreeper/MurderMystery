package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.configs.ConfigHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

public class ItemCollectListener implements Listener {
    @EventHandler
    public void onItemCollect(InventoryPickupItemEvent event) {
        if (event.getItem() == ItemType.GOLD_INGOT) {
            if (shouldGetBow(event.getInventory())) {
                tradeGoldBow(event.getInventory());
            }
        }
    }

    private boolean shouldGetBow(Inventory inv) {
        ItemStack goldStack = new ItemStack(Material.GOLD_INGOT, ConfigHandler.getInstance().getMinGoldForBow());
        if (inv.contains(goldStack)) {
            return true;
        }
        return false;
    }

    // Call this with 1 to signal inventory of murderer
    private void tradeGoldBow(Inventory inv) {
        tradeGoldBow(inv, 0);
    }

    private void tradeGoldBow(Inventory inv, int murdererOffset) {
        inv.remove(Material.GOLD_INGOT);
        int arrowNumber = inv.getItem(3).getAmount();
        ++arrowNumber;
        inv.setItem(2 + murdererOffset, new ItemStack(Material.BOW, 1));
        inv.setItem(3 + murdererOffset, new ItemStack(Material.ARROW, arrowNumber));
    }
}
