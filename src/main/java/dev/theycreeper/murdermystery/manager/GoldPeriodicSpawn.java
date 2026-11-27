package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryGame;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.model.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class GoldPeriodicSpawn {
    private BukkitRunnable spawner;
//    private MurderMysteryGame game;
    public void startGoldSpawning(MurderMysteryZone zone) {
        stopSpawner();

        if (zone.getAllGoldLoc().isEmpty()) {
            throw new IllegalStateException("The map has no gold spawn locations");
        }

        spawner = new BukkitRunnable() {
            @Override
            public void run() {
                Location spawnLoc = getRandomValidLocation(zone.getAllGoldLoc());
                World world = spawnLoc.getWorld();

                if (world == null) {
                    cancel();
                    return;
                }

                ItemStack itemStack = new ItemStack(Material.GOLD_INGOT, 1);

                Item droppedItem = world.dropItemNaturally(
                        spawnLoc,
                        itemStack
                );

                droppedItem.setPickupDelay(20);
            }
        };

        // Run immediately, then every 100 ticks (5 seconds)
        spawner.runTaskTimer(MurderMystery.getInstance(), 0L, 120L);
    }

    private Location getRandomValidLocation(List<Pair<Location,Location>> locations) {
        int locationIndex = (int) (Math.random() * locations.size());

        Pair<Location, Location> pair = locations.get(locationIndex);

        Location corner1 = pair.getFirst();
        Location corner2 = pair.getSecond();

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());

        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());

        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        double x = minX + Math.random() * (maxX - minX);
        double z = minZ + Math.random() * (maxZ - minZ);

        return new Location(
                corner1.getWorld(),
                x,
                corner1.getY() + 1,
                z
        );
    }

    public void stopSpawner() {
        if (spawner != null) {
            spawner.cancel();
            spawner = null;
        }
    }
}
