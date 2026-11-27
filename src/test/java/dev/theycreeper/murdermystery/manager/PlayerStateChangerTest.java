package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerStateChangerTest {
    @Test
    void normalModeRestoresInventoryAndGamemode() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        Inventory saved = mock(Inventory.class);
        ItemStack item = mock(ItemStack.class);
        ItemStack helmet = mock(ItemStack.class);
        ItemStack offhand = mock(ItemStack.class);
        UUID uuid = UUID.randomUUID();
        SQLHandler database = mock(SQLHandler.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getInventory()).thenReturn(inventory);
        when(database.getStoredInventory(uuid)).thenReturn(saved);
        when(saved.getItem(4)).thenReturn(item);
        when(saved.getItem(36)).thenReturn(helmet);
        when(saved.getItem(40)).thenReturn(offhand);
        try (MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class);
             MockedStatic<MurderMystery> plugin = mockStatic(MurderMystery.class)) {
            sql.when(SQLHandler::getInstance).thenReturn(database);
            plugin.when(MurderMystery::getInstance).thenReturn(mock(MurderMystery.class));
            PlayerStateChanger.normalMode(player, GameMode.CREATIVE);
            verify(player).setGameMode(GameMode.CREATIVE);
            verify(inventory).clear();
            verify(inventory).setItem(4, item);
            verify(inventory).setHelmet(helmet);
            verify(inventory).setItemInOffHand(offhand);
        }
    }

    @Test
    void failedInventoryLoadDoesNotClearCurrentInventory() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        UUID uuid = UUID.randomUUID();
        SQLHandler database = mock(SQLHandler.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getInventory()).thenReturn(inventory);
        when(database.getStoredInventory(uuid)).thenThrow(new IllegalStateException("database failure"));
        try (MockedStatic<SQLHandler> sql = mockStatic(SQLHandler.class)) {
            sql.when(SQLHandler::getInstance).thenReturn(database);
            assertThrows(IllegalStateException.class, () -> PlayerStateChanger.normalMode(player, GameMode.SURVIVAL));
            verify(inventory, never()).clear();
            verify(player, never()).setGameMode(any());
        }
    }
}
