package dev.theycreeper.murdermystery.game;

import dev.theycreeper.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlayerSpawnTest {
    @Test
    void teleportsPlayersToDistinctSafeBlocksWithinReversedCorners() {
        try (Fixture f = new Fixture()) {
            f.floor(0, Material.STONE);
            f.floor(1, Material.STONE);
            Player first = f.addPlayer();
            Player second = f.addPlayer();
            f.game.StartGame();

            ArgumentCaptor<Location> firstSpawn = ArgumentCaptor.forClass(Location.class);
            ArgumentCaptor<Location> secondSpawn = ArgumentCaptor.forClass(Location.class);
            verify(first).teleport(firstSpawn.capture());
            verify(second).teleport(secondSpawn.capture());
            assertEquals(10, firstSpawn.getValue().getBlockY());
            assertEquals(10, secondSpawn.getValue().getBlockY());
            assertEquals(0, firstSpawn.getValue().getBlockZ());
            assertEquals(0, secondSpawn.getValue().getBlockZ());
            assertNotEquals(firstSpawn.getValue().getBlockX(), secondSpawn.getValue().getBlockX());
            assertEquals(90, firstSpawn.getValue().getYaw());
        }
    }

    @Test
    void refusesUnsafeFloorsAndBlockedHeadroomWithoutTeleporting() {
        try (Fixture f = new Fixture()) {
            f.floor(0, Material.MAGMA_BLOCK);
            f.floor(1, Material.STONE);
            f.blocks.put("1,11,0", f.block(Material.STONE));
            Player player = f.addPlayer();
            IllegalStateException error = assertThrows(IllegalStateException.class, f.game::StartGame);
            assertTrue(error.getMessage().contains("safe covered spawn"));
            verify(player, never()).teleport(any(Location.class));
            verify(f.pluginManager, never()).registerEvents(any(), any());
        }
    }

    @Test
    void sharesTheOnlySafeSpotAndRejectsPositionsOutsideWorldBorder() {
        try (Fixture f = new Fixture()) {
            f.floor(0, Material.STONE);
            f.floor(1, Material.STONE);
            when(f.border.isInside(any(Location.class))).thenAnswer(
                    invocation -> ((Location) invocation.getArgument(0)).getBlockX() == 1);
            Player first = f.addPlayer();
            Player second = f.addPlayer();
            f.game.StartGame();
            verify(first).teleport(argThat((Location location) -> location.getBlockX() == 1));
            verify(second).teleport(argThat((Location location) -> location.getBlockX() == 1));
        }
    }

    @Test
    void rollsBackEarlierTeleportsWhenAPlayerCannotTeleport() {
        try (Fixture f = new Fixture()) {
            f.floor(0, Material.STONE);
            Player first = f.addPlayer();
            Player second = f.addPlayer();
            Location original = first.getLocation();
            when(second.teleport(any(Location.class))).thenReturn(false);
            assertThrows(IllegalStateException.class, f.game::StartGame);
            verify(first).teleport(original);
            verify(f.pluginManager, never()).registerEvents(any(), any());
        }
    }

    @Test
    void spawnsUnderRoofRatherThanOnTopOfIt() {
        try (Fixture f = new Fixture()) {
            f.floor(0, Material.STONE);
            f.zone.setCorner1(new Location(f.world, 1, 16, 0));
            Player player = f.addPlayer();
            f.game.StartGame();
            verify(player).teleport(argThat((Location location) ->
                    location.getBlockX() == 0 && location.getBlockY() == 10));
        }
    }

    @Test
    void refusesAnExposedFloorEvenWhenThereIsHeadroom() {
        try (Fixture f = new Fixture()) {
            f.floor(0, Material.STONE);
            f.blocks.remove("0,12,0");
            Player player = f.addPlayer();
            assertThrows(IllegalStateException.class, f.game::StartGame);
            verify(player, never()).teleport(any(Location.class));
        }
    }

    @Test
    void ignoresInvisibleBarriersAsCeilings() {
        try (Fixture f = new Fixture()) {
            f.floor(0, Material.STONE);
            f.blocks.put("0,12,0", f.block(Material.BARRIER));
            Player player = f.addPlayer();
            assertThrows(IllegalStateException.class, f.game::StartGame);
            verify(player, never()).teleport(any(Location.class));
        }
    }

    private static class Fixture implements AutoCloseable {
        final MurderMystery plugin = mock(MurderMystery.class);
        final Server server = mock(Server.class);
        final PluginManager pluginManager = mock(PluginManager.class);
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        final World world = mock(World.class);
        final WorldBorder border = mock(WorldBorder.class);
        final Map<String, Block> blocks = new HashMap<>();
        final Block air = block(Material.AIR);
        final MurderMysteryZone zone = new MurderMysteryZone();
        final MurderMysteryGame game = new MurderMysteryGame(zone);
        final MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);

        Fixture() {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(plugin.getServer()).thenReturn(server);
            when(server.getPluginManager()).thenReturn(pluginManager);
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L)))
                    .thenReturn(mock(BukkitTask.class));
            when(world.getMinHeight()).thenReturn(-64);
            when(world.getMaxHeight()).thenReturn(320);
            when(world.getWorldBorder()).thenReturn(border);
            when(border.isInside(any(Location.class))).thenReturn(true);
            when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                    blocks.getOrDefault(invocation.getArgument(0) + "," + invocation.getArgument(1)
                            + "," + invocation.getArgument(2), air));
            when(world.getHighestBlockYAt(anyInt(), anyInt())).thenAnswer(invocation -> {
                int x = invocation.getArgument(0);
                int z = invocation.getArgument(1);
                for (int y = 16; y >= 9; y--) {
                    if (!blocks.getOrDefault(x + "," + y + "," + z, air).isEmpty()) return y;
                }
                return -64;
            });
            zone.setCorner1(new Location(world, 1, 11, 0));
            zone.setCorner2(new Location(world, 0, 10, 0));
        }

        Block block(Material material) {
            Block block = mock(Block.class);
            when(block.getType()).thenReturn(material);
            when(block.isEmpty()).thenReturn(material == Material.AIR);
            when(block.isSolid()).thenReturn(material != Material.AIR);
            return block;
        }

        void floor(int x, Material material) {
            blocks.put(x + ",9,0", block(material));
            blocks.put(x + ",12,0", block(Material.STONE));
        }

        Player addPlayer() {
            Player player = mock(Player.class);
            when(player.getLocation()).thenReturn(new Location(world, -20, 10, -20, 90, 0));
            when(player.teleport(any(Location.class))).thenReturn(true);
            game.addPlayer(player);
            return player;
        }

        @Override
        public void close() {
            bukkit.close();
            instance.close();
        }
    }
}
