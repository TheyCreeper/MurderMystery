package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.model.Party;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PartyManager implements Listener {
    private final Map<UUID, Party> memberships = new HashMap<>();
    private final Set<Party> parties = new HashSet<>();

    public Party getParty(Player player) {
        return memberships.get(player.getUniqueId());
    }

    public void invite(Player leader, Player invitee) {
        if (leader.getUniqueId().equals(invitee.getUniqueId())) {
            leader.sendMessage("You cannot invite yourself.");
            return;
        }
        Party party = getParty(leader);
        if (party != null && !party.isLeader(leader)) {
            leader.sendMessage("Only the party leader can invite players.");
            return;
        }
        if (getParty(invitee) != null) {
            leader.sendMessage(invitee.getName() + " is already in a party.");
            return;
        }
        if (party == null) {
            party = new Party(leader);
            parties.add(party);
            memberships.put(leader.getUniqueId(), party);
        }
        if (!party.invite(invitee)) {
            leader.sendMessage(invitee.getName() + " already has an invitation to your party.");
            return;
        }
        leader.sendMessage("Invited " + invitee.getName() + " to your party.");
        invitee.sendMessage(leader.getName() + " invited you to their party. Use /party accept "
                + leader.getName() + " or /party decline " + leader.getName() + ".");
    }

    public void accept(Player player, Player leader) {
        if (getParty(player) != null) {
            player.sendMessage("Leave your current party before accepting an invitation.");
            return;
        }
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader) || !party.removeInvitation(player)) {
            player.sendMessage("You do not have an invitation from that party leader.");
            return;
        }
        removeInvitations(player);
        party.addPlayer(player);
        memberships.put(player.getUniqueId(), party);
        broadcast(party, player.getName() + " joined the party.");
    }

    public void decline(Player player, Player leader) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader) || !party.removeInvitation(player)) {
            player.sendMessage("You do not have an invitation from that party leader.");
            return;
        }
        player.sendMessage("Declined " + leader.getName() + "'s party invitation.");
        leader.sendMessage(player.getName() + " declined your party invitation.");
    }

    public void leave(Player player) {
        Party party = getParty(player);
        if (party == null) {
            player.sendMessage("You are not in a party.");
            return;
        }
        removeMember(party, player, " left the party.");
        player.sendMessage("You left the party.");
    }

    public void kick(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader)) {
            leader.sendMessage("Only a party leader can kick players.");
            return;
        }
        if (party.isLeader(target) || !party.contains(target)) {
            leader.sendMessage("That player is not another member of your party.");
            return;
        }
        removeMember(party, target, " was removed from the party.");
        target.sendMessage("You were removed from the party.");
    }

    public void disband(Player leader) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader)) {
            leader.sendMessage("Only a party leader can disband a party.");
            return;
        }
        broadcast(party, "The party was disbanded.");
        for (Player member : party.getPlayerList()) memberships.remove(member.getUniqueId());
        cancelInvitations(party);
        parties.remove(party);
    }

    public void list(Player player) {
        Party party = getParty(player);
        if (party == null) {
            player.sendMessage("You are not in a party.");
            return;
        }
        player.sendMessage("Party leader: " + party.getLeader().getName() + ". Members: "
                + String.join(", ", party.getPlayerList().stream().map(Player::getName).toList()));
    }

    public ArrayList<Player> playersForGame(Player starter) {
        Party party = getParty(starter);
        if (party == null) return new ArrayList<>(List.of(starter));
        if (!party.isLeader(starter)) {
            throw new IllegalStateException("Only the party leader can start a game");
        }
        return new ArrayList<>(party.getPlayerList());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeInvitations(player);
        Party party = getParty(player);
        if (party != null) removeMember(party, player, " left the party.");
    }

    private void removeMember(Party party, Player player, String message) {
        boolean wasLeader = party.isLeader(player);
        party.removePlayer(player);
        memberships.remove(player.getUniqueId());
        broadcast(party, player.getName() + message);
        if (wasLeader) {
            cancelInvitations(party);
            if (!party.getPlayerList().isEmpty()) {
                party.transferLeadership();
                broadcast(party, party.getLeader().getName() + " is now the party leader.");
            }
        }
        if (party.getPlayerList().isEmpty()) parties.remove(party);
    }

    private void removeInvitations(Player player) {
        for (Party party : parties) party.removeInvitation(player);
    }

    private void cancelInvitations(Party party) {
        for (Player invitee : party.clearInvitations()) {
            invitee.sendMessage("A party invitation was cancelled because the party leader left or disbanded the party.");
        }
    }

    private void broadcast(Party party, String message) {
        for (Player member : party.getPlayerList()) member.sendMessage(message);
    }
}
