package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartyCommandTest {
    @Test
    void partyCommandRegistersPAlias() throws InvalidDescriptionException {
        PluginDescriptionFile plugin = new PluginDescriptionFile(
                Objects.requireNonNull(getClass().getResourceAsStream("/plugin.yml")));
        assertEquals(List.of("p"), plugin.getCommands().get("party").get("aliases"));
    }

    @Test
    void commandsInviteAcceptAndLeave() {
        PartyManager parties = new PartyManager();
        PartyCommand executor = new PartyCommand(parties);
        Command command = mock(Command.class);
        Player leader = player("leader");
        Player member = player("member");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("member")).thenReturn(member);
            bukkit.when(() -> Bukkit.getPlayerExact("leader")).thenReturn(leader);
            executor.onCommand(leader, command, "party", new String[]{"invite", "member"});
            executor.onCommand(member, command, "party", new String[]{"accept", "leader"});
            assertSame(parties.getParty(leader), parties.getParty(member));
            executor.onCommand(member, command, "party", new String[]{"leave"});
            assertNull(parties.getParty(member));
            executor.onCommand(leader, command, "party", new String[]{"invite", "missing"});
            verify(leader).sendMessage("Player not found or offline: missing");
        }
    }

    @Test
    void shorthandInvitesEachOnlinePlayerAndContinuesPastMissingPlayers() {
        PartyManager parties = new PartyManager();
        PartyCommand executor = new PartyCommand(parties);
        Command command = mock(Command.class);
        Player leader = player("leader");
        Player first = player("first");
        Player second = player("second");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("first")).thenReturn(first);
            bukkit.when(() -> Bukkit.getPlayerExact("second")).thenReturn(second);
            bukkit.when(() -> Bukkit.getPlayerExact("leader")).thenReturn(leader);

            executor.onCommand(leader, command, "p", new String[]{"first", "missing", "second"});
            verify(leader).sendMessage("Player not found or offline: missing");
            executor.onCommand(first, command, "p", new String[]{"accept", "leader"});
            executor.onCommand(second, command, "party", new String[]{"accept", "leader"});
            assertNotNull(parties.getParty(leader));
            assertSame(parties.getParty(leader), parties.getParty(first));
            assertSame(parties.getParty(leader), parties.getParty(second));
        }
    }

    @Test
    void shorthandInvitesOnePlayerAndPreservesSubcommandPrecedence() {
        PartyManager parties = mock(PartyManager.class);
        PartyCommand executor = new PartyCommand(parties);
        Command command = mock(Command.class);
        Player leader = player("leader");
        Player target = player("target");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("target")).thenReturn(target);
            executor.onCommand(leader, command, "p", new String[]{"target"});
            verify(parties).invite(leader, target);
            executor.onCommand(leader, command, "p", new String[]{"LiSt"});
            verify(parties).list(leader);
            executor.onCommand(leader, command, "p", new String[]{"invite"});
            executor.onCommand(leader, command, "p", new String[]{"list", "target"});
            verifyNoMoreInteractions(parties);
        }
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }
}
