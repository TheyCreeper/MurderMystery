package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.game.PlayerState;
import dev.theycreeper.murdermystery.model.Pair;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class ZoneCreationManager {
    private GameMode previousUserGamemode;
    private MurderMysteryZone zone = new MurderMysteryZone();
    private ArrayList<UUID> edittingPlayers = new ArrayList<>();
    private List<BlockDisplay> blockHighlight;
    private Map<UUID, CurrentPlayerMenu> playerMode = new HashMap<>();
    private Location tempGoldCorner1, tempGoldCorner2;
    private final Map<String, List<BlockDisplay>> obstructedDisplays = new HashMap<>();

    public void handleClickEvent(PlayerInteractEvent e) {
        switch (playerMode.get(e.getPlayer().getUniqueId())) {
            case CurrentPlayerMenu.mainMenu -> handleMainMenuClick(e);
            case CurrentPlayerMenu.goldZoneEdit -> handleGoldAreaClick(e);
            case CurrentPlayerMenu.zoneEdit -> handleAreaSelectionMenuClick(e);
            case CurrentPlayerMenu.offLimitZoneEdit -> handleBarrierClick(e);
        }
    }

    public void handleMainMenuClick(PlayerInteractEvent e) {
        switch (e.getItem().getType()) {
            case Material.STICK -> setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.zoneEdit);
            case Material.GOLD_INGOT -> setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.goldZoneEdit);
            case Material.BARRIER -> setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.offLimitZoneEdit);
            case Material.GREEN_WOOL -> {
                saveZone(e.getPlayer());
            }
            case Material.RED_WOOL -> {
                // do something to remove everything ig
            }
        }
        e.setCancelled(true);
    }

    public void handleAreaSelectionMenuClick(PlayerInteractEvent e) {
        switch (e.getItem().getType()) {
            case Material.STICK -> {
                if (e.getClickedBlock() == null && e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    changeCornerZone(e.getPlayer().getLocation(), e.getAction());
                } else {
                    changeCornerZone(e.getClickedBlock().getLocation(), e.getAction());
                }
            }
            case Material.GREEN_WOOL -> {
                removeDisplays("zone");
                blockHighlight = BlockDisplayHelper.createDisplayHighlight(zone.getCorner1(), zone.getCorner2(), Color.AQUA, "zone", false);
                setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.mainMenu);
            }
            case Material.RED_WOOL -> setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.mainMenu);
        }
    }

    public void handleGoldAreaClick(PlayerInteractEvent e) {
        switch (e.getItem().getType()) {
            case Material.GOLD_INGOT -> {
                if (e.getClickedBlock() == null && e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    changeCornerGold(e.getPlayer().getLocation(), e.getAction());
                } else {
                    changeCornerGold(e.getClickedBlock().getLocation(), e.getAction());
                }
            }
            case Material.GREEN_DYE -> {
                removeDisplays("goldZoneTemp");
                addGoldCornerZone();
                displayGoldZone();
            }
            case Material.RED_DYE -> {
                assert e.getClickedBlock() != null;
                removeGoldCornerZone(e.getClickedBlock().getLocation());
                displayGoldZone();
            }
            case Material.GREEN_WOOL -> {
                displayGoldZone();
                setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.mainMenu);
            }
            case Material.RED_WOOL -> setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.mainMenu);
        }
    }

    public void handleBarrierClick(PlayerInteractEvent e) {
        switch (e.getItem().getType()) {
            case Material.GREEN_WOOL -> {
                setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.mainMenu);
            }
            case Material.RED_WOOL -> {
                // ig remove the ones that the user just placed or smt
            }
        }
    }

    private void changeCornerZone(Location location, Action action) {
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            zone.setCorner1(location);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            zone.setCorner2(location);
        }
        removeDisplays("zone");
        blockHighlight = BlockDisplayHelper.createDisplayHighlight(zone.getCorner1(), zone.getCorner2(), Color.AQUA, "zone", true);
    }

    private void changeCornerGold(Location location, Action action) {
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            tempGoldCorner1 = location;
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            tempGoldCorner2 = location;
        }
        removeDisplays("goldZoneTemp");
        blockHighlight = BlockDisplayHelper.createDisplayHighlight(tempGoldCorner1, tempGoldCorner2, Color.YELLOW, "goldZoneTemp", true);
    }

    private void removeGoldCornerZone(Location loc) {
        zone.removeGoldLocationZone(loc);
        if (tempGoldCorner1 != null && tempGoldCorner2 != null &&
                loc.getWorld().equals(tempGoldCorner1.getWorld())
                && loc.getWorld().equals(tempGoldCorner2.getWorld())
                && loc.getX() >= Math.min(tempGoldCorner1.getX(), tempGoldCorner2.getX())
                && loc.getX() <= Math.max(tempGoldCorner1.getX(), tempGoldCorner2.getX())
                && loc.getY() >= Math.min(tempGoldCorner1.getY(), tempGoldCorner2.getY())
                && loc.getY() <= Math.max(tempGoldCorner1.getY(), tempGoldCorner2.getY())
                && loc.getZ() >= Math.min(tempGoldCorner1.getZ(), tempGoldCorner2.getZ())
                && loc.getZ() <= Math.max(tempGoldCorner1.getZ(), tempGoldCorner2.getZ())) {
            removeDisplays("goldZoneTemp");
            tempGoldCorner1 = null;
            tempGoldCorner2 = null;
        }
    }

    private void addGoldCornerZone() {
        if (tempGoldCorner1 != null && tempGoldCorner2 != null) {
            zone.addGoldLocation(new Pair<>(tempGoldCorner1, tempGoldCorner2));
            tempGoldCorner1 = null;
            tempGoldCorner2 = null;
        }
    }

    private void displayGoldZone() {
        removeDisplays("goldZone");
        removeDisplays("goldZoneTemp");
        for (var loc : zone.getAllGoldLoc()) {
            blockHighlight = BlockDisplayHelper.createDisplayHighlight(loc.getFirst(), loc.getSecond(), Color.YELLOW, "goldZone", false);
        }
    }

    public void removeDisplays() {
        removeDisplays("zone");
        removeDisplays("goldZone");
        removeDisplays("goldZoneTemp");
    }

    public void removeDisplays(String name) {
        zone.getCorner1().getWorld()
                .getEntitiesByClass(BlockDisplay.class)
                .stream()
                .filter(entity -> Objects.equals(entity.getCustomName(), name))
                .forEach(Entity::remove);
    }

    public void addOffLimitBlock(Location loc) {
        zone.addOffLimits(loc);
    }
    public void remOffLimitBlock(Location loc) {
        zone.remOffLimits(loc);
    }
    public void setZoneCreationMode(Player player, String mapName) {
        // sets the inventory to set the zone
        zone.setMapName(mapName);
        edittingPlayers.add(player.getUniqueId());
        previousUserGamemode =  PlayerStateChanger.editingMode(player);
        setPlayerMenu(player, CurrentPlayerMenu.mainMenu);
    }

    public void saveZone(Player p) {
        try {
            SQLHandler.getInstance().storeZone(zone);
        } catch (IllegalArgumentException | IllegalStateException e) {
            p.sendMessage("Could not save map: " + e.getMessage());
            return;
        }
        unloadMap(zone);
        removeDisplays();
        PlayerStateChanger.normalMode(p, previousUserGamemode);
        edittingPlayers.remove(p.getUniqueId());
        playerMode.remove(p.getUniqueId());
        zone = new MurderMysteryZone();
        tempGoldCorner1 = null;
        tempGoldCorner2 = null;
        p.sendMessage("Map saved.");
    }

    // Sets the player mode in the map & sets the correct menu inventory
    private void setPlayerMenu(Player p, CurrentPlayerMenu newMode) {
        switch (newMode) {
            case CurrentPlayerMenu.mainMenu -> {
                PlayerStateChanger.PushMainMenu(p);
            }
            case CurrentPlayerMenu.goldZoneEdit -> {
                PlayerStateChanger.PushGoldAreaMenu(p);
            }
            case CurrentPlayerMenu.offLimitZoneEdit -> {
                PlayerStateChanger.PushLimitMaker(p);
            }
            case CurrentPlayerMenu.zoneEdit -> {
                PlayerStateChanger.PushSelectionMenu(p);
            }
        }
        playerMode.put(p.getUniqueId(), newMode);
    }

    public CurrentPlayerMenu getPlayerState(UUID uuid) {
        return playerMode.get(uuid);
    }

    public boolean isPlayerEditing(UUID uuid) {
        return edittingPlayers.contains(uuid);
    }

    public boolean loadMap(MurderMysteryZone map) {
        clearObstructionHighlights(map.getMapName());
        List<Location> obstructions = new ArrayList<>();
        for (Location location : map.getOffLimits()) {
            Block block = location.getBlock();
            if (!block.isEmpty() && block.getType() != Material.BARRIER) {
                obstructions.add(location);
            }
        }
        if (!obstructions.isEmpty()) {
            List<BlockDisplay> displays = new ArrayList<>();
            obstructedDisplays.put(map.getMapName(), displays);
            for (Location location : obstructions) {
                displays.addAll(BlockDisplayHelper.createDisplayHighlight(
                        location, location, Color.RED, "mapObstruction", false));
            }
            return false;
        }
        List<Block> placed = new ArrayList<>();
        try {
            for (Location location : map.getOffLimits()) {
                Block block = location.getBlock();
                if (block.isEmpty()) {
                    block.setType(Material.BARRIER, false);
                    placed.add(block);
                }
            }
        } catch (RuntimeException e) {
            for (Block block : placed) {
                try {
                    if (block.getType() == Material.BARRIER) block.setType(Material.AIR, false);
                } catch (RuntimeException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            throw e;
        }
        return true;
    }

    public void unloadMap(MurderMysteryZone map) {
        clearObstructionHighlights(map.getMapName());
        for (Location location : map.getOffLimits()) {
            Block block = location.getBlock();
            if (block.getType() == Material.BARRIER) block.setType(Material.AIR, false);
        }
    }

    public void clearObstructionHighlights() {
        obstructedDisplays.values().forEach(displays -> displays.forEach(Entity::remove));
        obstructedDisplays.clear();
    }

    private void clearObstructionHighlights(String mapName) {
        List<BlockDisplay> displays = obstructedDisplays.remove(mapName);
        if (displays != null) displays.forEach(Entity::remove);
    }
}