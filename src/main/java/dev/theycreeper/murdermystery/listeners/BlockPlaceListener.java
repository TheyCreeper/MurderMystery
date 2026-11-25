package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.manager.CurrentPlayerMenu;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    @EventHandler
    private void onBlockPlaced(BlockPlaceEvent e) {
        if (MurderMystery.getInstance().getZoneCreationManager().isPlayerEditing(e.getPlayer().getUniqueId())
                && e.getBlock().getBlockData().getMaterial() != Material.BARRIER &&
                MurderMystery.getInstance().getZoneCreationManager().getPlayerState(e.getPlayer().getUniqueId()) != CurrentPlayerMenu.offLimitZoneEdit) {
            e.setCancelled(true);
            MurderMystery.getInstance().getZoneCreationManager().addOffLimitBlock(e.getBlock().getLocation());
        }
    }
    @EventHandler
    private void onBlockBreak(BlockBreakEvent e) {
        if (MurderMystery.getInstance().getZoneCreationManager().isPlayerEditing(e.getPlayer().getUniqueId())
                && e.getBlock().getBlockData().getMaterial() != Material.BARRIER) {
            e.setCancelled(true);
            MurderMystery.getInstance().getZoneCreationManager().remOffLimitBlock(e.getBlock().getLocation());
        }
    }
}
