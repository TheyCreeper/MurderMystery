package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
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
