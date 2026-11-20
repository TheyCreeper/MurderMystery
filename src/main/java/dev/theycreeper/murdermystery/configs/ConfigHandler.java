package dev.theycreeper.murdermystery.configs;

import dev.theycreeper.murdermystery.MurderMystery;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigHandler {

    private static ConfigHandler instance;

    private int minGoldForBow;

    public static ConfigHandler getInstance() {
        if (instance == null) instance = new ConfigHandler();
        return instance;
    }

    public void loadConfigValues() {
        MurderMystery.getInstance().reloadConfig();
        FileConfiguration config = MurderMystery.getInstance().getConfig();
        List<String> blockList = config.getStringList("blacklisted-blocks");
        minGoldForBow = config.getInt("min-gold-for-bow", 10);
    }

    public int getMinGoldForBow() {
        return minGoldForBow;
    }
}