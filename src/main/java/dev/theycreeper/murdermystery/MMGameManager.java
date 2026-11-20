package dev.theycreeper.murdermystery;

import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.UUID;

public class MMGameManager implements Listener {
    ArrayList<MurderMysteryGame> activeGames = new ArrayList<>();

    public record MurderMysteryGameSession(MurderMysteryGame game) {
    }

    public void StartGame(ArrayList<Player> p, Player murd) {
        MurderMysteryGame game = new MurderMysteryGame();
        activeGames.add(game);
        p.forEach(game::addPlayer);
        game.setMurderer(murd);
        game.StartGame();
    }

    public void StopGame() {
        MurderMysteryGame game = activeGames.get(0);
        game.EndGame();
        activeGames.remove(game);
    }

    public boolean isPlayerInGame(Player p) {
        return activeGames.stream()
                .flatMap(game -> game.getPlayerList().stream())
                .anyMatch(mmPlayer ->
                        mmPlayer.getPlayer().getUniqueId().equals(p.getUniqueId()));
    }

    public boolean isPlayerCanKill(Player p) {
        return activeGames.stream().anyMatch(game -> game.isPlayerCanKill(p));
    }
}
