package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.manager.MMGameManager;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartMurderMysteryTest {
    @Test
    void requiresAndStartsTheNamedMap() {
        Player player = mock(Player.class);
        Command command = mock(Command.class);
        MurderMystery plugin = mock(MurderMystery.class);
        MMGameManager games = mock(MMGameManager.class);
        SQLHandler database = mock(SQLHandler.class);
        MurderMysteryZone map = new MurderMysteryZone();
        when(plugin.getGameManager()).thenReturn(games);
        when(database.getZone("house")).thenReturn(map);
        when(games.StartGame(any(), eq(player), eq(map))).thenReturn(true);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            sql.when(SQLHandler::getInstance).thenReturn(database);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(new ArrayList<>(List.of(player)));
            StartMurderMystery executor = new StartMurderMystery();
            assertTrue(executor.onCommand(player, command, "mmstart", new String[0]));
            verifyNoInteractions(database);
            assertTrue(executor.onCommand(player, command, "mmstart", new String[]{"house"}));
            verify(games).StartGame(new ArrayList<>(List.of(player)), player, map);
        }
    }
}
