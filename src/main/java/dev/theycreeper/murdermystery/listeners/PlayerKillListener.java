package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MMPlayer;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.PlayerState;
import dev.theycreeper.murdermystery.game.PlayerType;
import org.bukkit.*;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class PlayerKillListener implements Listener {
    private final MurderMysteryGame game;

    public PlayerKillListener(MurderMysteryGame game) {
        this.game = game;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player hitPlayer)) return;
        if (event.getDamager() instanceof AbstractArrow arrow && arrow.getShooter() instanceof Player shooter) {
            handleArrowHit(event, arrow, shooter, hitPlayer);
            return;
        }
        if (!(event.getDamager() instanceof Player damagerPlayer)) return;
        if (game.getMmPlayer(damagerPlayer) == null && game.getMmPlayer(hitPlayer) == null) return;
        if (!playersInGame(damagerPlayer, hitPlayer) ||
                !isAlive(damagerPlayer) || !isAlive(hitPlayer) ||
                !game.isPlayerCanKill(damagerPlayer) ||
                !isValidWeapon(event)) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        killPlayer(hitPlayer);
    }

    private void handleArrowHit(EntityDamageByEntityEvent event, AbstractArrow arrow, Player shooter, Player target) {
        MMPlayer shootingPlayer = game.getMmPlayer(shooter);
        MMPlayer hitPlayer = game.getMmPlayer(target);
        if (shootingPlayer == null && hitPlayer == null) return;
        event.setCancelled(true);
        ItemStack weapon = arrow.getWeapon();
        arrow.remove();
        if (shootingPlayer == null || hitPlayer == null || shooter.equals(target)
                || shootingPlayer.getPlayerState() != PlayerState.alive
                || hitPlayer.getPlayerState() != PlayerState.alive
                || !isInnocentTeam(shootingPlayer.getPlayerType())
                || weapon == null || weapon.getType() != Material.BOW) return;

        if (hitPlayer.getPlayerType() == PlayerType.murderer) {
            MurderMystery.getInstance().getGameManager().innocentsWin(game);
        } else if (isInnocentTeam(hitPlayer.getPlayerType())) {
            killPlayer(target);
            killPlayer(shooter);
        }
    }

    private boolean isInnocentTeam(PlayerType type) {
        return type == PlayerType.innocent || type == PlayerType.detective;
    }

    private boolean isAlive(Player player) {
        MMPlayer mmPlayer = game.getMmPlayer(player);
        return mmPlayer != null && mmPlayer.getPlayerState() == PlayerState.alive;
    }

    private void killPlayer(Player player) {
        game.createCorpse(player);
        game.getMmPlayer(player).setPlayerState(PlayerState.dead);
        spectatePlayer(player);
        player.sendTitle(
                ChatColor.RED + "You Died!",   // Main title
                ChatColor.GRAY + "You are now a spectator", // Subtitle (or "" / null)
                10,                               // Fade in (ticks)
                70,                               // Stay on screen (ticks)
                20                                // Fade out (ticks)
        );
        player.getWorld().playSound(player.getLocation(), "minecraft:entity.player.hurt", 3.0F, 1.0F);
    }

    private void spectatePlayer(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        player.setAllowFlight(true);
        player.setFlying(true);

        // 3. No collision (players/mobs won't push them, arrows/items pass through)
        player.setCollidable(false);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hidePlayer(MurderMystery.getInstance(), player);
        }
    }

    private boolean isValidWeapon(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        ItemStack weaponUsed = null;

        // 1. Direct Melee Attack (Player, Zombie, etc.)
        if (damager instanceof LivingEntity livingDamager) {
            EntityEquipment equipment = livingDamager.getEquipment();
            if (equipment != null) {
                weaponUsed = equipment.getItemInMainHand();
            }
        }
        // Verify a valid item was found (empty hand returns Material.AIR)
        if (weaponUsed != null && weaponUsed.getType() != Material.AIR) {
            Material toolType = weaponUsed.getType();
            return toolType == Material.STONE_SWORD;
        }
        return false;
    }

    private boolean playersInGame(Player p1, Player p2) {
        return game.getMmPlayer(p1) != null && game.getMmPlayer(p2) != null;
    }
}
