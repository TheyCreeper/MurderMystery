package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ZoneCreationManager {
    private MurderMysteryZone zone = new MurderMysteryZone();
    private ArrayList<UUID> edittingPlayers = new ArrayList<>();
    private List<BlockDisplay> blockHighlight;

    public void handleClickEvent(PlayerInteractEvent e) {
        switch (e.getItem().getType()) {
            case Material.STICK -> {
                if (e.getClickedBlock() == null && e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    changeCorner(e.getPlayer().getLocation(), e.getAction());
                } else {
                    changeCorner(e.getClickedBlock().getLocation(), e.getAction());
                }
            }
            case Material.GOLD_INGOT -> {

            }
        }
    }

    private void changeCorner(Location location, Action action) {
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            zone.setCorner1(location);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            zone.setCorner2(location);
        }
        zone.getCorner1().getWorld().getEntitiesByClass(BlockDisplay.class).forEach(Entity::remove);
        blockHighlight = createDisplayHighlight(zone.getCorner1(), zone.getCorner2(), Color.AQUA);
    }

    public void saveZone(Player p) {
        setPlayerNormalMode(p);
        // do something here
    }

    public void setZoneCreationMode(Player player, String mapName) {
        // sets the inventory to set the zone
        zone.setMapName(mapName);
        edittingPlayers.add(player.getUniqueId());
        setPlayerEdittingMode(player);
    }


    public List<BlockDisplay> createDisplayHighlight(Location corner1, Location corner2, Color borderColor) {
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

    private boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        if (loc1.getWorld() == null || !loc1.getWorld().equals(loc2.getWorld())) return false;
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    private List<BlockDisplay> buildBoxEdges(World world, double minX, double minY, double minZ,
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

    private BlockDisplay spawnEdge(World world, double x, double y, double z,
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

    private void pushEdittingInventory(Player p) {
        Inventory inv = p.getInventory();
        // area selector tool
        ItemStack stickItem = new ItemStack(Material.STICK, 1);
        ItemMeta meta = stickItem.getItemMeta();
        meta.displayName(Component.text("Area selector"));
        meta.addEnchant(Enchantment.MENDING, 1, false);
        stickItem.setItemMeta(meta);
        inv.setItem(0, stickItem);

        inv.setItem(1, new ItemStack(Material.GOLD_INGOT, 1));
        inv.setItem(3, new ItemStack(Material.GREEN_WOOL, 1));
        inv.setItem(3, new ItemStack(Material.RED_WOOL, 1));
    }

    private void setPlayerEdittingMode(Player player) {
        SQLHandler.getInstance().storeInventory(player);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setGameMode(GameMode.ADVENTURE);

        player.setAllowFlight(true);
        player.setFlying(true);

        // 3. No collision (players/mobs won't push them, arrows/items pass through)
        player.setCollidable(false);
        pushEdittingInventory(player);
    }

    public void setPlayerNormalMode(Player player) {
        // set the inventory back
        Inventory backedInv = SQLHandler.getInstance().getStoredInventory(player.getUniqueId());
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, backedInv.getItem(i));
        }
        inv.setHelmet(backedInv.getItem(36));
        inv.setChestplate(backedInv.getItem(37));
        inv.setLeggings(backedInv.getItem(38));
        inv.setBoots(backedInv.getItem(39));
        inv.setItemInOffHand(backedInv.getItem(40));

        player.getInventory().clear();
        player.setFlying(false);
        player.setAllowFlight(false);

        player.setCollidable(true);

        player.showPlayer(MurderMystery.getInstance(), player);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }

    public boolean isPlayerEditing(UUID uuid) {
        return edittingPlayers.contains(uuid);
    }
}
