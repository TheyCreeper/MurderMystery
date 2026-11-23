package dev.theycreeper.murdermystery.listeners;

import com.destroystokyo.paper.Title;
import com.sun.source.doctree.HiddenTree;
import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.awt.*;

public class PlayerKillListener implements Listener {
    private final MurderMysteryGame game;

    public PlayerKillListener(MurderMysteryGame game) {
        this.game = game;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
//        var test = event.getDamageSource();
        // Bunch of checks before actually killing someone
        if (!(event.getDamager() instanceof Player damagerPlayer)) return;
        if (!(event.getEntity() instanceof Player hitPlayer)) return;
        if (game.getMmPlayer(damagerPlayer) == null && game.getMmPlayer(hitPlayer) == null) return;
        if (!playersInGame(damagerPlayer, hitPlayer) ||
                !game.isPlayerCanKill(damagerPlayer) ||
                !isValidWeapon(event)) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        game.createCorpse(hitPlayer);
        spectatePlayer(hitPlayer);
        hitPlayer.sendTitle(
                ChatColor.RED + "You Died!",   // Main title
                ChatColor.GRAY + "You are now a spectator", // Subtitle (or "" / null)
                10,                               // Fade in (ticks)
                70,                               // Stay on screen (ticks)
                20                                // Fade out (ticks)
        );
        hitPlayer.getWorld().playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT, 3.0F, 1.0F);
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
//        // 2. Ranged Attack (Bow, Crossbow, Trident)
//        else if (damager instanceof Projectile projectile) {
//            ProjectileSource shooter = projectile.getShooter();
//            if (shooter instanceof LivingEntity livingShooter) {
//                EntityEquipment equipment = livingShooter.getEquipment();
//                if (equipment != null) {
//                    // Note: This gets what they are currently holding, which could
//                    // differ if they switched slots after firing.
//                    weaponUsed = equipment.getItemInMainHand();
//                }
//            }
//        }

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
