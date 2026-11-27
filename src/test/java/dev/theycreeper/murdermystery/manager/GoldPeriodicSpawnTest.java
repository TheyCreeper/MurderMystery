package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.model.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoldPeriodicSpawnTest {
    @Test
    void selectsLocationOnEachRun() {
        MurderMystery plugin = mock(MurderMystery.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        World world = mock(World.class);
        Item item = mock(Item.class);
        MurderMysteryZone zone = new MurderMysteryZone();
        Location first = new Location(world, 1, 2, 3);
        Location second = new Location(world, 10, 20, 30);
        zone.addGoldLocation(new Pair<>(first, first));
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(120L)))
                .thenReturn(mock(BukkitTask.class));
        when(world.dropItemNaturally(any(Location.class), any(ItemStack.class))).thenReturn(item);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedConstruction<ItemStack> itemStacks = mockConstruction(ItemStack.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            new GoldPeriodicSpawn().startGoldSpawning(zone);

            ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).runTaskTimer(eq(plugin), task.capture(), eq(0L), eq(120L));
            task.getValue().run();
            zone.getAllGoldLoc().clear();
            zone.addGoldLocation(new Pair<>(second, second));
            task.getValue().run();

            ArgumentCaptor<Location> locations = ArgumentCaptor.forClass(Location.class);
            verify(world, times(2)).dropItemNaturally(locations.capture(), any(ItemStack.class));
            assertEquals(new Location(world, 1, 3, 3), locations.getAllValues().get(0));
            assertEquals(new Location(world, 10, 21, 30), locations.getAllValues().get(1));
        }
    }

    @Test
    void rejectsMapWithoutGoldLocationsBeforeScheduling() {
        assertThrows(IllegalStateException.class,
                () -> new GoldPeriodicSpawn().startGoldSpawning(new MurderMysteryZone()));
    }
}
