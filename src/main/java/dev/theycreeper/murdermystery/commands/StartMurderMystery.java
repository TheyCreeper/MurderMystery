package dev.theycreeper.murdermystery.commands;

import dev.theycreeper.murdermystery.MMGameManager;
import dev.theycreeper.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class StartMurderMystery implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player p)) return false;
        MMGameManager gameManager = MurderMystery.getInstance().getGameManager();
        gameManager.StartGame(CommandUtils.getAllPlayers(), p);
        commandSender.sendRichMessage("Started game listeners");
        return true;
    }
}
