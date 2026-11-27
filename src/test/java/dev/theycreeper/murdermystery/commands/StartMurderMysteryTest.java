package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.manager.MMGameManager;
import dev.theycreeper.murdermystery.manager.PartyManager;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartMurderMysteryTest {
    @Test
    void requiresAndStartsTheNamedMap() {
        Player player = mock(Player.class);
        Command command = mock(Command.class);
        MurderMystery plugin = mock(MurderMystery.class);
        MMGameManager games = mock(MMGameManager.class);
        PartyManager parties = new PartyManager();
        SQLHandler database = mock(SQLHandler.class);
        MurderMysteryZone map = new MurderMysteryZone();
        when(plugin.getGameManager()).thenReturn(games);
        when(plugin.getPartyManager()).thenReturn(parties);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(database.getZone("house")).thenReturn(map);
        when(games.StartGame(any(), eq(map))).thenReturn(true);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            sql.when(SQLHandler::getInstance).thenReturn(database);
            StartMurderMystery executor = new StartMurderMystery();
            assertTrue(executor.onCommand(player, command, "mmstart", new String[0]));
            verifyNoInteractions(database);
            assertTrue(executor.onCommand(player, command, "mmstart", new String[]{"house"}));
            verify(games).StartGame(new ArrayList<>(List.of(player)), map);
        }
    }

    @Test
    void startsOnlyPartyMembersAndRejectsNonLeader() {
        Player leader = player("leader");
        Player member = player("member");
        Player outsider = player("outsider");
        PartyManager parties = new PartyManager();
        parties.invite(leader, member);
        parties.accept(member, leader);
        MurderMystery plugin = mock(MurderMystery.class);
        MMGameManager games = mock(MMGameManager.class);
        SQLHandler database = mock(SQLHandler.class);
        MurderMysteryZone map = new MurderMysteryZone();
        when(plugin.getGameManager()).thenReturn(games);
        when(plugin.getPartyManager()).thenReturn(parties);
        when(database.getZone("house")).thenReturn(map);
        when(games.StartGame(any(), eq(map))).thenReturn(true);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            sql.when(SQLHandler::getInstance).thenReturn(database);
            StartMurderMystery executor = new StartMurderMystery();
            Command command = mock(Command.class);
            executor.onCommand(member, command, "mmstart", new String[]{"house"});
            verify(games, never()).StartGame(any(), any());
            verify(member).sendMessage("Could not start game: Only the party leader can start a game");
            executor.onCommand(leader, command, "mmstart", new String[]{"house"});
            verify(games).StartGame(new ArrayList<>(List.of(leader, member)), map);
            assertEquals(List.of(outsider), parties.playersForGame(outsider));
        }
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }
}
