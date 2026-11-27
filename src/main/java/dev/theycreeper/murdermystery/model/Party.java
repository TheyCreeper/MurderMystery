package dev.theycreeper.murdermystery.model;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Party {
    private final Map<UUID, Player> members = new LinkedHashMap<>();
    private final Map<UUID, Player> invitations = new LinkedHashMap<>();
    private UUID leader;

    public Party(Player leader) {
        this.leader = leader.getUniqueId();
        addPlayer(leader);
    }

    public Player getLeader() {
        return members.get(leader);
    }

    public boolean isLeader(Player player) {
        return leader.equals(player.getUniqueId());
    }

    public List<Player> getPlayerList() {
        return List.copyOf(members.values());
    }

    public boolean contains(Player player) {
        return members.containsKey(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        members.put(player.getUniqueId(), player);
    }

    public void removePlayer(Player player) {
        members.remove(player.getUniqueId());
    }

    public void transferLeadership() {
        leader = members.keySet().iterator().next();
    }

    public boolean invite(Player player) {
        return invitations.putIfAbsent(player.getUniqueId(), player) == null;
    }

    public boolean removeInvitation(Player player) {
        return invitations.remove(player.getUniqueId()) != null;
    }

    public List<Player> clearInvitations() {
        List<Player> invitees = new ArrayList<>(invitations.values());
        invitations.clear();
        return invitees;
    }
}
