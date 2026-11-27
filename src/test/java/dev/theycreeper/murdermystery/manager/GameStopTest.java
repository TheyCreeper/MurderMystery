package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.commands.StopMurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameStopTest {
    @Test
    void stopCommandRestoresAllPlayersWithTheirSavedModes() {
        Player first = player();
        Player second = player();
        MurderMystery plugin = mock(MurderMystery.class);
        ZoneCreationManager zones = mock(ZoneCreationManager.class);
        SQLHandler database = mock(SQLHandler.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        World world = mock(World.class);
        WorldBorder border = mock(WorldBorder.class);
        Block stone = mock(Block.class);
        Block air = mock(Block.class);
        MurderMysteryZone map = new MurderMysteryZone();
        Location firstLocation = new Location(world, -20, 70, -20);
        Location secondLocation = new Location(world, -21, 70, -20);
        map.setCorner1(new Location(world, 0, 10, 0));
        map.setCorner2(new Location(world, 0, 12, 0));
        MMGameManager manager = new MMGameManager();
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getWorldBorder()).thenReturn(border);
        when(border.isInside(any(Location.class))).thenReturn(true);
        when(stone.getType()).thenReturn(Material.STONE);
        when(stone.isSolid()).thenReturn(true);
        when(air.isEmpty()).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int y = invocation.getArgument(1);
            return y == 9 || y == 12 ? stone : air;
        });
        when(world.getHighestBlockYAt(0, 0)).thenReturn(12);
        when(first.getLocation()).thenReturn(firstLocation);
        when(second.getLocation()).thenReturn(secondLocation);
        when(first.teleport(any(Location.class))).thenReturn(true);
        when(second.teleport(any(Location.class))).thenReturn(true);
        when(plugin.getGameManager()).thenReturn(manager);
        when(plugin.getCorpseManager()).thenReturn(mock(CorpseManager.class));
        when(plugin.getZoneCreationManager()).thenReturn(zones);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(zones.loadMap(map)).thenReturn(true);
        when(database.getPlayerGamemode(first.getUniqueId())).thenReturn(GameMode.CREATIVE);
        when(database.getPlayerGamemode(second.getUniqueId())).thenReturn(GameMode.SURVIVAL);
        when(database.getPlayerLocation(first.getUniqueId())).thenReturn(firstLocation);
        when(database.getPlayerLocation(second.getUniqueId())).thenReturn(secondLocation);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L)))
                .thenReturn(mock(BukkitTask.class));
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<PlayerStateChanger> states = mockStatic(PlayerStateChanger.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            sql.when(SQLHandler::getInstance).thenReturn(database);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(first, second));
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            stubScoreboards(bukkit);
            manager.StartGame(new ArrayList<>(List.of(first, second)), map);
            assertTrue(new StopMurderMystery().onCommand(first, mock(Command.class), "mmstop", new String[0]));
            states.verify(() -> PlayerStateChanger.normalMode(first, GameMode.CREATIVE));
            states.verify(() -> PlayerStateChanger.normalMode(second, GameMode.SURVIVAL));
            verify(first).setAllowFlight(false);
            verify(second).setCollidable(true);
            verify(first).teleport(firstLocation);
            verify(second).teleport(secondLocation);
            assertFalse(manager.hasActiveGame());
            verify(zones).unloadMap(map);
        }
    }

    @Test
    void partialStartRestoresOnlyPreparedPlayersAndContinuesAfterFailure() {
        Player first = player();
        Player second = player();
        Player unprepared = player();
        SQLHandler database = mock(SQLHandler.class);
        MurderMystery plugin = mock(MurderMystery.class);
        Location secondLocation = new Location(mock(World.class), 1, 2, 3);
        when(plugin.getCorpseManager()).thenReturn(mock(CorpseManager.class));
        when(database.getPlayerGamemode(first.getUniqueId())).thenReturn(GameMode.CREATIVE);
        when(database.getPlayerGamemode(second.getUniqueId())).thenReturn(GameMode.SURVIVAL);
        when(database.getPlayerLocation(second.getUniqueId())).thenReturn(secondLocation);
        when(second.teleport(secondLocation)).thenReturn(true);
        try (MockedStatic<MurderMystery> instance = mockStatic(MurderMystery.class);
             MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<PlayerStateChanger> states = mockStatic(PlayerStateChanger.class)) {
            instance.when(MurderMystery::getInstance).thenReturn(plugin);
            sql.when(SQLHandler::getInstance).thenReturn(database);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            stubScoreboards(bukkit);
            MurderMysteryGame game = new MurderMysteryGame(new MurderMysteryZone());
            game.addPlayer(first);
            game.addPlayer(second);
            game.addPlayer(unprepared);
            game.setMurderer(first);
            game.setDetective(second);
            states.when(() -> PlayerStateChanger.normalMode(first, GameMode.CREATIVE))
                    .thenThrow(new IllegalStateException("restore failed"));
            IllegalStateException failure = assertThrows(IllegalStateException.class, game::EndGame);
            assertEquals("restore failed", failure.getMessage());
            states.verify(() -> PlayerStateChanger.normalMode(second, GameMode.SURVIVAL));
            verify(second).teleport(secondLocation);
            verify(database, never()).getPlayerGamemode(unprepared.getUniqueId());
        }
    }

    private void stubScoreboards(MockedStatic<Bukkit> bukkit) {
        ScoreboardManager manager = mock(ScoreboardManager.class);
        when(manager.getNewScoreboard()).thenReturn(mock(Scoreboard.class));
        bukkit.when(Bukkit::getScoreboardManager).thenReturn(manager);
    }

    private Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }
}
