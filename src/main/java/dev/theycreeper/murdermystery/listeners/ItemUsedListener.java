package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.manager.ZoneCreationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class ItemUsedListener implements Listener {
    private ZoneCreationManager zoneManager;

    public ItemUsedListener(ZoneCreationManager manager) {
        zoneManager = manager;
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent e) {
        if (MurderMystery.getInstance().getZoneCreationManager().isPlayerEditing(e.getPlayer().getUniqueId())) {
            MurderMystery.getInstance().getZoneCreationManager().handleClickEvent(e);
        }
    }
}
