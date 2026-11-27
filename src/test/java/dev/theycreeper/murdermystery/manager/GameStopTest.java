package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.commands.StopMurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameStopTest {
    @Test
    void stopCommandRestoresAllPlayersWithTheirSavedModes() {
        Player first = player();
        Player second = player();
        MurderMystery plugin = mock(MurderMystery.class);
        ZoneCreationManager zones = mock(ZoneCreationManager.class);
        SQLHandler database = mock(SQLHandler.class);
        Server server = mock(Server.class);
        MurderMysteryZone map = new MurderMysteryZone();
        MMGameManager manager = new MMGameManager();
        when(plugin.getGameManager()).thenReturn(manager);
        when(plugin.getCorpseManager()).thenReturn(mock(CorpseManager.class));
        when(plugin.getZoneCreationManager()).thenReturn(zones);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(zones.loadMap(map)).thenReturn(true);
        when(database.getPlayerGamemode(first.getUniqueId())).thenReturn(GameMode.CREATIVE);
        when(database.getPlayerGamemode(second.getUniqueId())).thenReturn(GameMode.SURVIVAL);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<PlayerStateChanger> states = mockStatic(PlayerStateChanger.class);
             MockedConstruction<GoldPeriodicSpawn> spawner = mockConstruction(GoldPeriodicSpawn.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            sql.when(SQLHandler::getInstance).thenReturn(database);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(first, second));
            manager.StartGame(new ArrayList<>(List.of(first, second)), map);
            assertTrue(new StopMurderMystery().onCommand(first, mock(Command.class), "mmstop", new String[0]));
            states.verify(() -> PlayerStateChanger.normalMode(first, GameMode.CREATIVE));
            states.verify(() -> PlayerStateChanger.normalMode(second, GameMode.SURVIVAL));
            verify(first).setAllowFlight(false);
            verify(second).setCollidable(true);
            assertFalse(manager.hasActiveGame());
            verify(zones).unloadMap(map);
        }
    }

    @Test
    void partialStartRestoresOnlyPreparedPlayersAndContinuesAfterFailure() {
        Player first = player();
        Player second = player();
        Player unprepared = player();
        SQLHandler database = mock(SQLHandler.class);
        MurderMystery plugin = mock(MurderMystery.class);
        when(plugin.getCorpseManager()).thenReturn(mock(CorpseManager.class));
        when(database.getPlayerGamemode(first.getUniqueId())).thenReturn(GameMode.CREATIVE);
        when(database.getPlayerGamemode(second.getUniqueId())).thenReturn(GameMode.SURVIVAL);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<PlayerStateChanger> states = mockStatic(PlayerStateChanger.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            sql.when(SQLHandler::getInstance).thenReturn(database);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            MurderMysteryGame game = new MurderMysteryGame(new MurderMysteryZone());
            game.addPlayer(first);
            game.addPlayer(second);
            game.addPlayer(unprepared);
            game.setMurderer(first);
            game.setDetective(second);
            states.when(() -> PlayerStateChanger.normalMode(first, GameMode.CREATIVE))
                    .thenThrow(new IllegalStateException("restore failed"));
            assertThrows(IllegalStateException.class, game::EndGame);
            states.verify(() -> PlayerStateChanger.normalMode(second, GameMode.SURVIVAL));
            verify(database, never()).getPlayerGamemode(unprepared.getUniqueId());
        }
    }

    private Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }
}
