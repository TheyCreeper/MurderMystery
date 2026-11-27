package dev.theycreeper.murdermystery.saving;

import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.model.Pair;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SQLHandlerTest {
    private Connection database;
    private SQLHandler handler;
    private MockedStatic<Bukkit> bukkit;
    private World world;

    @BeforeEach
    void setUp() throws Exception {
        database = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = database.createStatement()) {
            statement.execute("CREATE TABLE location (id INTEGER PRIMARY KEY AUTOINCREMENT, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL)");
            statement.execute("CREATE TABLE player (uuid TEXT PRIMARY KEY, realname TEXT)");
        }

        handler = new SQLHandler(database, Logger.getLogger("test"));
        handler.init();
        handler.init();
        UUID worldId = UUID.randomUUID();
        world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkit != null) bukkit.close();
        database.close();
    }

    @Test
    void storesTheLatestGamemodeForExistingPlayers() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("player");
        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE, GameMode.SURVIVAL);
        Location first = new Location(world, -3.25, 70.5, 6, 90, -15);
        Location second = new Location(world, 10, 64, -2, 20, 5);
        when(player.getLocation()).thenReturn(first, second);
        handler.storeInventory(player);
        assertEquals(GameMode.CREATIVE, handler.getPlayerGamemode(uuid));
        assertEquals(first, handler.getPlayerLocation(uuid));
        handler.storeInventory(player);
        assertEquals(GameMode.SURVIVAL, handler.getPlayerGamemode(uuid));
        assertEquals(second, handler.getPlayerLocation(uuid));
    }

    @Test
    void playerLocationsCanBeSetAndRetrievedByUuid() {
        UUID uuid = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Location first = new Location(world, -1.25, 72.5, 3.75, 35, -12);
        Location second = new Location(world, 4, 5, 6, -90, 30);
        assertThrows(IllegalStateException.class, () -> handler.getPlayerLocation(uuid));
        handler.setPlayerLocation(uuid, first);
        handler.setPlayerLocation(other, second);
        handler.setPlayerLocation(uuid, second);
        assertEquals(second, handler.getPlayerLocation(uuid));
        assertEquals(second, handler.getPlayerLocation(other));
        assertThrows(IllegalArgumentException.class,
                () -> handler.setPlayerLocation(uuid, new Location(world, Double.NaN, 1, 2)));
        assertEquals(second, handler.getPlayerLocation(uuid));
        UUID worldId = world.getUID();
        bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> handler.getPlayerLocation(uuid));
    }

    @Test
    void inventoryFailureRollsBackLocationAndGamemode() throws Exception {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        UUID uuid = UUID.randomUUID();
        Location first = new Location(world, 1, 2, 3);
        Location second = new Location(world, 8, 9, 10);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("player");
        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL, GameMode.CREATIVE);
        when(player.getLocation()).thenReturn(first, second);
        when(inventory.getSize()).thenReturn(0, 1);
        when(inventory.getItem(0)).thenReturn(item);
        when(item.getType()).thenReturn(Material.GOLD_INGOT);
        when(item.getAmount()).thenReturn(1);
        when(item.serializeAsBytes()).thenReturn(new byte[]{1});
        handler.storeInventory(player);
        try (Statement statement = database.createStatement()) {
            statement.execute("CREATE TRIGGER fail_inventory BEFORE INSERT ON inventory_item BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }
        assertThrows(IllegalStateException.class, () -> handler.storeInventory(player));
        assertEquals(first, handler.getPlayerLocation(uuid));
        assertEquals(GameMode.SURVIVAL, handler.getPlayerGamemode(uuid));
    }

    @Test
    void roundTripsAndReplacesAllMapData() throws Exception {
        Location first = new Location(world, -1.25, 70.5, 2.75, 35, -12);
        Location second = new Location(world, 20, 90, 30);
        Location gold = new Location(world, 4.5, 71, 6);
        MurderMysteryZone zone = new MurderMysteryZone();
        zone.setMapName("map 'one'");
        zone.setCorner1(first);
        zone.setCorner2(second);
        zone.addGoldLocation(new Pair<>(gold, second));
        zone.addOffLimits(first);
        zone.addOffLimits(gold);

        assertNull(handler.getZone("missing"));
        handler.storeZone(zone);
        assertEquals(List.of("map 'one'"), handler.getMurderMysteryZones());
        MurderMysteryZone loaded = handler.getZone("map 'one'");
        assertEquals(first, loaded.getCorner1());
        assertEquals(second, loaded.getCorner2());
        assertEquals(gold, loaded.getAllGoldLoc().getFirst().getFirst());
        assertEquals(second, loaded.getAllGoldLoc().getFirst().getSecond());
        assertEquals(List.of(first, gold), loaded.getOffLimits());

        zone.getAllGoldLoc().clear();
        zone.getOffLimits().clear();
        zone.setCorner1(gold);
        handler.storeZone(zone);
        loaded = handler.getZone("map 'one'");
        assertEquals(gold, loaded.getCorner1());
        assertTrue(loaded.getAllGoldLoc().isEmpty());
        assertTrue(loaded.getOffLimits().isEmpty());
        try (Statement statement = database.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM location")) {
            assertTrue(rows.next());
            assertEquals(2, rows.getInt(1));
        }
    }

    @Test
    void failedReplacementRollsBackAndMissingWorldIsReported() throws Exception {
        MurderMysteryZone zone = new MurderMysteryZone();
        zone.setMapName("map");
        zone.setCorner1(new Location(world, 1, 2, 3));
        zone.setCorner2(new Location(world, 4, 5, 6));
        handler.storeZone(zone);
        try (Statement statement = database.createStatement()) {
            statement.execute("CREATE TRIGGER fail_gold BEFORE INSERT ON map_gold BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }
        zone.addGoldLocation(new Pair<>(zone.getCorner1(), zone.getCorner2()));
        assertThrows(IllegalStateException.class, () -> handler.storeZone(zone));
        assertTrue(handler.getZone("map").getAllGoldLoc().isEmpty());
        assertEquals(zone.getCorner1(), handler.getZone("map").getCorner1());

        UUID worldId = world.getUID();
        bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> handler.getZone("map"));
    }

    @Test
    void rejectsIncompleteMapsAndBrokenLocationReferences() throws Exception {
        MurderMysteryZone zone = new MurderMysteryZone();
        zone.setMapName("map");
        zone.setCorner1(new Location(world, 1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> handler.storeZone(zone));
        assertTrue(handler.getMurderMysteryZones().isEmpty());

        zone.setCorner2(new Location(world, 4, 5, 6));
        zone.addOffLimits(zone.getCorner1());
        handler.storeZone(zone);
        try (Statement statement = database.createStatement()) {
            statement.execute("DELETE FROM location WHERE id = (SELECT coordinate_id FROM mm_offlimit)");
        }
        assertThrows(IllegalStateException.class, () -> handler.getZone("map"));
    }
}
