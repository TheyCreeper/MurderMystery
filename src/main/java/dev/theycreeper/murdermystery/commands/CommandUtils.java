package dev.theycreeper.MurderMystery.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandUtils {
    private static final List<String> OVERRIDE_ARGS = List.of("-override", "-o");

    public static List<String> getSanitizedArgsList(String[] args){
        List<String> argsList =  new ArrayList<>(List.of(args));
        argsList.removeIf(OVERRIDE_ARGS::contains);
        return argsList;
    }

    public static UUID getPlayerUUID(String playerName) {
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }
        return Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }

    public static Player getPlayer(String playerName) {
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }
        return Bukkit.getOfflinePlayer(playerName).getPlayer();
    }

    public static ArrayList<String> getAllPlayers() {
        ArrayList<String> playerNames = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            playerNames.add(player.getName());
        }
        return playerNames;
    }

    public static Location MakeLocationValid(Location location) {
        while (location.getBlock().isSuffocating()) {
            location.add(0, 1, 0);
        }
        return location;
    }
}
