package com.shanebeestudios.stress;

import com.shanebeestudios.stress.api.bot.BotManager;
import com.shanebeestudios.stress.api.util.Logger;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.exceptions.UnsupportedVersionException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Main plugin class
 */
@SuppressWarnings("unused")
public class StressTestBots extends JavaPlugin {

    private static StressTestBots instance;
    private boolean commandApiCanLoad;
    private BotManager botManager;

    /**
     * @hidden
     */
    @Override
    public void onLoad() {
        if (Bukkit.getOnlineMode()) {
            // Don't load CommandAPI
            return;
        }
        try {
            CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .verboseOutput(false).silentLogs(true).skipReloadDatapacks(true));
            this.commandApiCanLoad = true;
        } catch (UnsupportedVersionException ignore) {
            this.commandApiCanLoad = false;
        }
    }

    /**
     * @hidden
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (Bukkit.getOnlineMode()) {
            Logger.error("***********************************************");
            Logger.error("*                                             *");
            Logger.error("* This plugin will only work in offline mode! *");
            Logger.error("*                                             *");
            Logger.error("***********************************************");
            pluginManager.disablePlugin(this);
            return;
        }

        instance = this;

        if (!this.commandApiCanLoad) {
            Logger.error("CommandAPI could not be loaded, plugin disabling!");
            pluginManager.disablePlugin(this);
            return;
        }
        loadNicknameFile();
        setupBotLogic();
        setupCommand();

        // Beta check + notice
        String version = getDescription().getVersion();
        if (version.contains("-")) {
            Logger.warn("This is a BETA build, things may not work as expected, please report any bugs on GitHub.");
            Logger.warn("https://github.com/ShaneBeee/StressTestBots/issues");
        }

        long finish = System.currentTimeMillis() - start;
        Logger.info("&aSuccessfully enabled v%s&7 in &b%s ms", version, finish);
    }

    /**
     * @hidden
     */
    @Override
    public void onDisable() {
        if (!Bukkit.getOnlineMode()) CommandAPI.onDisable();
        instance = null;
    }

    private void loadNicknameFile() {
        if (!new File(getDataFolder(), "nicks.txt").exists()) {
            this.saveResource("nicks.txt", false);
        }
    }

    private void setupBotLogic() {
        this.botManager = new BotManager();
    }

    private void setupCommand() {
        CommandAPI.onEnable();
        new Command(this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    /**
     * Get an instance of this plugin
     *
     * @return Instance of this plugin
     */
    // Getters
    public static StressTestBots getInstance() {
        return instance;
    }

    /**
     * Get an instance of the {@link BotManager}
     *
     * @return Instance of bot manager
     */
    public BotManager getBotManager() {
        return this.botManager;
    }

}
