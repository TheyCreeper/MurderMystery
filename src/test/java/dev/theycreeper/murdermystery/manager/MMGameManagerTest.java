package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.game.PlayerType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MMGameManagerTest {
    @Test
    void innocentWinAnnouncesAndStopsTheMatchingGame() {
        MurderMystery plugin = mock(MurderMystery.class);
        ZoneCreationManager zones = mock(ZoneCreationManager.class);
        MurderMysteryZone map = new MurderMysteryZone();
        Player first = mock(Player.class);
        Player second = mock(Player.class);
        when(first.getUniqueId()).thenReturn(UUID.randomUUID());
        when(second.getUniqueId()).thenReturn(UUID.randomUUID());
        when(plugin.getZoneCreationManager()).thenReturn(zones);
        when(zones.loadMap(map)).thenReturn(true);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedConstruction<MurderMysteryGame> games = mockConstruction(MurderMysteryGame.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            MMGameManager manager = new MMGameManager();
            manager.StartGame(new ArrayList<>(List.of(first, second)), map);
            MurderMysteryGame game = games.constructed().getFirst();
            assertThrows(IllegalStateException.class, () -> manager.innocentsWin(mock(MurderMysteryGame.class)));
            assertTrue(manager.hasActiveGame());
            manager.innocentsWin(game);
            verify(game).innocentsWin();
            verify(game).EndGame();
            verify(zones).unloadMap(map);
            assertFalse(manager.hasActiveGame());
        }
    }

    @Test
    void innocentWinAnnouncesTheOutcomeForEachRole() {
        MurderMysteryGame game = new MurderMysteryGame(new MurderMysteryZone());
        Player innocent = mock(Player.class);
        Player murderer = mock(Player.class);
        game.addPlayer(innocent);
        game.addPlayer(murderer);
        game.getMmPlayer(murderer).setPlayerType(PlayerType.murderer);

        game.innocentsWin();

        verify(innocent).sendTitle(eq(ChatColor.GREEN + "You win!"), anyString(), eq(10), eq(70), eq(20));
        verify(murderer).sendTitle(eq(ChatColor.RED + "You lost!"), anyString(), eq(10), eq(70), eq(20));
    }

    @Test
    void loadsSelectedMapForGameAndUnloadsOnStop() {
        MurderMystery plugin = mock(MurderMystery.class);
        ZoneCreationManager zones = mock(ZoneCreationManager.class);
        MurderMysteryZone map = new MurderMysteryZone();
        Player player = mock(Player.class);
        Player member = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(member.getUniqueId()).thenReturn(UUID.randomUUID());
        when(plugin.getZoneCreationManager()).thenReturn(zones);
        when(zones.loadMap(map)).thenReturn(true);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedConstruction<MurderMysteryGame> games = mockConstruction(MurderMysteryGame.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            MMGameManager manager = new MMGameManager();
            ArrayList<Player> players = new ArrayList<>(List.of(player, member));
            assertTrue(manager.StartGame(players, map));
            assertTrue(manager.hasActiveGame());
            MurderMysteryGame game = games.constructed().getFirst();
            verify(game).addPlayer(player);
            verify(game).addPlayer(member);
            verify(game).setMurderer(any());
            verify(game).setDetective(any());
            verify(game).StartGame();
            assertThrows(IllegalStateException.class, () -> manager.StartGame(players, map));
            manager.StopGame();
            assertFalse(manager.hasActiveGame());
            verify(game).EndGame();
            verify(zones).unloadMap(map);
        }
    }

    @Test
    void obstructionPreventsStartingGame() {
        MurderMystery plugin = mock(MurderMystery.class);
        ZoneCreationManager zones = mock(ZoneCreationManager.class);
        MurderMysteryZone map = new MurderMysteryZone();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(plugin.getZoneCreationManager()).thenReturn(zones);
        when(zones.loadMap(map)).thenReturn(false);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedConstruction<MurderMysteryGame> games = mockConstruction(MurderMysteryGame.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            MMGameManager manager = new MMGameManager();
            assertFalse(manager.StartGame(new ArrayList<>(List.of(player)), map));
            assertFalse(manager.hasActiveGame());
            assertTrue(games.constructed().isEmpty());
            verify(zones, never()).unloadMap(map);
        }
    }

    @Test
    void unloadsMapEvenIfGameCleanupFails() {
        MurderMystery plugin = mock(MurderMystery.class);
        ZoneCreationManager zones = mock(ZoneCreationManager.class);
        MurderMysteryZone map = new MurderMysteryZone();
        Player player = mock(Player.class);
        Player member = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(member.getUniqueId()).thenReturn(UUID.randomUUID());
        when(plugin.getZoneCreationManager()).thenReturn(zones);
        when(zones.loadMap(map)).thenReturn(true);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedConstruction<MurderMysteryGame> games = mockConstruction(MurderMysteryGame.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            MMGameManager manager = new MMGameManager();
            manager.StartGame(new ArrayList<>(List.of(player, member)), map);
            doThrow(new IllegalStateException("cleanup failed")).when(games.constructed().getFirst()).EndGame();
            assertThrows(IllegalStateException.class, manager::StopGame);
            verify(zones).unloadMap(map);
            assertFalse(manager.hasActiveGame());
        }
    }
}
