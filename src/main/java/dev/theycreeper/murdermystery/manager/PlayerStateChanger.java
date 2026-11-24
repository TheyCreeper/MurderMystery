package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerStateChanger {
    public static void normalMode(Player player) {
        player.getInventory().clear();
        Inventory backedInv = SQLHandler.getInstance().getStoredInventory(player.getUniqueId());
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, backedInv.getItem(i));
        }
        inv.setHelmet(backedInv.getItem(36));
        inv.setChestplate(backedInv.getItem(37));
        inv.setLeggings(backedInv.getItem(38));
        inv.setBoots(backedInv.getItem(39));
        inv.setItemInOffHand(backedInv.getItem(40));

        player.setFlying(false);
        player.setAllowFlight(false);

        player.setCollidable(true);

        player.showPlayer(MurderMystery.getInstance(), player);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }

    public static void editingMode(Player p) {
        SQLHandler.getInstance().storeInventory(p);
        p.getInventory().clear();
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setCollidable(false);
    }

    public static void PushMainMenu(Player p) {
        Inventory inv = p.getInventory();
        inv.clear();
        //  menu item
        ItemStack stickMenuItem = getCustomItemName(Material.STICK, "Area Selection");
        ItemStack goldMenuItem = getCustomItemName(Material.GOLD_INGOT, "Gold spawn area");
        ItemStack barrierMenuItem = getCustomItemName(Material.BARRIER, "Off-limit zones");

        inv.setItem(0, stickMenuItem);
        inv.setItem(1, goldMenuItem);
        inv.setItem(2, barrierMenuItem);
        inv.setItem(7, getCustomItemName(Material.GREEN_WOOL, "Confirm"));
        inv.setItem(8, getCustomItemName(Material.RED_WOOL, "Cancel"));
    }

    public static void PushLimitMaker(Player p) {
        PushSingleMenuItem(p, Material.BARRIER, "Barrier");
    }

    public static void PushGoldAreaMenu(Player p) {
        PushSingleMenuItem(p, Material.GOLD_INGOT, "Gold spawning areas");
    }

    public static void PushSelectionMenu(Player p) {
        PushSingleMenuItem(p, Material.STICK, "Area selection tool");
    }

    private static void PushSingleMenuItem(Player p, Material material, String name) {
        Inventory inv = p.getInventory();
        inv.clear();

        inv.setItem(0, getCustomItemName(material, name));
        inv.setItem(7, getCustomItemName(Material.GREEN_WOOL, "Confirm"));
        inv.setItem(8, getCustomItemName(Material.RED_WOOL, "Cancel"));
    }

    private static ItemStack getCustomItemName(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }
}
