package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class PartyCommand implements CommandExecutor {
    private final PartyManager parties;

    public PartyCommand(PartyManager parties) {
        this.parties = parties;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use party commands.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("leave") || action.equals("disband") || action.equals("list")) {
            if (args.length != 1) {
                sendUsage(player);
            } else {
                switch (action) {
                    case "leave" -> parties.leave(player);
                    case "disband" -> parties.disband(player);
                    case "list" -> parties.list(player);
                }
            }
        } else if (isPlayerAction(action)) {
            if (args.length != 2) {
                sendUsage(player);
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage("Player not found or offline: " + args[1]);
                return true;
            }
            switch (action) {
                case "invite" -> parties.invite(player, target);
                case "accept" -> parties.accept(player, target);
                case "decline" -> parties.decline(player, target);
                case "kick" -> parties.kick(player, target);
                default -> throw new IllegalStateException("Unknown party action");
            }
        } else {
            for (String name : args) {
                Player target = Bukkit.getPlayerExact(name);
                if (target == null) {
                    player.sendMessage("Player not found or offline: " + name);
                } else {
                    parties.invite(player, target);
                }
            }
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage("Usage: /party <player...> or /party <invite|accept|decline|kick> <player> or /party <leave|list|disband>");
    }

    private boolean isPlayerAction(String action) {
        return switch (action) {
            case "invite", "accept", "decline", "kick" -> true;
            default -> false;
        };
    }
}
