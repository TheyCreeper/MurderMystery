package dev.theycreeper.murdermystery.game;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.listeners.ItemCollectListener;
import dev.theycreeper.murdermystery.listeners.PlayerKillListener;
import dev.theycreeper.murdermystery.manager.GoldPeriodicSpawn;
import dev.theycreeper.murdermystery.manager.PlayerStateChanger;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;

public class MurderMysteryGame {
    List<MMPlayer> playerList = new ArrayList<MMPlayer>();
    int playersLeft, murdererKills;
    Timer timer = new Timer();
    private List<Listener> eventListeners = new ArrayList<Listener>();
    private final List<UUID> corpses = new ArrayList<>();
    private final Set<UUID> preparedPlayers = new HashSet<>();
    private GoldPeriodicSpawn goldSpawn;
    private int currentGoldAmount;
    private MurderMysteryZone zone;

    public MurderMysteryGame(MurderMysteryZone zone) {
        eventListeners.add(new ItemCollectListener());
        eventListeners.add(new PlayerKillListener(this));
        this.zone = zone;
        goldSpawn = new GoldPeriodicSpawn();
    }

    /* Get/Set methods and general utils stuff. Not actual game logic */
    // Queued players
    public void addPlayer(Player player) {
        playerList.add(new MMPlayer(player));
    }

    public void setMurderer(Player player) {
        MMPlayer mmPlayer = getMmPlayer(player);
        preparePlayer(player);
        mmPlayer.setPlayerType(PlayerType.murderer);
        PlayerStateChanger.pushInventoryPlayerMurderer(player);
        player.sendTitle(
                ChatColor.RED + "Murderer!",   // Main title
                ChatColor.GRAY + "Kill all innocents", // Subtitle (or "" / null)
                10,                               // Fade in (ticks)
                70,                               // Stay on screen (ticks)
                20                                // Fade out (ticks)
        );
    }

    public void setDetective(Player player) {
        MMPlayer mmPlayer = getMmPlayer(player);
        preparePlayer(player);
        mmPlayer.setPlayerType(PlayerType.detective);
        PlayerStateChanger.pushInventoryPlayerDetective(player);
        player.sendTitle(
                ChatColor.AQUA + "Detective!",   // Main title
                ChatColor.GRAY + "Find and shoot the murderer", // Subtitle (or "" / null)
                10,                               // Fade in (ticks)
                70,                               // Stay on screen (ticks)
                20                                // Fade out (ticks)
        );
    }

    public void setInnocents(ArrayList<Player> p) {
        for (Player player : p) {
            MMPlayer mmPlayer = getMmPlayer(player);
            preparePlayer(player);
            mmPlayer.setPlayerType(PlayerType.innocent);
            PlayerStateChanger.pushInventoryInnocent(player);
            player.sendTitle(
                    ChatColor.AQUA + "Innocent!",   // Main title
                    ChatColor.GRAY + "Don't get killed by the murderer!", // Subtitle (or "" / null)
                    10,                               // Fade in (ticks)
                    70,                               // Stay on screen (ticks)
                    20                                // Fade out (ticks)
            );
        }
    }

    private void preparePlayer(Player player) {
        if (preparedPlayers.contains(player.getUniqueId())) {
            throw new IllegalStateException("Player has already been prepared for the game");
        }
        SQLHandler.getInstance().storeInventory(player);
        preparedPlayers.add(player.getUniqueId());
    }

    public void removeQueuePlayer(Player player) {
        playerList.remove(getMmPlayer(player));
    }

    public MMPlayer getMmPlayer(Player player) {
        return playerList.stream().filter(m -> m.getPlayer().equals(player)).findFirst().orElse(null);
    }

    public List<MMPlayer> getPlayerList() {
        return playerList;
    }

    public int getPlayerCount() {
        return playerList.size();
    }

    // utils
    public boolean isPlayerCanKill(Player p) {
        MMPlayer player = getMmPlayer(p);
        if (player == null) return false;
        PlayerType pt = player.getPlayerType();
        return pt == PlayerType.murderer ||
                pt == PlayerType.assassin ||
                pt == PlayerType.alpha ||
                pt == PlayerType.infected;
    }
    public boolean isPlayerCanShoot(Player p) {
        MMPlayer player = getMmPlayer(p);
        if (player == null) return false;
        PlayerType pt = player.getPlayerType();
        return pt == PlayerType.innocent ||
                pt == PlayerType.detective ||
                pt == PlayerType.survivor;
    }


    public void StartGame() {
        var pluginManager = MurderMystery.getInstance().getServer().getPluginManager();
        eventListeners.forEach(listener -> pluginManager.registerEvents(listener, MurderMystery.getInstance()));
        goldSpawn.startGoldSpawning(zone);
    }

    public void createCorpse(Player player) {
        corpses.add(MurderMystery.getInstance().getCorpseManager().spawn(player).getUuid());
    }

    public void EndGame() {
        RuntimeException failure = null;
        for (MMPlayer mmPlayer : playerList) {
            Player player = mmPlayer.getPlayer();
            if (!preparedPlayers.contains(player.getUniqueId())) continue;
            try {
                PlayerStateChanger.normalMode(player,
                        SQLHandler.getInstance().getPlayerGamemode(player.getUniqueId()));
                revivePlayer(player);
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        eventListeners.forEach(HandlerList::unregisterAll);
        corpses.forEach(MurderMystery.getInstance().getCorpseManager()::remove);
        corpses.clear();
        playerList.clear();
        preparedPlayers.clear();
        timer.cancel();
        playersLeft = 0;
        murdererKills = 0;
        goldSpawn.stopSpawner();
        if (failure != null) throw failure;
    }

    public void revivePlayer(Player player) {
        player.setFlying(false);
        player.setAllowFlight(false);

        player.setCollidable(true);

        player.showPlayer(MurderMystery.getInstance(), player);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(MurderMystery.getInstance(), player);
        }
    }

//    public int getCurrentGoldAmount() {
//        return currentGoldAmount;
//    }
//    public void setCurrentGoldAmount(int currentGoldAmount) {
//        this.currentGoldAmount = currentGoldAmount;
//    }
}
