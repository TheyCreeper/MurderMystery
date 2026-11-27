package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;


public class OnPlayerLeaveListener implements Listener {
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        MurderMysteryGame game = MurderMystery.getInstance().getGameManager().getPlayerGame(event.getPlayer());
        if (game != null) {
            game.removePlayer(event.getPlayer());
        }
    }
}
