package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MMPlayer;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.PlayerState;
import dev.theycreeper.murdermystery.game.PlayerType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DetectiveArrowListener implements Listener {
    private final MurderMysteryGame game;
    private final Map<UUID, List<Recharge>> pending = new HashMap<>();

    public DetectiveArrowListener(MurderMysteryGame game) {
        this.game = game;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBowShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getProjectile() instanceof AbstractArrow)
                || event.getBow() == null || event.getBow().getType() != Material.BOW
                || !isDetective(player)) return;

        List<Recharge> recharges = pending.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>());
        Recharge recharge = new Recharge();
        recharges.add(recharge);
        recharge.task = Bukkit.getScheduler().runTaskTimer(MurderMystery.getInstance(),
                () -> tick(player, recharge), 20L, 20L);
        if (recharges.size() == 1) showCountdown(player, recharge.secondsLeft);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelPlayer(event.getPlayer().getUniqueId());
    }

    public void cancelAll() {
        for (List<Recharge> recharges : pending.values()) {
            recharges.forEach(recharge -> recharge.task.cancel());
        }
        pending.clear();
    }

    private void tick(Player player, Recharge recharge) {
        if (!player.isOnline() || !isDetective(player)) {
            cancelPlayer(player.getUniqueId());
            return;
        }

        List<Recharge> recharges = pending.get(player.getUniqueId());
        boolean first = recharges.getFirst() == recharge;
        if (--recharge.secondsLeft == 0) {
            recharge.task.cancel();
            recharges.remove(recharge);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack(Material.ARROW));
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                player.sendMessage("Your inventory is full, so your arrow was dropped.");
            }
            if (recharges.isEmpty()) {
                pending.remove(player.getUniqueId());
                sendActionBar(player, ChatColor.GREEN + "Arrow ready!");
            } else if (first) {
                showCountdown(player, recharges.getFirst().secondsLeft);
            }
        } else if (first) {
            showCountdown(player, recharge.secondsLeft);
        }
    }

    private boolean isDetective(Player player) {
        MMPlayer mmPlayer = game.getMmPlayer(player);
        return mmPlayer != null && mmPlayer.getPlayerType() == PlayerType.detective
                && mmPlayer.getPlayerState() == PlayerState.alive;
    }

    private void cancelPlayer(UUID playerId) {
        List<Recharge> recharges = pending.remove(playerId);
        if (recharges != null) recharges.forEach(recharge -> recharge.task.cancel());
    }

    private void showCountdown(Player player, int seconds) {
        sendActionBar(player, ChatColor.GRAY + "Arrow recharging: " + ChatColor.YELLOW + seconds + "s");
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private static class Recharge {
        private int secondsLeft = 5;
        private BukkitTask task;
    }
}
