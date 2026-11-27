package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MMPlayer;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.PlayerState;
import dev.theycreeper.murdermystery.game.PlayerType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DetectiveArrowListenerTest {
    @Test
    void detectiveGetsOneArrowAfterFiveSecondsWithCountdown() {
        try (Fixture f = new Fixture()) {
            f.listener.onBowShot(f.shot);
            Runnable timer = f.timers.getFirst();
            verifyActionBar(f.spigot, "Arrow recharging: 5s", 1);
            for (int i = 0; i < 4; i++) timer.run();
            verifyActionBar(f.spigot, "Arrow recharging: 1s", 1);
            verify(f.inventory, never()).addItem(any(ItemStack.class));
            timer.run();
            ArgumentCaptor<ItemStack> arrow = ArgumentCaptor.forClass(ItemStack.class);
            verify(f.inventory).addItem(arrow.capture());
            assertEquals(Material.ARROW, arrow.getValue().getType());
            assertEquals(1, arrow.getValue().getAmount());
            verify(f.tasks.getFirst()).cancel();
            verifyActionBar(f.spigot, "Arrow ready!", 1);
        }
    }

    @Test
    void eachShotRechargesIndependentlyAndDisplaysEarliestCountdown() {
        try (Fixture f = new Fixture()) {
            f.listener.onBowShot(f.shot);
            Runnable first = f.timers.getFirst();
            first.run();
            first.run();
            f.listener.onBowShot(f.shot);
            Runnable second = f.timers.get(1);
            for (int i = 0; i < 2; i++) {
                first.run();
                second.run();
            }
            first.run();
            verify(f.inventory).addItem(any(ItemStack.class));
            verifyActionBar(f.spigot, "Arrow ready!", 0);
            for (int i = 0; i < 3; i++) second.run();
            verify(f.inventory, times(2)).addItem(any(ItemStack.class));
            verifyActionBar(f.spigot, "Arrow ready!", 1);
        }
    }

    @Test
    void onlyLivingDetectivesShootingArrowsFromBowsAreScheduled() throws Exception {
        EventHandler handler = DetectiveArrowListener.class.getMethod("onBowShot", EntityShootBowEvent.class)
                .getAnnotation(EventHandler.class);
        assertTrue(handler.ignoreCancelled());
        assertEquals(EventPriority.MONITOR, handler.priority());
        try (Fixture f = new Fixture()) {
            f.participant.setPlayerType(PlayerType.innocent);
            f.listener.onBowShot(f.shot);
            f.participant.setPlayerType(PlayerType.detective);
            f.participant.setPlayerState(PlayerState.dead);
            f.listener.onBowShot(f.shot);
            f.participant.setPlayerState(PlayerState.alive);
            when(f.shot.getBow().getType()).thenReturn(Material.CROSSBOW);
            f.listener.onBowShot(f.shot);
            when(f.shot.getBow().getType()).thenReturn(Material.BOW);
            when(f.shot.getProjectile()).thenReturn(mock(org.bukkit.entity.Snowball.class));
            f.listener.onBowShot(f.shot);
            when(f.game.getMmPlayer(f.player)).thenReturn(null);
            when(f.shot.getProjectile()).thenReturn(mock(Arrow.class));
            f.listener.onBowShot(f.shot);
            assertTrue(f.timers.isEmpty());
        }
    }

    @Test
    void pendingRechargesStopOnDeathQuitOrGameEnd() {
        try (Fixture f = new Fixture()) {
            f.listener.onBowShot(f.shot);
            f.participant.setPlayerState(PlayerState.dead);
            f.timers.getFirst().run();
            verify(f.tasks.getFirst()).cancel();
            verify(f.inventory, never()).addItem(any(ItemStack.class));

            f.participant.setPlayerState(PlayerState.alive);
            f.listener.onBowShot(f.shot);
            PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
            when(quit.getPlayer()).thenReturn(f.player);
            f.listener.onQuit(quit);
            verify(f.tasks.get(1)).cancel();

            f.listener.onBowShot(f.shot);
            f.listener.onBowShot(f.shot);
            f.listener.cancelAll();
            verify(f.tasks.get(2)).cancel();
            verify(f.tasks.get(3)).cancel();
            verify(f.inventory, never()).addItem(any(ItemStack.class));
        }
    }

    @Test
    void pendingRechargeStopsIfPlayerGoesOfflineOrIsNoLongerDetective() {
        try (Fixture f = new Fixture()) {
            f.listener.onBowShot(f.shot);
            when(f.player.isOnline()).thenReturn(false);
            f.timers.getFirst().run();
            verify(f.tasks.getFirst()).cancel();
            when(f.player.isOnline()).thenReturn(true);
            f.listener.onBowShot(f.shot);
            f.participant.setPlayerType(PlayerType.innocent);
            f.timers.get(1).run();
            verify(f.tasks.get(1)).cancel();
            verify(f.inventory, never()).addItem(any(ItemStack.class));
        }
    }

    @Test
    void fullInventoryDropsTheRechargedArrow() {
        try (Fixture f = new Fixture()) {
            ItemStack leftover = mock(ItemStack.class);
            HashMap<Integer, ItemStack> leftovers = new HashMap<>();
            leftovers.put(0, leftover);
            when(f.inventory.addItem(any(ItemStack.class))).thenReturn(leftovers);
            f.listener.onBowShot(f.shot);
            for (int i = 0; i < 5; i++) f.timers.getFirst().run();
            verify(f.player.getWorld()).dropItemNaturally(f.player.getLocation(), leftover);
            verify(f.player).sendMessage("Your inventory is full, so your arrow was dropped.");
        }
    }

    private static void verifyActionBar(Player.Spigot spigot, String text, int count) {
        ArgumentCaptor<BaseComponent[]> messages = ArgumentCaptor.forClass(BaseComponent[].class);
        verify(spigot, atLeastOnce()).sendMessage(eq(ChatMessageType.ACTION_BAR), messages.capture());
        assertEquals(count, messages.getAllValues().stream()
                .filter(components -> BaseComponent.toPlainText(components).equals(text)).count());
    }

    private static class Fixture implements AutoCloseable {
        final MurderMysteryGame game = mock(MurderMysteryGame.class);
        final Player player = mock(Player.class);
        final Player.Spigot spigot = mock(Player.Spigot.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        final MMPlayer participant = new MMPlayer(player);
        final DetectiveArrowListener listener = new DetectiveArrowListener(game);
        final EntityShootBowEvent shot = mock(EntityShootBowEvent.class);
        final List<Runnable> timers = new ArrayList<>();
        final List<BukkitTask> tasks = new ArrayList<>();
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        final MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
        final MockedConstruction<ItemStack> itemStacks = mockConstruction(ItemStack.class, (item, context) -> {
            when(item.getType()).thenReturn((Material) context.arguments().getFirst());
            when(item.getAmount()).thenReturn(1);
        });

        Fixture() {
            MurderMystery plugin = mock(MurderMystery.class);
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(20L), eq(20L)))
                    .thenAnswer(invocation -> {
                        timers.add(invocation.getArgument(1));
                        BukkitTask task = mock(BukkitTask.class);
                        tasks.add(task);
                        return task;
                    });
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.spigot()).thenReturn(spigot);
            when(player.isOnline()).thenReturn(true);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getWorld()).thenReturn(mock(World.class));
            when(inventory.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());
            participant.setPlayerType(PlayerType.detective);
            when(game.getMmPlayer(player)).thenReturn(participant);
            ItemStack bow = mock(ItemStack.class);
            when(bow.getType()).thenReturn(Material.BOW);
            when(shot.getEntity()).thenReturn(player);
            when(shot.getProjectile()).thenReturn(mock(Arrow.class));
            when(shot.getBow()).thenReturn(bow);
        }

        @Override
        public void close() {
            itemStacks.close();
            instance.close();
            bukkit.close();
        }
    }
}
