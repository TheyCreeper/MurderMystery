package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.MurderMystery;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateNewMMZone implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player p)) return false;
        if (MurderMystery.getInstance().getGameManager().hasActiveGame()) {
            sender.sendMessage("Finish the current game before editing a map.");
            return true;
        }
        String name = CommandUtils.getSanitizedArgsList(args).getFirst();
        MurderMystery.getInstance().getZoneCreationManager().setZoneCreationMode(p, name);
        return false;
    }
}
