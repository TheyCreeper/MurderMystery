package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;

import java.util.ArrayList;

public class MMGameManager implements Listener {
    private final ArrayList<MurderMysteryGameSession> activeGames = new ArrayList<>();

    public record MurderMysteryGameSession(MurderMysteryGame game, MurderMysteryZone map) {
    }

    public boolean StartGame(ArrayList<Player> p, MurderMysteryZone map) {
        if (hasActiveGame()) throw new IllegalStateException("A game is already running");
        ZoneCreationManager zones = MurderMystery.getInstance().getZoneCreationManager();
        if (p.stream().anyMatch(player -> zones.isPlayerEditing(player.getUniqueId()))) {
            throw new IllegalStateException("Finish editing maps before starting a game");
        }
        if (!zones.loadMap(map)) return false;
        MurderMysteryGame game = null;
        try {
            game = new MurderMysteryGame(map);
            for (Player player : p) game.addPlayer(player);
            // TODO: make this based on percentages like real MM
            Player murderer = getRandomPlayer(p);
            p.remove(murderer);
            Player detective = getRandomPlayer(p);
            p.remove(detective);
            game.setMurderer(murderer);
            game.setDetective(detective);
            game.setInnocents(p);
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

    public Player getRandomPlayer(ArrayList<Player> p) {
        int playerIndex = (int) (Math.random() * p.size());
        return p.get(playerIndex);
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

    public void innocentsWin(MurderMysteryGame game) {
        if (activeGames.isEmpty() || activeGames.getFirst().game() != game) {
            throw new IllegalStateException("Cannot end a game that is not active");
        }
        game.innocentsWin();
        StopGame();
    }

    public void murdererWin(MurderMysteryGame game) {
        if (activeGames.isEmpty() || activeGames.getFirst().game() != game) {
            throw new IllegalStateException("Cannot end a game that is not active");
        }
        game.murdererWin();
        StopGame();
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

    public MurderMysteryGame getPlayerGame(Player p) {
        return activeGames.stream()
                .filter(session -> session.game().getPlayerList().stream()
                        .anyMatch(mmPlayer ->
                                mmPlayer.getPlayer().getUniqueId().equals(p.getUniqueId())))
                .map(session -> session.game())
                .findFirst()
                .orElse(null);
    }

    public boolean isPlayerCanKill(Player p) {
        return activeGames.stream().anyMatch(session -> session.game().isPlayerCanKill(p));
    }
}
