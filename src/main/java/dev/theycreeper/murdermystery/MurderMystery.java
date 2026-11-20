package dev.theycreeper.murdermystery;

import dev.theycreeper.murdermystery.commands.StartMurderMystery;
import dev.theycreeper.murdermystery.commands.StopMurderMystery;
import dev.theycreeper.murdermystery.configs.ConfigHandler;
import dev.theycreeper.murdermystery.corpse.CorpseManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class MurderMystery extends JavaPlugin implements Listener {
    private static MurderMystery instance;
    private MMGameManager gameManager;
    private CorpseManager corpseManager;

    public static MurderMystery getInstance() {
        return instance;
    }
    public MMGameManager getGameManager() {
        return gameManager;
    }
    public CorpseManager getCorpseManager() {
        return corpseManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        gameManager = new MMGameManager();
        corpseManager = new CorpseManager(this);
        saveDefaultConfig();
        registerCommands();
        handleConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MurderMystery enabled!");
    }

    @Override
    public void onDisable() {
        corpseManager.removeAll();
    }


    private void registerCommands() {
        Objects.requireNonNull(this.getCommand("mmstart")).setExecutor(new StartMurderMystery());
        Objects.requireNonNull(this.getCommand("mmstop")).setExecutor(new StopMurderMystery());
    }

    private void handleConfig(){
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        ConfigHandler.getInstance().loadConfigValues();
    }
}
