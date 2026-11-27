package dev.theycreeper.murdermystery.game;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.listeners.DetectiveArrowListener;
import dev.theycreeper.murdermystery.listeners.ItemCollectListener;
import dev.theycreeper.murdermystery.listeners.PlayerKillListener;
import dev.theycreeper.murdermystery.manager.GoldPeriodicSpawn;
import dev.theycreeper.murdermystery.manager.PlayerStateChanger;
import dev.theycreeper.murdermystery.model.MMTimer;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MurderMysteryGame implements Listener {
    private BukkitRunnable ticker;
    List<MMPlayer> playerList = new ArrayList<MMPlayer>();
    int playersLeft, murdererKills;
    MMTimer mmTimer;
    int minutesLeft, secondsLeft;
    private List<Listener> eventListeners = new ArrayList<Listener>();
    private final List<UUID> corpses = new ArrayList<>();
    private final Set<UUID> preparedPlayers = new HashSet<>();
    private GoldPeriodicSpawn goldSpawn;
    private int currentGoldAmount;
    private MurderMysteryZone zone;
    private ItemDisplay droppedBow;
    private final DetectiveArrowListener detectiveArrowListener = new DetectiveArrowListener(this);

    public MurderMysteryGame(MurderMysteryZone zone) {
        eventListeners.add(new ItemCollectListener());
        eventListeners.add(new PlayerKillListener(this));
        eventListeners.add(detectiveArrowListener);
        this.zone = zone;
        goldSpawn = new GoldPeriodicSpawn();
    }

    /* Get/Set methods and general utils stuff. Not actual game logic */
    // Queued players
    public void addPlayer(Player player) {
        playerList.add(new MMPlayer(player));
    }

    public void setMurderer(Player p) {
        MMPlayer mmPlayer = getMmPlayer(p);
        preparePlayer(p);
        mmPlayer.setPlayerType(PlayerType.murderer);
        PlayerStateChanger.pushInventoryPlayerMurderer(p);
        p.sendTitle(
                ChatColor.RED + "Murderer!",   // Main title
                ChatColor.GRAY + "Kill all innocents", // Subtitle (or "" / null)
                10,                               // Fade in (ticks)
                70,                               // Stay on screen (ticks)
                20                                // Fade out (ticks)
        );
    }

    public void setDetective(Player p, boolean shouldPrepare) {
        MMPlayer mmPlayer = getMmPlayer(p);
        if (shouldPrepare) {
            preparePlayer(p);
        }
        mmPlayer.setPlayerType(PlayerType.detective);
        PlayerStateChanger.pushInventoryPlayerDetective(p);
        p.sendTitle(
                ChatColor.AQUA + "Detective!",   // Main title
                ChatColor.GRAY + "Find and shoot the murderer", // Subtitle (or "" / null)
                10,                               // Fade in (ticks)
                70,                               // Stay on screen (ticks)
                20                                // Fade out (ticks)
        );
    }

    public void setDetective(Player p) {
        setDetective(p, true);
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

    public PlayerType getPlayerType(Player p) {
        return getMmPlayer(p).getPlayerType();
    }
    public PlayerState getPlayerState(Player p) {
        return getMmPlayer(p).getPlayerState();
    }

    public List<MMPlayer> getPlayerList() {
        return playerList;
    }

    public int getPlayerCount() {
        return playerList.size();
    }

    public void createDetectiveBow(Location loc) {
        droppedBow = spawnDetectiveBow(loc);
    }

    public void removeDetectiveBow() {
        droppedBow.remove();
        droppedBow = null;
    }

    public void giveBowIfLocValid(Player p) {
        if (droppedBow == null || !droppedBow.isValid()) return;
        if (getPlayerType(p) == PlayerType.murderer) return;
        if (getPlayerState(p) == PlayerState.dead) return;

        Location bowLoc = droppedBow.getLocation();

        if (Math.abs(p.getLocation().getY() - bowLoc.getY()) <= 5) {
            setDetective(p, false);
            removeDetectiveBow();
        }
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
        mmTimer = new MMTimer(4, 30);
        startTicker();
    }

    public void createCorpse(Player player) {
        MMPlayer mmPlayer = getMmPlayer(player);
        if (mmPlayer.getPlayerType() == PlayerType.detective) createDetectiveBow(player.getLocation());
        corpses.add(MurderMystery.getInstance().getCorpseManager().spawn(player).getUuid());
    }

    public void EndGame() {
        detectiveArrowListener.cancelAll();
        RuntimeException failure = null;
        for (MMPlayer mmPlayer : playerList) {
            Player player = mmPlayer.getPlayer();
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
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
        playersLeft = 0;
        stopTicker();
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

    public void startTicker() {
        stopTicker();

        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                // Tick the timer
                mmTimer.tick();
                for (MMPlayer mmPlayer : playerList) {
                    setScoreboard(mmPlayer, playersLeft, mmTimer.toString(), "Alive", zone.getMapName());
                }
            }
        };

        // Run immediately, then every 100 ticks (5 seconds)
        ticker.runTaskTimer(MurderMystery.getInstance(), 0L, 20L);
    }

    public void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    public static void setScoreboard(MMPlayer p, int playerLeft, String timeLeft, String detectiveState, String mapName) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective(
                "serverInfo",
                "dummy",
                ChatColor.GOLD + "Murder mystery game"
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score roleLine = objective.getScore(ChatColor.YELLOW + "Role:  " + ChatColor.GREEN + p.getPlayerType().toString());
        roleLine.setScore(2);

        Score innocentsLeft = objective.getScore(ChatColor.YELLOW + "Innocents left: " + ChatColor.GREEN + playerLeft);
        innocentsLeft.setScore(1);
        Score timeLeftLine = objective.getScore(ChatColor.YELLOW + "Time left: " + ChatColor.GREEN + timeLeft);
        timeLeftLine.setScore(1);
        Score detectiveLine = objective.getScore(ChatColor.YELLOW + "Detective: " + ChatColor.GREEN + detectiveState);
        detectiveLine.setScore(1);
        Score mapLine = objective.getScore(ChatColor.YELLOW + "Map: " + ChatColor.GREEN + mapName);
        detectiveLine.setScore(1);

        p.getPlayer().setScoreboard(board);
    }

    public ItemDisplay spawnDetectiveBow(Location corpseLocation) {
        Location bowLocation = corpseLocation.clone().add(0, 1, 0);

        return bowLocation.getWorld().spawn(
                bowLocation,
                ItemDisplay.class,
                bow -> {
                    bow.setItemStack(new ItemStack(Material.BOW));

                    animateBow(bow);
                    bow.setGravity(false);
                    bow.setInvulnerable(true);
                    bow.setPersistent(false);

                    // Keep its orientation fixed
                    bow.setBillboard(Display.Billboard.FIXED);
                }
        );
    }

    public void animateBow(ItemDisplay bow) {
        Bukkit.getScheduler().runTaskTimer(
                MurderMystery.getInstance(),
                task -> {
                    if (!bow.isValid()) {
                        task.cancel();
                        return;
                    }

                    bow.setTransformation(
                            new Transformation(
                                    new Vector3f(0, 0, 0),
                                    new AxisAngle4f(
                                            (float) Math.toRadians(
                                                    (System.currentTimeMillis() / 20) % 360
                                            ),
                                            0, 1, 0
                                    ),
                                    new Vector3f(1f, 1f, 1f),
                                    new AxisAngle4f()
                            )
                    );
                },
                0L,
                1L
        );
    }
}
