package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZoneMapLifecycleTest {
    private final ZoneCreationManager manager = new ZoneCreationManager();
    private final MurderMysteryZone map = new MurderMysteryZone();

    @Test
    void loadsAndUnloadsOnlyBarrierLocations() {
        map.setMapName("map");
        Block air = block(Material.AIR);
        Block barrier = block(Material.BARRIER);
        Block other = block(Material.STONE);
        map.addOffLimits(location(air));
        map.addOffLimits(location(barrier));
        assertTrue(manager.loadMap(map));
        assertEquals(Material.BARRIER, air.getType());
        assertEquals(Material.BARRIER, barrier.getType());

        map.addOffLimits(location(other));
        manager.unloadMap(map);
        assertEquals(Material.AIR, air.getType());
        assertEquals(Material.AIR, barrier.getType());
        assertEquals(Material.STONE, other.getType());
        verify(other, never()).setType(any(), anyBoolean());
    }

    @Test
    void highlightsObstructionsAndDoesNotPartiallyLoad() {
        map.setMapName("map");
        Block air = block(Material.AIR);
        Block stone = block(Material.STONE);
        Location clear = location(air);
        Location obstruction = location(stone);
        map.addOffLimits(clear);
        map.addOffLimits(obstruction);
        BlockDisplay display = mock(BlockDisplay.class);
        try (MockedStatic<BlockDisplayHelper> helper = mockStatic(BlockDisplayHelper.class)) {
            helper.when(() -> BlockDisplayHelper.createDisplayHighlight(
                    obstruction, obstruction, Color.RED, "mapObstruction", false)).thenReturn(List.of(display));
            assertFalse(manager.loadMap(map));
            assertEquals(Material.AIR, air.getType());
            assertEquals(Material.STONE, stone.getType());
            helper.verify(() -> BlockDisplayHelper.createDisplayHighlight(
                    obstruction, obstruction, Color.RED, "mapObstruction", false));

            stone.setType(Material.AIR, false);
            assertTrue(manager.loadMap(map));
            verify(display).remove();
            assertEquals(Material.BARRIER, air.getType());
            assertEquals(Material.BARRIER, stone.getType());
            manager.unloadMap(map);
        }
    }

    @Test
    void rollsBackNewBarriersWhenPlacementFails() {
        map.setMapName("map");
        Block first = block(Material.AIR);
        Block second = block(Material.AIR);
        map.addOffLimits(location(first));
        map.addOffLimits(location(second));
        doThrow(new IllegalStateException("failed")).when(second).setType(Material.BARRIER, false);
        assertThrows(IllegalStateException.class, () -> manager.loadMap(map));
        assertEquals(Material.AIR, first.getType());
    }

    private Location location(Block block) {
        Location location = mock(Location.class);
        when(location.getBlock()).thenReturn(block);
        return location;
    }

    private Block block(Material initial) {
        Block block = mock(Block.class);
        AtomicReference<Material> material = new AtomicReference<>(initial);
        when(block.getType()).thenAnswer(invocation -> material.get());
        when(block.isEmpty()).thenAnswer(invocation -> material.get() == Material.AIR);
        doAnswer(invocation -> {
            material.set(invocation.getArgument(0));
            return null;
        }).when(block).setType(any(), anyBoolean());
        return block;
    }
}
