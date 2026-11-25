package dev.theycreeper.murdermystery.saving;

import dev.theycreeper.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

public class SQLHandler {

    private Connection connection;
    Logger logger = MurderMystery.getInstance().getLogger();

    private SQLHandler() {
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
                    CREATE TABLE IF NOT EXISTS ""
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS "idx_items_owner_type"
                    ON "inventory_item" ("uuid")""");
        } catch (SQLException e) {
            logger.severe("Failed to initialize the inventory database: " + e.getMessage());
            throw new IllegalStateException("Failed to initialize the inventory database", e);
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
