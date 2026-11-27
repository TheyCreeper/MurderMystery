package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartyCommandTest {
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

    private Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }
}
