package dev.theycreeper.murdermystery.saving;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.model.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class SQLHandler {

    private Connection connection;
    private final Logger logger;

    private SQLHandler() {
        this(null, MurderMystery.getInstance().getLogger());
    }

    SQLHandler(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    private static SQLHandler instance;

    public static SQLHandler getInstance() {
        if (instance == null) instance = new SQLHandler();
        return instance;
    }

    public void init() {
        try (Statement statement = getConnection().createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "player" (
                        "uuid" TEXT NOT NULL PRIMARY KEY,
                        "realname" TEXT
                    )""");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "inventory_item" (
                        "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                        "uuid" TEXT NOT NULL,
                        "slot" INTEGER NOT NULL,
                        "material" TEXT NOT NULL,
                        "amount" INTEGER NOT NULL DEFAULT 1,
                        "item_data" BLOB NOT NULL,
                        FOREIGN KEY("uuid") REFERENCES "player"("uuid")
                    )""");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "location" (
                        "id" INTEGER NOT NULL UNIQUE,
                        "x" REAL NOT NULL,
                        "y" REAL NOT NULL,
                        "z" REAL NOT NULL,
                        "world" TEXT,
                        "yaw" REAL NOT NULL DEFAULT 0,
                        "pitch" REAL NOT NULL DEFAULT 0,
                        PRIMARY KEY("id" AUTOINCREMENT)
                    );""");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "map_gold" (
                        "id" INTEGER NOT NULL UNIQUE,
                        "corner1" INTEGER NOT NULL,
                        "corner2" INTEGER NOT NULL,
                        "map_id" INTEGER,
                        PRIMARY KEY("id" AUTOINCREMENT),
                        FOREIGN KEY("corner1") REFERENCES "location"("id"),
                        FOREIGN KEY("corner2") REFERENCES "location"("id"),
                        FOREIGN KEY("map_id") REFERENCES "mm_map"("id")
                    );
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "map_zone" (
                        "id" INTEGER NOT NULL UNIQUE,
                        "corner1" INTEGER NOT NULL,
                        "corner2" INTEGER NOT NULL,
                        "map_id" INTEGER,
                        PRIMARY KEY("id" AUTOINCREMENT),
                        FOREIGN KEY("corner1") REFERENCES "location"("id"),
                        FOREIGN KEY("corner2") REFERENCES "location"("id"),
                        FOREIGN KEY("map_id") REFERENCES "mm_map"("id")
                    );
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "mm_map" (
                        "id" INTEGER NOT NULL UNIQUE,
                        "name" TEXT NOT NULL,
                        PRIMARY KEY("id" AUTOINCREMENT)
                    );
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "mm_offlimit" (
                        "id" INTEGER NOT NULL UNIQUE,
                        "map_id" INTEGER NOT NULL,
                        "coordinate_id" INTEGER NOT NULL,
                        PRIMARY KEY("id" AUTOINCREMENT),
                        FOREIGN KEY("coordinate_id") REFERENCES "location"("id"),
                        FOREIGN KEY("map_id") REFERENCES "mm_map"("id")
                    );
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS "idx_items_owner_type"
                    ON "inventory_item" ("uuid")""");
            addLocationColumnIfMissing(statement, "world", "TEXT");
            addLocationColumnIfMissing(statement, "yaw", "REAL NOT NULL DEFAULT 0");
            addLocationColumnIfMissing(statement, "pitch", "REAL NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            logger.severe("Failed to initialize the database: " + e.getMessage());
            throw new IllegalStateException("Failed to initialize the database", e);
        }
    }

    private void addLocationColumnIfMissing(Statement statement, String name, String definition) throws SQLException {
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(location)")) {
            while (columns.next()) {
                if (name.equals(columns.getString("name"))) return;
            }
        }
        statement.execute("ALTER TABLE location ADD COLUMN " + name + " " + definition);
    }

    public List<String> getMurderMysteryZones() {
        List<String> names = new ArrayList<>();
        try (Statement statement = getConnection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT name FROM mm_map ORDER BY name")) {
            while (rows.next()) names.add(rows.getString("name"));
            return names;
        } catch (SQLException e) {
            logger.severe("Failed to list maps: " + e.getMessage());
            throw new IllegalStateException("Failed to list maps", e);
        }
    }

    public MurderMysteryZone getZone(String name) {
        try {
            Connection database = getConnection();
            try (PreparedStatement statement = database.prepareStatement("SELECT id FROM mm_map WHERE name = ?")) {
                statement.setString(1, name);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) return null;
                    long mapId = rows.getLong("id");
                    if (rows.next()) throw new IllegalStateException("Multiple maps named " + name);
                    MurderMysteryZone zone = new MurderMysteryZone();
                    zone.setMapName(name);
                    List<Pair<Location, Location>> corners = getLocationPairs(database, "map_zone", mapId);
                    if (corners.size() != 1) {
                        throw new IllegalStateException("Map " + name + " must have exactly one zone");
                    }
                    zone.setCorner1(corners.getFirst().getFirst());
                    zone.setCorner2(corners.getFirst().getSecond());
                    getLocationPairs(database, "map_gold", mapId).forEach(zone::addGoldLocation);
                    try (PreparedStatement offLimits = database.prepareStatement("""
                            SELECT l.* FROM mm_offlimit o LEFT JOIN location l ON l.id = o.coordinate_id
                            WHERE o.map_id = ? ORDER BY o.id""")) {
                        offLimits.setLong(1, mapId);
                        try (ResultSet locations = offLimits.executeQuery()) {
                            while (locations.next()) zone.addOffLimits(readLocation(locations, ""));
                        }
                    }
                    return zone;
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to retrieve map " + name + ": " + e.getMessage());
            throw new IllegalStateException("Failed to retrieve map " + name, e);
        }
    }

    private List<Pair<Location, Location>> getLocationPairs(Connection database, String table, long mapId) throws SQLException {
        List<Pair<Location, Location>> pairs = new ArrayList<>();
        String query = """
                SELECT a.world AS a_world, a.x AS a_x, a.y AS a_y, a.z AS a_z, a.yaw AS a_yaw, a.pitch AS a_pitch,
                       b.world AS b_world, b.x AS b_x, b.y AS b_y, b.z AS b_z, b.yaw AS b_yaw, b.pitch AS b_pitch
                FROM %s p LEFT JOIN location a ON a.id = p.corner1 LEFT JOIN location b ON b.id = p.corner2
                WHERE p.map_id = ? ORDER BY p.id""".formatted(table);
        try (PreparedStatement statement = database.prepareStatement(query)) {
            statement.setLong(1, mapId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) pairs.add(new Pair<>(readLocation(rows, "a_"), readLocation(rows, "b_")));
            }
        }
        return pairs;
    }

    private Location readLocation(ResultSet row, String prefix) throws SQLException {
        String worldId = row.getString(prefix + "world");
        if (worldId == null) throw new IllegalStateException("Stored location has no world");
        World world;
        try {
            world = Bukkit.getWorld(UUID.fromString(worldId));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Stored location has invalid world UUID: " + worldId, e);
        }
        if (world == null) throw new IllegalStateException("World is not loaded: " + worldId);
        return new Location(world, row.getDouble(prefix + "x"), row.getDouble(prefix + "y"),
                row.getDouble(prefix + "z"), row.getFloat(prefix + "yaw"), row.getFloat(prefix + "pitch"));
    }

    public void storeZone(MurderMysteryZone zone) {
        if (zone == null || zone.getMapName() == null || zone.getMapName().isBlank()) {
            throw new IllegalArgumentException("Map must have a name");
        }
        validateLocation(zone.getCorner1());
        validateLocation(zone.getCorner2());
        for (Pair<Location, Location> gold : zone.getAllGoldLoc()) {
            if (gold == null) throw new IllegalArgumentException("Gold area must have two corners");
            validateLocation(gold.getFirst());
            validateLocation(gold.getSecond());
        }
        for (Location location : zone.getOffLimits()) validateLocation(location);

        String name = zone.getMapName();
        try {
            Connection database = getConnection();
            boolean previousAutoCommit = database.getAutoCommit();
            try {
                database.setAutoCommit(false);
                Long mapId = null;
                try (PreparedStatement find = database.prepareStatement("SELECT id FROM mm_map WHERE name = ?")) {
                    find.setString(1, name);
                    try (ResultSet rows = find.executeQuery()) {
                        if (rows.next()) {
                            mapId = rows.getLong(1);
                            if (rows.next()) throw new IllegalStateException("Multiple maps named " + name);
                        }
                    }
                }
                if (mapId == null) {
                    try (PreparedStatement insert = database.prepareStatement("INSERT INTO mm_map (name) VALUES (?)")) {
                        insert.setString(1, name);
                        insert.executeUpdate();
                    }
                    mapId = lastInsertId(database);
                } else {
                    List<Long> oldLocations = new ArrayList<>();
                    try (PreparedStatement locations = database.prepareStatement("""
                            SELECT corner1 FROM map_zone WHERE map_id = ?
                            UNION SELECT corner2 FROM map_zone WHERE map_id = ?
                            UNION SELECT corner1 FROM map_gold WHERE map_id = ?
                            UNION SELECT corner2 FROM map_gold WHERE map_id = ?
                            UNION SELECT coordinate_id FROM mm_offlimit WHERE map_id = ?""")) {
                        for (int i = 1; i <= 5; i++) locations.setLong(i, mapId);
                        try (ResultSet rows = locations.executeQuery()) {
                            while (rows.next()) oldLocations.add(rows.getLong(1));
                        }
                    }
                    for (String table : List.of("map_zone", "map_gold", "mm_offlimit")) {
                        try (PreparedStatement delete = database.prepareStatement("DELETE FROM " + table + " WHERE map_id = ?")) {
                            delete.setLong(1, mapId);
                            delete.executeUpdate();
                        }
                    }
                    try (PreparedStatement cleanup = database.prepareStatement("""
                                DELETE FROM location WHERE id = ? AND id NOT IN (
                                    SELECT corner1 FROM map_zone UNION SELECT corner2 FROM map_zone
                                    UNION SELECT corner1 FROM map_gold UNION SELECT corner2 FROM map_gold
                                    UNION SELECT coordinate_id FROM mm_offlimit)""")) {
                        for (long id : oldLocations) {
                            cleanup.setLong(1, id);
                            cleanup.addBatch();
                        }
                        cleanup.executeBatch();
                    }
                }
                insertLocationPair(database, "map_zone", mapId, zone.getCorner1(), zone.getCorner2());
                for (Pair<Location, Location> gold : zone.getAllGoldLoc()) {
                    insertLocationPair(database, "map_gold", mapId, gold.getFirst(), gold.getSecond());
                }
                for (Location location : zone.getOffLimits()) {
                    long locationId = insertLocation(database, location);
                    try (PreparedStatement insert = database.prepareStatement(
                            "INSERT INTO mm_offlimit (map_id, coordinate_id) VALUES (?, ?)")) {
                        insert.setLong(1, mapId);
                        insert.setLong(2, locationId);
                        insert.executeUpdate();
                    }
                }
                database.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    database.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                database.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            logger.severe("Failed to store map " + name + ": " + e.getMessage());
            throw new IllegalStateException("Failed to store map " + name, e);
        }
    }

    private void validateLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Each map location must have a loaded world");
        }
    }

    private void insertLocationPair(Connection database, String table, long mapId, Location first, Location second)
            throws SQLException {
        long firstId = insertLocation(database, first);
        long secondId = insertLocation(database, second);
        try (PreparedStatement insert = database.prepareStatement(
                "INSERT INTO " + table + " (map_id, corner1, corner2) VALUES (?, ?, ?)")) {
            insert.setLong(1, mapId);
            insert.setLong(2, firstId);
            insert.setLong(3, secondId);
            insert.executeUpdate();
        }
    }

    private long insertLocation(Connection database, Location location) throws SQLException {
        try (PreparedStatement insert = database.prepareStatement(
                "INSERT INTO location (world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, location.getWorld().getUID().toString());
            insert.setDouble(2, location.getX());
            insert.setDouble(3, location.getY());
            insert.setDouble(4, location.getZ());
            insert.setFloat(5, location.getYaw());
            insert.setFloat(6, location.getPitch());
            insert.executeUpdate();
        }
        return lastInsertId(database);
    }

    private long lastInsertId(Connection database) throws SQLException {
        try (Statement statement = database.createStatement();
             ResultSet row = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!row.next()) throw new SQLException("Failed to retrieve inserted row ID");
            return row.getLong(1);
        }
    }

    public Inventory getStoredInventory(UUID uuid) {
        Inventory inventory = Bukkit.createInventory(null, 45);
        String query = "SELECT slot, item_data FROM inventory_item WHERE uuid = ? ORDER BY slot";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int slot = resultSet.getInt("slot");
                    if (slot < 0 || slot >= 41) {
                        throw new IllegalStateException("Stored inventory contains invalid slot " + slot);
                    }
                    inventory.setItem(slot, ItemStack.deserializeBytes(resultSet.getBytes("item_data")));
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to retrieve inventory for " + uuid + ": " + e.getMessage());
            throw new IllegalStateException("Failed to retrieve stored inventory for " + uuid, e);
        }
        return inventory;
    }

    public void storeInventory(Player player) {
        UUID uuid = player.getUniqueId();
        Connection database;
        try {
            database = getConnection();
            boolean previousAutoCommit = database.getAutoCommit();
            try {
                database.setAutoCommit(false);
                try (PreparedStatement ensurePlayer = database.prepareStatement(
                             "INSERT OR IGNORE INTO player (uuid, realname) VALUES (?, ?)");
                     PreparedStatement deleteInventory = database.prepareStatement(
                             "DELETE FROM inventory_item WHERE uuid = ?");
                     PreparedStatement insertItem = database.prepareStatement(
                             "INSERT INTO inventory_item (uuid, slot, material, amount, item_data) VALUES (?, ?, ?, ?, ?)")) {
                    ensurePlayer.setString(1, uuid.toString());
                    ensurePlayer.setString(2, player.getName());
                    ensurePlayer.executeUpdate();

                    deleteInventory.setString(1, uuid.toString());
                    deleteInventory.executeUpdate();

                    for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                        ItemStack item = player.getInventory().getItem(slot);
                        if (item == null || item.isEmpty()) {
                            continue;
                        }

                        insertItem.setString(1, uuid.toString());
                        insertItem.setInt(2, slot);
                        insertItem.setString(3, item.getType().name());
                        insertItem.setInt(4, item.getAmount());
                        insertItem.setBytes(5, item.serializeAsBytes());
                        insertItem.addBatch();
                    }
                    insertItem.executeBatch();
                }
                database.commit();
            } catch (SQLException e) {
                try {
                    database.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                database.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            logger.severe("Failed to store inventory for " + uuid + ": " + e.getMessage());
            throw new IllegalStateException("Failed to store inventory for " + uuid, e);
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = sqlOrSqlLite();
        }
        return connection;
    }

    private Connection sqlOrSqlLite() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + MurderMystery.getInstance().getDataFolder() + "/homes.db");
    }
}
