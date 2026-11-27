package dev.theycreeper.murdermystery.game;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.listeners.LocationMoveListener;
import dev.theycreeper.murdermystery.listeners.OnPlayerLeaveListener;
import dev.theycreeper.murdermystery.manager.CorpseManager;
import dev.theycreeper.murdermystery.manager.MMGameManager;
import dev.theycreeper.murdermystery.manager.PlayerStateChanger;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlayerReturnTest {
    @Test
    void gameEndRestoresInventoryAndLocationDespiteMapBoundaryListener() {
        try (Fixture f = new Fixture()) {
            f.game.setInnocents(new ArrayList<>(List.of(f.player)));
            f.game.EndGame();
            f.states.verify(() -> PlayerStateChanger.normalMode(f.player, GameMode.CREATIVE));
            verify(f.player).teleport(f.savedLocation);
            verify(f.move, never()).setCancelled(true);
            assertFalse(f.game.isReturningPlayer(f.player));
        }
    }

    @Test
    void detectiveQuitRestoresOnceAndDropsBowAtGameLocation() {
        try (Fixture f = new Fixture()) {
            f.game.setDetective(f.player);
            doNothing().when(f.game).createDetectiveBow(any(Location.class));
            PlayerQuitEvent event = mock(PlayerQuitEvent.class);
            when(event.getPlayer()).thenReturn(f.player);
            new OnPlayerLeaveListener().onPlayerLeave(event);

            verify(f.player).teleport(f.savedLocation);
            verify(f.game).createDetectiveBow(f.gameLocation);
            assertNull(f.game.getMmPlayer(f.player));
            f.game.EndGame();
            f.states.verify(() -> PlayerStateChanger.normalMode(f.player, GameMode.CREATIVE), times(1));
            verify(f.player, times(1)).teleport(f.savedLocation);
        }
    }

    @Test
    void murdererQuitRestoresBeforeEndingTheGame() {
        try (Fixture f = new Fixture()) {
            f.game.setMurderer(f.player);
            doAnswer(invocation -> {
                assertNull(f.game.getMmPlayer(f.player));
                verify(f.player).teleport(f.savedLocation);
                return null;
            }).when(f.manager).innocentsWin(f.game);
            f.game.removePlayer(f.player);
            verify(f.manager).innocentsWin(f.game);
        }
    }

    @Test
    void rejectedReturnIsReportedWithoutRestoringInventoryTwice() {
        try (Fixture f = new Fixture()) {
            f.game.setInnocents(new ArrayList<>(List.of(f.player)));
            doReturn(false).when(f.player).teleport(f.savedLocation);
            assertThrows(IllegalStateException.class, () -> f.game.removePlayer(f.player));
            assertNull(f.game.getMmPlayer(f.player));
            assertFalse(f.game.isReturningPlayer(f.player));
            f.game.EndGame();
            f.states.verify(() -> PlayerStateChanger.normalMode(f.player, GameMode.CREATIVE), times(1));
        }
    }

    private static class Fixture implements AutoCloseable {
        final MurderMystery plugin = mock(MurderMystery.class);
        final MMGameManager manager = mock(MMGameManager.class);
        final SQLHandler database = mock(SQLHandler.class);
        final World world = mock(World.class);
        final Player player = mock(Player.class);
        final UUID uuid = UUID.randomUUID();
        final Location savedLocation = new Location(world, -20.5, 65, -4.5, 60, 10);
        final Location gameLocation = new Location(world, 10.5, 70, 10.5);
        final MurderMysteryGame game = spy(new MurderMysteryGame(new MurderMysteryZone()));
        final PlayerMoveEvent move = mock(PlayerMoveEvent.class);
        final MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
        final MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class);
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        final MockedStatic<PlayerStateChanger> states = mockStatic(PlayerStateChanger.class);

        Fixture() {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            sql.when(SQLHandler::getInstance).thenReturn(database);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            ScoreboardManager scoreboards = mock(ScoreboardManager.class);
            when(scoreboards.getNewScoreboard()).thenReturn(mock(Scoreboard.class));
            bukkit.when(Bukkit::getScoreboardManager).thenReturn(scoreboards);
            when(plugin.getGameManager()).thenReturn(manager);
            when(plugin.getCorpseManager()).thenReturn(mock(CorpseManager.class));
            when(player.getUniqueId()).thenReturn(uuid);
            when(player.getLocation()).thenReturn(gameLocation);
            when(database.getPlayerGamemode(uuid)).thenReturn(GameMode.CREATIVE);
            when(database.getPlayerLocation(uuid)).thenReturn(savedLocation);
            when(manager.isPlayerInGame(player)).thenReturn(true);
            when(manager.getPlayerGame(player)).thenReturn(game);
            when(move.getPlayer()).thenReturn(player);
            when(move.getTo()).thenReturn(savedLocation);
            when(player.teleport(any(Location.class))).thenAnswer(invocation -> {
                new LocationMoveListener().onPlayerTouchBow(move);
                return true;
            });
            game.addPlayer(player);
        }

        @Override
        public void close() {
            states.close();
            bukkit.close();
            sql.close();
            instance.close();
        }
    }
}
