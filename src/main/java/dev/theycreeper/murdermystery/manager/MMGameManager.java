package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;

public class MMGameManager implements Listener {
    private final ArrayList<MurderMysteryGameSession> activeGames = new ArrayList<>();

    public record MurderMysteryGameSession(MurderMysteryGame game, MurderMysteryZone map) {
    }

    public boolean StartGame(ArrayList<Player> p, Player murd, MurderMysteryZone map) {
        if (hasActiveGame()) throw new IllegalStateException("A game is already running");
        ZoneCreationManager zones = MurderMystery.getInstance().getZoneCreationManager();
        if (p.stream().anyMatch(player -> zones.isPlayerEditing(player.getUniqueId()))) {
            throw new IllegalStateException("Finish editing maps before starting a game");
        }
        if (!zones.loadMap(map)) return false;
        MurderMysteryGame game = null;
        try {
            game = new MurderMysteryGame();
            for (Player player : p) game.addPlayer(player);
            game.setMurderer(murd);
            game.StartGame();
            activeGames.add(new MurderMysteryGameSession(game, map));
        } catch (RuntimeException e) {
            try {
                if (game != null) game.EndGame();
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            try {
                zones.unloadMap(map);
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
        return true;
    }

    public void StopGame() {
        if (activeGames.isEmpty()) throw new IllegalStateException("No game is running");
        MurderMysteryGameSession session = activeGames.getFirst();
        try {
            session.game().EndGame();
        } finally {
            MurderMystery.getInstance().getZoneCreationManager().unloadMap(session.map());
            activeGames.remove(session);
        }
    }

    public boolean hasActiveGame() {
        return !activeGames.isEmpty();
    }

    public boolean isPlayerInGame(Player p) {
        return activeGames.stream()
                .flatMap(session -> session.game().getPlayerList().stream())
                .anyMatch(mmPlayer ->
                        mmPlayer.getPlayer().getUniqueId().equals(p.getUniqueId()));
    }

    public boolean isPlayerCanKill(Player p) {
        return activeGames.stream().anyMatch(session -> session.game().isPlayerCanKill(p));
    }
}
