package dev.theycreeper.murdermystery;

import dev.theycreeper.murdermystery.commands.StartMurderMystery;
import dev.theycreeper.murdermystery.configs.ConfigHandler;
import dev.theycreeper.murdermystery.listeners.ItemCollectListener;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class MurderMystery extends JavaPlugin implements Listener {
    private static MurderMystery instance;
    private static ItemCollectListener itemCollectListener;
    public static MurderMystery getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        itemCollectListener = new ItemCollectListener();
        getServer().getPluginManager().registerEvents(itemCollectListener, this);
        saveDefaultConfig();
        registerCommands();
        handleConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MurderMystery enabled!");
    }

    private void registerCommands() {
        Objects.requireNonNull(this.getCommand("startmm")).setExecutor(new StartMurderMystery());
    }

    private void handleConfig(){
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        ConfigHandler.getInstance().loadConfigValues();
    }
}
