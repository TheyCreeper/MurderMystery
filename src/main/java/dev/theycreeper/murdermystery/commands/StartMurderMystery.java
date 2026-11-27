package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.manager.MMGameManager;
import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StartMurderMystery implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player p)) return false;
        if (strings.length != 1) {
            commandSender.sendMessage("Usage: /mmstart <map>");
            return true;
        }
        MMGameManager gameManager = MurderMystery.getInstance().getGameManager();
        try {
            MurderMysteryZone map = SQLHandler.getInstance().getZone(strings[0]);
            if (map == null) {
                commandSender.sendMessage("Map not found: " + strings[0]);
                return true;
            }
            if (!gameManager.StartGame(MurderMystery.getInstance().getPartyManager().playersForGame(p), map)) {
                commandSender.sendMessage("Cannot start: barrier locations are obstructed and highlighted in red.");
                return true;
            }
        } catch (IllegalStateException e) {
            commandSender.sendMessage("Could not start game: " + e.getMessage());
            return true;
        }
        commandSender.sendRichMessage("Started game listeners");
        return true;
    }
}
