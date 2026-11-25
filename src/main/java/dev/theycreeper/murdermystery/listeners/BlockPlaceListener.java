package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.manager.CurrentPlayerMenu;
import dev.theycreeper.murdermystery.manager.ZoneCreationManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onBlockPlaced(BlockPlaceEvent e) {
        ZoneCreationManager manager = MurderMystery.getInstance().getZoneCreationManager();
        if (manager.isPlayerEditing(e.getPlayer().getUniqueId()) && !isOffLimitBarrier(manager, e.getPlayer(), e.getBlockPlaced())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onBlockBreak(BlockBreakEvent e) {
        ZoneCreationManager manager = MurderMystery.getInstance().getZoneCreationManager();
        if (manager.isPlayerEditing(e.getPlayer().getUniqueId()) && !isOffLimitBarrier(manager, e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void recordBlockPlaced(BlockPlaceEvent e) {
        ZoneCreationManager manager = MurderMystery.getInstance().getZoneCreationManager();
        if (isOffLimitBarrier(manager, e.getPlayer(), e.getBlockPlaced())) {
            manager.addOffLimitBlock(e.getBlockPlaced().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void recordBlockBreak(BlockBreakEvent e) {
        ZoneCreationManager manager = MurderMystery.getInstance().getZoneCreationManager();
        if (isOffLimitBarrier(manager, e.getPlayer(), e.getBlock())) {
            manager.remOffLimitBlock(e.getBlock().getLocation());
        }
    }

    private boolean isOffLimitBarrier(ZoneCreationManager manager, Player player, Block block) {
        return manager.isPlayerEditing(player.getUniqueId())
                && manager.getPlayerState(player.getUniqueId()) == CurrentPlayerMenu.offLimitZoneEdit
                && block.getType() == Material.BARRIER;
    }
}
