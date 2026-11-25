package dev.theycreeper.murdermystery.game;

import dev.theycreeper.murdermystery.model.Pair;
import org.bukkit.Color;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class MurderMysteryZone {
    private Color CORNER_1_COLOR = Color.GREEN;
    private Color CORNER_2_COLOR = Color.BLUE;
    private Location corner1;
    private Location corner2;
    private List<Pair<Location, Location>> goldSpawnLocations = new ArrayList<>();
    private ArrayList<Location> offLimits = new ArrayList<>();
    private String mapName;

    public MurderMysteryZone() {
    }

    public void setMapName(String name) {
        this.mapName = name;
    }

    public void setCorner1(Location corner1) {
        this.corner1 = corner1;
    }

    public void setCorner2(Location corner2) {
        this.corner2 = corner2;
    }

    public void addGoldLocation(Pair<Location, Location> goldSpawnLocation) {
        this.goldSpawnLocations.add(goldSpawnLocation);
    }

    public void removeGoldLocationZone(Location goldLoc) {
        goldSpawnLocations.remove(getGoldSpawnLocationZone(goldLoc));
    }

    public void addOffLimits(Location offLimit) {
        this.offLimits.add(offLimit);
    }
    public void remOffLimits(Location offLimit) {
        this.offLimits.remove(offLimit);
    }

    public String getMapName() {
        return mapName;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public Location getNonNullCorner() {
        if (corner1 == null) return corner2;
        return corner1;
    }

    public Color getNonNullColor() {
        if (corner1 == null) return CORNER_2_COLOR;
        return CORNER_1_COLOR;
    }

    public Color getCORNER_1_COLOR() {
        return CORNER_1_COLOR;
    }

    public Color getCORNER_2_COLOR() {
        return CORNER_2_COLOR;
    }
    public List<Pair<Location, Location>> getAllGoldLoc() {
        return goldSpawnLocations;
    }
    public Pair<Location, Location> getGoldSpawnLocationZone(Location goldLoc) {
        for (Pair<Location, Location> pair : goldSpawnLocations) {
            Location c1 = pair.getFirst();
            Location c2 = pair.getSecond();

            if (goldLoc.getWorld().equals(c1.getWorld())
                    && goldLoc.getWorld().equals(c2.getWorld())
                    && goldLoc.getX() >= Math.min(c1.getX(), c2.getX())
                    && goldLoc.getX() <= Math.max(c1.getX(), c2.getX())
                    && goldLoc.getY() >= Math.min(c1.getY(), c2.getY())
                    && goldLoc.getY() <= Math.max(c1.getY(), c2.getY())
                    && goldLoc.getZ() >= Math.min(c1.getZ(), c2.getZ())
                    && goldLoc.getZ() <= Math.max(c1.getZ(), c2.getZ())) {
                return pair;
            }
        }

        return null;
    }

    public ArrayList<Location> getOffLimits() {
        return offLimits;
    }

}
