package dev.theycreeper.murdermystery.manager;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.game.MurderMysteryZone;
import dev.theycreeper.murdermystery.game.PlayerState;
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
import org.checkerframework.checker.units.qual.Current;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class ZoneCreationManager {
    private MurderMysteryZone zone = new MurderMysteryZone();
    private ArrayList<UUID> edittingPlayers = new ArrayList<>();
    private List<BlockDisplay> blockHighlight;
    private Map<UUID, CurrentPlayerMenu> playerMode = new HashMap<>();

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
                // Ig confirm some stuff or modify the outline of the zone
            }
            case Material.RED_WOOL -> saveZone(e.getPlayer());
        }
    }

    public void handleAreaSelectionMenuClick(PlayerInteractEvent e) {
        switch (e.getItem().getType()) {
            case Material.STICK -> {
                if (e.getClickedBlock() == null && e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    changeCorner(e.getPlayer().getLocation(), e.getAction());
                } else {
                    changeCorner(e.getClickedBlock().getLocation(), e.getAction());
                }
            }
            case Material.GREEN_WOOL -> {
                // Ig confirm some stuff or modify the outline of the zone
            }
            case Material.RED_WOOL -> setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.mainMenu);
        }
    }

    public void handleGoldAreaClick(PlayerInteractEvent e) {
        switch (e.getItem().getType()) {
            case Material.STICK -> {
                if (e.getClickedBlock() == null && e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    changeCorner(e.getPlayer().getLocation(), e.getAction());
                } else {
                    changeCorner(e.getClickedBlock().getLocation(), e.getAction());
                }
            }
            case Material.GREEN_WOOL -> {
                // Ig confirm some stuff or modify the outline of the zone
            }
            case Material.RED_WOOL -> setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.mainMenu);
        }
    }

    public void handleBarrierClick(PlayerInteractEvent e) {
        switch (e.getItem().getType()) {
            case Material.STICK -> {
                if (e.getClickedBlock() == null && e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    changeCorner(e.getPlayer().getLocation(), e.getAction());
                } else {
                    changeCorner(e.getClickedBlock().getLocation(), e.getAction());
                }
            }
            case Material.GREEN_WOOL -> {
                // Ig confirm some stuff or modify the outline of the zone
            }
            case Material.RED_WOOL -> setPlayerMenu(e.getPlayer(), CurrentPlayerMenu.mainMenu);
        }
    }

    private void changeCorner(Location location, Action action) {
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            zone.setCorner1(location);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            zone.setCorner2(location);
        }
        zone.getCorner1().getWorld().getEntitiesByClass(BlockDisplay.class).forEach(Entity::remove);
        blockHighlight = BlockDisplayHelper.createDisplayHighlight(zone.getCorner1(), zone.getCorner2(), Color.AQUA);
    }

    public void setZoneCreationMode(Player player, String mapName) {
        // sets the inventory to set the zone
        zone.setMapName(mapName);
        edittingPlayers.add(player.getUniqueId());
        PlayerStateChanger.editingMode(player);
        setPlayerMenu(player, CurrentPlayerMenu.mainMenu);
    }

    public void saveZone(Player p) {
        PlayerStateChanger.normalMode(p);
        // do something here
//        blockHighlight = BlockDisplayHelper.createDisplayHighlight(zone.getCorner1(), zone.getCorner2(), Color.AQUA);
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

    public boolean isPlayerEditing(UUID uuid) {
        return edittingPlayers.contains(uuid);
    }
}
