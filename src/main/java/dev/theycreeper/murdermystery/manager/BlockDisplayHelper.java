package dev.theycreeper.murdermystery.manager;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class BlockDisplayHelper {
    public static List<BlockDisplay> createDisplayHighlight(Location corner1, Location corner2, Color borderColor) {
        List<BlockDisplay> displays = new ArrayList<>(36);

        World world1 = (corner1 != null) ? corner1.getWorld() : null;
        World world2 = (corner2 != null) ? corner2.getWorld() : null;
        // 1. If both corners exist in the same world, draw the outer bounding box (Aqua)
        if (world1 != null && world1.equals(world2)) {
            double minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
            double minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
            double minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());

            double maxX = Math.max(corner1.getBlockX(), corner2.getBlockX()) + 1.0;
            double maxY = Math.max(corner1.getBlockY(), corner2.getBlockY()) + 1.0;
            double maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ()) + 1.0;

            displays.addAll(buildBoxEdges(
                    world1, minX, minY, minZ, maxX, maxY, maxZ, 0.02f, borderColor
            ));
        }

        // 2. Draw Corner 1 wireframe (Red) snapped to full block grid
        if (world1 != null) {
            int c1X = corner1.getBlockX();
            int c1Y = corner1.getBlockY();
            int c1Z = corner1.getBlockZ();

            displays.addAll(buildBoxEdges(
                    world1,
                    c1X - 0.005, c1Y - 0.005, c1Z - 0.005,
                    c1X + 1.005, c1Y + 1.005, c1Z + 1.005,
                    0.025f, Color.RED
            ));
        }

        // 3. Draw Corner 2 wireframe (Lime) snapped to full block grid (skip if same block as Corner 1)
        if (world2 != null && !isSameBlock(corner1, corner2)) {
            int c2X = corner2.getBlockX();
            int c2Y = corner2.getBlockY();
            int c2Z = corner2.getBlockZ();

            displays.addAll(buildBoxEdges(
                    world2,
                    c2X - 0.005, c2Y - 0.005, c2Z - 0.005,
                    c2X + 1.005, c2Y + 1.005, c2Z + 1.005,
                    0.025f, Color.LIME
            ));
        }

        return displays;
    }

    private static boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        if (loc1.getWorld() == null || !loc1.getWorld().equals(loc2.getWorld())) return false;
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    private static List<BlockDisplay> buildBoxEdges(World world, double minX, double minY, double minZ,
                                             double maxX, double maxY, double maxZ,
                                             float thickness, Color glowColor) {
        float sizeX = (float) (maxX - minX);
        float sizeY = (float) (maxY - minY);
        float sizeZ = (float) (maxZ - minZ);
        float halfT = thickness / 2.0f;

        List<BlockDisplay> edges = new ArrayList<>(12);

        // 4 X-axis edges
        edges.add(spawnEdge(world, minX, minY - halfT, minZ - halfT, sizeX, thickness, thickness, glowColor));
        edges.add(spawnEdge(world, minX, minY - halfT, maxZ - halfT, sizeX, thickness, thickness, glowColor));
        edges.add(spawnEdge(world, minX, maxY - halfT, minZ - halfT, sizeX, thickness, thickness, glowColor));
        edges.add(spawnEdge(world, minX, maxY - halfT, maxZ - halfT, sizeX, thickness, thickness, glowColor));

        // 4 Z-axis edges
        edges.add(spawnEdge(world, minX - halfT, minY - halfT, minZ, thickness, thickness, sizeZ, glowColor));
        edges.add(spawnEdge(world, maxX - halfT, minY - halfT, minZ, thickness, thickness, sizeZ, glowColor));
        edges.add(spawnEdge(world, minX - halfT, maxY - halfT, minZ, thickness, thickness, sizeZ, glowColor));
        edges.add(spawnEdge(world, maxX - halfT, maxY - halfT, minZ, thickness, thickness, sizeZ, glowColor));

        // 4 Y-axis edges
        edges.add(spawnEdge(world, minX - halfT, minY, minZ - halfT, thickness, sizeY, thickness, glowColor));
        edges.add(spawnEdge(world, maxX - halfT, minY, minZ - halfT, thickness, sizeY, thickness, glowColor));
        edges.add(spawnEdge(world, minX - halfT, minY, maxZ - halfT, thickness, sizeY, thickness, glowColor));
        edges.add(spawnEdge(world, maxX - halfT, minY, maxZ - halfT, thickness, sizeY, thickness, glowColor));

        return edges;
    }

    private static BlockDisplay spawnEdge(World world, double x, double y, double z,
                                   float scaleX, float scaleY, float scaleZ, Color glowColor) {
        Location loc = new Location(world, x, y, z);
        BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);

        display.setBlock(Material.WHITE_CONCRETE.createBlockData());
        display.setGlowing(true);
        display.setGlowColorOverride(glowColor);
        display.setPersistent(false);
        display.setBrightness(new Display.Brightness(15, 15));

        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(scaleX, scaleY, scaleZ),
                new AxisAngle4f()
        ));

        return display;
    }
}
