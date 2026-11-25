package dev.theycreeper.murdermystery.listeners;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.manager.CurrentPlayerMenu;
import dev.theycreeper.murdermystery.manager.ZoneCreationManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockPlaceListenerTest {
    private final BlockPlaceListener listener = new BlockPlaceListener();
    private final ZoneCreationManager manager = mock(ZoneCreationManager.class);
    private final MurderMystery plugin = mock(MurderMystery.class);
    private final Player player = mock(Player.class);
    private final Block block = mock(Block.class);
    private final Location location = mock(Location.class);
    private final UUID playerId = UUID.randomUUID();
    private MockedStatic<MurderMystery> pluginInstance;

    @BeforeEach
    void setUp() {
        pluginInstance = mockStatic(MurderMystery.class);
        pluginInstance.when(MurderMystery::getInstance).thenReturn(plugin);
        when(plugin.getZoneCreationManager()).thenReturn(manager);
        when(player.getUniqueId()).thenReturn(playerId);
        when(block.getType()).thenReturn(Material.BARRIER);
        when(block.getLocation()).thenReturn(location);
    }

    @AfterEach
    void tearDown() {
        pluginInstance.close();
    }

    @Test
    void nonEditorsAreUnaffected() {
        BlockPlaceEvent place = placeEvent();
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        listener.onBlockPlaced(place);
        listener.onBlockBreak(breakEvent);
        listener.recordBlockPlaced(place);
        listener.recordBlockBreak(breakEvent);
        assertFalse(place.isCancelled());
        assertFalse(breakEvent.isCancelled());
        verify(manager, never()).addOffLimitBlock(any());
        verify(manager, never()).remOffLimitBlock(any());
    }

    @Test
    void editorCanOnlyModifyBarriersInOffLimitMode() {
        when(manager.isPlayerEditing(playerId)).thenReturn(true);
        BlockPlaceEvent place = placeEvent();
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        when(manager.getPlayerState(playerId)).thenReturn(CurrentPlayerMenu.mainMenu);
        listener.onBlockPlaced(place);
        listener.onBlockBreak(breakEvent);
        assertTrue(place.isCancelled());
        assertTrue(breakEvent.isCancelled());

        when(manager.getPlayerState(playerId)).thenReturn(CurrentPlayerMenu.offLimitZoneEdit);
        when(block.getType()).thenReturn(Material.STONE);
        place = placeEvent();
        breakEvent = new BlockBreakEvent(block, player);
        listener.onBlockPlaced(place);
        listener.onBlockBreak(breakEvent);
        assertTrue(place.isCancelled());
        assertTrue(breakEvent.isCancelled());

        when(block.getType()).thenReturn(Material.BARRIER);
        place = placeEvent();
        breakEvent = new BlockBreakEvent(block, player);
        listener.onBlockPlaced(place);
        listener.onBlockBreak(breakEvent);
        assertFalse(place.isCancelled());
        assertFalse(breakEvent.isCancelled());
        listener.recordBlockPlaced(place);
        listener.recordBlockBreak(breakEvent);
        verify(manager).addOffLimitBlock(location);
        verify(manager).remOffLimitBlock(location);
    }

    @Test
    void cancelledEventsAreNotRecorded() throws Exception {
        when(manager.isPlayerEditing(playerId)).thenReturn(true);
        when(manager.getPlayerState(playerId)).thenReturn(CurrentPlayerMenu.offLimitZoneEdit);
        BlockPlaceEvent place = placeEvent();
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        place.setCancelled(true);
        breakEvent.setCancelled(true);
        register("recordBlockPlaced", BlockPlaceEvent.class).callEvent(place);
        register("recordBlockBreak", BlockBreakEvent.class).callEvent(breakEvent);
        verify(manager, never()).addOffLimitBlock(any());
        verify(manager, never()).remOffLimitBlock(any());
    }

    private BlockPlaceEvent placeEvent() {
        return new BlockPlaceEvent(block, mock(org.bukkit.block.BlockState.class), block,
                mock(ItemStack.class), player, true, EquipmentSlot.HAND);
    }

    private RegisteredListener register(String name, Class<? extends org.bukkit.event.Event> eventType) throws Exception {
        var method = BlockPlaceListener.class.getDeclaredMethod(name, eventType);
        EventHandler annotation = method.getAnnotation(EventHandler.class);
        return new RegisteredListener(listener, EventExecutor.create(method, eventType),
                annotation.priority(), plugin, annotation.ignoreCancelled());
    }
}
