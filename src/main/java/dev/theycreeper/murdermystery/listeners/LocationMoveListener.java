package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class LocationMoveListener implements Listener {
    @EventHandler
    public void onPlayerTouchBow(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (!MurderMystery.getInstance().getGameManager().isPlayerInGame(p)) return;
        if (MurderMystery.getInstance().getGameManager().getPlayerGame(p) instanceof MurderMysteryGame game) {
            if (game.isReturningPlayer(p)) return;
            if (game.isGoingOutOfBounds(event)) {
                event.getPlayer().sendMessage(ChatColor.RED + "You can't move out of bounds!");
                event.setCancelled(true);
            }
            game.giveBowIfLocValid(event.getPlayer());
        }
    }
}
