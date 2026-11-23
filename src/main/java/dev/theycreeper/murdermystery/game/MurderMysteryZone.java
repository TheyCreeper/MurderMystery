package dev.theycreeper.murdermystery.game;

import org.bukkit.Color;
import org.bukkit.Location;

import java.util.ArrayList;

public class MurderMysteryZone {
    private Color CORNER_1_COLOR = Color.GREEN;
    private Color CORNER_2_COLOR = Color.BLUE;
    private Location corner1;
    private Location corner2;
    private ArrayList<Location> goldSpawnLocations = new ArrayList<>();
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

    public void addGoldLocation(ArrayList<Location> goldSpawnLocations) {
        this.goldSpawnLocations.addAll(goldSpawnLocations);
    }

    public void setOffLimits(ArrayList<Location> offLimits) {
        this.offLimits.addAll(offLimits);
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

    public ArrayList<Location> getGoldSpawnLocations() {
        return goldSpawnLocations;
    }

    public ArrayList<Location> getOffLimits() {
        return offLimits;
    }

}
