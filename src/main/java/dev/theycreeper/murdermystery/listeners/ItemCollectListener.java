package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.configs.ConfigHandler;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

public class ItemCollectListener implements Listener {
    MurderMysteryGame game;
    public ItemCollectListener(MurderMysteryGame game) {
        this.game = game;
    }

    @EventHandler
    public void onItemCollect(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!MurderMystery.getInstance().getGameManager().isPlayerInGame(p)) return;
        if (event.getItem().getItemStack().getType() == Material.GOLD_INGOT) {
            if (shouldGetBow(p.getInventory())) {
                event.setCancelled(true);
                event.getItem().remove();
                tradeGoldBow(p);
            }
        }
        game.removeGoldFromDropped(event.getItem());
    }

    private boolean shouldGetBow(Inventory inv) {
        return inv.contains(Material.GOLD_INGOT, ConfigHandler.getInstance().getMinGoldForBow() - 1);
    }

    // Call this with 1 to signal inventory of murderer
    private void tradeGoldBow(Player p) {
        tradeGoldBow(p, 0);
    }

    private void tradeGoldBow(Player p, int murdererOffset) {
        Inventory inv = p.getInventory();
        inv.remove(Material.GOLD_INGOT);
        int arrowNumber = getAmount(inv, 3);
        ++arrowNumber;
        inv.setItem(2 + murdererOffset, new ItemStack(Material.BOW, 1));
        inv.setItem(3 + murdererOffset, new ItemStack(Material.ARROW, arrowNumber));
    }

    private int getAmount(Inventory inventory, int index) {
        try {
            return inventory.getItem(index).getAmount();
        } catch (NullPointerException e) {
            return 0;
        }
    }
}
