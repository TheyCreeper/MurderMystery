package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MMPlayer;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.PlayerState;
import dev.theycreeper.murdermystery.game.PlayerType;
import dev.theycreeper.murdermystery.manager.MMGameManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerKillListenerTest {
    @Test
    void swordEliminationStillWorksAndDeadPlayersCannotAttack() {
        MurderMysteryGame game = mock(MurderMysteryGame.class);
        Player murderer = player();
        Player target = player();
        participant(game, murderer, PlayerType.murderer);
        MMPlayer victim = participant(game, target, PlayerType.innocent);
        when(game.isPlayerCanKill(murderer)).thenReturn(true);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack sword = mock(ItemStack.class);
        when(murderer.getEquipment()).thenReturn(equipment);
        when(equipment.getItemInMainHand()).thenReturn(sword);
        when(sword.getType()).thenReturn(Material.STONE_SWORD);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(murderer);
        when(event.getEntity()).thenReturn(target);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            PlayerKillListener listener = new PlayerKillListener(game);
            listener.onPlayerHit(event);
            listener.onPlayerHit(event);
        }
        assertEquals(PlayerState.dead, victim.getPlayerState());
        verify(game).createCorpse(target);
        verify(event, times(2)).setCancelled(true);
    }

    @Test
    void innocentOrDetectiveBowShotEndsGameWhenMurdererIsHit() {
        for (PlayerType role : List.of(PlayerType.innocent, PlayerType.detective)) {
            MurderMysteryGame game = mock(MurderMysteryGame.class);
            Player shooter = player();
            Player target = player();
            MMPlayer shootingPlayer = participant(game, shooter, role);
            participant(game, target, PlayerType.murderer);
            EntityDamageByEntityEvent event = arrowHit(shooter, target, Material.BOW);
            MurderMystery plugin = mock(MurderMystery.class);
            MMGameManager manager = mock(MMGameManager.class);
            when(plugin.getGameManager()).thenReturn(manager);
            try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class)) {
                instance.when(MurderMystery::getInstance).thenReturn(plugin);
                new PlayerKillListener(game).onPlayerHit(event);
            }
            verify(event).setCancelled(true);
            verify(event.getDamager()).remove();
            verify(manager).innocentsWin(game);
            verify(game, never()).createCorpse(any());
            assertEquals(PlayerState.alive, shootingPlayer.getPlayerState());
        }
    }

    @Test
    void friendlyBowShotsEliminateBothAndDeadPlayersCannotAttackAgain() {
        for (PlayerType shooterRole : List.of(PlayerType.innocent, PlayerType.detective)) {
            for (PlayerType targetRole : List.of(PlayerType.innocent, PlayerType.detective)) {
                MurderMysteryGame game = mock(MurderMysteryGame.class);
                Player shooter = player();
                Player target = player();
                MMPlayer shootingPlayer = participant(game, shooter, shooterRole);
                MMPlayer hitPlayer = participant(game, target, targetRole);
                EntityDamageByEntityEvent event = arrowHit(shooter, target, Material.BOW);
                try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                    bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
                    PlayerKillListener listener = new PlayerKillListener(game);
                    listener.onPlayerHit(event);
                    listener.onPlayerHit(event);
                }
                assertEquals(PlayerState.dead, shootingPlayer.getPlayerState());
                assertEquals(PlayerState.dead, hitPlayer.getPlayerState());
                verify(game).createCorpse(shooter);
                verify(game).createCorpse(target);
                verify(shooter).setFlying(true);
                verify(target).setFlying(true);
                verify(event, times(2)).setCancelled(true);
            }
        }
    }

    @Test
    void invalidShotsAndNonParticipantsCannotTriggerOutcomes() {
        MurderMysteryGame game = mock(MurderMysteryGame.class);
        Player shooter = player();
        Player target = player();
        Player outsider = player();
        MMPlayer shootingPlayer = participant(game, shooter, PlayerType.detective);
        participant(game, target, PlayerType.murderer);
        PlayerKillListener listener = new PlayerKillListener(game);
        EntityDamageByEntityEvent crossbow = arrowHit(shooter, target, Material.CROSSBOW);
        EntityDamageByEntityEvent outsiderShot = arrowHit(outsider, target, Material.BOW);
        EntityDamageByEntityEvent selfShot = arrowHit(shooter, shooter, Material.BOW);
        EntityDamageByEntityEvent outsideTarget = arrowHit(shooter, outsider, Material.BOW);
        EntityDamageByEntityEvent missingWeapon = arrowHit(shooter, target, Material.BOW);
        when(((AbstractArrow) missingWeapon.getDamager()).getWeapon()).thenReturn(null);
        listener.onPlayerHit(crossbow);
        listener.onPlayerHit(outsiderShot);
        listener.onPlayerHit(selfShot);
        listener.onPlayerHit(outsideTarget);
        listener.onPlayerHit(missingWeapon);
        shootingPlayer.setPlayerState(PlayerState.dead);
        EntityDamageByEntityEvent deadShot = arrowHit(shooter, target, Material.BOW);
        listener.onPlayerHit(deadShot);
        verify(crossbow).setCancelled(true);
        verify(outsiderShot).setCancelled(true);
        verify(selfShot).setCancelled(true);
        verify(outsideTarget).setCancelled(true);
        verify(missingWeapon).setCancelled(true);
        verify(deadShot).setCancelled(true);
        verify(game, never()).createCorpse(any());

        Player otherOutsider = player();
        EntityDamageByEntityEvent unrelated = arrowHit(outsider, otherOutsider, Material.BOW);
        listener.onPlayerHit(unrelated);
        verify(unrelated, never()).setCancelled(anyBoolean());
    }

    private Player player() {
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(mock(World.class));
        return player;
    }

    private MMPlayer participant(MurderMysteryGame game, Player player, PlayerType role) {
        MMPlayer participant = new MMPlayer(player);
        participant.setPlayerType(role);
        when(game.getMmPlayer(player)).thenReturn(participant);
        return participant;
    }

    private EntityDamageByEntityEvent arrowHit(Player shooter, Player target, Material weaponType) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        AbstractArrow arrow = mock(AbstractArrow.class);
        ItemStack weapon = mock(ItemStack.class);
        when(event.getEntity()).thenReturn(target);
        when(event.getDamager()).thenReturn(arrow);
        when(arrow.getShooter()).thenReturn(shooter);
        when(arrow.getWeapon()).thenReturn(weapon);
        when(weapon.getType()).thenReturn(weaponType);
        return event;
    }
}
