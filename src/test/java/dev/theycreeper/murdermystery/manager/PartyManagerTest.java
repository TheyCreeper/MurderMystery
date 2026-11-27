package dev.theycreeper.murdermystery.manager;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartyManagerTest {
    @Test
    void invitationsRequireAcceptanceAndOnePartyAtATime() {
        PartyManager parties = new PartyManager();
        Player leader = player("leader");
        Player otherLeader = player("other");
        Player member = player("member");
        parties.invite(leader, member);
        parties.invite(otherLeader, member);
        assertNull(parties.getParty(member));
        parties.accept(member, leader);
        assertEquals(List.of(leader, member), parties.playersForGame(leader));
        assertThrows(IllegalStateException.class, () -> parties.playersForGame(member));
        parties.accept(member, otherLeader);
        assertSame(parties.getParty(leader), parties.getParty(member));
        parties.leave(member);
        parties.accept(member, otherLeader);
        assertNull(parties.getParty(member));
    }

    @Test
    void leadershipTransfersAndPendingInvitesAreCancelledOnLeaveAndDisconnect() {
        PartyManager parties = new PartyManager();
        Player leader = player("leader");
        Player member = player("member");
        Player invitee = player("invitee");
        parties.invite(leader, member);
        parties.accept(member, leader);
        parties.invite(leader, invitee);
        parties.onQuit(quit(leader));
        assertNull(parties.getParty(leader));
        assertEquals(List.of(member), parties.playersForGame(member));
        parties.accept(invitee, member);
        assertNull(parties.getParty(invitee));
        parties.invite(member, invitee);
        parties.accept(invitee, member);
        parties.leave(member);
        assertEquals(List.of(invitee), parties.playersForGame(invitee));
        parties.leave(invitee);
        assertNull(parties.getParty(invitee));
    }

    @Test
    void declineKickDisbandAndQuitClearMembershipAndInvitations() {
        PartyManager parties = new PartyManager();
        Player leader = player("leader");
        Player member = player("member");
        Player invitee = player("invitee");
        parties.invite(leader, member);
        parties.decline(member, leader);
        parties.accept(member, leader);
        assertNull(parties.getParty(member));
        parties.invite(leader, member);
        parties.accept(member, leader);
        parties.kick(member, leader);
        assertSame(parties.getParty(leader), parties.getParty(member));
        parties.kick(leader, member);
        assertNull(parties.getParty(member));
        parties.invite(leader, invitee);
        parties.onQuit(quit(invitee));
        parties.accept(invitee, leader);
        assertNull(parties.getParty(invitee));
        parties.invite(leader, member);
        parties.disband(leader);
        assertNull(parties.getParty(leader));
        parties.accept(member, leader);
        assertNull(parties.getParty(member));
    }

    @Test
    void onlyLeadersCanInviteAndPlayersCannotInviteThemselves() {
        PartyManager parties = new PartyManager();
        Player leader = player("leader");
        Player member = player("member");
        Player invitee = player("invitee");
        parties.invite(leader, leader);
        assertNull(parties.getParty(leader));
        parties.invite(leader, member);
        parties.accept(member, leader);
        parties.invite(member, invitee);
        parties.accept(invitee, leader);
        assertNull(parties.getParty(invitee));
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }

    private PlayerQuitEvent quit(Player player) {
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }
}
