package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.MMGameManager;
import dev.theycreeper.murdermystery.MurderMystery;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StopMurderMystery implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player p)) return false;
        MMGameManager gameManager = MurderMystery.getInstance().getGameManager();
        gameManager.StopGame();
        sender.sendRichMessage("Stopped the game");
        return false;
    }
}
