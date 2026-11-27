package dev.theycreeper.murdermystery.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapBoundsTest {
    @Test
    void checksAllAxesUsingInclusiveCornerBlocksEvenWhenCornersAreReversed() {
        World world = mock(World.class);
        MurderMysteryZone zone = new MurderMysteryZone();
        zone.setCorner1(new Location(world, 2, 12, 4));
        zone.setCorner2(new Location(world, -2, 10, -3));
        MurderMysteryGame game = new MurderMysteryGame(zone);
        PlayerMoveEvent event = mock(PlayerMoveEvent.class);

        for (Location inside : new Location[]{
                new Location(world, -2, 10, -3),
                new Location(world, 2.99, 12.99, 4.99),
                new Location(world, -1.5, 11, 0)}) {
            when(event.getTo()).thenReturn(inside);
            assertFalse(game.isGoingOutOfBounds(event));
        }
        for (Location outside : new Location[]{
                new Location(world, -2.01, 11, 0), new Location(world, 3, 11, 0),
                new Location(world, 0, 9.99, 0), new Location(world, 0, 13, 0),
                new Location(world, 0, 11, -3.01), new Location(world, 0, 11, 5)}) {
            when(event.getTo()).thenReturn(outside);
            assertTrue(game.isGoingOutOfBounds(event));
        }
    }

    @Test
    void rejectsAnotherWorldAndAllowsMovementBackInside() {
        World world = mock(World.class);
        MurderMysteryZone zone = new MurderMysteryZone();
        zone.setCorner1(new Location(world, 0, 0, 0));
        zone.setCorner2(new Location(world, 2, 2, 2));
        MurderMysteryGame game = new MurderMysteryGame(zone);
        PlayerMoveEvent event = mock(PlayerMoveEvent.class);
        when(event.getFrom()).thenReturn(new Location(world, -1, 1, 1));
        when(event.getTo()).thenReturn(new Location(mock(World.class), 1, 1, 1));
        assertTrue(game.isGoingOutOfBounds(event));
        when(event.getTo()).thenReturn(new Location(world, 1, 1, 1));
        assertFalse(game.isGoingOutOfBounds(event));
    }

    @Test
    void rejectsInvalidMapCornersAndCoordinates() {
        World world = mock(World.class);
        MurderMysteryZone zone = new MurderMysteryZone();
        zone.setCorner1(new Location(world, 0, 0, 0));
        zone.setCorner2(new Location(mock(World.class), 2, 2, 2));
        MurderMysteryGame game = new MurderMysteryGame(zone);
        PlayerMoveEvent event = mock(PlayerMoveEvent.class);
        assertThrows(IllegalStateException.class, () -> game.isGoingOutOfBounds(event));
        zone.setCorner2(new Location(world, 2, 2, 2));
        when(event.getTo()).thenReturn(new Location(world, Double.NaN, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> game.isGoingOutOfBounds(event));
    }
}
