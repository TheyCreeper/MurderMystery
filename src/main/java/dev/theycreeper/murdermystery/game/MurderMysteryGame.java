package dev.theycreeper.murdermystery.game;

import dev.theycreeper.murdermystery.MurderMystery;
import dev.theycreeper.murdermystery.listeners.ItemCollectListener;
import dev.theycreeper.murdermystery.listeners.PlayerKillListener;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

public class MurderMysteryGame {
    List<MMPlayer> playerList = new ArrayList<MMPlayer>();
    int playersLeft, murdererKills;
    Timer timer = new Timer();
    private List<Listener> eventListeners = new ArrayList<Listener>();
    private final List<UUID> corpses = new ArrayList<>();

    public MurderMysteryGame() {
        eventListeners.add(new ItemCollectListener());
        eventListeners.add(new PlayerKillListener(this));
    }

    /* Get/Set methods and general utils stuff. Not actual game logic */
    // Queued players
    public void addPlayer(Player player) {
        playerList.add(new MMPlayer(player));
    }

    public void setMurderer(Player player) {
        MMPlayer mmPlayer = getMmPlayer(player);
        mmPlayer.setPlayerType(PlayerType.murderer);
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


    public void StartGame() {
        var pluginManager = MurderMystery.getInstance().getServer().getPluginManager();
        eventListeners.forEach(listener -> pluginManager.registerEvents(listener, MurderMystery.getInstance()));
    }

    public void createCorpse(Player player) {
        corpses.add(MurderMystery.getInstance().getCorpseManager().spawn(player).getUuid());
    }

    public void EndGame() {
        eventListeners.forEach(HandlerList::unregisterAll);
        corpses.forEach(MurderMystery.getInstance().getCorpseManager()::remove);
        corpses.clear();
        playerList.clear();
        timer.cancel();
        playersLeft = 0;
        murdererKills = 0;

        for (MMPlayer mmPlayer : playerList) {
            revivePlayer(mmPlayer.getPlayer());
        }
    }

    public void revivePlayer(Player player) {

        player.setInvisible(false);
        player.setFlying(false);
        player.setAllowFlight(false);

        player.setCollidable(true);
        player.setCollidable(true);

        player.showPlayer(MurderMystery.getInstance(), player);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }
}
