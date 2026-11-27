package dev.theycreeper.murdermystery;

import dev.theycreeper.murdermystery.commands.CreateNewMMZone;
import dev.theycreeper.murdermystery.commands.PartyCommand;
import dev.theycreeper.murdermystery.commands.StartMurderMystery;
import dev.theycreeper.murdermystery.commands.StopMurderMystery;
import dev.theycreeper.murdermystery.configs.ConfigHandler;
import dev.theycreeper.murdermystery.listeners.BlockPlaceListener;
import dev.theycreeper.murdermystery.listeners.ItemUsedListener;
import dev.theycreeper.murdermystery.listeners.LocationMoveListener;
import dev.theycreeper.murdermystery.manager.CorpseManager;
import dev.theycreeper.murdermystery.manager.MMGameManager;
import dev.theycreeper.murdermystery.manager.PartyManager;
import dev.theycreeper.murdermystery.manager.ZoneCreationManager;
import dev.theycreeper.murdermystery.saving.SQLHandler;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class MurderMystery extends JavaPlugin implements Listener {
    private static MurderMystery instance;
    private MMGameManager gameManager;
    private PartyManager partyManager;
    private CorpseManager corpseManager;
    private ZoneCreationManager zoneCreationManager;
    private ItemUsedListener itemUsedListener;
    private BlockPlaceListener blockPlaceListener;
    private LocationMoveListener locationMoveListener;
    public static MurderMystery getInstance() {
        return instance;
    }

    public MMGameManager getGameManager() {
        return gameManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public CorpseManager getCorpseManager() {
        return corpseManager;
    }
    public ZoneCreationManager getZoneCreationManager() { return zoneCreationManager; }

    @Override
    public void onEnable() {
        instance = this;
        itemUsedListener = new ItemUsedListener(zoneCreationManager);
        blockPlaceListener = new BlockPlaceListener();
        gameManager = new MMGameManager();
        partyManager = new PartyManager();
        corpseManager = new CorpseManager(this);
        zoneCreationManager = new ZoneCreationManager();
        locationMoveListener = new LocationMoveListener();
        getServer().getPluginManager().registerEvents(itemUsedListener, this);
        getServer().getPluginManager().registerEvents(blockPlaceListener, this);
        getServer().getPluginManager().registerEvents(partyManager, this);
        getServer().getPluginManager().registerEvents(locationMoveListener, this);
        saveDefaultConfig();
        registerCommands();
        handleConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MurderMystery enabled!");
        SQLHandler.getInstance().init();
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.hasActiveGame()) gameManager.StopGame();
        if (zoneCreationManager != null) zoneCreationManager.clearObstructionHighlights();
        corpseManager.removeAll();
    }


    private void registerCommands() {
        Objects.requireNonNull(this.getCommand("mmstart")).setExecutor(new StartMurderMystery());
        Objects.requireNonNull(this.getCommand("mmstop")).setExecutor(new StopMurderMystery());
        Objects.requireNonNull(this.getCommand("mmnew")).setExecutor(new CreateNewMMZone());
        Objects.requireNonNull(this.getCommand("party")).setExecutor(new PartyCommand(partyManager));
    }

    private void handleConfig() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        ConfigHandler.getInstance().loadConfigValues();
    }
}
