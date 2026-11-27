package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.manager.PlayerStateChanger;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;


public class OnPlayerLeaveListener implements Listener {
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        MurderMysteryGame game = MurderMystery.getInstance().getGameManager().getPlayerGame(event.getPlayer());
        if (game != null) {
            PlayerStateChanger.normalMode(event.getPlayer(), SQLHandler.getInstance().getPlayerGamemode(event.getPlayer().getUniqueId()));
            game.removePlayer(event.getPlayer());
        }
    }
}
